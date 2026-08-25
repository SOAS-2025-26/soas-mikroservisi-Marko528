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

@FeignClient(name = "crypto-wallet")
public interface CryptoWalletProxy {
    @GetMapping("/internal/crypto-wallets/{email}")
    List<CryptoWalletDto> findByEmail(@PathVariable("email") String email);

    @PostMapping("/internal/crypto-wallets/{email}/default")
    List<CryptoWalletDto> createDefaultWallet(@PathVariable("email") String email);

    @DeleteMapping("/internal/crypto-wallets/{email}")
    void deleteByEmail(@PathVariable("email") String email);

    @PostMapping("/internal/crypto-wallets/{email}/rename")
    void changeEmail(@PathVariable("email") String email, @RequestParam("newEmail") String newEmail);

    @PostMapping("/internal/crypto-wallets/{email}/debit")
    List<CryptoWalletDto> debit(@PathVariable("email") String email,
                                @RequestParam("cryptoCode") String cryptoCode,
                                @RequestParam("amount") BigDecimal amount);

    @PostMapping("/internal/crypto-wallets/{email}/credit")
    List<CryptoWalletDto> credit(@PathVariable("email") String email,
                                 @RequestParam("cryptoCode") String cryptoCode,
                                 @RequestParam("amount") BigDecimal amount);
}
