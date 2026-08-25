package com.soas.bankaccount.controller;

import com.soas.bankaccount.service.BankAccountService;
import com.soas.library.dto.BankAccountDto;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/internal/bank-accounts")
public class InternalBankAccountController {
    private final BankAccountService service;

    public InternalBankAccountController(BankAccountService service) {
        this.service = service;
    }

    @GetMapping("/{email}")
    public List<BankAccountDto> findByEmail(@PathVariable String email) {
        return service.findByEmailInternal(email);
    }

    @PostMapping("/{email}/default")
    public List<BankAccountDto> createDefaultAccount(@PathVariable String email) {
        return service.createDefaultAccount(email);
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
    public List<BankAccountDto> debit(@PathVariable String email,
                                      @RequestParam String currencyCode,
                                      @RequestParam BigDecimal amount) {
        return service.debit(email, currencyCode, amount);
    }

    @PostMapping("/{email}/credit")
    public List<BankAccountDto> credit(@PathVariable String email,
                                       @RequestParam String currencyCode,
                                       @RequestParam BigDecimal amount) {
        return service.credit(email, currencyCode, amount);
    }
}
