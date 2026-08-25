package com.soas.cryptoexchange.proxy;

import com.soas.cryptoexchange.dto.ExternalCryptoRatesResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "external-crypto-rates-api", url = "${external.crypto-api.url}")
public interface ExternalCryptoRatesProxy {
    @GetMapping("/v2/exchange-rates")
    ExternalCryptoRatesResponse exchangeRates(@RequestParam("currency") String currency);
}
