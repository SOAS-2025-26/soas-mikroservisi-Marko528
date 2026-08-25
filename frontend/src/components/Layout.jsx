import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Layout({ children }) {
  const { user, isLoggedIn, isOwner, isAdmin, isUser, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/kursevi');
  }

  return (
    <>
      <header className="app-header">
        <div className="brand">SOAS &middot; Razmena valuta</div>

        <nav className="nav">
          <NavLink to="/kursevi">Kursevi</NavLink>

          {(isOwner || isAdmin) && <NavLink to="/korisnici">Korisnici</NavLink>}

          {(isAdmin || isUser) && <NavLink to="/racuni">Bankovni računi</NavLink>}
          {(isAdmin || isUser) && <NavLink to="/novcanici">Crypto novčanici</NavLink>}

          {isUser && <NavLink to="/razmena-fiat">Razmena fiat valuta</NavLink>}
          {isUser && <NavLink to="/trgovina">Trgovina crypto</NavLink>}
        </nav>

        <div className="user-box">
          {isLoggedIn ? (
            <>
              <span className="muted">{user.email}</span>
              <span className="role-badge">{user.role}</span>
              <button className="secondary small" onClick={handleLogout}>
                Odjava
              </button>
            </>
          ) : (
            <button className="small" onClick={() => navigate('/prijava')}>
              Prijava
            </button>
          )}
        </div>
      </header>

      <main className="page">{children}</main>
    </>
  );
}
