package com.soas.usersservice.service;

import com.soas.library.dto.Role;
import com.soas.library.dto.UserDto;
import com.soas.library.proxy.BankAccountProxy;
import com.soas.library.proxy.CryptoWalletProxy;
import com.soas.library.security.AuthContext;
import com.soas.usersservice.entity.User;
import com.soas.usersservice.repository.UserRepository;
import com.soas.util.exception.DuplicateResourceException;
import com.soas.util.exception.InvalidRequestException;
import com.soas.util.exception.ResourceNotFoundException;
import com.soas.util.exception.UnauthorizedActionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Poslovna logika nad korisnicima, ukljucujuci proveru ovlascenja:
 *
 *  - OWNER moze da dodaje, azurira i brise sve korisnike
 *  - ADMIN moze da dodaje i azurira korisnike sa ulogom USER
 *  - USER nema pristup ovom servisu
 *
 * U sistemu moze postojati samo jedan korisnik sa ulogom OWNER.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repository;
    private final AuthContext auth;
    private final BankAccountProxy bankAccountProxy;
    private final CryptoWalletProxy cryptoWalletProxy;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repository,
                       AuthContext auth,
                       BankAccountProxy bankAccountProxy,
                       CryptoWalletProxy cryptoWalletProxy) {
        this.repository = repository;
        this.auth = auth;
        this.bankAccountProxy = bankAccountProxy;
        this.cryptoWalletProxy = cryptoWalletProxy;
    }

    // ------------------------------------------------------------------
    // Citanje
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        auth.requireAnyOf(Role.OWNER, Role.ADMIN);
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        auth.requireAnyOf(Role.OWNER, Role.ADMIN);
        return toDto(getOrThrow(id));
    }

    /** Interna pretraga po email-u - koriste je drugi mikroservisi, bez provere uloge. */
    @Transactional(readOnly = true)
    public UserDto findByEmailInternal(String email) {
        User user = repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Korisnik sa email adresom " + email + " ne postoji u sistemu."));
        return toDto(user);
    }

    // ------------------------------------------------------------------
    // Kreiranje
    // ------------------------------------------------------------------

    @Transactional
    public UserDto create(UserDto dto) {
        Role callerRole = auth.requireAnyOf(Role.OWNER, Role.ADMIN);

        if (callerRole == Role.ADMIN && dto.getRole() != Role.USER) {
            throw new UnauthorizedActionException(
                    "ADMIN moze da dodaje iskljucivo korisnike sa ulogom USER.");
        }
        if (dto.getRole() == Role.OWNER && repository.countByRole(Role.OWNER) > 0) {
            throw new DuplicateResourceException(
                    "U sistemu vec postoji korisnik sa ulogom OWNER. Dozvoljen je samo jedan.");
        }
        if (repository.existsByEmailIgnoreCase(dto.getEmail())) {
            throw new DuplicateResourceException(
                    "Korisnik sa email adresom " + dto.getEmail() + " vec postoji.");
        }

        User saved = repository.save(new User(
                dto.getEmail().trim(),
                encoder.encode(dto.getPassword()),
                dto.getRole()));

        // Novi korisnik sa ulogom USER automatski dobija bankovni racun i novcanik.
        if (saved.getRole() == Role.USER) {
            bankAccountProxy.createDefaultAccount(saved.getEmail());
            cryptoWalletProxy.createDefaultWallet(saved.getEmail());
            log.info("Kreiran bankovni racun i crypto novcanik za korisnika {}", saved.getEmail());
        }

        return toDto(saved);
    }

    // ------------------------------------------------------------------
    // Azuriranje
    // ------------------------------------------------------------------

    @Transactional
    public UserDto update(Long id, UserDto dto) {
        Role callerRole = auth.requireAnyOf(Role.OWNER, Role.ADMIN);

        User user = getOrThrow(id);
        Role previousRole = user.getRole();
        String previousEmail = user.getEmail();
        Role newRole = dto.getRole() == null ? previousRole : dto.getRole();

        if (callerRole == Role.ADMIN && (previousRole != Role.USER || newRole != Role.USER)) {
            throw new UnauthorizedActionException(
                    "ADMIN moze da azurira iskljucivo korisnike sa ulogom USER i ne moze da im menja ulogu.");
        }
        if (previousRole == Role.OWNER && newRole != Role.OWNER) {
            throw new InvalidRequestException(
                    "Korisniku sa ulogom OWNER nije moguce promeniti ulogu - sistem mora imati tacno jednog OWNER-a.");
        }
        if (newRole == Role.OWNER && previousRole != Role.OWNER) {
            throw new DuplicateResourceException(
                    "U sistemu vec postoji korisnik sa ulogom OWNER. Dozvoljen je samo jedan.");
        }

        String newEmail = dto.getEmail() == null ? previousEmail : dto.getEmail().trim();
        if (!newEmail.equalsIgnoreCase(previousEmail) && repository.existsByEmailIgnoreCase(newEmail)) {
            throw new DuplicateResourceException("Korisnik sa email adresom " + newEmail + " vec postoji.");
        }

        user.setEmail(newEmail);
        user.setRole(newRole);
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(encoder.encode(dto.getPassword()));
        }
        User saved = repository.save(user);

        synchronizeAccounts(previousRole, newRole, previousEmail, newEmail);

        return toDto(saved);
    }

    /**
     * Odrzava bankovni racun i novcanik uskladjenim sa promenama nad korisnikom:
     * promena email-a, gubitak uloge USER ili dobijanje uloge USER.
     */
    private void synchronizeAccounts(Role previousRole, Role newRole, String previousEmail, String newEmail) {
        if (previousRole == Role.USER && newRole != Role.USER) {
            bankAccountProxy.deleteByEmail(previousEmail);
            cryptoWalletProxy.deleteByEmail(previousEmail);
            log.info("Korisnik {} vise nema ulogu USER - obrisani racun i novcanik", previousEmail);
            return;
        }
        if (previousRole != Role.USER && newRole == Role.USER) {
            bankAccountProxy.createDefaultAccount(newEmail);
            cryptoWalletProxy.createDefaultWallet(newEmail);
            log.info("Korisnik {} je dobio ulogu USER - kreiran racun i novcanik", newEmail);
            return;
        }
        if (newRole == Role.USER && !previousEmail.equalsIgnoreCase(newEmail)) {
            bankAccountProxy.changeEmail(previousEmail, newEmail);
            cryptoWalletProxy.changeEmail(previousEmail, newEmail);
            log.info("Email korisnika promenjen sa {} na {} - azurirani racun i novcanik", previousEmail, newEmail);
        }
    }

    // ------------------------------------------------------------------
    // Brisanje
    // ------------------------------------------------------------------

    @Transactional
    public void delete(Long id) {
        auth.requireAnyOf(Role.OWNER);

        User user = getOrThrow(id);
        if (user.getRole() == Role.OWNER) {
            throw new InvalidRequestException(
                    "Korisnika sa ulogom OWNER nije moguce obrisati - sistem mora imati tacno jednog OWNER-a.");
        }

        repository.delete(user);

        // Brisanje korisnika sa ulogom USER povlaci brisanje racuna i novcanika.
        if (user.getRole() == Role.USER) {
            bankAccountProxy.deleteByEmail(user.getEmail());
            cryptoWalletProxy.deleteByEmail(user.getEmail());
            log.info("Obrisan korisnik {} zajedno sa bankovnim racunom i novcanikom", user.getEmail());
        }
    }

    // ------------------------------------------------------------------
    // Autentikacija (poziva je iskljucivo API-Gateway)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public UserDto authenticate(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            throw new InvalidRequestException("Email i lozinka su obavezni.");
        }
        User user = repository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UnauthorizedActionException("Neispravan email ili lozinka."));
        if (!encoder.matches(rawPassword, user.getPassword())) {
            throw new UnauthorizedActionException("Neispravan email ili lozinka.");
        }
        return toDto(user);
    }

    // ------------------------------------------------------------------

    private User getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik sa id vrednoscu " + id + " ne postoji."));
    }

    /** Lozinka se nikada ne vraca klijentu. */
    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), null, user.getRole());
    }
}
