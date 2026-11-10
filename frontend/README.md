# formaFit frontend

Frontend indipendente in Angular 22.2.6. I dati gestionali vengono letti e modificati esclusivamente tramite le API REST del backend Spring.

## Avvio locale

Avviare il backend nella root del repository:

```powershell
.\mvnw.cmd spring-boot:run
```

In un secondo terminale:

```powershell
cd frontend
npm ci
npm start
```

Aprire `http://localhost:4200`. Il dev server inoltra `/api` a `http://localhost:8080`; in produzione configurare il reverse proxy per mantenere frontend e API sulla stessa origine, oppure consentire in CORS l'origine frontend esatta. L'autenticazione usa la sessione HttpOnly e l'interceptor richiede il token CSRF prima di ogni scrittura.

I clienti possono registrarsi dalla pagina di accesso; il backend assegna sempre il ruolo `CUSTOMER`. Per il primo accesso, creare l'amministratore seguendo la procedura di bootstrap nel README della root. L'amministratore crea gli account di segreteria.

## Verifica

```powershell
npm test -- --watch=false
npm run build
```

Le foto e i font attualmente utilizzati sono caricati da Unsplash e Google Fonts; sostituirli con asset approvati della palestra prima del rilascio pubblico.
