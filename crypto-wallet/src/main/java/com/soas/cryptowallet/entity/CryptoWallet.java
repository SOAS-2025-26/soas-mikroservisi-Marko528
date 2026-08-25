package com.soas.cryptowallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(name = "crypto_wallets",
        uniqueConstraints = @UniqueConstraint(name = "uk_bank_account_email_currency",
                columnNames = {"email", "crypto_code"}))
public class CryptoWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "crypto_code", nullable = false, length = 10)
    private String cryptoCode;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal amount;

    public CryptoWallet() {
    }

    public CryptoWallet(String email, String cryptoCode, BigDecimal amount) {
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
