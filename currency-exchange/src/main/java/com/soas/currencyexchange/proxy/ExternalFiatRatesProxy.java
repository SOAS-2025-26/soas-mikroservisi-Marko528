package com.soas.currencyexchange.proxy;

import com.soas.currencyexchange.dto.ExternalFiatRatesResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "external-fiat-rates-api", url = "${external.fiat-api.url}")
public interface ExternalFiatRatesProxy {
    @GetMapping("/v6/latest/{base}")
    ExternalFiatRatesResponse latestRates(@PathVariable("base") String base);
}
