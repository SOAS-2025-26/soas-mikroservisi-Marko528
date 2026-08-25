import { useEffect, useState } from 'react';
import { api } from '../api/client';

export default function RatesPage() {
  const [currencies, setCurrencies] = useState([]);
  const [from, setFrom] = useState('EUR');
  const [to, setTo] = useState('RSD');
  const [fiatRate, setFiatRate] = useState(null);
  const [fiatError, setFiatError] = useState(null);
  const [fiatLoading, setFiatLoading] = useState(false);

  const [priceCurrency, setPriceCurrency] = useState('EUR');
  const [cryptoPrices, setCryptoPrices] = useState(null);
  const [cryptoError, setCryptoError] = useState(null);
  const [cryptoLoading, setCryptoLoading] = useState(false);

  useEffect(() => {
    api.fiatCurrencies().then(setCurrencies).catch(() => setCurrencies([]));
  }, []);

  useEffect(() => {
    loadFiatRate(from, to);
  }, []);

  useEffect(() => {
    loadCryptoPrices(priceCurrency);
  }, [priceCurrency]);

  async function loadFiatRate(source, target) {
    setFiatLoading(true);
    setFiatError(null);
    try {
      setFiatRate(await api.fiatRate(source, target));
    } catch (err) {
      setFiatRate(null);
      setFiatError(err.message);
    } finally {
      setFiatLoading(false);
    }
  }

  async function loadCryptoPrices(currency) {
    setCryptoLoading(true);
    setCryptoError(null);
    try {
      setCryptoPrices(await api.cryptoPrices(currency));
    } catch (err) {
      setCryptoPrices(null);
      setCryptoError(err.message);
    } finally {
      setCryptoLoading(false);
    }
  }

  return (
    <>
      <h1 className="page-title">Kursevi valuta</h1>

      <div className="card">
        <h2>Kurs fiat valuta &middot; currency-exchange</h2>

        <form
          className="form-row"
          onSubmit={(e) => {
            e.preventDefault();
            loadFiatRate(from, to);
          }}
        >
          <div className="field">
            <label htmlFor="from">Iz valute</label>
            <CurrencySelect id="from" value={from} onChange={setFrom} options={currencies} />
          </div>

          <span className="arrow">&rarr;</span>

          <div className="field">
            <label htmlFor="to">U valutu</label>
            <CurrencySelect id="to" value={to} onChange={setTo} options={currencies} />
          </div>

          <button type="submit" disabled={fiatLoading}>
            {fiatLoading ? 'Učitavanje...' : 'Prikaži kurs'}
          </button>
        </form>

        {fiatError && <div className="alert alert-error" style={{ marginTop: 16 }}>{fiatError}</div>}

        {fiatRate && !fiatError && (
          <div className="alert alert-info" style={{ marginTop: 16 }}>
            <strong>
              1 {fiatRate.from} = <span className="numeric">{fiatRate.conversionMultiple}</span>{' '}
              {fiatRate.to}
            </strong>
          </div>
        )}
      </div>

      <div className="card">
        <h2>Cene kripto valuta &middot; crypto-exchange</h2>

        <div className="form-row" style={{ marginBottom: 16 }}>
          <div className="field" style={{ maxWidth: 220 }}>
            <label htmlFor="priceCurrency">Prikaži cene u valuti</label>
            <select
              id="priceCurrency"
              value={priceCurrency}
              onChange={(e) => setPriceCurrency(e.target.value)}
            >
              <option value="EUR">EUR</option>
              <option value="USD">USD</option>
              <option value="RSD">RSD</option>
              <option value="CHF">CHF</option>
              <option value="GBP">GBP</option>
            </select>
          </div>
        </div>

        {cryptoLoading && <div className="loading">Učitavanje cena...</div>}
        {cryptoError && <div className="alert alert-error">{cryptoError}</div>}

        {cryptoPrices && !cryptoLoading && (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Kripto valuta</th>
                  <th>Cena ({priceCurrency})</th>
                </tr>
              </thead>
              <tbody>
                {Object.entries(cryptoPrices).map(([code, price]) => (
                  <tr key={code}>
                    <td>
                      <strong>{code}</strong>
                    </td>
                    <td className="numeric">{formatPrice(price)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}

function CurrencySelect({ id, value, onChange, options }) {
  if (!options.length) {
    return (
      <input
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value.toUpperCase())}
        maxLength={3}
        placeholder="EUR"
      />
    );
  }
  return (
    <select id={id} value={value} onChange={(e) => onChange(e.target.value)}>
      {options.map((code) => (
        <option key={code} value={code}>
          {code}
        </option>
      ))}
    </select>
  );
}

function formatPrice(price) {
  const value = Number(price);
  if (!Number.isFinite(value)) return String(price);
  if (value >= 1) {
    return value.toLocaleString('sr-RS', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  return value.toLocaleString('sr-RS', { maximumFractionDigits: 8 });
}
