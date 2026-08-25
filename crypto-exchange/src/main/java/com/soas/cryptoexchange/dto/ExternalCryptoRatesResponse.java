package com.soas.cryptoexchange.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalCryptoRatesResponse {
    private Data data;

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    public boolean isSuccess() {
        return data != null && data.getRates() != null && !data.getRates().isEmpty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String currency;
        private Map<String, BigDecimal> rates;

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public Map<String, BigDecimal> getRates() { return rates; }
        public void setRates(Map<String, BigDecimal> rates) { this.rates = rates; }
    }
}
