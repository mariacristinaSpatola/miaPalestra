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

## Profili e schema

Il profilo `dev` usa il database H2 su file e abilita la console. Il profilo `prod` disabilita la console e richiede `DB_USERNAME` e `DB_PASSWORD`; `DB_URL` può sovrascrivere l'URL H2 predefinito. In entrambi i profili Flyway gestisce lo schema e Hibernate usa `ddl-auto=validate`; date e orari applicativi adottano `Europe/Rome`.

La migrazione iniziale crea `utenti` e `corsi` e inserisce Yoga, Pilates e Acquagym. Le API e i modelli delle fasi successive non sono ancora implementati.
