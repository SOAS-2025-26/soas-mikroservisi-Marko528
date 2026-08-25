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

@RestController
@RequestMapping("/crypto-exchange")
public class CryptoExchangeController {
    private final CryptoExchangeService service;

    public CryptoExchangeController(CryptoExchangeService service) {
        this.service = service;
    }

    @GetMapping("/from/{from}/to/{to}")
    public CryptoRateDto retrieveCryptoRate(@PathVariable String from, @PathVariable String to) {
        return service.retrieveCryptoRate(from, to);
    }

    @GetMapping("/currencies")
    public List<String> supportedCurrencies() {
        return service.supportedCryptoCurrencies();
    }

    @GetMapping("/prices")
    public Map<String, BigDecimal> prices(@RequestParam(defaultValue = "USD") String currency) {
        return service.priceList(currency);
    }

    @GetMapping("/is-crypto/{code}")
    public boolean isCrypto(@PathVariable String code) {
        return service.isSupportedCrypto(code);
    }
}
