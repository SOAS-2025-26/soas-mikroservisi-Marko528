package com.soas.bankaccount.controller;

import com.soas.bankaccount.service.BankAccountService;
import com.soas.library.dto.BankAccountDto;
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

/**
 * Javni REST interfejs bank-account mikroservisa (dostupan kroz API-Gateway).
 *
 * Autorizacija:
 *  - OWNER: nema pristup
 *  - ADMIN: dodavanje, azuriranje, brisanje i pregled svih racuna
 *  - USER: pregled iskljucivo sopstvenog racuna
 */
@RestController
@RequestMapping("/bank-accounts")
public class BankAccountController {

    private final BankAccountService service;

    public BankAccountController(BankAccountService service) {
        this.service = service;
    }

    /** Svi bankovni racuni u sistemu (samo ADMIN). */
    @GetMapping
    public List<BankAccountDto> findAll() {
        return service.findAll();
    }

    /** Racun prijavljenog korisnika. */
    @GetMapping("/me")
    public List<BankAccountDto> findMyAccount() {
        return service.findMyAccount();
    }

    /** Racun konkretnog korisnika (ADMIN bilo koji, USER samo svoj). */
    @GetMapping("/{email}")
    public List<BankAccountDto> findByEmail(@PathVariable String email) {
        return service.findByEmail(email);
    }

    @PostMapping
    public ResponseEntity<BankAccountDto> create(@Valid @RequestBody BankAccountDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public BankAccountDto update(@PathVariable Long id, @RequestBody BankAccountDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
