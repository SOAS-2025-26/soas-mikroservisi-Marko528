import { useEffect, useState } from 'react';
import { api } from '../api/client';

export default function ConversionPage() {
  const [account, setAccount] = useState([]);
  const [currencies, setCurrencies] = useState([]);

  const [from, setFrom] = useState('');
  const [to, setTo] = useState('RSD');
  const [quantity, setQuantity] = useState('');

  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    loadAccount();
    api.fiatCurrencies().then(setCurrencies).catch(() => setCurrencies([]));
  }, []);

  async function loadAccount() {
    try {
      const data = await api.myBankAccount();
      setAccount(data);
      if (data.length && !from) {
        setFrom(data[0].currencyCode);
      }
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
      const response = await api.convert(from, to, quantity);
      setResult(response);
      setAccount(response.bankAccount);
      setQuantity('');
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const selectedBalance = account.find((item) => item.currencyCode === from);

  return (
    <>
      <h1 className="page-title">Razmena fiat valuta</h1>

      <div className="card">
        <h2>Stanje bankovnog računa</h2>
        <div className="balance-list">
          {account.map((item) => (
            <div className="balance-chip" key={item.id}>
              <div className="code">{item.currencyCode}</div>
              <div className="value">{formatFiat(item.amount)}</div>
            </div>
          ))}
          {!account.length && <span className="muted">Račun je prazan.</span>}
        </div>
      </div>

      <div className="card">
        <h2>Nova razmena</h2>

        {error && <div className="alert alert-error">{error}</div>}

        <form className="form-row" onSubmit={handleSubmit}>
          <div className="field" style={{ maxWidth: 190 }}>
            <label htmlFor="convFrom">Iz valute (sa računa)</label>
            <select id="convFrom" value={from} onChange={(e) => setFrom(e.target.value)} required>
              {account.map((item) => (
                <option key={item.id} value={item.currencyCode}>
                  {item.currencyCode} ({formatFiat(item.amount)})
                </option>
              ))}
            </select>
          </div>

          <span className="arrow">&rarr;</span>

          <div className="field" style={{ maxWidth: 190 }}>
            <label htmlFor="convTo">U valutu</label>
            {currencies.length ? (
              <select id="convTo" value={to} onChange={(e) => setTo(e.target.value)} required>
                {currencies.map((code) => (
                  <option key={code} value={code}>
                    {code}
                  </option>
                ))}
              </select>
            ) : (
              <input
                id="convTo"
                value={to}
                onChange={(e) => setTo(e.target.value.toUpperCase())}
                maxLength={3}
                required
              />
            )}
          </div>

          <div className="field" style={{ maxWidth: 190 }}>
            <label htmlFor="convQuantity">Količina</label>
            <input
              id="convQuantity"
              type="number"
              step="any"
              min="0"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              placeholder="100"
              required
            />
          </div>

          <button type="submit" disabled={busy || !from}>
            {busy ? 'Razmena...' : 'Zameni'}
          </button>
        </form>

        {selectedBalance && (
          <p className="muted" style={{ marginTop: 14, marginBottom: 0 }}>
            Dostupno za razmenu: {formatFiat(selectedBalance.amount)} {selectedBalance.currencyCode}
          </p>
        )}
      </div>

      {result && (
        <div className="card">
          <h2>Rezultat transakcije</h2>
          <div className="alert alert-success">{result.message}</div>

          <div className="table-wrapper">
            <table>
              <tbody>
                <tr>
                  <th>Primenjeni kurs</th>
                  <td className="numeric">
                    1 {result.from} = {result.conversionMultiple} {result.to}
                  </td>
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

          <h2 style={{ marginTop: 20 }}>Novo stanje računa</h2>
          <div className="balance-list">
            {result.bankAccount.map((item) => (
              <div className="balance-chip" key={item.id}>
                <div className="code">{item.currencyCode}</div>
                <div className="value">{formatFiat(item.amount)}</div>
              </div>
            ))}
          </div>
        </div>
      )}
    </>
  );
}

function formatFiat(amount) {
  const value = Number(amount);
  if (!Number.isFinite(value)) return String(amount);
  return value.toLocaleString('sr-RS', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
