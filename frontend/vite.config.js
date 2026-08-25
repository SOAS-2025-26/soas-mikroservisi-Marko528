import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Razvojni server korisnickog interfejsa.
// Zahtevi idu direktno na API-Gateway (port 8765), koji dozvoljava CORS
// za lokalne adrese.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    open: false,
  },
});
