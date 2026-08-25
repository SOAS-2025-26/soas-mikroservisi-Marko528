package com.soas.tradeservice.service;

import com.soas.library.dto.CryptoRateDto;
import com.soas.library.proxy.CryptoExchangeProxy;
import com.soas.util.exception.ExternalServiceException;
import com.soas.util.exception.InvalidRequestException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ResilientRateService {
    private static final Logger log = LoggerFactory.getLogger(ResilientRateService.class);
    private static final String INSTANCE = "cryptoExchange";

    private final CryptoExchangeProxy cryptoExchangeProxy;

    public ResilientRateService(CryptoExchangeProxy cryptoExchangeProxy) {
        this.cryptoExchangeProxy = cryptoExchangeProxy;
    }

    @Retry(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE, fallbackMethod = "cryptoRateFallback")
    public CryptoRateDto cryptoRate(String from, String to) {
        return cryptoExchangeProxy.retrieveCryptoRate(from, to);
    }

    @SuppressWarnings("unused")
    private CryptoRateDto cryptoRateFallback(String from, String to, Throwable cause) {
        if (cause instanceof InvalidRequestException invalid) {
            throw invalid;
        }
        log.error("Kurs za par {}/{} nije pribavljen: {}", from, to, cause.toString());
        throw new ExternalServiceException(
                "Servis sa kursevima kripto valuta trenutno nije dostupan, pa razmena "
                        + from + " u " + to + " nije moguća. Pokušajte ponovo za nekoliko trenutaka.");
    }

    @Retry(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE, fallbackMethod = "isCryptoFallback")
    public boolean isCrypto(String code) {
        return cryptoExchangeProxy.isCrypto(code);
    }

    @SuppressWarnings("unused")
    private boolean isCryptoFallback(String code, Throwable cause) {
        log.error("Provera da li je {} kripto valuta nije uspela: {}", code, cause.toString());
        throw new ExternalServiceException(
                "Servis sa kursevima kripto valuta trenutno nije dostupan, pa vrstu valute "
                        + code + " nije moguće utvrditi. Pokušajte ponovo za nekoliko trenutaka.");
    }
}
