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

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String ROLE_PREFIX = "ROLE_";

    @PostMapping("/login")
    public Mono<Map<String, Object>> login() {
        return currentUser();
    }

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
