# MiaPalestra

Backend REST per la gestione dei corsi e delle prenotazioni di una palestra.

## Requisiti

- Java 26
- Maven Wrapper (non è necessaria un'installazione Maven globale)

## Avvio in sviluppo

```powershell
./mvnw.cmd spring-boot:run
```

Il profilo predefinito è `dev`: usa H2 persistente in `./data/gymdb` e abilita la console H2 su `/h2-console` con utente `sa` e password vuota. Il database e le credenziali locali non vengono versionati.

Per compilare ed eseguire i test:

```powershell
./mvnw.cmd verify
```

## Bootstrap iniziale ADMIN

Il bootstrap è disabilitato per impostazione predefinita. Per la prima esecuzione, impostare le credenziali nell'ambiente e abilitarlo esplicitamente:

```powershell
$env:APP_BOOTSTRAP_ADMIN_ENABLED = "true"
$env:APP_BOOTSTRAP_ADMIN_USERNAME = "admin"
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = "<password-forte>"
$env:APP_BOOTSTRAP_ADMIN_FIRST_NAME = "Admin"
$env:APP_BOOTSTRAP_ADMIN_LAST_NAME = "Palestra"
./mvnw.cmd spring-boot:run
```

Dopo la creazione, fermare l'applicazione e disabilitare il bootstrap (`APP_BOOTSTRAP_ADMIN_ENABLED=false` o rimuovere la variabile). Se lo username esiste già, il bootstrap non crea né modifica account. La password viene salvata con BCrypt e non viene scritta nei log. Non salvare le credenziali in file versionati.

## Autenticazione e utenti

L'API usa sessioni HTTP con cookie `MIA_PALESTRA_SESSION` (timeout inattività 30 minuti). Tutte le rotte, salvo `GET /api/auth/csrf` e `POST /api/auth/login`, richiedono un'identità autenticata. Per accedere, il client recupera il token CSRF da `/api/auth/csrf`, invia username e password a `/api/auth/login` con il token nell'header `X-CSRF-TOKEN` e conserva il cookie di sessione. Dopo il login, richiedere un nuovo token CSRF per le richieste `POST`, `PUT`, `PATCH` e `DELETE`, incluso il logout (`POST /api/auth/logout`). Il logout invalida la sessione.

Il cookie è HttpOnly e SameSite=Lax; nel profilo `prod` è anche Secure e richiede HTTPS. Le origini cross-origin autorizzate si configurano con `APP_CORS_ALLOWED_ORIGINS`, come elenco separato da virgole di origini esatte; il valore predefinito vuoto non abilita richieste cross-origin. Le richieste CORS consentono credenziali, senza wildcard.

Solo ADMIN può creare, elencare, modificare e disattivare utenti (`/api/utenti`). ADMIN e SECRETARY possono cercare clienti (`GET /api/utenti/clienti?query=...`, almeno due caratteri). Ogni utente autenticato può consultare o modificare il proprio profilo (`/api/utenti/me`) e cambiare la propria password verificando quella corrente. Non esiste registrazione pubblica. L'identità delle operazioni personali deriva dalla sessione e le risposte non contengono password o hash.

## Profili e schema

Il profilo `dev` usa il database H2 su file e abilita la console. Il profilo `prod` disabilita la console e richiede `DB_USERNAME` e `DB_PASSWORD`; `DB_URL` può sovrascrivere l'URL H2 predefinito. In entrambi i profili Flyway gestisce lo schema e Hibernate usa `ddl-auto=validate`; date e orari applicativi adottano `Europe/Rome`.

Le migrazioni creano utenti, corsi, programmazioni, occorrenze e prenotazioni. Sono disponibili le API di autenticazione e gestione utenti; gli altri casi d'uso descritti nel piano sono in fasi successive.
