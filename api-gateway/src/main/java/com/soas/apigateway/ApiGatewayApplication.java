package com.soas.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway - ulazna tacka aplikacije. Svi korisnicki zahtevi se salju ka
 * njemu, on ih autentikuje i prosledjuje odgovarajucem mikroservisu koji
 * pronalazi preko Eureka naming servera.
 *
 * Pokrece se na portu 8765.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
