package com.soas.library.security;

import com.soas.library.dto.Role;
import com.soas.util.exception.UnauthorizedActionException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class AuthContext {
    public String currentEmail() {
        String email = header(AuthHeaders.EMAIL);
        if (email == null || email.isBlank()) {
            throw new UnauthorizedActionException(
                    "Zahtev nije autentikovan. Pristupite servisu preko API-Gateway-a (port 8765) uz basic autentikaciju.");
        }
        return email;
    }

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

    public void requireOwnDataIfUser(String targetEmail) {
        if (currentRole() == Role.USER && !currentEmail().equalsIgnoreCase(targetEmail)) {
            throw new UnauthorizedActionException(
                    "Korisnik sa ulogom USER može da pristupi isključivo sopstvenim podacima.");
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
