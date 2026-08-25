package com.soas.currencyconversion.controller;

import com.soas.currencyconversion.service.CurrencyConversionService;
import com.soas.library.dto.ConversionResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
public class CurrencyConversionController {
    private final CurrencyConversionService service;

    public CurrencyConversionController(CurrencyConversionService service) {
        this.service = service;
    }

    @GetMapping("/currency-conversion")
    public ConversionResponse convert(@RequestParam String from,
                                      @RequestParam String to,
                                      @RequestParam BigDecimal quantity) {
        return service.convert(from, to, quantity);
    }
}
