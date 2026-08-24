package com.soas.library.security;

/**
 * Nazivi zaglavlja kojima API-Gateway prosledjuje identitet vec autentikovanog
 * korisnika ka ostalim mikroservisima.
 */
public final class AuthHeaders {

    public static final String EMAIL = "X-Auth-Email";
    public static final String ROLE = "X-Auth-Role";

    private AuthHeaders() {
    }
}
