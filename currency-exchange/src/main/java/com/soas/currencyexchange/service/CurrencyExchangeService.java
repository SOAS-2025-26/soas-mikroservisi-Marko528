package com.soas.currencyexchange.service;

import com.soas.currencyexchange.dto.ExternalFiatRatesResponse;
import com.soas.currencyexchange.proxy.ExternalFiatRatesProxy;
import com.soas.library.dto.ExchangeRateDto;
import com.soas.util.exception.ExternalServiceException;
import com.soas.util.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CurrencyExchangeService {
    private static final Logger log = LoggerFactory.getLogger(CurrencyExchangeService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final ExternalFiatRatesProxy proxy;
    private final String environment;
    private final Map<String, CachedRates> cache = new ConcurrentHashMap<>();

    public CurrencyExchangeService(ExternalFiatRatesProxy proxy,
                                   @Value("${server.port}") String port) {
        this.proxy = proxy;
        this.environment = "currency-exchange na portu " + port;
    }

    public ExchangeRateDto retrieveExchangeValue(String from, String to) {
        String base = normalize(from);
        String target = normalize(to);

        if (base.equals(target)) {
            return new ExchangeRateDto(base, target, BigDecimal.ONE, environment);
        }

        Map<String, BigDecimal> rates = ratesFor(base);
        BigDecimal rate = rates.get(target);
        if (rate == null) {
            throw new InvalidRequestException(
                    "Valuta " + target + " nije podrzana od strane eksternog servisa kurseva.");
        }
        return new ExchangeRateDto(base, target, rate, environment);
    }

    public List<String> supportedCurrencies() {
        return ratesFor("EUR").keySet().stream().sorted().toList();
    }

    public Map<String, BigDecimal> allRates(String base) {
        return ratesFor(normalize(base));
    }

    private Map<String, BigDecimal> ratesFor(String base) {
        CachedRates cached = cache.get(base);
        if (cached != null && !cached.isExpired()) {
            return cached.rates();
        }

        ExternalFiatRatesResponse response;
        try {
            response = proxy.latestRates(base);
        } catch (Exception ex) {
            log.warn("Eksterni API kurseva nije dostupan za valutu {}: {}", base, ex.getMessage());
            if (cached != null) {
                log.info("Koriste se poslednji poznati kursevi iz kesa za valutu {}", base);
                return cached.rates();
            }
            throw new ExternalServiceException(
                    "Eksterni servis sa kursevima fiat valuta trenutno nije dostupan. Pokušajte ponovo kasnije.");
        }

        if (response == null || !response.isSuccess() || response.getRates() == null) {
            throw new InvalidRequestException(
                    "Valuta " + base + " nije podrzana od strane eksternog servisa kurseva.");
        }

        cache.put(base, new CachedRates(response.getRates(), Instant.now()));
        return response.getRates();
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidRequestException("Kod valute je obavezan.");
        }
        String normalized = code.trim().toUpperCase();
        if (!normalized.matches("[A-Z]{3}")) {
            throw new InvalidRequestException(
                    "Kod valute mora imati tacno tri slova (npr. EUR, USD, RSD), a prosleđeno je: " + code);
        }
        return normalized;
    }

    private record CachedRates(Map<String, BigDecimal> rates, Instant fetchedAt) {
        boolean isExpired() {
            return Duration.between(fetchedAt, Instant.now()).compareTo(CACHE_TTL) > 0;
        }
    }
}
