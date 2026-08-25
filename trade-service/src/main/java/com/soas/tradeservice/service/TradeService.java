package com.soas.tradeservice.service;

import com.soas.library.dto.BankAccountDto;
import com.soas.library.dto.ConversionResponse;
import com.soas.library.dto.CryptoRateDto;
import com.soas.library.dto.CryptoWalletDto;
import com.soas.library.dto.Role;
import com.soas.library.dto.TradeResponse;
import com.soas.library.proxy.BankAccountProxy;
import com.soas.library.proxy.CryptoWalletProxy;
import com.soas.library.proxy.CurrencyConversionProxy;
import com.soas.library.security.AuthContext;
import com.soas.util.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

/**
 * Razmena obicnih (fiat) i crypto valuta.
 *
 * Podrzane su tri vrste razmene:
 *  - <b>crypto u crypto</b>: sa novcanika se skida polazna, a dodaje ciljna kripto valuta,
 *  - <b>fiat u crypto</b>: sa bankovnog racuna se skida fiat iznos, a na novcanik dodaje kripto,
 *  - <b>crypto u fiat</b>: sa novcanika se skida kripto, a na bankovni racun dodaje fiat iznos.
 *
 * Kripto valute je moguce kupovati i prodavati iskljucivo za USD i EUR. Ako se
 * u zahtevu nadje neka druga fiat valuta, ona se prvo konvertuje u EUR (odnosno
 * iz EUR u trazenu valutu) kroz currency-conversion mikroservis.
 *
 * Autorizacija:
 *  - OWNER ne moze da pristupi ovom servisu
 *  - ADMIN ne moze da pristupi ovom servisu
 *  - USER je autorizovan za upotrebu ovog servisa
 */
@Service
public class TradeService {

    private static final Logger log = LoggerFactory.getLogger(TradeService.class);

    /** Jedine fiat valute za koje je dozvoljena direktna kupovina i prodaja kripto valuta. */
    private static final Set<String> DIRECT_FIAT = Set.of("USD", "EUR");

    private static final int FIAT_SCALE = 2;
    private static final int CRYPTO_SCALE = 8;

    private final ResilientRateService rateService;
    private final CryptoWalletProxy walletProxy;
    private final BankAccountProxy bankAccountProxy;
    private final CurrencyConversionProxy conversionProxy;
    private final AuthContext auth;
    private final String pivotCurrency;
    private final String environment;

    public TradeService(ResilientRateService rateService,
                        CryptoWalletProxy walletProxy,
                        BankAccountProxy bankAccountProxy,
                        CurrencyConversionProxy conversionProxy,
                        AuthContext auth,
                        @Value("${trade.pivot-currency:EUR}") String pivotCurrency,
                        @Value("${server.port}") String port) {
        this.rateService = rateService;
        this.walletProxy = walletProxy;
        this.bankAccountProxy = bankAccountProxy;
        this.conversionProxy = conversionProxy;
        this.auth = auth;
        this.pivotCurrency = pivotCurrency.trim().toUpperCase();
        this.environment = "trade-service na portu " + port;
    }

    /**
     * Glavna ulazna tacka: prepoznaje vrstu razmene i prosledjuje je
     * odgovarajucoj metodi.
     */
    public TradeResponse trade(String from, String to, BigDecimal quantity) {
        auth.requireAnyOf(Role.USER);
        String email = auth.currentEmail();

        String source = normalize(from);
        String target = normalize(to);
        BigDecimal amount = requirePositive(quantity);

        if (source.equals(target)) {
            throw new InvalidRequestException(
                    "Polazna i ciljna valuta su iste (" + source + ") - razmena nema efekta.");
        }

        boolean sourceIsCrypto = rateService.isCrypto(source);
        boolean targetIsCrypto = rateService.isCrypto(target);

        if (sourceIsCrypto && targetIsCrypto) {
            return cryptoToCrypto(email, source, target, amount);
        }
        if (!sourceIsCrypto && targetIsCrypto) {
            return fiatToCrypto(email, source, target, amount);
        }
        if (sourceIsCrypto) {
            return cryptoToFiat(email, source, target, amount);
        }
        throw new InvalidRequestException(
                "Razmena dve fiat valute (" + source + " u " + target + ") se ne obavlja preko trade servisa. "
                        + "Za tu namenu koristite currency-conversion servis.");
    }

    // ------------------------------------------------------------------
    // Razmena crypto u crypto
    // ------------------------------------------------------------------

    private TradeResponse cryptoToCrypto(String email, String source, String target, BigDecimal amount) {
        CryptoRateDto rate = rateService.cryptoRate(source, target);
        BigDecimal converted = scale(amount.multiply(rate.getConversionMultiple()), CRYPTO_SCALE);
        requireNonZero(converted, target, rate.getConversionMultiple());

        // Skidanje sa novcanika prvo - ono baca gresku ako nema dovoljno sredstava.
        walletProxy.debit(email, source, amount);
        List<CryptoWalletDto> wallet = walletProxy.credit(email, target, converted);

        log.info("Razmena crypto u crypto za {}: {} {} -> {} {}", email, amount, source, converted, target);

        TradeResponse response = baseResponse(email, "CRYPTO_U_CRYPTO", source, target, amount,
                converted, rate.getConversionMultiple(), rate.getEnvironment());
        response.setCryptoWallet(wallet);
        response.setMessage("Uspesno je izvrsena razmena " + source + ": " + amount
                + " za " + target + ": " + converted);
        return response;
    }

