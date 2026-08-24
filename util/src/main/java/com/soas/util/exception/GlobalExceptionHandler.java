package com.soas.util.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * Globalni handler izuzetaka - zajednicki za sve mikroservise.
 *
 * Svaki izuzetak se pretvara u {@link ErrorResponse}: statusni kod + jasno
 * tekstualno objasnjenje greske. Stack-trace se nikada ne salje korisniku,
 * vec se samo loguje na serveru.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ---------- Poslovna logika ----------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedActionException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalService(ExternalServiceException ex, HttpServletRequest req) {
        log.warn("Greska eksternog servisa: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req);
    }

    // ---------- Medjuservisna (Feign) komunikacija ----------

    /**
     * Greska koju je vratio drugi mikroservis. Prosledjujemo originalni statusni
     * kod i originalnu poruku, umesto da korisniku prikazemo Feign stack-trace.
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeign(FeignException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null || ex.status() <= 0) {
            return build(HttpStatus.SERVICE_UNAVAILABLE,
                    "Ciljni mikroservis trenutno nije dostupan. Pokusajte ponovo kasnije.", req);
        }
        return build(status, extractMessage(ex), req);
    }

    private String extractMessage(FeignException ex) {
        String body = ex.contentUTF8();
        if (body == null || body.isBlank()) {
            return "Greska pri komunikaciji sa drugim mikroservisom (status " + ex.status() + ").";
        }
        try {
            JsonNode node = MAPPER.readTree(body);
            if (node.hasNonNull("message")) {
                return node.get("message").asText();
            }
            if (node.hasNonNull("error")) {
                return node.get("error").asText();
            }
        } catch (Exception ignored) {
            // telo nije JSON - vracamo ga kao obican tekst ispod
        }
        return body.length() > 300 ? body.substring(0, 300) : body;
    }

    // ---------- Validacija i losi zahtevi ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::describe)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "Neispravni podaci: " + message, req);
    }

    private String describe(FieldError error) {
        return error.getField() + " - " + error.getDefaultMessage();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Nedostaje obavezan parametar zahteva: " + ex.getParameterName(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Parametar " + ex.getName() + " ima neispravnu vrednost: " + ex.getValue(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Telo zahteva nije u ispravnom JSON formatu.", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Narusen integritet baze: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "Operacija narusava ogranicenja baze podataka (verovatno duplirana vrednost).", req);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandler(NoHandlerFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Trazena putanja ne postoji: " + ex.getRequestURL(), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    // ---------- Fault tolerance ----------

    /**
     * Circuit breaker je otvoren (resilience4j). Klasa se poredi po imenu kako
     * util modul ne bi morao da zavisi od resilience4j biblioteke.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex, HttpServletRequest req) {
        if ("CallNotPermittedException".equals(ex.getClass().getSimpleName())) {
            return build(HttpStatus.SERVICE_UNAVAILABLE,
                    "Servis je privremeno nedostupan (circuit breaker je otvoren). Pokusajte ponovo za nekoliko trenutaka.", req);
        }
        log.error("Neocekivan runtime izuzetak", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Doslo je do neocekivane greske pri obradi zahteva.", req);
    }

    // ---------- Poslednja linija odbrane ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest req) {
        log.error("Neobradjen izuzetak", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Doslo je do neocekivane greske na serveru.", req);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message == null ? status.getReasonPhrase() : message,
                req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
