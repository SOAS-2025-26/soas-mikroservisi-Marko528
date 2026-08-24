package com.soas.library.proxy;

import com.soas.library.dto.ConversionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * Feign proxy ka currency-conversion mikroservisu.
 *
 * Koristi ga trade-service kada zahtev sadrzi fiat valutu razlicitu od USD/EUR,
 * pa je prvo potrebno konvertovati je u dolar ili euro.
 */
@FeignClient(name = "currency-conversion")
public interface CurrencyConversionProxy {

    @GetMapping("/currency-conversion")
    ConversionResponse convert(@RequestParam("from") String from,
                               @RequestParam("to") String to,
                               @RequestParam("quantity") BigDecimal quantity);
}
