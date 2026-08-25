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

    @Transactional(readOnly = true)
    public UserDto findByEmailInternal(String email) {
        User user = repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Korisnik sa email adresom " + email + " ne postoji u sistemu."));
        return toDto(user);
    }

    @Transactional
    public UserDto create(UserDto dto) {
        Role callerRole = auth.requireAnyOf(Role.OWNER, Role.ADMIN);

        if (callerRole == Role.ADMIN && dto.getRole() != Role.USER) {
            throw new UnauthorizedActionException(
                    "ADMIN može da dodaje isključivo korisnike sa ulogom USER.");
        }
        if (dto.getRole() == Role.OWNER && repository.countByRole(Role.OWNER) > 0) {
            throw new DuplicateResourceException(
                    "U sistemu već postoji korisnik sa ulogom OWNER. Dozvoljen je samo jedan.");
        }
        if (repository.existsByEmailIgnoreCase(dto.getEmail())) {
            throw new DuplicateResourceException(
                    "Korisnik sa email adresom " + dto.getEmail() + " već postoji.");
        }

        User saved = repository.save(new User(
                dto.getEmail().trim(),
                encoder.encode(dto.getPassword()),
                dto.getRole()));

        if (saved.getRole() == Role.USER) {
            bankAccountProxy.createDefaultAccount(saved.getEmail());
            cryptoWalletProxy.createDefaultWallet(saved.getEmail());
            log.info("Kreiran bankovni račun i crypto novčanik za korisnika {}", saved.getEmail());
        }

        return toDto(saved);
    }

    @Transactional
    public UserDto update(Long id, UserDto dto) {
        Role callerRole = auth.requireAnyOf(Role.OWNER, Role.ADMIN);

        User user = getOrThrow(id);
        Role previousRole = user.getRole();
        String previousEmail = user.getEmail();
        Role newRole = dto.getRole() == null ? previousRole : dto.getRole();

        if (callerRole == Role.ADMIN && (previousRole != Role.USER || newRole != Role.USER)) {
            throw new UnauthorizedActionException(
                    "ADMIN može da ažurira isključivo korisnike sa ulogom USER i ne može da im menja ulogu.");
        }
        if (previousRole == Role.OWNER && newRole != Role.OWNER) {
            throw new InvalidRequestException(
                    "Korisniku sa ulogom OWNER nije moguće promeniti ulogu - sistem mora imati tacno jednog OWNER-a.");
        }
        if (newRole == Role.OWNER && previousRole != Role.OWNER) {
            throw new DuplicateResourceException(
                    "U sistemu već postoji korisnik sa ulogom OWNER. Dozvoljen je samo jedan.");
        }

        String newEmail = dto.getEmail() == null ? previousEmail : dto.getEmail().trim();
        if (!newEmail.equalsIgnoreCase(previousEmail) && repository.existsByEmailIgnoreCase(newEmail)) {
            throw new DuplicateResourceException("Korisnik sa email adresom " + newEmail + " već postoji.");
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

    private void synchronizeAccounts(Role previousRole, Role newRole, String previousEmail, String newEmail) {
        if (previousRole == Role.USER && newRole != Role.USER) {
            bankAccountProxy.deleteByEmail(previousEmail);
            cryptoWalletProxy.deleteByEmail(previousEmail);
            log.info("Korisnik {} vise nema ulogu USER - obrisani račun i novčanik", previousEmail);
            return;
        }
        if (previousRole != Role.USER && newRole == Role.USER) {
            bankAccountProxy.createDefaultAccount(newEmail);
            cryptoWalletProxy.createDefaultWallet(newEmail);
            log.info("Korisnik {} je dobio ulogu USER - kreiran račun i novčanik", newEmail);
            return;
        }
        if (newRole == Role.USER && !previousEmail.equalsIgnoreCase(newEmail)) {
            bankAccountProxy.changeEmail(previousEmail, newEmail);
            cryptoWalletProxy.changeEmail(previousEmail, newEmail);
            log.info("Email korisnika promenjen sa {} na {} - ažurirani račun i novčanik", previousEmail, newEmail);
        }
    }

    @Transactional
    public void delete(Long id) {
        auth.requireAnyOf(Role.OWNER);

        User user = getOrThrow(id);
        if (user.getRole() == Role.OWNER) {
            throw new InvalidRequestException(
                    "Korisnika sa ulogom OWNER nije moguće obrisati - sistem mora imati tacno jednog OWNER-a.");
        }

        repository.delete(user);

        if (user.getRole() == Role.USER) {
            bankAccountProxy.deleteByEmail(user.getEmail());
            cryptoWalletProxy.deleteByEmail(user.getEmail());
            log.info("Obrisan korisnik {} zajedno sa bankovnim računom i novčanikom", user.getEmail());
        }
    }

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

    private User getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik sa id vrednošću " + id + " ne postoji."));
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), null, user.getRole());
    }
}
