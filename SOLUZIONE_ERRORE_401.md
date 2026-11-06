# ✅ SOLUZIONE: Errore 401 in Swagger - Come Testare gli Endpoint Protetti

## 🎯 Problema Risolto

**Errore**: `Error: response status is 401 Unauthorized` quando provi a creare un utente

**Causa**: Gli endpoint POST `/api/utenti` (e altri che modificano i dati) richiedono:
- ✅ Login come utente autenticato
- ✅ Ruolo appropriato (es. ADMIN per creare utenti)
- ✅ Token CSRF nell'header `X-CSRF-TOKEN`

**Soluzione**: Segui i 3 step sottostanti

---

## 📝 Istruzioni Passo-Passo

### **STEP 1: Ottieni il Token CSRF** 🔑

Il token CSRF è una protezione di sicurezza contro attacchi di tipo "Cross-Site Request Forgery".

1. Apri Swagger UI: http://localhost:8080/swagger-ui.html
2. Espandi il gruppo **"Autenticazione"**
3. Clicca su **GET /api/auth/csrf**
4. Clicca il pulsante **"Try it out"**
5. Clicca **"Execute"**

**Risposta attesa (200 OK):**
```json
{
  "token": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
}
```

✏️ **Azione**: Copia il valore di `token` - Lo userai negli step seguenti!

---

### **STEP 2: Esegui il Login come ADMIN** 🔐

1. Nel gruppo **"Autenticazione"**, clicca su **POST /api/auth/login**
2. Clicca **"Try it out"**
3. Nel campo **Request body**, sostituisci il contenuto con:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

4. **FONDAMENTALE**: Aggiungi l'header CSRF
   - Scorri verso il basso fino alla sezione **"Headers"**
   - Clicca **"Add header"**
   - Nome header: `X-CSRF-TOKEN`
   - Valore: Incolla il token da STEP 1

   Esempio:
   ```
   X-CSRF-TOKEN: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
   ```

5. Clicca **"Execute"**

**Risposta attesa (200 OK):**
```json
{
  "id": 1,
  "username": "admin",
  "firstName": "Admin",
  "lastName": "User",
  "email": "admin@gym.local",
  "phone": "0000000000",
  "role": "ADMIN",
  "active": true,
  "createdAt": "2026-11-06T15:00:00"
}
```

✅ **Sei ora autenticato come ADMIN!** La sessione è memorizzata nei cookie.

---

### **STEP 3: Crea il Nuovo Utente** 👤

1. Nel gruppo **"Gestione Utenti"**, clicca su **POST /api/utenti**
2. Clicca **"Try it out"**
3. Nel campo **Request body**, inserisci i dati del nuovo utente:

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

4. **Aggiungi l'header CSRF** (stesso valore di STEP 2):
   - **Headers** → **Add header**
   - Nome: `X-CSRF-TOKEN`
   - Valore: `a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6`

5. Clicca **"Execute"**

**Risposta attesa (201 Created):**
```json
{
  "id": 2,
  "username": "mario.rossi",
  "firstName": "Mario",
  "lastName": "Rossi",
  "email": "mario.rossi@example.com",
  "phone": "3331234567",
  "role": "CUSTOMER",
  "active": true,
  "createdAt": "2026-11-06T15:10:00"
}
```

✅ **Utente creato con successo!**

---

## 📋 Riferimento Valori

### Ruoli disponibili (`role`)
| Ruolo | Descrizione | Permessi |
|-------|-------------|----------|
| `ADMIN` | Amministratore | Gestisce utenti, corsi, programmazioni, tutto |
| `SECRETARY` | Segretaria | Gestisce clienti e prenotazioni |
| `CUSTOMER` | Cliente | Gestisce solo le proprie prenotazioni |

### Criteri password
La password deve:
- ✅ Minimo 8 caratteri
- ✅ Contiene almeno una **MAIUSCOLA** (A-Z)
- ✅ Contiene almeno una **minuscola** (a-z)
- ✅ Contiene almeno un **numero** (0-9)

**Esempi validi**:
- `Password123!`
- `SecurePass99`
- `MyP@ssw0rd`

