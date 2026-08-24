package com.soas.library.security;

import com.soas.library.dto.Role;
import com.soas.util.exception.UnauthorizedActionException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Cita identitet korisnika iz zaglavlja koja postavlja API-Gateway nakon
 * uspesne basic autentikacije, i proverava da li je korisnik ovlascen za
 * trazenu akciju.
 *
 * Autentikacija se radi na gateway-u; ovde se radi iskljucivo autorizacija.
 */
@Component
public class AuthContext {

    /** Email trenutno prijavljenog korisnika. */
    public String currentEmail() {
        String email = header(AuthHeaders.EMAIL);
        if (email == null || email.isBlank()) {
            throw new UnauthorizedActionException(
                    "Zahtev nije autentikovan. Pristupite servisu preko API-Gateway-a (port 8765) uz basic autentikaciju.");
        }
        return email;
    }

    /** Uloga trenutno prijavljenog korisnika. */
    public Role currentRole() {
        String role = header(AuthHeaders.ROLE);
        if (role == null || role.isBlank()) {
            throw new UnauthorizedActionException(
                    "Zahtev nije autentikovan. Pristupite servisu preko API-Gateway-a (port 8765) uz basic autentikaciju.");
        }
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedActionException("Nepoznata uloga korisnika: " + role);
        }
    }

    /**
     * Dozvoljava pristup samo navedenim ulogama, u suprotnom baca 403.
     */
    public Role requireAnyOf(Role... allowed) {
        Role role = currentRole();
        boolean permitted = Arrays.stream(allowed).anyMatch(r -> r == role);
        if (!permitted) {
            String allowedNames = Arrays.stream(allowed).map(Enum::name).collect(Collectors.joining(", "));
            throw new UnauthorizedActionException(
                    "Uloga " + role + " nije autorizovana za ovu akciju. Dozvoljene uloge: " + allowedNames + ".");
        }
        return role;
    }

    /**
     * Korisnik sa ulogom USER sme da radi samo nad sopstvenim podacima.
     * ADMIN i OWNER prolaze bez ogranicenja (ako su prethodno propusteni kroz requireAnyOf).
     */
    public void requireOwnDataIfUser(String targetEmail) {
        if (currentRole() == Role.USER && !currentEmail().equalsIgnoreCase(targetEmail)) {
            throw new UnauthorizedActionException(
                    "Korisnik sa ulogom USER moze da pristupi iskljucivo sopstvenim podacima.");
        }
    }

    private String header(String name) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader(name);
    }
}
