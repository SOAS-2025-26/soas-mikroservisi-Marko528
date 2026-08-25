package com.soas.cryptoexchange.service;

import com.soas.cryptoexchange.dto.ExternalCryptoRatesResponse;
import com.soas.cryptoexchange.proxy.ExternalCryptoRatesProxy;
import com.soas.library.dto.CryptoRateDto;
import com.soas.util.exception.ExternalServiceException;
import com.soas.util.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CryptoExchangeService {
    private static final Logger log = LoggerFactory.getLogger(CryptoExchangeService.class);
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private static final Set<String> SUPPORTED_CRYPTO = new LinkedHashSet<>(List.of(
            "BTC", "ETH", "USDT", "USDC", "BNB", "SOL", "XRP", "ADA", "DOGE", "TRX",
            "DOT", "MATIC", "LTC", "AVAX", "LINK", "XLM", "BCH", "UNI", "ATOM", "ETC"));

    private final ExternalCryptoRatesProxy proxy;
    private final String environment;
    private final Map<String, CachedRates> cache = new ConcurrentHashMap<>();

    public CryptoExchangeService(ExternalCryptoRatesProxy proxy,
                                 @Value("${server.port}") String port) {
        this.proxy = proxy;
        this.environment = "crypto-exchange na portu " + port;
    }

    public CryptoRateDto retrieveCryptoRate(String from, String to) {
        String base = normalize(from);
        String target = normalize(to);

        if (base.equals(target)) {
            return new CryptoRateDto(base, target, BigDecimal.ONE, environment);
        }

        Map<String, BigDecimal> rates = ratesFor(base);
        BigDecimal rate = rates.get(target);
        if (rate == null || rate.signum() == 0) {
            throw new InvalidRequestException(
                    "Kurs za par " + base + "/" + target + " nije dostupan na eksternom servisu.");
        }
        return new CryptoRateDto(base, target, rate, environment);
    }

    public List<String> supportedCryptoCurrencies() {
        return List.copyOf(SUPPORTED_CRYPTO);
    }

    public boolean isSupportedCrypto(String code) {
        return code != null && SUPPORTED_CRYPTO.contains(code.trim().toUpperCase());
    }

    public Map<String, BigDecimal> priceList(String fiatCurrency) {
        String fiat = normalize(fiatCurrency);
        Map<String, BigDecimal> prices = new java.util.LinkedHashMap<>();
        for (String crypto : SUPPORTED_CRYPTO) {
            try {
                prices.put(crypto, ratesFor(crypto).get(fiat));
            } catch (RuntimeException ex) {
                log.debug("Preskačem {} - cena nije dostupna: {}", crypto, ex.getMessage());
            }
        }
        prices.values().removeIf(java.util.Objects::isNull);
        if (prices.isEmpty()) {
            throw new ExternalServiceException(
                    "Eksterni servis sa kursevima kripto valuta trenutno nije dostupan.");
        }
        return prices;
    }

    private Map<String, BigDecimal> ratesFor(String base) {
        CachedRates cached = cache.get(base);
        if (cached != null && !cached.isExpired()) {
            return cached.rates();
        }

        ExternalCryptoRatesResponse response;
        try {
            response = proxy.exchangeRates(base);
        } catch (Exception ex) {
            log.warn("Eksterni API kripto kurseva nije dostupan za {}: {}", base, ex.getMessage());
            if (cached != null) {
                log.info("Koriste se poslednji poznati kursevi iz kesa za {}", base);
                return cached.rates();
            }
            throw new ExternalServiceException(
                    "Eksterni servis sa kursevima kripto valuta trenutno nije dostupan. Pokušajte ponovo kasnije.");
        }

        if (response == null || !response.isSuccess()) {
            throw new InvalidRequestException(
                    "Valuta " + base + " nije podrzana od strane eksternog servisa kripto kurseva.");
        }

        cache.put(base, new CachedRates(response.getData().getRates(), Instant.now()));
        return response.getData().getRates();
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidRequestException("Kod valute je obavezan.");
        }
        return code.trim().toUpperCase();
    }

    private record CachedRates(Map<String, BigDecimal> rates, Instant fetchedAt) {
        boolean isExpired() {
            return Duration.between(fetchedAt, Instant.now()).compareTo(CACHE_TTL) > 0;
        }
    }
}
