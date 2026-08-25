package com.soas.tradeservice.controller;

import com.soas.library.dto.TradeResponse;
import com.soas.tradeservice.service.TradeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * REST interfejs trade mikroservisa.
 *
 * Prema specifikaciji, korisnicki zahtev kroz API-Gateway ima oblik:
 *   localhost:8765/trade-service?from=X&to=Y&quantity=Q
 */
@RestController
public class TradeController {

    private final TradeService service;

    public TradeController(TradeService service) {
        this.service = service;
    }

    @GetMapping("/trade-service")
    public TradeResponse trade(@RequestParam String from,
                               @RequestParam String to,
                               @RequestParam BigDecimal quantity) {
        return service.trade(from, to, quantity);
    }
}
