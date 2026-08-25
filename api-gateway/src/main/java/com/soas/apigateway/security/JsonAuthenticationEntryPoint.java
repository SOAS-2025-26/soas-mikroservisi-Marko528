package com.soas.apigateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Odgovor na neuspesnu ili nedostajucu basic autentikaciju.
 *
 * Umesto praznog tela ili stack-trace-a, korisniku se vraca isti JSON format
 * greske koji koriste i ostali mikroservisi.
 */
@Component
public class JsonAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // Zadrzano standardno basic zaglavlje, da bi i pregledac ponudio prijavu.
        response.getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"SOAS aplikacija\"");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().format(FORMATTER));
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
        body.put("message", "Zahtev nije autentikovan. Posaljite ispravan email i lozinku "
                + "kroz Authorization: Basic zaglavlje.");
        body.put("path", exchange.getRequest().getPath().value());

        return write(response, body);
    }

    private Mono<Void> write(ServerHttpResponse response, Map<String, Object> body) {
        byte[] bytes;
        try {
            bytes = mapper.writeValueAsBytes(body);
        } catch (Exception ex) {
            bytes = "{\"message\":\"Zahtev nije autentikovan.\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
