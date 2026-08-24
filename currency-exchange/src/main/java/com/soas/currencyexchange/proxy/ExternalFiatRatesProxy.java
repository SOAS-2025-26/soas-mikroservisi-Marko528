package com.soas.currencyexchange.proxy;

import com.soas.currencyexchange.dto.ExternalFiatRatesResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign klijent ka eksternom API-ju za kurseve fiat valuta.
 *
 * Koristi se javni endpoint https://open.er-api.com/v6/latest/{base} koji
 * ne zahteva API kljuc i pokriva vise od 160 valuta (ukljucujuci RSD).
 */
@FeignClient(name = "external-fiat-rates-api", url = "${external.fiat-api.url}")
public interface ExternalFiatRatesProxy {

    @GetMapping("/v6/latest/{base}")
    ExternalFiatRatesResponse latestRates(@PathVariable("base") String base);
}
