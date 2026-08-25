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

@FeignClient(name = "bank-account")
public interface BankAccountProxy {
    @GetMapping("/internal/bank-accounts/{email}")
    List<BankAccountDto> findByEmail(@PathVariable("email") String email);

    @PostMapping("/internal/bank-accounts/{email}/default")
    List<BankAccountDto> createDefaultAccount(@PathVariable("email") String email);

    @DeleteMapping("/internal/bank-accounts/{email}")
    void deleteByEmail(@PathVariable("email") String email);

    @PostMapping("/internal/bank-accounts/{email}/rename")
    void changeEmail(@PathVariable("email") String email, @RequestParam("newEmail") String newEmail);

    @PostMapping("/internal/bank-accounts/{email}/debit")
    List<BankAccountDto> debit(@PathVariable("email") String email,
                               @RequestParam("currencyCode") String currencyCode,
                               @RequestParam("amount") BigDecimal amount);

    @PostMapping("/internal/bank-accounts/{email}/credit")
    List<BankAccountDto> credit(@PathVariable("email") String email,
                                @RequestParam("currencyCode") String currencyCode,
                                @RequestParam("amount") BigDecimal amount);
}
