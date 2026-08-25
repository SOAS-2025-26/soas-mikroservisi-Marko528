package com.soas.apigateway.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prijava korisnika na aplikaciju.
 *
 * Endpoint ne prima telo zahteva - kredencijali se salju kroz standardno
 * Authorization: Basic zaglavlje, koje gateway vec proverava. Ako je provera
 * prosla, ovde se korisniku vraca njegov email i uloga, sto korisnicki
 * interfejs koristi da prikaze odgovarajuce ekrane.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String ROLE_PREFIX = "ROLE_";

    /** Provera kredencijala - uspesan odgovor znaci da je prijava validna. */
    @PostMapping("/login")
    public Mono<Map<String, Object>> login() {
        return currentUser();
    }

    /** Podaci o trenutno prijavljenom korisniku. */
    @GetMapping("/me")
    public Mono<Map<String, Object>> me() {
        return currentUser();
    }

    private Mono<Map<String, Object>> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .map(this::describe);
    }

    private Map<String, Object> describe(Authentication authentication) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("authenticated", true);
        body.put("email", authentication.getName());
        body.put("role", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith(ROLE_PREFIX)
                        ? authority.substring(ROLE_PREFIX.length())
                        : authority)
                .findFirst()
                .orElse(""));
        return body;
    }
}
