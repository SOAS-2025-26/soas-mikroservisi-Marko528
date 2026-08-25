import { useEffect, useState } from 'react';
import { api } from '../api/client';

const TRADE_TYPES = [
  { id: 'FIAT_U_CRYPTO', label: 'Kupovina kripto valute (fiat -> crypto)' },
  { id: 'CRYPTO_U_FIAT', label: 'Prodaja kripto valute (crypto -> fiat)' },
  { id: 'CRYPTO_U_CRYPTO', label: 'Zamena kripto valuta (crypto -> crypto)' },
];

export default function TradePage() {
  const [account, setAccount] = useState([]);
  const [wallet, setWallet] = useState([]);
  const [cryptoCurrencies, setCryptoCurrencies] = useState([]);
  const [fiatCurrencies, setFiatCurrencies] = useState([]);

  const [tradeType, setTradeType] = useState('FIAT_U_CRYPTO');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [quantity, setQuantity] = useState('');

  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    loadBalances();
    api.cryptoCurrencies().then(setCryptoCurrencies).catch(() => setCryptoCurrencies([]));
    api.fiatCurrencies().then(setFiatCurrencies).catch(() => setFiatCurrencies([]));
  }, []);

  useEffect(() => {
    if (tradeType === 'FIAT_U_CRYPTO') {
      setFrom(account[0]?.currencyCode ?? 'EUR');
      setTo('BTC');
    } else if (tradeType === 'CRYPTO_U_FIAT') {
      setFrom(wallet[0]?.cryptoCode ?? 'ETH');
      setTo('EUR');
    } else {
      setFrom(wallet[0]?.cryptoCode ?? 'ETH');
      setTo('BTC');
    }
  }, [tradeType, account, wallet]);

  async function loadBalances() {
    try {
      const [accountData, walletData] = await Promise.all([
        api.myBankAccount().catch(() => []),
        api.myWallet().catch(() => []),
      ]);
      setAccount(accountData);
      setWallet(walletData);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      const response = await api.trade(from, to, quantity);
      setResult(response);
      setQuantity('');
      await loadBalances();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const sourceOptions = tradeType === 'FIAT_U_CRYPTO' ? account : wallet;
  const sourceField = tradeType === 'FIAT_U_CRYPTO' ? 'currencyCode' : 'cryptoCode';
  const targetOptions = tradeType === 'CRYPTO_U_FIAT' ? fiatCurrencies : cryptoCurrencies;

  return (
    <>
      <h1 className="page-title">Trgovina kripto valutama</h1>

      <div className="grid-2">
        <div className="card">
          <h2>Bankovni račun</h2>
          <div className="balance-list">
            {account.map((item) => (
              <div className="balance-chip" key={item.id}>
                <div className="code">{item.currencyCode}</div>
                <div className="value">{format(item.amount, 2)}</div>
              </div>
            ))}
            {!account.length && <span className="muted">Račun je prazan.</span>}
          </div>
        </div>

        <div className="card">
          <h2>Crypto novčanik</h2>
          <div className="balance-list">
            {wallet.map((item) => (
              <div className="balance-chip" key={item.id}>
                <div className="code">{item.cryptoCode}</div>
                <div className="value">{format(item.amount, 8)}</div>
              </div>
            ))}
            {!wallet.length && <span className="muted">Novčanik je prazan.</span>}
          </div>
        </div>
      </div>

      <div className="card">
        <h2>Nova transakcija</h2>

        {error && <div className="alert alert-error">{error}</div>}

        <div className="form-row" style={{ marginBottom: 18 }}>
          <div className="field">
            <label htmlFor="tradeType">Vrsta razmene</label>
            <select
              id="tradeType"
              value={tradeType}
              onChange={(e) => setTradeType(e.target.value)}
            >
              {TRADE_TYPES.map((type) => (
                <option key={type.id} value={type.id}>
                  {type.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <form className="form-row" onSubmit={handleSubmit}>
          <div className="field" style={{ maxWidth: 200 }}>
            <label htmlFor="tradeFrom">Iz valute</label>
            <select id="tradeFrom" value={from} onChange={(e) => setFrom(e.target.value)} required>
              {sourceOptions.map((item) => (
                <option key={item.id} value={item[sourceField]}>
                  {item[sourceField]} ({format(item.amount, sourceField === 'cryptoCode' ? 8 : 2)})
                </option>
              ))}
              {!sourceOptions.length && <option value="">nema sredstava</option>}
            </select>
          </div>

          <span className="arrow">&rarr;</span>

          <div className="field" style={{ maxWidth: 200 }}>
            <label htmlFor="tradeTo">U valutu</label>
            <select id="tradeTo" value={to} onChange={(e) => setTo(e.target.value)} required>
              {targetOptions.map((code) => (
                <option key={code} value={code}>
                  {code}
                </option>
              ))}
            </select>
          </div>

          <div className="field" style={{ maxWidth: 190 }}>
            <label htmlFor="tradeQuantity">Količina</label>
            <input
              id="tradeQuantity"
              type="number"
              step="any"
              min="0"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              placeholder="0.01"
              required
            />
          </div>

          <button type="submit" disabled={busy || !from || !to}>
            {busy ? 'Razmena...' : 'Izvrši razmenu'}
          </button>
        </form>
      </div>

      {result && (
        <div className="card">
          <h2>Rezultat transakcije</h2>
          <div className="alert alert-success">{result.message}</div>

          <div className="table-wrapper">
            <table>
              <tbody>
                <tr>
                  <th>Vrsta razmene</th>
                  <td>{result.tradeType}</td>
                </tr>
                <tr>
                  <th>Primenjeni kurs</th>
                  <td className="numeric">{result.conversionMultiple}</td>
                </tr>
                <tr>
                  <th>Razmenjeno</th>
                  <td className="numeric">
                    {result.quantity} {result.from} &rarr; {result.convertedAmount} {result.to}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          {result.cryptoWallet && (
            <>
              <h2 style={{ marginTop: 20 }}>Novo stanje novčanika</h2>
              <div className="balance-list">
                {result.cryptoWallet.map((item) => (
                  <div className="balance-chip" key={item.id}>
                    <div className="code">{item.cryptoCode}</div>
                    <div className="value">{format(item.amount, 8)}</div>
                  </div>
                ))}
              </div>
            </>
          )}

          {result.bankAccount && (
            <>
              <h2 style={{ marginTop: 20 }}>Novo stanje računa</h2>
              <div className="balance-list">
                {result.bankAccount.map((item) => (
                  <div className="balance-chip" key={item.id}>
                    <div className="code">{item.currencyCode}</div>
                    <div className="value">{format(item.amount, 2)}</div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      )}
    </>
  );
}

function format(amount, decimals) {
  const value = Number(amount);
  if (!Number.isFinite(value)) return String(amount);
  return value.toLocaleString('sr-RS', {
    minimumFractionDigits: 2,
    maximumFractionDigits: decimals,
  });
}
