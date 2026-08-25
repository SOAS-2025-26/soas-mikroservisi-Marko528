import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ allowedRoles, children }) {
  const { isLoggedIn, role } = useAuth();

  if (!isLoggedIn) {
    return <Navigate to="/prijava" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(role)) {
    return (
      <div className="card">
        <div className="alert alert-error">
          Uloga {role} nije autorizovana za pristup ovoj stranici.
        </div>
      </div>
    );
  }

  return children;
}
