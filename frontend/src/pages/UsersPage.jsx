import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';

const EMPTY_FORM = { email: '', password: '', role: 'USER' };

export default function UsersPage() {
  const { isOwner } = useAuth();

  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadUsers();
  }, []);

  async function loadUsers() {
    setLoading(true);
    setError(null);
    try {
      setUsers(await api.listUsers());
    } catch (err) {
      setError(err.message);
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
      if (editingId) {
        const payload = { email: form.email.trim(), role: form.role };
        if (form.password.trim()) {
          payload.password = form.password;
        }
        await api.updateUser(editingId, payload);
        setSuccess(`Korisnik ${form.email} je ažuriran.`);
      } else {
        await api.createUser({
          email: form.email.trim(),
          password: form.password,
          role: form.role,
        });
        setSuccess(
          `Korisnik ${form.email} je dodat.` +
            (form.role === 'USER'
              ? ' Automatski su mu kreirani bankovni račun (EUR 0) i crypto novčanik (ETH 0).'
              : ''),
        );
      }
      resetForm();
      await loadUsers();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(user) {
    const confirmed = window.confirm(
      `Obrisati korisnika ${user.email}?` +
        (user.role === 'USER'
          ? '\n\nZajedno sa njim biće obrisani i njegov bankovni račun i crypto novčanik.'
          : ''),
    );
    if (!confirmed) return;

    setError(null);
    setSuccess(null);
    try {
      await api.deleteUser(user.id);
      setSuccess(`Korisnik ${user.email} je obrisan.`);
      await loadUsers();
    } catch (err) {
      setError(err.message);
    }
  }

  function startEdit(user) {
    setEditingId(user.id);
    setForm({ email: user.email, password: '', role: user.role });
    setSuccess(null);
    setError(null);
  }

  function resetForm() {
    setEditingId(null);
    setForm(EMPTY_FORM);
  }

  return (
    <>
      <h1 className="page-title">Korisnici</h1>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="card">
        <h2>{editingId ? 'Izmena korisnika' : 'Novi korisnik'}</h2>

        <form className="form-row" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="userEmail">Email adresa</label>
            <input
              id="userEmail"
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              placeholder="korisnik@soas.rs"
              required
            />
          </div>

          <div className="field">
            <label htmlFor="userPassword">
              Lozinka {editingId && <span style={{ textTransform: 'none' }}>(prazno = bez izmene)</span>}
            </label>
            <input
              id="userPassword"
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required={!editingId}
              minLength={editingId ? 0 : 4}
            />
          </div>

          <div className="field" style={{ maxWidth: 160 }}>
            <label htmlFor="userRole">Uloga</label>
            <select
              id="userRole"
              value={form.role}
              onChange={(e) => setForm({ ...form, role: e.target.value })}
            >
              <option value="USER">USER</option>
              {isOwner && <option value="ADMIN">ADMIN</option>}
            </select>
          </div>

          <button type="submit" disabled={saving}>
            {saving ? 'Čuvanje...' : editingId ? 'Sačuvaj izmene' : 'Dodaj korisnika'}
          </button>

          {editingId && (
            <button type="button" className="secondary" onClick={resetForm}>
              Odustani
            </button>
          )}
        </form>

      </div>

      <div className="card">
        <h2>Svi korisnici ({users.length})</h2>

        {loading ? (
          <div className="loading">Učitavanje...</div>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Email</th>
                  <th>Uloga</th>
                  <th style={{ width: 200 }}>Akcije</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => (
                  <tr key={user.id}>
                    <td className="numeric">{user.id}</td>
                    <td>{user.email}</td>
                    <td>
                      <span className="role-badge">{user.role}</span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: 8 }}>
                        <button className="secondary small" onClick={() => startEdit(user)}>
                          Izmeni
                        </button>
                        {isOwner && user.role !== 'OWNER' && (
                          <button className="danger" onClick={() => handleDelete(user)}>
                            Obriši
                          </button>
                        )}
                      </div>
                    </td>
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
