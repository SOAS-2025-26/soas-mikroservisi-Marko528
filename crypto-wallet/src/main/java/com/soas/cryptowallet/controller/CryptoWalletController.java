package com.soas.cryptowallet.controller;

import com.soas.cryptowallet.service.CryptoWalletService;
import com.soas.library.dto.CryptoWalletDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/crypto-wallets")
public class CryptoWalletController {
    private final CryptoWalletService service;

    public CryptoWalletController(CryptoWalletService service) {
        this.service = service;
    }

    @GetMapping
    public List<CryptoWalletDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/me")
    public List<CryptoWalletDto> findMyWallet() {
        return service.findMyWallet();
    }

    @GetMapping("/{email}")
    public List<CryptoWalletDto> findByEmail(@PathVariable String email) {
        return service.findByEmail(email);
    }

    @PostMapping
    public ResponseEntity<CryptoWalletDto> create(@Valid @RequestBody CryptoWalletDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public CryptoWalletDto update(@PathVariable Long id, @RequestBody CryptoWalletDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
