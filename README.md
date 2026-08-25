# Aplikacija za razmenu obicnih (fiat) i crypto valuta

Projektni zadatak iz predmeta **Servisno orijentisana arhitektura sistema**, skolska 2025/26. godina.

Aplikacija je realizovana kao skup Spring Boot mikroservisa registrovanih na Eureka naming server,
sa API-Gateway-em kao jedinstvenom ulaznom tackom i React korisnickim interfejsom.

---

## Sadrzaj

- [Arhitektura](#arhitektura)
- [Tehnologije](#tehnologije)
- [Kredencijali za sve korisnike u sistemu](#kredencijali-za-sve-korisnike-u-sistemu)
- [Funkcionalni URL-ovi preko API-Gateway-a](#funkcionalni-url-ovi-preko-api-gateway-a)
- [Pokretanje aplikacije](#pokretanje-aplikacije)
- [Docker](#docker)
- [Korisnicki interfejs](#korisnicki-interfejs)
- [Autorizacija po ulogama](#autorizacija-po-ulogama)
- [Obrada izuzetaka](#obrada-izuzetaka)
- [Fault tolerance](#fault-tolerance)
- [Struktura projekta](#struktura-projekta)

---

## Arhitektura

```
                            ┌──────────────────────┐
                            │   React front-end    │
                            │  (localhost:5173)    │
                            └───────────┬──────────┘
                                        │  Basic autentikacija
                            ┌───────────▼──────────┐
                            │     API Gateway      │
                            │  (localhost:8765)    │
                            └───────────┬──────────┘
                                        │  Feign / lb://
        ┌───────────────┬───────────────┼───────────────┬───────────────┐
        │               │               │               │               │
┌───────▼──────┐ ┌──────▼───────┐ ┌─────▼──────┐ ┌──────▼──────┐ ┌──────▼───────┐
│users-service │ │ bank-account │ │crypto-     │ │  currency-  │ │    trade-    │
│    8770      │ │     8200     │ │wallet 8300 │ │conversion   │ │service 8600  │
│  H2 baza     │ │   H2 baza    │ │  H2 baza   │ │    8100     │ │              │
└──────────────┘ └──────────────┘ └────────────┘ └──────┬──────┘ └──────┬───────┘
                                                        │               │
                                            ┌───────────▼─────┐ ┌───────▼────────┐
                                            │currency-exchange│ │crypto-exchange │
                                            │      8000       │ │      8400      │
                                            └───────────┬─────┘ └───────┬────────┘
                                                        │               │
                                              open.er-api.com     api.coinbase.com

                            ┌──────────────────────┐
                            │  Eureka naming server │
                            │   (localhost:8761)    │
                            └──────────────────────┘
```

Svi mikroservisi su registrovani na Eureka naming server. Medjuservisna komunikacija se odvija
iskljucivo putem **Feign klijenta** - `RestTemplate` se koristi jedino u API-Gateway-u, gde ga
specifikacija izricito dozvoljava (provera kredencijala nad users-service-om).

Postovan je **database per service** obrazac: users-service, bank-account i crypto-wallet imaju
svaki svoju zasebnu H2 in-memory bazu.

### Eksterni API servisi

| Servis | Eksterni API | API kljuc |
|---|---|---|
| currency-exchange | `https://open.er-api.com` (exchangerate-api.com) | nije potreban |
| crypto-exchange | `https://api.coinbase.com` | nije potreban |

Oba servisa su otvorena i ne zahtevaju registraciju, pa aplikacija radi odmah nakon pokretanja.

---

## Tehnologije

| Tehnologija | Verzija |
|---|---|
| Java | 17 |
| Maven | 3.9 (multi-module projekat) |
| Spring Boot | 3.2.5 |
| Spring Cloud | 2023.0.1 (Eureka, Gateway, OpenFeign, Resilience4j) |
| H2 | in-memory baza |
| React + Vite | 18.3 / 5.4 |
| Docker | slike i docker-compose |

---

## Kredencijali za sve korisnike u sistemu

Lozinke se u bazi cuvaju kao BCrypt hash; ispod su navedene originalne vrednosti za prijavu.

| Email | Lozinka | Uloga |
|---|---|---|
| `owner@soas.rs` | `owner123` | OWNER |
| `admin@soas.rs` | `admin123` | ADMIN |
| `marko@soas.rs` | `marko123` | USER |
| `ana@soas.rs` | `ana123` | USER |

### Pocetno stanje racuna i novcanika

| Korisnik | Bankovni racun | Crypto novcanik |
|---|---|---|
| `marko@soas.rs` | EUR 1000, USD 500, RSD 120000 | ETH 0.5, BTC 0.01 |
| `ana@soas.rs` | EUR 250, CHF 80 | ETH 0.25 |

Svaki **novi** korisnik sa ulogom USER automatski dobija bankovni racun sa stanjem `EUR 0`
i crypto novcanik sa stanjem `ETH 0`.

> H2 je in-memory baza, pa se pri svakom ponovnom pokretanju aplikacije podaci vracaju
> na pocetno stanje navedeno iznad.

---

## Funkcionalni URL-ovi preko API-Gateway-a

Svi zahtevi idu na **`http://localhost:8765`**. Svi zahtevi osim onih ka `currency-exchange` i
`crypto-exchange` zahtevaju basic autentikaciju.

Primer poziva iz komandne linije:

```bash
curl -u marko@soas.rs:marko123 "http://localhost:8765/currency-conversion?from=EUR&to=RSD&quantity=100"
```

### Prijava (API-Gateway)

| Metoda | URL | Opis | Uloge |
|---|---|---|---|
| POST | `http://localhost:8765/auth/login` | Provera kredencijala, vraca email i ulogu | sve |
| GET | `http://localhost:8765/auth/me` | Podaci o prijavljenom korisniku | sve |

### Users service

| Metoda | URL | Opis | Uloge |
|---|---|---|---|
| GET | `http://localhost:8765/users` | Svi korisnici | OWNER, ADMIN |
| GET | `http://localhost:8765/users/{id}` | Jedan korisnik | OWNER, ADMIN |
| POST | `http://localhost:8765/users` | Novi korisnik | OWNER (sve uloge), ADMIN (samo USER) |
| PUT | `http://localhost:8765/users/{id}` | Izmena korisnika | OWNER (sve), ADMIN (samo USER) |
| DELETE | `http://localhost:8765/users/{id}` | Brisanje korisnika | OWNER |

Telo zahteva za POST i PUT:

```json
{ "email": "petar@soas.rs", "password": "petar123", "role": "USER" }
```

### Bank account

| Metoda | URL | Opis | Uloge |
|---|---|---|---|
| GET | `http://localhost:8765/bank-accounts` | Svi bankovni racuni | ADMIN |
| GET | `http://localhost:8765/bank-accounts/me` | Racun prijavljenog korisnika | ADMIN, USER |
| GET | `http://localhost:8765/bank-accounts/{email}` | Racun konkretnog korisnika | ADMIN (svi), USER (samo svoj) |
| POST | `http://localhost:8765/bank-accounts` | Nova stavka racuna | ADMIN |
| PUT | `http://localhost:8765/bank-accounts/{id}` | Izmena stavke | ADMIN |
| DELETE | `http://localhost:8765/bank-accounts/{id}` | Brisanje stavke | ADMIN |

Telo zahteva za POST i PUT:

```json
{ "email": "marko@soas.rs", "currencyCode": "USD", "amount": 250.00 }
```

### Crypto wallet

| Metoda | URL | Opis | Uloge |
|---|---|---|---|
| GET | `http://localhost:8765/crypto-wallets` | Svi novcanici | ADMIN |
| GET | `http://localhost:8765/crypto-wallets/me` | Novcanik prijavljenog korisnika | ADMIN, USER |
| GET | `http://localhost:8765/crypto-wallets/{email}` | Novcanik konkretnog korisnika | ADMIN (svi), USER (samo svoj) |
| POST | `http://localhost:8765/crypto-wallets` | Nova stavka novcanika | ADMIN |
| PUT | `http://localhost:8765/crypto-wallets/{id}` | Izmena stavke | ADMIN |
| DELETE | `http://localhost:8765/crypto-wallets/{id}` | Brisanje stavke | ADMIN |

Telo zahteva za POST i PUT:

```json
{ "email": "marko@soas.rs", "cryptoCode": "BTC", "amount": 0.05 }
```

### Currency exchange (dostupno bez prijave)

| Metoda | URL | Opis | Uloge |
|---|---|---|---|
| GET | `http://localhost:8765/currency-exchange/from/EUR/to/RSD` | Kurs izmedju dve fiat valute | sve |
| GET | `http://localhost:8765/currency-exchange/currencies` | Spisak podrzanih fiat valuta | sve |
| GET | `http://localhost:8765/currency-exchange/rates?base=EUR` | Svi kursevi za zadatu baznu valutu | sve |

### Crypto exchange (dostupno bez prijave)

| Metoda | URL | Opis | Uloge |
|---|---|---|---|
| GET | `http://localhost:8765/crypto-exchange/from/BTC/to/EUR` | Kurs za par sa kripto valutom | sve |
| GET | `http://localhost:8765/crypto-exchange/currencies` | Spisak podrzanih kripto valuta | sve |
| GET | `http://localhost:8765/crypto-exchange/prices?currency=EUR` | Cene kripto valuta u zadatoj fiat valuti | sve |

### Currency conversion

| Metoda | URL | Opis | Uloge |
|---|---|---|---|
| GET | `http://localhost:8765/currency-conversion?from=X&to=Y&quantity=Q` | Razmena fiat valuta | USER |

Primer: `http://localhost:8765/currency-conversion?from=EUR&to=RSD&quantity=100`

```json
{
  "email": "marko@soas.rs",
  "from": "EUR", "to": "RSD",
  "quantity": 100,
  "conversionMultiple": 117.327386,
  "convertedAmount": 11732.74,
  "message": "Uspesno je izvrsena razmena EUR: 100 za RSD: 11732.74",
  "bankAccount": [
    { "id": 1, "email": "marko@soas.rs", "currencyCode": "EUR", "amount": 900.00 },
    { "id": 3, "email": "marko@soas.rs", "currencyCode": "RSD", "amount": 131732.74 }
  ]
}
```

### Trade service

| Metoda | URL | Opis | Uloge |
|---|---|---|---|
| GET | `http://localhost:8765/trade-service?from=X&to=Y&quantity=Q` | Razmena fiat i crypto valuta | USER |

Podrzana su tri smera razmene:

| Smer | Primer |
|---|---|
| crypto u crypto | `http://localhost:8765/trade-service?from=ETH&to=BTC&quantity=0.1` |
| fiat u crypto | `http://localhost:8765/trade-service?from=EUR&to=BTC&quantity=100` |
| crypto u fiat | `http://localhost:8765/trade-service?from=BTC&to=EUR&quantity=0.005` |

Kripto valute je moguce kupovati i prodavati samo za **USD** i **EUR**. Ako se u zahtevu nadje
druga fiat valuta, trade-service je prvo konvertuje u EUR (odnosno iz EUR u trazenu valutu)
pozivom currency-conversion mikroservisa:

```
http://localhost:8765/trade-service?from=RSD&to=ETH&quantity=5000
```

```json
{
  "email": "marko@soas.rs",
  "tradeType": "FIAT_U_CRYPTO",
  "from": "RSD", "to": "ETH",
  "quantity": 5000,
  "convertedAmount": 0.02012304,
  "message": "Uspesno je izvrsena razmena RSD: 5000 za ETH: 0.02012304 (medjukorak: RSD: 5000 zamenjeno za EUR: 42.62)",
  "cryptoWallet": [
    { "id": 1, "email": "marko@soas.rs", "cryptoCode": "ETH", "amount": 0.42012304 }
  ]
}
```

### Infrastrukturni URL-ovi

| URL | Opis |
|---|---|
| `http://localhost:8761` | Eureka konzola sa spiskom registrovanih servisa |
| `http://localhost:8765/actuator/health` | Stanje API-Gateway-a |
| `http://localhost:8600/actuator/circuitbreakers` | Stanje circuit breaker-a u trade servisu |

> Putanje `/internal/**` sluze iskljucivo za medjuservisnu Feign komunikaciju i **blokirane su**
> na nivou API-Gateway-a.

---

## Pokretanje aplikacije

### Preduslovi

- Java 17
- Maven 3.9+
- Node.js 18+ (za korisnicki interfejs)
- Docker Desktop (za pokretanje u kontejnerima)

### Varijanta 1 - Docker (preporuceno)

```bash
docker compose up -d
```

Aplikacija se podize u devet kontejnera. Kada svi predju u stanje `healthy` (proveriti sa
`docker compose ps`), backend je dostupan na `http://localhost:8765`.

Gasenje:

```bash
docker compose down
```

### Varijanta 2 - lokalno, bez Docker-a

```bash
mvn clean install -DskipTests
```

Zatim pokrenuti servise **redom**, pri cemu naming-server mora biti prvi:

```bash
java -jar naming-server/target/naming-server-1.0.0.jar
java -jar users-service/target/users-service-1.0.0.jar
java -jar currency-exchange/target/currency-exchange-1.0.0.jar
java -jar currency-conversion/target/currency-conversion-1.0.0.jar
java -jar bank-account/target/bank-account-1.0.0.jar
java -jar crypto-wallet/target/crypto-wallet-1.0.0.jar
java -jar crypto-exchange/target/crypto-exchange-1.0.0.jar
java -jar trade-service/target/trade-service-1.0.0.jar
java -jar api-gateway/target/api-gateway-1.0.0.jar
```

### Korisnicki interfejs

Front-end se, prema specifikaciji, ne dokerizuje:

```bash
cd frontend
npm install
npm run dev
```

Interfejs je dostupan na `http://localhost:5173`.

> Nakon pokretanja backend-a potrebno je sacekati oko 30 sekundi da se svi servisi registruju
> na Eureka server i da API-Gateway povuce registar. Do tada gateway vraca poruku da ciljni
> mikroservis jos nije registrovan.

---

## Docker

Slike su okacene na Docker Hub nalog **[marko528](https://hub.docker.com/u/marko528)**, sve sa
`latest` tagom:

| Mikroservis | Docker Hub slika |
|---|---|
| naming-server | `marko528/soas-naming-server:latest` |
| users-service | `marko528/soas-users-service:latest` |
| currency-exchange | `marko528/soas-currency-exchange:latest` |
| currency-conversion | `marko528/soas-currency-conversion:latest` |
| bank-account | `marko528/soas-bank-account:latest` |
| crypto-wallet | `marko528/soas-crypto-wallet:latest` |
| crypto-exchange | `marko528/soas-crypto-exchange:latest` |
| trade-service | `marko528/soas-trade-service:latest` |
| api-gateway | `marko528/soas-api-gateway:latest` |

Pomocne skripte:

```powershell
.\scripts\docker-build.ps1    # Maven build + izgradnja svih devet slika
.\scripts\docker-push.ps1     # slanje slika na Docker Hub (prethodno: docker login)
```

---

## Korisnicki interfejs

React aplikacija pokriva funkcionalnosti svih mikroservisa osim naming-server-a i API-Gateway-a.

| Stranica | Putanja | Mikroservis | Pristup |
|---|---|---|---|
| Kursevi | `/kursevi` | currency-exchange, crypto-exchange | **bez prijave** |
| Prijava | `/prijava` | API-Gateway (basic auth) | - |
| Korisnici | `/korisnici` | users-service | OWNER, ADMIN |
| Bankovni racuni | `/racuni` | bank-account | ADMIN, USER |
| Crypto novcanici | `/novcanici` | crypto-wallet | ADMIN, USER |
| Razmena fiat valuta | `/razmena-fiat` | currency-conversion | USER |
| Trgovina crypto | `/trgovina` | trade-service | USER |

Kako specifikacija nalaze, jedino stranica sa kursevima stoji ispred Login stranice - sve ostale
funkcionalnosti zahtevaju prijavu.

---

## Autorizacija po ulogama

Autentikacija se obavlja **iskljucivo na API-Gateway-u** (basic autentikacija, provera nad
users-service-om). Gateway zatim mikroservisima prosledjuje identitet korisnika kroz zaglavlja
`X-Auth-Email` i `X-Auth-Role`, koja klijent ne moze da falsifikuje jer ih gateway uvek prvo
uklanja iz dolaznog zahteva.

| Mikroservis | OWNER | ADMIN | USER |
|---|---|---|---|
| users-service | dodaje, azurira i brise sve korisnike | dodaje i azurira samo korisnike sa ulogom USER | nema pristup |
| bank-account | nema pristup | dodaje, azurira i pregleda sve racune | pregleda samo svoj racun |
| crypto-wallet | nema pristup | dodaje, azurira i pregleda sve novcanike | pregleda samo svoj novcanik |
| currency-exchange | pristup | pristup | pristup |
| crypto-exchange | pristup | pristup | pristup |
| currency-conversion | nema pristup | nema pristup | pristup |
| trade-service | nema pristup | nema pristup | pristup |

U sistemu moze postojati **samo jedan** korisnik sa ulogom OWNER.

### Automatsko kreiranje i brisanje racuna

- Dodavanjem korisnika sa ulogom USER, users-service Feign pozivima kreira njegov bankovni racun
  (`EUR 0`) i crypto novcanik (`ETH 0`).
- Brisanjem korisnika sa ulogom USER brisu se i njegov racun i novcanik.
- Promenom email adrese korisnika azuriraju se i racun i novcanik.

---

## Obrada izuzetaka

Modul **util** sadrzi `GlobalExceptionHandler` koji koriste svi mikroservisi. Svaki izuzetak se
pretvara u jedinstven JSON odgovor sa statusnim kodom i jasnim tekstualnim objasnjenjem - korisnik
nikada ne dobija stack-trace.

```json
{
  "timestamp": "24-08-2026 21:51:12",
  "status": 400,
  "error": "Bad Request",
  "message": "Nedovoljno sredstava: na racunu je dostupno 800.00 EUR, a za razmenu je trazeno 999999 EUR.",
  "path": "/currency-conversion"
}
```

| Situacija | Status |
|---|---|
| Resurs ne postoji | 404 |
| Nedovoljno sredstava, neispravan zahtev, neispravni podaci | 400 |
| Uloga nije autorizovana | 403 |
| Korisnik/racun sa datim podacima vec postoji | 409 |
| Eksterni servis ili mikroservis nedostupan, circuit breaker otvoren | 503 |

Greske koje jedan mikroservis primi od drugog (Feign) se ne prikazuju kao tehnicka greska, vec se
prosledjuju sa originalnim statusnim kodom i originalnom porukom.

API-Gateway je reaktivna aplikacija i koristi sopstveni `GatewayExceptionHandler` koji vraca
identican format greske.

---

## Fault tolerance

U trade mikroservisu su implementirana **oba** mehanizma (specifikacija trazi jedan):

- **Retry** - poziv ka crypto-exchange servisu se ponavlja do tri puta sa eksponencijalnim
  cekanjem. Poslovne greske (nedovoljno sredstava, neispravan zahtev) se ne ponavljaju.
- **Circuit breaker** - ako vise od 50% poziva u posmatranom prozoru padne, kolo se otvara na 20
  sekundi i naredni pozivi se odmah odbijaju umesto da cekaju timeout.

Kada nijedan pokusaj ne uspe ili je kolo otvoreno, poziva se fallback metoda koja korisniku vraca
status 503 i razumljivu poruku.

Stanje kola se moze pratiti na `http://localhost:8600/actuator/circuitbreakers`.

---

## Struktura projekta

```
Projekat SOAS/
├── pom.xml                     roditeljski Maven projekat
├── docker-compose.yaml         pokretanje kompletne aplikacije u Docker-u
├── README.md
│
├── util/                       globalna obrada izuzetaka za sve mikroservise
├── service-library/            DTO objekti, Feign proxy-ji, provera ovlascenja
│
├── naming-server/              Eureka server                        (8761)
├── users-service/              korisnici + H2 baza                  (8770)
├── currency-exchange/          kursevi fiat valuta                  (8000)
├── currency-conversion/        razmena fiat valuta                  (8100)
├── bank-account/               bankovni racuni + H2 baza            (8200)
├── crypto-wallet/              crypto novcanici + H2 baza           (8300)
├── crypto-exchange/            kursevi kripto valuta                (8400)
├── trade-service/              razmena fiat i crypto valuta         (8600)
├── api-gateway/                ulazna tacka + basic autentikacija   (8765)
│
├── frontend/                   React korisnicki interfejs           (5173)
└── scripts/                    skripte za Docker build i push
```

### Uloga zajednickih modula

**service-library** - sadrzi sve DTO objekte (`UserDto`, `BankAccountDto`, `CryptoWalletDto`,
`ExchangeRateDto`, `CryptoRateDto`, `ConversionResponse`, `TradeResponse`), sve Feign proxy-je
(`UsersServiceProxy`, `BankAccountProxy`, `CryptoWalletProxy`, `CurrencyExchangeProxy`,
`CryptoExchangeProxy`, `CurrencyConversionProxy`) i pomocne komponente za autorizaciju
(`AuthContext`) i prosledjivanje identiteta kroz Feign pozive.

**util** - sadrzi `GlobalExceptionHandler`, jedinstveni format greske `ErrorResponse` i sve
prilagodjene izuzetke poslovne logike.