**Esempi INVALIDI**:
- `password` ❌ (manca maiuscola e numero)
- `123456` ❌ (solo numeri)
- `ADMIN` ❌ (solo maiuscole)

---

## ⚠️ Errori Comuni e Soluzioni

### ❌ Errore 401 Unauthorized
```
Error: response status is 401 Unauthorized
```
**Cause:**
- Non sei loggato (non hai completato STEP 2)
- La sessione è scaduta
- Hai sbagliato username/password

**Soluzione:**
- Ripeti STEP 2 con credenziali corrette (`admin`/`admin123`)

---

### ❌ Errore 403 Forbidden
```
Error: response status is 403 Forbidden
```
**Cause:**
- Sei loggato ma non hai il ruolo ADMIN
- Es. sei CUSTOMER o SECRETARY

**Soluzione:**
- Assicurati di loggare come `admin` nel STEP 2
- Per creare utenti serve il ruolo ADMIN

---

### ❌ Errore 400 Bad Request
```json
{
  "errore": "Username già esiste"
}
```
**Cause:**
- Username già utilizzato da un altro utente
- Email già registrata
- Password non soddisfa i criteri
- Ruolo sconosciuto

**Soluzione:**
- Usa uno username diverso
- Usa una password che soddisfi i criteri (maiuscola, minuscola, numero, 8+ char)
- Usa un ruolo valido: `ADMIN`, `SECRETARY`, `CUSTOMER`

---

## 🧪 Test Rapido: Sei Loggato?

Per verificare se la sessione è ancora attiva:

1. Nel gruppo **"Gestione Utenti"**, clicca su **GET /api/utenti/me**
2. **NON** è necessario aggiungere header o body
3. Clicca **"Try it out"** → **"Execute"**

**Risultati:**
- ✅ **200 OK** = Sei loggato
- ❌ **401 Unauthorized** = Non sei loggato (ripeti STEP 2)

---

## 🔧 Miglioramenti Implementati

✅ **OpenAPI spec migliorata**
- Aggiunto titolo e descrizione dell'API
- Documentazione dettagliata di ogni DTO (Data Transfer Object)
- Esempi di payload in Swagger

✅ **DTOs documentati**
- Ogni campo ha una descrizione
- Ogni campo ha un esempio di valore
- Vincoli evidenziati (min length, max length, etc.)

✅ **Guida completa**
- Questo documento ti spiega esattamente cosa fare
- Tutti gli errori comuni hanno una soluzione

---

## 📚 Documentazione Aggiunta

I seguenti file sono stati creati/aggiornati:

1. **RISOLVI_ERRORE_401.md** - Guida dettagliata (5977 bytes)
2. **SOLUZIONE_ERRORE_401.md** - Questo documento (questo file)
3. **SWAGGER_DOCUMENTATION.md** - Documentazione Swagger completa
4. **SWAGGER_LOGIN_GUIDE.md** - Guida step-by-step

---

## 🚀 Comandi Veloci

### Avvia l'app
```bash
cd C:\2024\Java\miaPalestra
.\mvnw.cmd spring-boot:run
```

### Accedi a Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### Accedi a OpenAPI spec
```
http://localhost:8080/v3/api-docs
```

---

## ✅ Verifiche Finali

- ✅ Compilazione: **BUILD SUCCESS**
- ✅ Test: **5/5 verdi**
- ✅ OpenAPI spec: Accessibile su http://localhost:8080/v3/api-docs
- ✅ Swagger UI: Disponibile su http://localhost:8080/swagger-ui.html
- ✅ DTOs documentati: Ogni campo ha descrizione e esempi
- ✅ Controlleri documentati: Ogni endpoint ha summary e responses

---

## 📞 Domande?

Se hai domande o problemi:
1. Verifica di aver seguito esattamente i 3 step
2. Assicurati di copiare il token CSRF correttamente (senza spazi)
3. Verifica che il ruolo sia valido: `ADMIN`, `SECRETARY`, o `CUSTOMER`
4. Controlla il formato della password (maiuscola, minuscola, numero, 8+ char)
