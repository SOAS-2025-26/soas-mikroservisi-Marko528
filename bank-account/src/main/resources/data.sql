-- Pocetno stanje bankovnih racuna za demonstraciju rada aplikacije.
-- Email adrese odgovaraju korisnicima sa ulogom USER iz users-service-a.
-- H2 je in-memory baza, pa se ovi podaci ucitavaju pri svakom pokretanju servisa.

INSERT INTO bank_accounts (email, currency_code, amount) VALUES ('marko@soas.rs', 'EUR', 1000.00);
INSERT INTO bank_accounts (email, currency_code, amount) VALUES ('marko@soas.rs', 'USD', 500.00);
INSERT INTO bank_accounts (email, currency_code, amount) VALUES ('marko@soas.rs', 'RSD', 120000.00);

INSERT INTO bank_accounts (email, currency_code, amount) VALUES ('ana@soas.rs', 'EUR', 250.00);
INSERT INTO bank_accounts (email, currency_code, amount) VALUES ('ana@soas.rs', 'CHF', 80.00);
