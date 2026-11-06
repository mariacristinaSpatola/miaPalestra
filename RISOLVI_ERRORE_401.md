# 🔐 Come risolvere l'errore 401 in Swagger

## 📌 Il Problema

Quando provi a testare **POST /api/utenti** (creazione nuovo utente) in Swagger, ricevi:

```
Error: response status is 401 Unauthorized
```

### Causa

L'endpoint POST `/api/utenti` è **protetto** e richiede:
1. ✅ Autenticazione (login)
2. ✅ Ruolo ADMIN
3. ✅ Token CSRF nell'header

Swagger **non ti autentica automaticamente**, quindi devi farlo tu manualmente.

---

## ✅ Soluzione: 3 Step

### **STEP 1** - Ottieni il Token CSRF

Il token CSRF protegge da attacchi di tipo cross-site request forgery.

1. Apri Swagger UI: **http://localhost:8080/swagger-ui.html**
2. Espandi il gruppo **"Autenticazione"**
3. Clicca su **GET /api/auth/csrf**
4. Clicca il bottone **"Try it out"**
5. Clicca **"Execute"**

**Risposta (200 OK):**
```json
{
  "token": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
}
```

✅ **Copia il valore del `token`** - Lo userai negli step seguenti!

---

### **STEP 2** - Esegui il Login come ADMIN

1. Nel gruppo **"Autenticazione"**, clicca su **POST /api/auth/login**
2. Clicca **"Try it out"**
3. Nel campo **"Request body"**, sostituisci il contenuto con:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

4. **IMPORTANTE - Aggiungi l'header CSRF**:
   - Scorri verso il basso fino a trovare la sezione **"Headers"**
   - Clicca **"Add header"**
   - Nome: `X-CSRF-TOKEN`
   - Valore: `<incolla_il_token_da_step_1>`
   
   Esempio:
   ```
   X-CSRF-TOKEN: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
   ```

5. Clicca **"Execute"**

**Risposta (200 OK):**
```json
{
  "id": 1,
  "username": "admin",
  "firstName": "Admin",
  "lastName": "User",
  "email": "admin@gym.local",
  "phone": "0000000000",
  "role": "ADMIN",
  "active": true
}
```

✅ **Ora sei autenticato come ADMIN** - La sessione è salvata nei cookie!

---

### **STEP 3** - Crea il nuovo utente

1. Nel gruppo **"Gestione Utenti"**, clicca su **POST /api/utenti**
2. Clicca **"Try it out"**
3. Nel campo **"Request body"**, inserisci i dati del nuovo utente:

```json
{
  "username": "mario.rossi",
  "password": "Password123!",
  "firstName": "Mario",
  "lastName": "Rossi",
  "email": "mario.rossi@example.com",
  "phone": "3331234567",
  "role": "CUSTOMER"
}
```

4. **Aggiungi l'header CSRF** (come nello STEP 2):
   - Nome: `X-CSRF-TOKEN`
   - Valore: `<stesso_token_da_step_1>`

5. Clicca **"Execute"**

**Risposta (201 Created):**
```json
{
  "id": 2,
  "username": "mario.rossi",
  "firstName": "Mario",
  "lastName": "Rossi",
  "email": "mario.rossi@example.com",
  "phone": "3331234567",
  "role": "CUSTOMER",
  "active": true
}
```

✅ **Utente creato con successo!**

---

## 🎯 Valori Accettati

### Campo `role`
```
ADMIN      - Accesso totale, gestisce utenti e sistema
SECRETARY  - Gestisce clienti e prenotazioni
CUSTOMER   - Cliente palestra, gestisce proprie prenotazioni
```

### Campo `password`
- Minimo 8 caratteri
- Deve contenere almeno un numero
- Deve contenere almeno una lettera maiuscola
- Deve contenere almeno una lettera minuscola

Esempio valido: `Password123!`

---

## ❌ Cosa succede se sbagli?

### Errore 401 Unauthorized
**Causa**: Non sei loggato o la sessione è scaduta
- Ripeti STEP 2 (login)

### Errore 403 Forbidden
**Causa**: Sei loggato ma non hai il ruolo ADMIN
- Assicurati di usare username: `admin`
- Il ruolo ADMIN è necessario per creare utenti

### Errore 400 Bad Request
**Cause possibili**:
- ❌ Password non soddisfa i criteri (troppo corta, senza maiuscola, etc.)
- ❌ Username già esiste
- ❌ Email non valida
- ❌ Ruolo sconosciuto
- ❌ Header CSRF mancante o errato

**Soluzione**: Controlla il messaggio di errore nella risposta e modifica i dati

---

## 💡 Verifiche Rapide

### Come sapere se sono loggato?

Chiama `GET /api/utenti/me` senza parametri:
- ✅ **200 OK** = Sei loggato
- ❌ **401 Unauthorized** = Non sei loggato

### Come sapere se ho il ruolo ADMIN?

Chiama `GET /api/utenti`:
- ✅ **200 OK** + lista utenti = Sei ADMIN
- ❌ **403 Forbidden** = Non sei ADMIN

---

## 📝 Alternative: Usa cURL da Terminale

Se preferisci testare da terminale (es. PowerShell):

```powershell
# Step 1: Ottieni token CSRF
$csrf = (Invoke-WebRequest -Uri "http://localhost:8080/api/auth/csrf" -UseBasicParsing).Content | ConvertFrom-Json | Select-Object -ExpandProperty token

Write-Host "Token CSRF: $csrf"

# Step 2: Login e salva i cookie
$loginBody = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

$headers = @{ "X-CSRF-TOKEN" = $csrf }
Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $loginBody `
    -Headers $headers `
    -SessionVariable session

Write-Host "Login effettuato"

# Step 3: Crea nuovo utente
$newUserBody = @{
    username = "mario.rossi"
    password = "Password123!"
    firstName = "Mario"
    lastName = "Rossi"
    email = "mario.rossi@example.com"
    phone = "3331234567"
    role = "CUSTOMER"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8080/api/utenti" `
    -Method POST `
    -ContentType "application/json" `
    -Body $newUserBody `
    -Headers $headers `
    -WebSession $session
```

---

## 🔑 Credenziali di Default

| Campo | Valore |
|-------|--------|
| Username | `admin` |
| Password | `admin123` |
| Role | `ADMIN` |

> ⚠️ **In produzione**: Cambia queste credenziali nel file `application.properties` e NON commitarle nel repository!

---

## 📞 Ancora problemi?

1. ✅ Assicurati che l'app sia in esecuzione: http://localhost:8080
2. ✅ Ricarica Swagger UI: F5
3. ✅ Verifica di aver copiato il token CSRF correttamente (senza spazi)
4. ✅ Prova a fare logout e rifare il login
5. ✅ Controlla il file `target/surefire-reports` se i test falliscono
