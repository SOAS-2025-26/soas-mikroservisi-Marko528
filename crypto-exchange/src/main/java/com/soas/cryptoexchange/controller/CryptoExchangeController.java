package com.soas.cryptoexchange.controller;

import com.soas.cryptoexchange.service.CryptoExchangeService;
import com.soas.library.dto.CryptoRateDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST interfejs crypto-exchange mikroservisa.
 *
 * Autorizacija: korisnik sa bilo kojom ulogom moze da pristupi ovom servisu.
 */
@RestController
@RequestMapping("/crypto-exchange")
public class CryptoExchangeController {

    private final CryptoExchangeService service;

    public CryptoExchangeController(CryptoExchangeService service) {
        this.service = service;
    }

    /** Kurs razmene za par u kome ucestvuje kripto valuta. */
    @GetMapping("/from/{from}/to/{to}")
    public CryptoRateDto retrieveCryptoRate(@PathVariable String from, @PathVariable String to) {
        return service.retrieveCryptoRate(from, to);
    }

    /** Spisak podrzanih kripto valuta. */
    @GetMapping("/currencies")
    public List<String> supportedCurrencies() {
        return service.supportedCryptoCurrencies();
    }

    /** Cene podrzanih kripto valuta u zadatoj fiat valuti (podrazumevano USD). */
    @GetMapping("/prices")
    public Map<String, BigDecimal> prices(@RequestParam(defaultValue = "USD") String currency) {
        return service.priceList(currency);
    }

    /** Provera da li je zadati kod podrzana kripto valuta - koristi ga trade-service. */
    @GetMapping("/is-crypto/{code}")
    public boolean isCrypto(@PathVariable String code) {
        return service.isSupportedCrypto(code);
    }
}
