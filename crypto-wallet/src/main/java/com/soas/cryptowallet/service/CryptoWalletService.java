package com.soas.cryptowallet.service;

import com.soas.cryptowallet.entity.CryptoWallet;
import com.soas.cryptowallet.repository.CryptoWalletRepository;
import com.soas.library.dto.CryptoWalletDto;
import com.soas.library.dto.Role;
import com.soas.library.dto.UserDto;
import com.soas.library.proxy.UsersServiceProxy;
import com.soas.library.security.AuthContext;
import com.soas.util.exception.DuplicateResourceException;
import com.soas.util.exception.InsufficientFundsException;
import com.soas.util.exception.InvalidRequestException;
import com.soas.util.exception.ResourceNotFoundException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Poslovna logika nad kripto novcanicima.
 *
 * Autorizacija:
 *  - OWNER nije autorizovan za koriscenje ovog servisa
 *  - ADMIN moze da dodaje, azurira i pregleda sve novcanike
 *  - USER moze da pregleda samo svoj novcanik
 */
@Service
public class CryptoWalletService {

    private static final Logger log = LoggerFactory.getLogger(CryptoWalletService.class);

    /** Kolicine kripto valuta se cuvaju sa osam decimala. */
    private static final int SCALE = 8;

    /** Kripto valuta koju svaki novi korisnik dobija sa stanjem 0. */
    private static final String DEFAULT_CRYPTO = "ETH";

    private final CryptoWalletRepository repository;
    private final UsersServiceProxy usersProxy;
    private final AuthContext auth;

    public CryptoWalletService(CryptoWalletRepository repository,
                              UsersServiceProxy usersProxy,
                              AuthContext auth) {
        this.repository = repository;
        this.usersProxy = usersProxy;
        this.auth = auth;
    }

    // ------------------------------------------------------------------
    // Javne operacije (kroz API-Gateway)
    // ------------------------------------------------------------------

    /** Pregled svih novcanika - dozvoljeno samo ADMIN-u. */
    @Transactional(readOnly = true)
    public List<CryptoWalletDto> findAll() {
        auth.requireAnyOf(Role.ADMIN);
        return repository.findAll().stream().map(this::toDto).toList();
    }

    /** Pregled novcanika jednog korisnika - ADMIN bilo koji, USER samo svoj. */
    @Transactional(readOnly = true)
    public List<CryptoWalletDto> findByEmail(String email) {
        auth.requireAnyOf(Role.ADMIN, Role.USER);
        auth.requireOwnDataIfUser(email);
        return loadWallet(email);
    }

    /** Novcanik trenutno prijavljenog korisnika. */
    @Transactional(readOnly = true)
    public List<CryptoWalletDto> findMyWallet() {
        auth.requireAnyOf(Role.ADMIN, Role.USER);
        return loadWallet(auth.currentEmail());
    }

    @Transactional
    public CryptoWalletDto create(CryptoWalletDto dto) {
        auth.requireAnyOf(Role.ADMIN);

        String email = normalizeEmail(dto.getEmail());
        String code = normalizeCrypto(dto.getCryptoCode());
        requireUserRole(email);

        repository.findByEmailIgnoreCaseAndCryptoCodeIgnoreCase(email, code).ifPresent(existing -> {
            throw new DuplicateResourceException(
                    "Korisnik " + email + " vec ima stavku novcanika za kripto valutu " + code + ".");
        });

        CryptoWallet saved = repository.save(
                new CryptoWallet(email, code, scaled(requireNonNegative(dto.getAmount()))));
        return toDto(saved);
    }

    @Transactional
    public CryptoWalletDto update(Long id, CryptoWalletDto dto) {
        auth.requireAnyOf(Role.ADMIN);

        CryptoWallet wallet = getOrThrow(id);
        if (dto.getCryptoCode() != null) {
            wallet.setCryptoCode(normalizeCrypto(dto.getCryptoCode()));
        }
        if (dto.getAmount() != null) {
            wallet.setAmount(scaled(requireNonNegative(dto.getAmount())));
        }
        return toDto(repository.save(wallet));
    }

    @Transactional
    public void delete(Long id) {
        auth.requireAnyOf(Role.ADMIN);
        repository.delete(getOrThrow(id));
    }

