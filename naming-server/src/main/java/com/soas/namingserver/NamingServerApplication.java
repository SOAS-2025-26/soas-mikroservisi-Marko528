package com.soas.namingserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka naming server. Svi mikroservisi aplikacije se registruju na njega,
 * cime se omogucava pronalazenje servisa po imenu (service discovery) i
 * Feign komunikacija bez tvrdo kodiranih adresa.
 *
 * Konzola: http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer
public class NamingServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NamingServerApplication.class, args);
    }
}
