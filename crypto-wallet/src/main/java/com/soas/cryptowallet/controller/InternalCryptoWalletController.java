package com.soas.cryptowallet.controller;

import com.soas.cryptowallet.service.CryptoWalletService;
import com.soas.library.dto.CryptoWalletDto;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interni interfejs koji koriste users-service, currency-conversion i
 * trade-service preko Feign klijenta.
 *
 * Putanja /internal/** se ne rutira kroz API-Gateway, pa nije dostupna
 * krajnjem korisniku.
 */
@RestController
@RequestMapping("/internal/crypto-wallets")
public class InternalCryptoWalletController {

    private final CryptoWalletService service;

    public InternalCryptoWalletController(CryptoWalletService service) {
        this.service = service;
    }

    @GetMapping("/{email}")
    public List<CryptoWalletDto> findByEmail(@PathVariable String email) {
        return service.findByEmailInternal(email);
    }

    @PostMapping("/{email}/default")
    public List<CryptoWalletDto> createDefaultWallet(@PathVariable String email) {
        return service.createDefaultWallet(email);
    }

    @DeleteMapping("/{email}")
    public void deleteByEmail(@PathVariable String email) {
        service.deleteByEmail(email);
    }

    @PostMapping("/{email}/rename")
    public void changeEmail(@PathVariable String email, @RequestParam String newEmail) {
        service.changeEmail(email, newEmail);
    }

    @PostMapping("/{email}/debit")
    public List<CryptoWalletDto> debit(@PathVariable String email,
                                      @RequestParam String cryptoCode,
                                      @RequestParam BigDecimal amount) {
        return service.debit(email, cryptoCode, amount);
    }

    @PostMapping("/{email}/credit")
    public List<CryptoWalletDto> credit(@PathVariable String email,
                                       @RequestParam String cryptoCode,
                                       @RequestParam BigDecimal amount) {
        return service.credit(email, cryptoCode, amount);
    }
}
