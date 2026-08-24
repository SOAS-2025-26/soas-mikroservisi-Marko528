package com.soas.currencyconversion.service;

import com.soas.library.dto.BankAccountDto;
import com.soas.library.dto.ConversionResponse;
import com.soas.library.dto.ExchangeRateDto;
import com.soas.library.dto.Role;
import com.soas.library.proxy.BankAccountProxy;
import com.soas.library.proxy.CurrencyExchangeProxy;
import com.soas.library.security.AuthContext;
import com.soas.util.exception.InsufficientFundsException;
import com.soas.util.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Razmena fiat valuta nad bankovnim racunom prijavljenog korisnika.
 *
 * Tok obrade jednog zahteva:
 *  1. provera da li je korisnik autorizovan (samo uloga USER),
 *  2. provera da li na racunu ima dovoljno sredstava u polaznoj valuti,
 *  3. pribavljanje kursa od currency-exchange servisa,
 *  4. skidanje polazne i dodavanje ciljne valute na bankovni racun,
 *  5. vracanje novog stanja racuna uz tekstualni izvestaj o transakciji.
 *
 * Autorizacija:
 *  - OWNER ne moze da pristupi ovom servisu
 *  - ADMIN ne moze da pristupi ovom servisu
 *  - USER je autorizovan za upotrebu ovog servisa
 */
@Service
public class CurrencyConversionService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyConversionService.class);
    private static final int SCALE = 2;

    private final CurrencyExchangeProxy exchangeProxy;
    private final BankAccountProxy bankAccountProxy;
    private final AuthContext auth;
    private final String environment;

    public CurrencyConversionService(CurrencyExchangeProxy exchangeProxy,
                                     BankAccountProxy bankAccountProxy,
                                     AuthContext auth,
                                     @Value("${server.port}") String port) {
        this.exchangeProxy = exchangeProxy;
        this.bankAccountProxy = bankAccountProxy;
        this.auth = auth;
        this.environment = "currency-conversion na portu " + port;
    }

    /**
     * Razmenjuje {@code quantity} jedinica valute {@code from} u valutu {@code to}
     * na bankovnom racunu prijavljenog korisnika.
     */
    public ConversionResponse convert(String from, String to, BigDecimal quantity) {
        auth.requireAnyOf(Role.USER);
        String email = auth.currentEmail();

        String source = normalize(from);
        String target = normalize(to);
        BigDecimal amount = requirePositive(quantity);

        if (source.equals(target)) {
            throw new InvalidRequestException(
                    "Polazna i ciljna valuta su iste (" + source + ") - razmena nema efekta.");
        }

        // Valuta koja se razmenjuje mora postojati na korisnikovom racunu.
        BigDecimal available = balanceOf(email, source);
        if (available.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Nedovoljno sredstava: na racunu je dostupno " + available + " " + source
                            + ", a za razmenu je trazeno " + amount + " " + source + ".");
        }

        // Kurs se pribavlja od currency-exchange mikroservisa preko Feign klijenta.
        ExchangeRateDto rate = exchangeProxy.retrieveExchangeValue(source, target);
        BigDecimal converted = amount.multiply(rate.getConversionMultiple())
                .setScale(SCALE, RoundingMode.HALF_UP);

        if (converted.signum() <= 0) {
            throw new InvalidRequestException(
                    "Iznos za razmenu je premali - po kursu " + rate.getConversionMultiple()
                            + " rezultat bi bio 0 " + target + ".");
        }

        // Izmena stanja na bankovnom racunu: skidanje polazne, dodavanje ciljne valute.
        bankAccountProxy.debit(email, source, amount);
        List<BankAccountDto> account = bankAccountProxy.credit(email, target, converted);

        log.info("Razmena za {}: {} {} -> {} {} (kurs {})", email, amount, source, converted, target,
                rate.getConversionMultiple());

        return buildResponse(email, source, target, amount, converted, rate, account);
    }

    private BigDecimal balanceOf(String email, String currencyCode) {
        return bankAccountProxy.findByEmail(email).stream()
                .filter(item -> item.getCurrencyCode().equalsIgnoreCase(currencyCode))
                .map(BankAccountDto::getAmount)
                .findFirst()
                .orElseThrow(() -> new InsufficientFundsException(
                        "Na bankovnom racunu ne postoje sredstva u valuti " + currencyCode + "."));
    }

    private ConversionResponse buildResponse(String email, String source, String target,
                                             BigDecimal amount, BigDecimal converted,
                                             ExchangeRateDto rate, List<BankAccountDto> account) {
        ConversionResponse response = new ConversionResponse();
        response.setEmail(email);
        response.setFrom(source);
        response.setTo(target);
        response.setQuantity(amount);
        response.setConversionMultiple(rate.getConversionMultiple());
        response.setConvertedAmount(converted);
        response.setEnvironment(environment + " | kurs: " + rate.getEnvironment());
        response.setBankAccount(account);
        response.setMessage("Uspesno je izvrsena razmena " + source + ": " + amount
                + " za " + target + ": " + converted);
        return response;
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidRequestException("Kod valute je obavezan.");
        }
        String normalized = code.trim().toUpperCase();
        if (!normalized.matches("[A-Z]{3}")) {
            throw new InvalidRequestException(
                    "Kod valute mora imati tacno tri slova (npr. EUR, USD, RSD), a prosledjeno je: " + code);
        }
        return normalized;
    }

    private BigDecimal requirePositive(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new InvalidRequestException("Kolicina za razmenu mora biti veca od nule.");
        }
        return quantity;
    }
}
