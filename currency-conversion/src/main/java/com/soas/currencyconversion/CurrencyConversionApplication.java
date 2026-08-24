package com.soas.currencyconversion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Currency conversion mikroservis - end-point korisnickih zahteva za razmenu
 * fiat valuta.
 *
 * Nema sopstvenu bazu: kurs pribavlja od currency-exchange servisa, a stanje
 * racuna cita i menja preko bank-account servisa, sve putem Feign klijenta.
 */
@SpringBootApplication(scanBasePackages = {"com.soas.currencyconversion", "com.soas.library", "com.soas.util"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.soas.library.proxy")
public class CurrencyConversionApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurrencyConversionApplication.class, args);
    }
}