    // ------------------------------------------------------------------
    // Razmena fiat u crypto
    // ------------------------------------------------------------------

    private TradeResponse fiatToCrypto(String email, String source, String target, BigDecimal amount) {
        String payCurrency = source;
        BigDecimal payAmount = amount;
        String intermediateNote = "";

        // Kripto se kupuje samo za USD ili EUR - ostale valute se prvo konvertuju.
        if (!DIRECT_FIAT.contains(source)) {
            ConversionResponse conversion = conversionProxy.convert(source, pivotCurrency, amount);
            payCurrency = pivotCurrency;
            payAmount = conversion.getConvertedAmount();
            intermediateNote = " (medjukorak: " + source + ": " + amount + " zamenjeno za "
                    + pivotCurrency + ": " + payAmount + ")";
            log.info("Medjukorak za {}: {} {} -> {} {}", email, amount, source, payAmount, pivotCurrency);
        }

        CryptoRateDto rate = rateService.cryptoRate(payCurrency, target);
        BigDecimal converted = scale(payAmount.multiply(rate.getConversionMultiple()), CRYPTO_SCALE);
        requireNonZero(converted, target, rate.getConversionMultiple());

        bankAccountProxy.debit(email, payCurrency, payAmount);
        List<CryptoWalletDto> wallet = walletProxy.credit(email, target, converted);

        log.info("Razmena fiat u crypto za {}: {} {} -> {} {}", email, payAmount, payCurrency, converted, target);

        TradeResponse response = baseResponse(email, "FIAT_U_CRYPTO", source, target, amount,
                converted, rate.getConversionMultiple(), rate.getEnvironment());
        response.setCryptoWallet(wallet);
        response.setMessage("Uspesno je izvrsena razmena " + source + ": " + amount
                + " za " + target + ": " + converted + intermediateNote);
        return response;
    }

    // ------------------------------------------------------------------
    // Razmena crypto u fiat
    // ------------------------------------------------------------------

    private TradeResponse cryptoToFiat(String email, String source, String target, BigDecimal amount) {
        // Kripto se prodaje samo za USD ili EUR; ostale valute se dobijaju naknadnom konverzijom.
        String receiveCurrency = DIRECT_FIAT.contains(target) ? target : pivotCurrency;

        CryptoRateDto rate = rateService.cryptoRate(source, receiveCurrency);
        BigDecimal converted = scale(amount.multiply(rate.getConversionMultiple()), FIAT_SCALE);
        requireNonZero(converted, receiveCurrency, rate.getConversionMultiple());

        walletProxy.debit(email, source, amount);
        List<BankAccountDto> account = bankAccountProxy.credit(email, receiveCurrency, converted);

        BigDecimal finalAmount = converted;
        String intermediateNote = "";

        if (!receiveCurrency.equals(target)) {
            ConversionResponse conversion = conversionProxy.convert(receiveCurrency, target, converted);
            account = conversion.getBankAccount();
            finalAmount = conversion.getConvertedAmount();
            intermediateNote = " (medjukorak: " + source + ": " + amount + " zamenjeno za "
                    + receiveCurrency + ": " + converted + ")";
        }

        log.info("Razmena crypto u fiat za {}: {} {} -> {} {}", email, amount, source, finalAmount, target);

        TradeResponse response = baseResponse(email, "CRYPTO_U_FIAT", source, target, amount,
                finalAmount, rate.getConversionMultiple(), rate.getEnvironment());
        response.setBankAccount(account);
        response.setMessage("Uspesno je izvrsena razmena " + source + ": " + amount
                + " za " + target + ": " + finalAmount + intermediateNote);
        return response;
    }

    // ------------------------------------------------------------------

    private TradeResponse baseResponse(String email, String tradeType, String source, String target,
                                       BigDecimal quantity, BigDecimal converted,
                                       BigDecimal rate, String rateEnvironment) {
        TradeResponse response = new TradeResponse();
        response.setEmail(email);
        response.setTradeType(tradeType);
        response.setFrom(source);
        response.setTo(target);
        response.setQuantity(quantity);
        response.setConversionMultiple(rate);
        response.setConvertedAmount(converted);
        response.setEnvironment(environment + " | kurs: " + rateEnvironment);
        return response;
    }

    private void requireNonZero(BigDecimal converted, String target, BigDecimal rate) {
        if (converted.signum() <= 0) {
            throw new InvalidRequestException(
                    "Iznos za razmenu je premali - po kursu " + rate
                            + " rezultat bi bio 0 " + target + ".");
        }
    }

    private BigDecimal scale(BigDecimal value, int scale) {
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidRequestException("Kod valute je obavezan.");
        }
        String normalized = code.trim().toUpperCase();
        if (!normalized.matches("[A-Z0-9]{2,5}")) {
            throw new InvalidRequestException(
                    "Kod valute mora imati 2 do 5 slova ili cifara (npr. EUR, USD, BTC, ETH), "
                            + "a prosledjeno je: " + code);
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
