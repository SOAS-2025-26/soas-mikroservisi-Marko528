import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';

export default function AssetManager({
  title,
  codeLabel,
  codePlaceholder,
  defaultCode,
  codeField,
  decimals,
  service,
}) {
  const { isAdmin, isUser } = useAuth();

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const emptyForm = { email: '', code: defaultCode, amount: '' };
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);

  const [searchEmail, setSearchEmail] = useState('');

  useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      setItems(isAdmin ? await service.listAll() : await service.mine());
    } catch (err) {
      setError(err.message);
      setItems([]);
    } finally {
      setLoading(false);
    }
  }

  async function handleSearch(event) {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    if (!searchEmail.trim()) {
      await load();
      return;
    }
    setLoading(true);
    try {
      setItems(await service.byEmail(searchEmail.trim()));
    } catch (err) {
      setError(err.message);
      setItems([]);
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const payload = {
        email: form.email.trim(),
        [codeField]: form.code.trim().toUpperCase(),
        amount: form.amount === '' ? '0' : form.amount,
      };
      if (editingId) {
        await service.update(editingId, payload);
        setSuccess('Zapis je ažuriran.');
      } else {
        await service.create(payload);
        setSuccess('Zapis je dodat.');
      }
      resetForm();
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(item) {
    if (!window.confirm(`Obrisati zapis ${item[codeField]} za ${item.email}?`)) return;
    setError(null);
    setSuccess(null);
    try {
      await service.remove(item.id);
      setSuccess('Zapis je obrisan.');
      await load();
    } catch (err) {
      setError(err.message);
    }
  }

  function startEdit(item) {
    setEditingId(item.id);
    setForm({ email: item.email, code: item[codeField], amount: String(item.amount) });
    setError(null);
    setSuccess(null);
  }

  function resetForm() {
    setEditingId(null);
    setForm(emptyForm);
  }

  return (
    <>
      <h1 className="page-title">{title}</h1>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {isUser && (
        <div className="card">
          <h2>Moje stanje</h2>
          {loading ? (
            <div className="loading">Učitavanje...</div>
          ) : (
            <div className="balance-list">
              {items.map((item) => (
                <div className="balance-chip" key={item.id}>
                  <div className="code">{item[codeField]}</div>
                  <div className="value">{formatAmount(item.amount, decimals)}</div>
                </div>
              ))}
              {!items.length && <span className="muted">Nema zapisa.</span>}
            </div>
          )}
        </div>
      )}

      {isAdmin && (
        <>
          <div className="card">
            <h2>{editingId ? 'Izmena zapisa' : 'Novi zapis'}</h2>

            <form className="form-row" onSubmit={handleSubmit}>
              <div className="field">
                <label htmlFor="assetEmail">Email korisnika</label>
                <input
                  id="assetEmail"
                  type="email"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  placeholder="korisnik@soas.rs"
                  required
                  disabled={Boolean(editingId)}
                />
              </div>

              <div className="field" style={{ maxWidth: 150 }}>
                <label htmlFor="assetCode">{codeLabel}</label>
                <input
                  id="assetCode"
                  value={form.code}
                  onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
                  placeholder={codePlaceholder}
                  required
                />
              </div>

              <div className="field" style={{ maxWidth: 180 }}>
                <label htmlFor="assetAmount">Količina</label>
                <input
                  id="assetAmount"
                  type="number"
                  step="any"
                  min="0"
                  value={form.amount}
                  onChange={(e) => setForm({ ...form, amount: e.target.value })}
                  placeholder="0"
                  required
                />
              </div>

              <button type="submit" disabled={saving}>
                {saving ? 'Čuvanje...' : editingId ? 'Sačuvaj izmene' : 'Dodaj'}
              </button>

              {editingId && (
                <button type="button" className="secondary" onClick={resetForm}>
                  Odustani
                </button>
              )}
            </form>

          </div>

          <div className="card">
            <h2>Pretraga po korisniku</h2>
            <form className="form-row" onSubmit={handleSearch}>
              <div className="field">
                <label htmlFor="searchEmail">Email korisnika</label>
                <input
                  id="searchEmail"
                  value={searchEmail}
                  onChange={(e) => setSearchEmail(e.target.value)}
                  placeholder="ostavi prazno za prikaz svih"
                />
              </div>
              <button type="submit">Prikaži</button>
              <button
                type="button"
                className="secondary"
                onClick={() => {
                  setSearchEmail('');
                  load();
                }}
              >
                Prikaži sve
              </button>
            </form>
          </div>
        </>
      )}

      <div className="card">
        <h2>{isAdmin ? `Svi zapisi (${items.length})` : `Detaljan prikaz (${items.length})`}</h2>

        {loading ? (
          <div className="loading">Učitavanje...</div>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Email</th>
                  <th>{codeLabel}</th>
                  <th>Količina</th>
                  {isAdmin && <th style={{ width: 200 }}>Akcije</th>}
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.id}>
                    <td className="numeric">{item.id}</td>
                    <td>{item.email}</td>
                    <td>
                      <strong>{item[codeField]}</strong>
                    </td>
                    <td className="numeric">{formatAmount(item.amount, decimals)}</td>
                    {isAdmin && (
                      <td>
                        <div style={{ display: 'flex', gap: 8 }}>
                          <button className="secondary small" onClick={() => startEdit(item)}>
                            Izmeni
                          </button>
                          <button className="danger" onClick={() => handleDelete(item)}>
                            Obriši
                          </button>
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
                {!items.length && (
                  <tr>
                    <td colSpan={isAdmin ? 5 : 4} className="muted">
                      Nema zapisa za prikaz.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}

function formatAmount(amount, decimals) {
  const value = Number(amount);
  if (!Number.isFinite(value)) return String(amount);
  return value.toLocaleString('sr-RS', {
    minimumFractionDigits: 2,
    maximumFractionDigits: decimals,
  });
}
