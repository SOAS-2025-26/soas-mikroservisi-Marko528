export const GATEWAY_URL = import.meta.env.VITE_GATEWAY_URL || 'http://localhost:8765';

const CREDENTIALS_KEY = 'soas.credentials';

export function saveCredentials(email, password) {
  sessionStorage.setItem(CREDENTIALS_KEY, btoa(`${email}:${password}`));
}

export function clearCredentials() {
  sessionStorage.removeItem(CREDENTIALS_KEY);
}

export function hasCredentials() {
  return sessionStorage.getItem(CREDENTIALS_KEY) !== null;
}

function authHeader(overrideToken) {
  const token = overrideToken || sessionStorage.getItem(CREDENTIALS_KEY);
  return token ? { Authorization: `Basic ${token}` } : {};
}

async function request(path, { method = 'GET', body, anonymous = false, token } = {}) {
  const headers = { Accept: 'application/json' };
  if (!anonymous) {
    Object.assign(headers, authHeader(token));
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  let response;
  try {
    response = await fetch(`${GATEWAY_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (networkError) {
    throw new Error(
      'Nije moguće doći do API-Gateway-a. Proveri da li je aplikacija pokrenuta na portu 8765.',
    );
  }

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  let payload = null;
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = text;
    }
  }

  if (!response.ok) {
    const message =
      (payload && payload.message) ||
      (typeof payload === 'string' && payload) ||
      `Greška ${response.status}`;
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return payload;
}

export const api = {
  login: (email, password) =>
    request('/auth/login', { method: 'POST', token: btoa(`${email}:${password}`) }),
  me: () => request('/auth/me'),

  listUsers: () => request('/users'),
  createUser: (user) => request('/users', { method: 'POST', body: user }),
  updateUser: (id, user) => request(`/users/${id}`, { method: 'PUT', body: user }),
  deleteUser: (id) => request(`/users/${id}`, { method: 'DELETE' }),

  listBankAccounts: () => request('/bank-accounts'),
  myBankAccount: () => request('/bank-accounts/me'),
  bankAccountOf: (email) => request(`/bank-accounts/${encodeURIComponent(email)}`),
  createBankAccount: (account) => request('/bank-accounts', { method: 'POST', body: account }),
  updateBankAccount: (id, account) =>
    request(`/bank-accounts/${id}`, { method: 'PUT', body: account }),
  deleteBankAccount: (id) => request(`/bank-accounts/${id}`, { method: 'DELETE' }),

  listWallets: () => request('/crypto-wallets'),
  myWallet: () => request('/crypto-wallets/me'),
  walletOf: (email) => request(`/crypto-wallets/${encodeURIComponent(email)}`),
  createWallet: (wallet) => request('/crypto-wallets', { method: 'POST', body: wallet }),
  updateWallet: (id, wallet) => request(`/crypto-wallets/${id}`, { method: 'PUT', body: wallet }),
  deleteWallet: (id) => request(`/crypto-wallets/${id}`, { method: 'DELETE' }),

  fiatCurrencies: () => request('/currency-exchange/currencies', { anonymous: true }),
  fiatRate: (from, to) =>
    request(`/currency-exchange/from/${from}/to/${to}`, { anonymous: true }),
  fiatRates: (base) =>
    request(`/currency-exchange/rates?base=${encodeURIComponent(base)}`, { anonymous: true }),

  cryptoCurrencies: () => request('/crypto-exchange/currencies', { anonymous: true }),
  cryptoRate: (from, to) => request(`/crypto-exchange/from/${from}/to/${to}`, { anonymous: true }),
  cryptoPrices: (currency) =>
    request(`/crypto-exchange/prices?currency=${encodeURIComponent(currency)}`, {
      anonymous: true,
    }),

  convert: (from, to, quantity) =>
    request(
      `/currency-conversion?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&quantity=${quantity}`,
    ),

  trade: (from, to, quantity) =>
    request(
      `/trade-service?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&quantity=${quantity}`,
    ),
};
