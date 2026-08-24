package com.soas.library.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Jedan zapis bankovnog racuna: kolicina jedne fiat valute koju korisnik poseduje.
 */
public class BankAccountDto {

    private Long id;

    @NotBlank(message = "email je obavezan")
    @Email(message = "email nije u ispravnom formatu")
    private String email;

    @NotBlank(message = "kod valute je obavezan (npr. EUR, USD, RSD)")
    private String currencyCode;

    @NotNull(message = "kolicina je obavezna")
    @DecimalMin(value = "0.0", message = "kolicina ne moze biti negativna")
    private BigDecimal amount;

    public BankAccountDto() {
    }

    public BankAccountDto(Long id, String email, String currencyCode, BigDecimal amount) {
        this.id = id;
        this.email = email;
        this.currencyCode = currencyCode;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
