package com.soas.bankaccount.repository;

import com.soas.bankaccount.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByEmailIgnoreCaseOrderByCurrencyCodeAsc(String email);

    Optional<BankAccount> findByEmailIgnoreCaseAndCurrencyCodeIgnoreCase(String email, String currencyCode);

    boolean existsByEmailIgnoreCase(String email);

    void deleteByEmailIgnoreCase(String email);
}
