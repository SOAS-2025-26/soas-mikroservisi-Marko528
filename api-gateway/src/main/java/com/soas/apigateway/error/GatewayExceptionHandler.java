package com.soas.apigateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(error);
        }

        HttpStatus status;
        String message;

        if (error instanceof AuthenticationServiceException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = error.getMessage();
        } else if (error instanceof ResponseStatusException statusException) {
            HttpStatus resolved = HttpStatus.resolve(statusException.getStatusCode().value());
            status = resolved == null ? HttpStatus.INTERNAL_SERVER_ERROR : resolved;
            message = describeRoutingProblem(status, statusException.getReason(), exchange);
        } else if (error instanceof IOException || error instanceof java.net.ConnectException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Ciljni mikroservis trenutno nije dostupan. Pokušajte ponovo za nekoliko trenutaka.";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Doslo je do neocekivane greške na API-Gateway-u.";
            log.error("Neobrađen izuzetak na gateway-u", error);
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().format(FORMATTER));
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", exchange.getRequest().getPath().value());

        return write(response, body);
    }

    private String describeRoutingProblem(HttpStatus status, String reason, ServerWebExchange exchange) {
        if (status == HttpStatus.NOT_FOUND) {
            return "Putanja " + exchange.getRequest().getPath().value()
                    + " ne odgovara nijednoj ruti API-Gateway-a.";
        }
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            return "Ciljni mikroservis nije registrovan na naming serveru ili trenutno nije dostupan.";
        }
        return reason == null ? status.getReasonPhrase() : reason;
    }

    private Mono<Void> write(ServerHttpResponse response, Map<String, Object> body) {
        byte[] bytes;
        try {
            bytes = mapper.writeValueAsBytes(body);
        } catch (Exception ex) {
            bytes = "{\"message\":\"Greška na API-Gateway-u.\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
