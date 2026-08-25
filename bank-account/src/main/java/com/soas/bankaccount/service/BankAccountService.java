package com.soas.bankaccount.service;

import com.soas.bankaccount.entity.BankAccount;
import com.soas.bankaccount.repository.BankAccountRepository;
import com.soas.library.dto.BankAccountDto;
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

@Service
public class BankAccountService {
    private static final Logger log = LoggerFactory.getLogger(BankAccountService.class);

    private static final int SCALE = 2;

    private static final String DEFAULT_CURRENCY = "EUR";

    private final BankAccountRepository repository;
    private final UsersServiceProxy usersProxy;
    private final AuthContext auth;

    public BankAccountService(BankAccountRepository repository,
                              UsersServiceProxy usersProxy,
                              AuthContext auth) {
        this.repository = repository;
        this.usersProxy = usersProxy;
        this.auth = auth;
    }

    @Transactional(readOnly = true)
    public List<BankAccountDto> findAll() {
        auth.requireAnyOf(Role.ADMIN);
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<BankAccountDto> findByEmail(String email) {
        auth.requireAnyOf(Role.ADMIN, Role.USER);
        auth.requireOwnDataIfUser(email);
        return loadAccount(email);
    }

    @Transactional(readOnly = true)
    public List<BankAccountDto> findMyAccount() {
        auth.requireAnyOf(Role.ADMIN, Role.USER);
        return loadAccount(auth.currentEmail());
    }

    @Transactional
    public BankAccountDto create(BankAccountDto dto) {
        auth.requireAnyOf(Role.ADMIN);

        String email = normalizeEmail(dto.getEmail());
        String currency = normalizeCurrency(dto.getCurrencyCode());
        requireUserRole(email);

        repository.findByEmailIgnoreCaseAndCurrencyCodeIgnoreCase(email, currency).ifPresent(existing -> {
            throw new DuplicateResourceException(
                    "Korisnik " + email + " već ima stavku računa za valutu " + currency + ".");
        });

        BankAccount saved = repository.save(
                new BankAccount(email, currency, scaled(requireNonNegative(dto.getAmount()))));
        return toDto(saved);
    }

    @Transactional
    public BankAccountDto update(Long id, BankAccountDto dto) {
        auth.requireAnyOf(Role.ADMIN);

        BankAccount account = getOrThrow(id);
        if (dto.getCurrencyCode() != null) {
            account.setCurrencyCode(normalizeCurrency(dto.getCurrencyCode()));
        }
        if (dto.getAmount() != null) {
            account.setAmount(scaled(requireNonNegative(dto.getAmount())));
        }
        return toDto(repository.save(account));
    }

    @Transactional
    public void delete(Long id) {
        auth.requireAnyOf(Role.ADMIN);
        repository.delete(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<BankAccountDto> findByEmailInternal(String email) {
        return loadAccount(normalizeEmail(email));
    }

    @Transactional
    public List<BankAccountDto> createDefaultAccount(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (repository.existsByEmailIgnoreCase(email)) {
            log.info("Bankovni račun za {} već postoji - preskačem kreiranje", email);
            return loadAccount(email);
        }
        repository.save(new BankAccount(email, DEFAULT_CURRENCY, scaled(BigDecimal.ZERO)));
        log.info("Kreiran podrazumevani bankovni račun za {} ({} 0)", email, DEFAULT_CURRENCY);
        return loadAccount(email);
    }

    @Transactional
    public void deleteByEmail(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        repository.deleteByEmailIgnoreCase(email);
        log.info("Obrisan bankovni račun korisnika {}", email);
    }

    @Transactional
    public void changeEmail(String rawOldEmail, String rawNewEmail) {
        String oldEmail = normalizeEmail(rawOldEmail);
        String newEmail = normalizeEmail(rawNewEmail);
        List<BankAccount> accounts = repository.findByEmailIgnoreCaseOrderByCurrencyCodeAsc(oldEmail);
        accounts.forEach(account -> account.setEmail(newEmail));
        repository.saveAll(accounts);
        log.info("Bankovni račun prebacen sa {} na {}", oldEmail, newEmail);
    }

    @Transactional
    public List<BankAccountDto> debit(String rawEmail, String rawCurrency, BigDecimal amount) {
        String email = normalizeEmail(rawEmail);
        String currency = normalizeCurrency(rawCurrency);
        BigDecimal value = scaled(requirePositive(amount));

        BankAccount account = repository.findByEmailIgnoreCaseAndCurrencyCodeIgnoreCase(email, currency)
                .orElseThrow(() -> new InsufficientFundsException(
                        "Na bankovnom računu ne postoje sredstva u valuti " + currency + "."));

        if (account.getAmount().compareTo(value) < 0) {
            throw new InsufficientFundsException(
                    "Nedovoljno sredstava: na računu je dostupno " + account.getAmount() + " " + currency
                            + ", a traženo je " + value + " " + currency + ".");
        }

        account.setAmount(scaled(account.getAmount().subtract(value)));
        repository.save(account);
        return loadAccount(email);
    }

    @Transactional
    public List<BankAccountDto> credit(String rawEmail, String rawCurrency, BigDecimal amount) {
        String email = normalizeEmail(rawEmail);
        String currency = normalizeCurrency(rawCurrency);
        BigDecimal value = scaled(requirePositive(amount));

        BankAccount account = repository.findByEmailIgnoreCaseAndCurrencyCodeIgnoreCase(email, currency)
                .orElseGet(() -> new BankAccount(email, currency, BigDecimal.ZERO));

        account.setAmount(scaled(account.getAmount().add(value)));
        repository.save(account);
        return loadAccount(email);
    }

    private List<BankAccountDto> loadAccount(String email) {
        List<BankAccount> accounts = repository.findByEmailIgnoreCaseOrderByCurrencyCodeAsc(email);
        if (accounts.isEmpty()) {
            throw new ResourceNotFoundException("Bankovni račun za korisnika " + email + " ne postoji.");
        }
        return accounts.stream().map(this::toDto).toList();
    }

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
                    "Bankovni račun je dozvoljen samo korisnicima sa ulogom USER, a " + email
                            + " ima ulogu " + user.getRole() + ".");
        }
    }

    private BankAccount getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stavka bankovnog računa sa id vrednošću " + id + " ne postoji."));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidRequestException("Email adresa je obavezna.");
        }
        return email.trim().toLowerCase();
    }

    private String normalizeCurrency(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidRequestException("Kod valute je obavezan.");
        }
        String normalized = code.trim().toUpperCase();
        if (!normalized.matches("[A-Z]{3}")) {
            throw new InvalidRequestException(
                    "Kod valute mora imati tacno tri slova (npr. EUR, USD, RSD), a prosleđeno je: " + code);
        }
        return normalized;
    }

    private BigDecimal requireNonNegative(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidRequestException("Količina je obavezna.");
        }
        if (amount.signum() < 0) {
            throw new InvalidRequestException("Količina ne može biti negativna.");
        }
        return amount;
    }

    private BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidRequestException("Količina za razmenu mora biti veća od nule.");
        }
        return amount;
    }

    private BigDecimal scaled(BigDecimal amount) {
        return amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BankAccountDto toDto(BankAccount account) {
        return new BankAccountDto(account.getId(), account.getEmail(),
                account.getCurrencyCode(), account.getAmount());
    }
}
