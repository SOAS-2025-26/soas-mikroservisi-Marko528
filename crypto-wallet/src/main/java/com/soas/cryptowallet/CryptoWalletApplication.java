package com.soas.cryptowallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Crypto wallet mikroservis - cuva stanje kripto valuta u novcanicima korisnika.
 *
 * Poseduje sopstvenu H2 bazu (database per service). Racuni su dozvoljeni
 * iskljucivo za korisnike sa ulogom USER, sto se proverava Feign pozivom
 * ka users-service mikroservisu.
 */
@SpringBootApplication(scanBasePackages = {"com.soas.cryptowallet", "com.soas.library", "com.soas.util"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.soas.library.proxy")
public class CryptoWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoWalletApplication.class, args);
    }
}
