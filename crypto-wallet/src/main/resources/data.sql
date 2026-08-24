-- Pocetno stanje kripto novcanika za demonstraciju rada aplikacije.
-- Email adrese odgovaraju korisnicima sa ulogom USER iz users-service-a.
-- H2 je in-memory baza, pa se ovi podaci ucitavaju pri svakom pokretanju servisa.

INSERT INTO crypto_wallets (email, crypto_code, amount) VALUES ('marko@soas.rs', 'ETH', 0.50000000);
INSERT INTO crypto_wallets (email, crypto_code, amount) VALUES ('marko@soas.rs', 'BTC', 0.01000000);

INSERT INTO crypto_wallets (email, crypto_code, amount) VALUES ('ana@soas.rs', 'ETH', 0.25000000);
