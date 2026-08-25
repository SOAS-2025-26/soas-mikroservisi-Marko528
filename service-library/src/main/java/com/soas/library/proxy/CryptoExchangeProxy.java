package com.soas.library.proxy;

import com.soas.library.dto.CryptoRateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "crypto-exchange")
public interface CryptoExchangeProxy {
    @GetMapping("/crypto-exchange/from/{from}/to/{to}")
    CryptoRateDto retrieveCryptoRate(@PathVariable("from") String from,
                                     @PathVariable("to") String to);

    @GetMapping("/crypto-exchange/is-crypto/{code}")
    boolean isCrypto(@PathVariable("code") String code);
}
