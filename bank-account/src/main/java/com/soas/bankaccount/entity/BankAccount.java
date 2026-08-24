package com.soas.bankaccount.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * Jedna stavka bankovnog racuna: kolicina jedne fiat valute koju korisnik poseduje.
 *
 * Bankovni racun jednog korisnika cine svi zapisi sa istom email adresom, po
 * jedan za svaku valutu. Zato je jedinstvenost postavljena na par
 * (email, currency_code) - isti korisnik ne moze imati dva zapisa za istu valutu.
 */
@Entity
@Table(name = "bank_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_bank_account_email_currency",
                columnNames = {"email", "currency_code"}))
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    public BankAccount() {
    }

    public BankAccount(String email, String currencyCode, BigDecimal amount) {
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
