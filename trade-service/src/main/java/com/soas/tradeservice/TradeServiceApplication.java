package com.soas.tradeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Trade mikroservis - obezbedjuje razmenu obicnih (fiat) i crypto valuta.
 *
 * Podrzane su tri vrste razmene:
 *  - crypto u crypto,
 *  - fiat u crypto,
 *  - crypto u fiat.
 *
 * Nema sopstvenu bazu; sve podatke pribavlja Feign pozivima ka
 * crypto-exchange, crypto-wallet, bank-account i currency-conversion servisima.
 */
@SpringBootApplication(scanBasePackages = {"com.soas.tradeservice", "com.soas.library", "com.soas.util"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.soas.library.proxy")
public class TradeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeServiceApplication.class, args);
    }
}
