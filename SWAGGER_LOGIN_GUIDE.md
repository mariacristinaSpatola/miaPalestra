# 🔐 Guida: Come testare gli endpoint protetti con Swagger

## Problema

Quando provi a testare POST `/api/utenti` ricevi **Error: 401 Unauthorized** perché l'endpoint richiede l'autenticazione con ruolo ADMIN.

## Soluzione: 3 step per testare

### Step 1️⃣ - Ottieni il token CSRF

1. Apri Swagger UI: http://localhost:8080/swagger-ui.html
2. Espandi **Autenticazione** → **GET /api/auth/csrf**
3. Clicca su **"Try it out"** → **"Execute"**
4. Copia il valore dal campo `token` nella risposta

**Esempio risposta:**
```json
{
  "token": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
}
```

### Step 2️⃣ - Accedi come ADMIN

1. Espandi **Autenticazione** → **POST /api/auth/login**
2. Clicca su **"Try it out"**
3. Nel campo **RequestBody**, inserisci le credenziali:
```json
{
  "username": "admin",
  "password": "admin123"
}
```
4. **Importante**: Prima di cliccare "Execute", aggiungi l'header CSRF:
   - Clicca su **Headers** (in basso)
   - Aggiungi header: `X-CSRF-TOKEN: <token_dal_step_1>`
5. Clicca **"Execute"**

**Esempio risposta (201 Created):**
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

### Step 3️⃣ - Crea un nuovo utente

1. Espandi **Gestione Utenti** → **POST /api/utenti**
2. Clicca su **"Try it out"**
3. Nel campo **RequestBody**, inserisci i dati del nuovo utente:
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
4. Aggiungi l'header CSRF (stesso token di Step 1):
   - **Headers** → `X-CSRF-TOKEN: <token_dal_step_1>`
5. Clicca **"Execute"**

**Risultato**: Se tutto è corretto, riceverai **201 Created** ✅

---

## ⚠️ Cosa causa l'errore 401?

- ❌ Non sei loggato (non hai eseguito Step 2)
- ❌ La sessione è scaduta
- ❌ Non hai il ruolo ADMIN

## ⚠️ Cosa causa l'errore 403?

- ❌ Sei loggato ma non hai il ruolo ADMIN (es. sei CUSTOMER)
- ❌ Manca l'header CSRF

## 💡 Tip: Come sapere se sei loggato?

Chiama GET `/api/utenti/me` - Se ricevi 200 ✅ sei loggato, se ricevi 401 ❌ non sei loggato.

## 🔧 Configurazione credenziali ADMIN

Le credenziali ADMIN di default sono configurate nel file `application.properties`:

```properties
app.bootstrap.admin.username=admin
app.bootstrap.admin.password=admin123
```

> **Nota**: In produzione, cambia queste credenziali nel file di configurazione e non commitarle nel repository!

---

## Alternativa: Disabilita autenticazione per test locali

Se vuoi testare senza autenticazione durante lo sviluppo, contatta il team per una configurazione di test alternativa.

---

## Test con cURL (da terminale)

Se preferisci testare da terminale:

```bash
# Step 1: Ottieni token CSRF
curl -s http://localhost:8080/api/auth/csrf | jq -r '.token'

# Step 2: Login
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-CSRF-TOKEN: <token_da_step_1>" \
  -d '{"username":"admin","password":"admin123"}' \
  -c cookies.txt | jq

# Step 3: Crea utente
curl -s -X POST http://localhost:8080/api/utenti \
  -H "Content-Type: application/json" \
  -H "X-CSRF-TOKEN: <token_da_step_1>" \
  -b cookies.txt \
  -d '{
    "username":"mario.rossi",
    "password":"Password123!",
    "firstName":"Mario",
    "lastName":"Rossi",
    "email":"mario.rossi@example.com",
    "phone":"3331234567",
    "role":"CUSTOMER"
  }' | jq
```
