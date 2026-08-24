package com.soas.library.proxy;

import com.soas.library.dto.CryptoWalletDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * Feign proxy ka crypto-wallet mikroservisu.
 *
 * Koriste ga users-service (automatsko kreiranje i brisanje novcanika)
 * i trade-service (izmene stanja novcanika).
 */
@FeignClient(name = "crypto-wallet")
public interface CryptoWalletProxy {

    /** Sve stavke crypto novcanika datog korisnika. */
    @GetMapping("/internal/crypto-wallets/{email}")
    List<CryptoWalletDto> findByEmail(@PathVariable("email") String email);

    /** Kreira podrazumevani novcanik (ETH sa stanjem 0) za novog korisnika sa ulogom USER. */
    @PostMapping("/internal/crypto-wallets/{email}/default")
    List<CryptoWalletDto> createDefaultWallet(@PathVariable("email") String email);

    /** Brise kompletan novcanik korisnika. */
    @DeleteMapping("/internal/crypto-wallets/{email}")
    void deleteByEmail(@PathVariable("email") String email);

    /** Menja email na novcaniku kada se korisniku promeni email u users-service-u. */
    @PostMapping("/internal/crypto-wallets/{email}/rename")
    void changeEmail(@PathVariable("email") String email, @RequestParam("newEmail") String newEmail);

    /** Skida kripto valutu sa novcanika; baca gresku ako nema dovoljno sredstava. */
    @PostMapping("/internal/crypto-wallets/{email}/debit")
    List<CryptoWalletDto> debit(@PathVariable("email") String email,
                                @RequestParam("cryptoCode") String cryptoCode,
                                @RequestParam("amount") BigDecimal amount);

    /** Dodaje kripto valutu na novcanik; kreira stavku ako ne postoji. */
    @PostMapping("/internal/crypto-wallets/{email}/credit")
    List<CryptoWalletDto> credit(@PathVariable("email") String email,
                                 @RequestParam("cryptoCode") String cryptoCode,
                                 @RequestParam("amount") BigDecimal amount);
}
