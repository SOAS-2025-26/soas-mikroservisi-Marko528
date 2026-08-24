package com.soas.library.proxy;

import com.soas.library.dto.BankAccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * Feign proxy ka bank-account mikroservisu.
 *
 * Koriste ga users-service (automatsko kreiranje i brisanje racuna),
 * currency-conversion i trade-service (izmene stanja racuna).
 */
@FeignClient(name = "bank-account")
public interface BankAccountProxy {

    /** Sve stavke bankovnog racuna datog korisnika. */
    @GetMapping("/internal/bank-accounts/{email}")
    List<BankAccountDto> findByEmail(@PathVariable("email") String email);

    /** Kreira podrazumevani racun (EUR sa stanjem 0) za novog korisnika sa ulogom USER. */
    @PostMapping("/internal/bank-accounts/{email}/default")
    List<BankAccountDto> createDefaultAccount(@PathVariable("email") String email);

    /** Brise kompletan bankovni racun korisnika. */
    @DeleteMapping("/internal/bank-accounts/{email}")
    void deleteByEmail(@PathVariable("email") String email);

    /** Menja email na racunu kada se korisniku promeni email u users-service-u. */
    @PostMapping("/internal/bank-accounts/{email}/rename")
    void changeEmail(@PathVariable("email") String email, @RequestParam("newEmail") String newEmail);

    /** Skida sredstva sa racuna; baca gresku ako nema dovoljno sredstava. */
    @PostMapping("/internal/bank-accounts/{email}/debit")
    List<BankAccountDto> debit(@PathVariable("email") String email,
                               @RequestParam("currencyCode") String currencyCode,
                               @RequestParam("amount") BigDecimal amount);

    /** Dodaje sredstva na racun; kreira stavku za valutu ako ne postoji. */
    @PostMapping("/internal/bank-accounts/{email}/credit")
    List<BankAccountDto> credit(@PathVariable("email") String email,
                                @RequestParam("currencyCode") String currencyCode,
                                @RequestParam("amount") BigDecimal amount);
}
