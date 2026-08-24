package com.soas.library.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Podaci o korisniku aplikacije koji se razmenjuju izmedju mikroservisa.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {

    private Long id;

    @NotBlank(message = "email je obavezan")
    @Email(message = "email nije u ispravnom formatu")
    private String email;

    @NotBlank(message = "lozinka je obavezna")
    @Size(min = 4, message = "lozinka mora imati najmanje 4 karaktera")
    private String password;

    @NotNull(message = "uloga je obavezna (OWNER, ADMIN ili USER)")
    private Role role;

    public UserDto() {
    }

    public UserDto(Long id, String email, String password, Role role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
