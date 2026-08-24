package com.soas.library.proxy;

import com.soas.library.dto.CryptoRateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign proxy ka crypto-exchange mikroservisu (kursevi kripto valuta).
 */
@FeignClient(name = "crypto-exchange")
public interface CryptoExchangeProxy {

    @GetMapping("/crypto-exchange/from/{from}/to/{to}")
    CryptoRateDto retrieveCryptoRate(@PathVariable("from") String from,
                                     @PathVariable("to") String to);

    /** Provera da li je zadati kod jedna od podrzanih kripto valuta. */
    @GetMapping("/crypto-exchange/is-crypto/{code}")
    boolean isCrypto(@PathVariable("code") String code);
}
