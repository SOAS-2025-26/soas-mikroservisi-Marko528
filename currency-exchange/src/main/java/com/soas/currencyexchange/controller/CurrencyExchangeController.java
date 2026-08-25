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

@RestController
@RequestMapping("/currency-exchange")
public class CurrencyExchangeController {
    private final CurrencyExchangeService service;

    public CurrencyExchangeController(CurrencyExchangeService service) {
        this.service = service;
    }

    @GetMapping("/from/{from}/to/{to}")
    public ExchangeRateDto retrieveExchangeValue(@PathVariable String from, @PathVariable String to) {
        return service.retrieveExchangeValue(from, to);
    }

    @GetMapping("/currencies")
    public List<String> supportedCurrencies() {
        return service.supportedCurrencies();
    }

    @GetMapping("/rates")
    public Map<String, BigDecimal> allRates(@RequestParam(defaultValue = "EUR") String base) {
        return service.allRates(base);
    }
}