    // ------------------------------------------------------------------
    // Interne operacije (pozivaju ih drugi mikroservisi preko Feign-a)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CryptoWalletDto> findByEmailInternal(String email) {
        return loadWallet(normalizeEmail(email));
    }

    /**
     * Kreira podrazumevani novcanik za novog korisnika sa ulogom USER:
     * stanje 0 za kripto valutu EUR.
     */
    @Transactional
    public List<CryptoWalletDto> createDefaultWallet(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (repository.existsByEmailIgnoreCase(email)) {
            log.info("Novcanik za {} vec postoji - preskacem kreiranje", email);
            return loadWallet(email);
        }
        repository.save(new CryptoWallet(email, DEFAULT_CRYPTO, scaled(BigDecimal.ZERO)));
        log.info("Kreiran podrazumevani novcanik za {} ({} 0)", email, DEFAULT_CRYPTO);
        return loadWallet(email);
    }

    /** Brise kompletan novcanik korisnika (poziva se pri brisanju korisnika). */
    @Transactional
    public void deleteByEmail(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        repository.deleteByEmailIgnoreCase(email);
        log.info("Obrisan novcanik korisnika {}", email);
    }

    /** Prati promenu email adrese korisnika u users-service-u. */
    @Transactional
    public void changeEmail(String rawOldEmail, String rawNewEmail) {
        String oldEmail = normalizeEmail(rawOldEmail);
        String newEmail = normalizeEmail(rawNewEmail);
        List<CryptoWallet> wallets = repository.findByEmailIgnoreCaseOrderByCryptoCodeAsc(oldEmail);
        wallets.forEach(wallet -> wallet.setEmail(newEmail));
        repository.saveAll(wallets);
        log.info("Novcanik prebacen sa {} na {}", oldEmail, newEmail);
    }

    /** Skida kripto valutu sa novcanika. Baca gresku ako nema dovoljno sredstava. */
    @Transactional
    public List<CryptoWalletDto> debit(String rawEmail, String rawCryptoCode, BigDecimal amount) {
        String email = normalizeEmail(rawEmail);
        String code = normalizeCrypto(rawCryptoCode);
        BigDecimal value = scaled(requirePositive(amount));

        CryptoWallet wallet = repository.findByEmailIgnoreCaseAndCryptoCodeIgnoreCase(email, code)
                .orElseThrow(() -> new InsufficientFundsException(
                        "Na novcaniku ne postoje sredstva u kripto valuti " + code + "."));

        if (wallet.getAmount().compareTo(value) < 0) {
            throw new InsufficientFundsException(
                    "Nedovoljno sredstava: na novcaniku je dostupno " + wallet.getAmount() + " " + code
                            + ", a trazeno je " + value + " " + code + ".");
        }

        wallet.setAmount(scaled(wallet.getAmount().subtract(value)));
        repository.save(wallet);
        return loadWallet(email);
    }

    /** Dodaje kripto valutu na novcanik; ako stavka za kripto valutu ne postoji, kreira je. */
    @Transactional
    public List<CryptoWalletDto> credit(String rawEmail, String rawCryptoCode, BigDecimal amount) {
        String email = normalizeEmail(rawEmail);
        String code = normalizeCrypto(rawCryptoCode);
        BigDecimal value = scaled(requirePositive(amount));

        CryptoWallet wallet = repository.findByEmailIgnoreCaseAndCryptoCodeIgnoreCase(email, code)
                .orElseGet(() -> new CryptoWallet(email, code, BigDecimal.ZERO));

        wallet.setAmount(scaled(wallet.getAmount().add(value)));
        repository.save(wallet);
        return loadWallet(email);
    }

    // ------------------------------------------------------------------

    private List<CryptoWalletDto> loadWallet(String email) {
        List<CryptoWallet> wallets = repository.findByEmailIgnoreCaseOrderByCryptoCodeAsc(email);
        if (wallets.isEmpty()) {
            throw new ResourceNotFoundException("Novcanik za korisnika " + email + " ne postoji.");
        }
        return wallets.stream().map(this::toDto).toList();
    }

    /**
     * Novcaniki su dozvoljeni samo za korisnike sa ulogom USER, a email
     * mora da se poklapa sa email-om korisnika iz users-service-a.
     */
    private void requireUserRole(String email) {
        UserDto user;
        try {
            user = usersProxy.findByEmail(email);
        } catch (FeignException.NotFound ex) {
            throw new InvalidRequestException(
                    "Korisnik sa email adresom " + email + " ne postoji u users-service-u.");
        }
        if (user.getRole() != Role.USER) {
            throw new InvalidRequestException(
                    "Novcanik je dozvoljen samo korisnicima sa ulogom USER, a " + email
                            + " ima ulogu " + user.getRole() + ".");
        }
    }

    private CryptoWallet getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stavka novcanika sa id vrednoscu " + id + " ne postoji."));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidRequestException("Email adresa je obavezna.");
        }
        return email.trim().toLowerCase();
    }

    private String normalizeCrypto(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidRequestException("Kod kripto valute je obavezan.");
        }
        String normalized = code.trim().toUpperCase();
        if (!normalized.matches("[A-Z0-9]{2,5}")) {
            throw new InvalidRequestException(
                    "Kod kripto valute mora imati 2 do 5 slova ili cifara (npr. BTC, ETH, USDT), a prosledjeno je: " + code);
        }
        return normalized;
    }

    private BigDecimal requireNonNegative(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidRequestException("Kolicina je obavezna.");
        }
        if (amount.signum() < 0) {
            throw new InvalidRequestException("Kolicina ne moze biti negativna.");
        }
        return amount;
    }

    private BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidRequestException("Kolicina za razmenu mora biti veca od nule.");
        }
        return amount;
    }

    private BigDecimal scaled(BigDecimal amount) {
        return amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private CryptoWalletDto toDto(CryptoWallet wallet) {
        return new CryptoWalletDto(wallet.getId(), wallet.getEmail(),
                wallet.getCryptoCode(), wallet.getAmount());
    }
}
