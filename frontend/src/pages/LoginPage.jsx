import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const { login, loading, isLoggedIn } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);

  if (isLoggedIn) {
    return <Navigate to="/" replace />;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);
    try {
      const profile = await login(email.trim(), password);
      navigate(profile.role === 'USER' ? '/racuni' : '/korisnici', { replace: true });
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="login-screen">
      <div className="login-box">
        <h1>Prijava</h1>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="email">Email adresa</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="korisnik@soas.rs"
              required
              autoFocus
            />
          </div>

          <div className="field">
            <label htmlFor="password">Lozinka</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          <button type="submit" disabled={loading}>
            {loading ? 'Prijavljivanje...' : 'Prijavi se'}
          </button>
        </form>

        <p className="muted" style={{ marginTop: 20, marginBottom: 0 }}>
          Kursevi valuta su dostupni <Link to="/kursevi" style={{ color: 'var(--accent)' }}>i bez prijave</Link>.
        </p>
      </div>
    </div>
  );
}
