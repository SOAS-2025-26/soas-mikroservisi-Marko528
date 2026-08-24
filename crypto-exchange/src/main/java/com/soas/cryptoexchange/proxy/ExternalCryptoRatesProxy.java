package com.soas.cryptoexchange.proxy;

import com.soas.cryptoexchange.dto.ExternalCryptoRatesResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign klijent ka eksternom Coinbase API-ju za kurseve kripto valuta.
 *
 * Endpoint https://api.coinbase.com/v2/exchange-rates?currency={code} ne
 * zahteva API kljuc i pokriva sve vece kripto valute, kao i fiat parove.
 */
@FeignClient(name = "external-crypto-rates-api", url = "${external.crypto-api.url}")
public interface ExternalCryptoRatesProxy {

    @GetMapping("/v2/exchange-rates")
    ExternalCryptoRatesResponse exchangeRates(@RequestParam("currency") String currency);
}
