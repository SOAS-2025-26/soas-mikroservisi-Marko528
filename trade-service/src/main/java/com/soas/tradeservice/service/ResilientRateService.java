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

/**
 * Omotac oko poziva ka crypto-exchange mikroservisu sa fault tolerance
 * mehanizmima (dodatne specifikacije projekta).
 *
 * Primenjena su oba mehanizma:
 *  - <b>retry</b>: privremeni pad mreze ili eksternog API-ja se automatski
 *    ponavlja do tri puta sa eksponencijalnim cekanjem,
 *  - <b>circuit breaker</b>: ako servis uporno pada, kolo se otvara i naredni
 *    pozivi se odmah odbijaju umesto da cekaju timeout, sto sprecava
 *    lancano rusenje ostalih servisa.
 *
 * Kada nijedan pokusaj ne uspe ili je kolo otvoreno, poziva se fallback metoda
 * koja korisniku vraca jasnu poruku umesto tehnicke greske.
 */
@Service
public class ResilientRateService {

    private static final Logger log = LoggerFactory.getLogger(ResilientRateService.class);
    private static final String INSTANCE = "cryptoExchange";

    private final CryptoExchangeProxy cryptoExchangeProxy;

    public ResilientRateService(CryptoExchangeProxy cryptoExchangeProxy) {
        this.cryptoExchangeProxy = cryptoExchangeProxy;
    }

    /** Kurs za par u kome ucestvuje kripto valuta. */
    @Retry(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE, fallbackMethod = "cryptoRateFallback")
    public CryptoRateDto cryptoRate(String from, String to) {
        return cryptoExchangeProxy.retrieveCryptoRate(from, to);
    }

    @SuppressWarnings("unused")
    private CryptoRateDto cryptoRateFallback(String from, String to, Throwable cause) {
        // Poslovne greske se ne "gutaju" - njih korisnik mora da vidi onakve kakve jesu.
        if (cause instanceof InvalidRequestException invalid) {
            throw invalid;
        }
        log.error("Kurs za par {}/{} nije pribavljen: {}", from, to, cause.toString());
        throw new ExternalServiceException(
                "Servis sa kursevima kripto valuta trenutno nije dostupan, pa razmena "
                        + from + " u " + to + " nije moguca. Pokusajte ponovo za nekoliko trenutaka.");
    }

    /** Provera da li je zadati kod podrzana kripto valuta. */
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
                        + code + " nije moguce utvrditi. Pokusajte ponovo za nekoliko trenutaka.");
    }
}
