package com.soas.apigateway.dto;

public class AuthResponse {
    private boolean authenticated;
    private String email;
    private String role;

    public AuthResponse() {
    }

    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
