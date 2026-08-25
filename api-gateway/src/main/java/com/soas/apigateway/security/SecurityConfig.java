package com.soas.apigateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Basic autentikacija na nivou API-Gateway-a - obavezan deo specifikacije.
 *
 * Pravila pristupa:
 *  - /internal/**                                 zabranjeno spolja (samo medjuservisna komunikacija)
 *  - /currency-exchange/**, /crypto-exchange/**   dostupno bez prijave (kursevi su javni)
 *  - sve ostalo                                   zahteva basic autentikaciju
 *
 * Kredencijali se proveravaju nad users-service mikroservisom
 * ({@link UsersServiceAuthenticationManager}).
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ReactiveAuthenticationManager authenticationManager,
                                                         JsonAuthenticationEntryPoint entryPoint) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(entryPoint))
                .authenticationManager(authenticationManager)
                .authorizeExchange(exchange -> exchange
                        // Preflight zahtevi korisnickog interfejsa.
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Interni endpoint-i nisu dostupni krajnjem korisniku.
                        .pathMatchers("/internal/**").denyAll()
                        .pathMatchers("/actuator/**").permitAll()
                        // Kursevi valuta stoje ispred Login stranice.
                        .pathMatchers("/currency-exchange/**", "/crypto-exchange/**").permitAll()
                        .anyExchange().authenticated())
                .build();
    }

    /**
     * Dozvoljava korisnickom interfejsu (React razvojni server) da salje
     * zahteve ka gateway-u zajedno sa Authorization zaglavljem.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
