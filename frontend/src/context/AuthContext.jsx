import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, clearCredentials, hasCredentials, saveCredentials } from '../api/client';

const AuthContext = createContext(null);

const USER_KEY = 'soas.user';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = sessionStorage.getItem(USER_KEY);
    return stored && hasCredentials() ? JSON.parse(stored) : null;
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (user && hasCredentials()) {
      api.me().catch(() => logout());
    }
  }, []);

  const login = useCallback(async (email, password) => {
    setLoading(true);
    try {
      const profile = await api.login(email, password);
      saveCredentials(email, password);
      sessionStorage.setItem(USER_KEY, JSON.stringify(profile));
      setUser(profile);
      return profile;
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    clearCredentials();
    sessionStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({
      user,
      loading,
      login,
      logout,
      isLoggedIn: Boolean(user),
      role: user?.role ?? null,
      isOwner: user?.role === 'OWNER',
      isAdmin: user?.role === 'ADMIN',
      isUser: user?.role === 'USER',
    }),
    [user, loading, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth mora biti pozvan unutar AuthProvider komponente.');
  }
  return context;
}
