package com.soas.library.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TradeResponse {
    private String email;
    private String tradeType;
    private String from;
    private String to;
    private BigDecimal quantity;
    private BigDecimal conversionMultiple;
    private BigDecimal convertedAmount;
    private String environment;
    private String message;
    private List<BankAccountDto> bankAccount;
    private List<CryptoWalletDto> cryptoWallet;

    public TradeResponse() {
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTradeType() { return tradeType; }
    public void setTradeType(String tradeType) { this.tradeType = tradeType; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getConversionMultiple() { return conversionMultiple; }
    public void setConversionMultiple(BigDecimal conversionMultiple) { this.conversionMultiple = conversionMultiple; }

    public BigDecimal getConvertedAmount() { return convertedAmount; }
    public void setConvertedAmount(BigDecimal convertedAmount) { this.convertedAmount = convertedAmount; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<BankAccountDto> getBankAccount() { return bankAccount; }
    public void setBankAccount(List<BankAccountDto> bankAccount) { this.bankAccount = bankAccount; }

    public List<CryptoWalletDto> getCryptoWallet() { return cryptoWallet; }
    public void setCryptoWallet(List<CryptoWalletDto> cryptoWallet) { this.cryptoWallet = cryptoWallet; }
}
