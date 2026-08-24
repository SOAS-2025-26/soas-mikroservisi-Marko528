package com.soas.library.dto;

/**
 * Odgovor users-service-a na proveru kredencijala.
 */
public class AuthResponse {

    private boolean authenticated;
    private String email;
    private Role role;

    public AuthResponse() {
    }

    public AuthResponse(boolean authenticated, String email, Role role) {
        this.authenticated = authenticated;
        this.email = email;
        this.role = role;
    }

    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
