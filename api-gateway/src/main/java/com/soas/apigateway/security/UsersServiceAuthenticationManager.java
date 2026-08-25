package com.soas.apigateway.security;

import com.soas.apigateway.dto.AuthRequest;
import com.soas.apigateway.dto.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Provera kredencijala basic autentikacije nad users-service mikroservisom.
 *
 * Instanca users-service-a se pronalazi preko Eureka naming servera, a poziv
 * se izvrsava RestTemplate-om. Specifikacija zabranjuje RestTemplate u
 * mikroservisima, uz izricit izuzetak za API-Gateway - sto je upravo ovaj slucaj.
 *
 * Poziv je blokirajuci, pa se izvrsava na zasebnom nitnom bazenu kako ne bi
 * blokirao reaktivnu petlju gateway-a.
 */
@Component
public class UsersServiceAuthenticationManager implements ReactiveAuthenticationManager {

    private static final Logger log = LoggerFactory.getLogger(UsersServiceAuthenticationManager.class);
    private static final String USERS_SERVICE = "users-service";

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate = new RestTemplate();

    public UsersServiceAuthenticationManager(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String email = authentication.getName();
        String password = String.valueOf(authentication.getCredentials());

        return Mono.fromCallable(() -> verify(email, password))
                .subscribeOn(Schedulers.boundedElastic())
                .map(this::toAuthentication);
    }

    private AuthResponse verify(String email, String password) {
        String url = resolveUsersServiceUrl() + "/internal/users/authenticate";
        try {
            AuthResponse response = restTemplate.postForObject(
                    url, new AuthRequest(email, password), AuthResponse.class);
            if (response == null || !response.isAuthenticated()) {
                throw new BadCredentialsException("Neispravan email ili lozinka.");
            }
            return response;
        } catch (HttpClientErrorException ex) {
            // users-service vraca 403 kada kredencijali ne odgovaraju.
            throw new BadCredentialsException("Neispravan email ili lozinka.");
        } catch (ResourceAccessException ex) {
            log.error("users-service nije dostupan: {}", ex.getMessage());
            throw new AuthenticationServiceException(
                    "Servis za autentikaciju trenutno nije dostupan. Pokusajte ponovo kasnije.");
        }
    }

    private String resolveUsersServiceUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances(USERS_SERVICE);
        if (instances.isEmpty()) {
            log.error("users-service nije registrovan na Eureka serveru");
            throw new AuthenticationServiceException(
                    "Servis za autentikaciju nije registrovan na naming serveru. Pokusajte ponovo kasnije.");
        }
        return instances.get(0).getUri().toString();
    }

    private Authentication toAuthentication(AuthResponse response) {
        return new UsernamePasswordAuthenticationToken(
                response.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + response.getRole())));
    }
}
