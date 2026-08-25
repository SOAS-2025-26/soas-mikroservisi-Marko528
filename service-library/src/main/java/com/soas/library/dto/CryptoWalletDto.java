package com.soas.library.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CryptoWalletDto {
    private Long id;

    @NotBlank(message = "email je obavezan")
    @Email(message = "email nije u ispravnom formatu")
    private String email;

    @NotBlank(message = "kod kripto valute je obavezan (npr. BTC, ETH)")
    private String cryptoCode;

    @NotNull(message = "količina je obavezna")
    @DecimalMin(value = "0.0", message = "količina ne može biti negativna")
    private BigDecimal amount;

    public CryptoWalletDto() {
    }

    public CryptoWalletDto(Long id, String email, String cryptoCode, BigDecimal amount) {
        this.id = id;
        this.email = email;
        this.cryptoCode = cryptoCode;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCryptoCode() { return cryptoCode; }
    public void setCryptoCode(String cryptoCode) { this.cryptoCode = cryptoCode; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
