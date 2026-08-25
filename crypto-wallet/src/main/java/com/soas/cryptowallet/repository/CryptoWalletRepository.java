package com.soas.cryptowallet.repository;

import com.soas.cryptowallet.entity.CryptoWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CryptoWalletRepository extends JpaRepository<CryptoWallet, Long> {
    List<CryptoWallet> findByEmailIgnoreCaseOrderByCryptoCodeAsc(String email);

    Optional<CryptoWallet> findByEmailIgnoreCaseAndCryptoCodeIgnoreCase(String email, String cryptoCode);

    boolean existsByEmailIgnoreCase(String email);

    void deleteByEmailIgnoreCase(String email);
}
