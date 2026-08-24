package com.soas.usersservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Users mikroservis - upravlja korisnicima aplikacije i njihovim ulogama.
 *
 * Poseduje sopstvenu H2 bazu (database per service). Prilikom dodavanja i
 * brisanja korisnika sa ulogom USER, preko Feign klijenta obavestava
 * bank-account i crypto-wallet mikroservise.
 */
@SpringBootApplication(scanBasePackages = {"com.soas.usersservice", "com.soas.library", "com.soas.util"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.soas.library.proxy")
public class UsersServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsersServiceApplication.class, args);
    }
}
