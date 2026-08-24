package com.soas.currencyexchange.controller;

import com.soas.currencyexchange.service.CurrencyExchangeService;
import com.soas.library.dto.ExchangeRateDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST interfejs currency-exchange mikroservisa.
 *
 * Autorizacija: korisnik sa bilo kojom ulogom moze da pristupi ovom servisu,
 * pa se ovde ne radi nikakva dodatna provera uloge.
 */
@RestController
@RequestMapping("/currency-exchange")
public class CurrencyExchangeController {

    private final CurrencyExchangeService service;

    public CurrencyExchangeController(CurrencyExchangeService service) {
        this.service = service;
    }

    /** Kurs razmene izmedju dve fiat valute. */
    @GetMapping("/from/{from}/to/{to}")
    public ExchangeRateDto retrieveExchangeValue(@PathVariable String from, @PathVariable String to) {
        return service.retrieveExchangeValue(from, to);
    }

    /** Spisak podrzanih fiat valuta. */
    @GetMapping("/currencies")
    public List<String> supportedCurrencies() {
        return service.supportedCurrencies();
    }

    /** Svi kursevi u odnosu na zadatu baznu valutu (podrazumevano EUR). */
    @GetMapping("/rates")
    public Map<String, BigDecimal> allRates(@RequestParam(defaultValue = "EUR") String base) {
        return service.allRates(base);
    }
}
