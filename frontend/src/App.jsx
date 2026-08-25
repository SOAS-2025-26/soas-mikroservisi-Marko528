import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import { useAuth } from './context/AuthContext';
import BankAccountsPage from './pages/BankAccountsPage';
import ConversionPage from './pages/ConversionPage';
import LoginPage from './pages/LoginPage';
import RatesPage from './pages/RatesPage';
import TradePage from './pages/TradePage';
import UsersPage from './pages/UsersPage';
import WalletsPage from './pages/WalletsPage';

export default function App() {
  const { isLoggedIn, role } = useAuth();

  return (
    <Routes>
      <Route path="/prijava" element={<LoginPage />} />

      <Route
        path="/*"
        element={
          <Layout>
            <Routes>
              <Route path="/" element={<Navigate to={homeFor(isLoggedIn, role)} replace />} />

              <Route path="/kursevi" element={<RatesPage />} />

              <Route
                path="/korisnici"
                element={
                  <ProtectedRoute allowedRoles={['OWNER', 'ADMIN']}>
                    <UsersPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/racuni"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'USER']}>
                    <BankAccountsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/novcanici"
                element={
                  <ProtectedRoute allowedRoles={['ADMIN', 'USER']}>
                    <WalletsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/razmena-fiat"
                element={
                  <ProtectedRoute allowedRoles={['USER']}>
                    <ConversionPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/trgovina"
                element={
                  <ProtectedRoute allowedRoles={['USER']}>
                    <TradePage />
                  </ProtectedRoute>
                }
              />

              <Route path="*" element={<Navigate to="/kursevi" replace />} />
            </Routes>
          </Layout>
        }
      />
    </Routes>
  );
}

function homeFor(isLoggedIn, role) {
  if (!isLoggedIn) return '/kursevi';
  if (role === 'OWNER' || role === 'ADMIN') return '/korisnici';
  return '/racuni';
}
