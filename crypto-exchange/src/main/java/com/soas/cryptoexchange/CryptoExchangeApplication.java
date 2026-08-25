package com.soas.cryptoexchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.soas.cryptoexchange", "com.soas.library", "com.soas.util"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.soas.library.proxy", "com.soas.cryptoexchange.proxy"})
public class CryptoExchangeApplication {
    public static void main(String[] args) {
        SpringApplication.run(CryptoExchangeApplication.class, args);
    }
}
