package com.soas.library.proxy;

import com.soas.library.dto.ExchangeRateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "currency-exchange")
public interface CurrencyExchangeProxy {
    @GetMapping("/currency-exchange/from/{from}/to/{to}")
    ExchangeRateDto retrieveExchangeValue(@PathVariable("from") String from,
                                          @PathVariable("to") String to);
}
