package com.soas.currencyexchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Currency exchange mikroservis - pribavlja kurseve za razmenu fiat valuta
 * sa eksternog API servisa (exchangerate-api.com, otvoren bez API kljuca).
 *
 * Komunikacija sa eksternim servisom ide preko Feign klijenta, jer
 * specifikacija zabranjuje upotrebu RestTemplate-a u mikroservisima.
 */
@SpringBootApplication(scanBasePackages = {"com.soas.currencyexchange", "com.soas.library", "com.soas.util"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.soas.library.proxy", "com.soas.currencyexchange.proxy"})
public class CurrencyExchangeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurrencyExchangeApplication.class, args);
    }
}
