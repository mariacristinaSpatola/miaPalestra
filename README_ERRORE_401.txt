╔════════════════════════════════════════════════════════════════╗
║  SOLUZIONE: Errore 401 Unauthorized in Swagger                ║
╚════════════════════════════════════════════════════════════════╝

📝 PROBLEMA:
Quando provi a testare POST /api/utenti in Swagger, ricevi:
"Error: response status is 401 Unauthorized"

🔧 SOLUZIONE VELOCE (3 STEP):

Step 1️⃣  - Ottieni il Token CSRF
─────────────────────────────────────────
1. Apri Swagger: http://localhost:8080/swagger-ui.html
2. Espandi "Autenticazione" → GET /api/auth/csrf
3. Clicca "Try it out" → "Execute"
4. Copia il token dalla risposta

Step 2️⃣  - Login come ADMIN
─────────────────────────────────────────
1. Espandi "Autenticazione" → POST /api/auth/login
2. Clicca "Try it out"
3. Body:
   {
     "username": "admin",
     "password": "admin123"
   }
4. Aggiungi Header (scorri giù):
   Nome:  X-CSRF-TOKEN
   Valore: <token_da_step_1>
5. Clicca "Execute"

Step 3️⃣  - Crea il Nuovo Utente
─────────────────────────────────────────
1. Espandi "Gestione Utenti" → POST /api/utenti
2. Clicca "Try it out"
3. Body:
   {
     "username": "mario.rossi",
     "password": "Password123!",
     "firstName": "Mario",
     "lastName": "Rossi",
     "email": "mario@example.com",
     "phone": "3331234567",
     "role": "CUSTOMER"
   }
4. Aggiungi Header (scorri giù):
   Nome:  X-CSRF-TOKEN
   Valore: <token_da_step_1>
5. Clicca "Execute"

✅ RISULTATO ATTESO: 201 Created
L'utente è stato creato!

📚 DOCUMENTAZIONE COMPLETA:
→ Leggi "SOLUZIONE_ERRORE_401.md" per la guida dettagliata
→ Leggi "RISOLVI_ERRORE_401.md" per il troubleshooting

🔑 CREDENZIALI ADMIN:
Username: admin
Password: admin123

💡 ERRORI COMUNI:
- 401 = Non sei loggato (ripeti Step 2)
- 403 = Non hai il ruolo ADMIN
- 400 = Dati invalidi (password, username, etc.)

✅ VERIFICHE:
- Compilazione: BUILD SUCCESS ✓
- Test: 5/5 verdi ✓
- Swagger UI: http://localhost:8080/swagger-ui.html ✓
- OpenAPI Spec: http://localhost:8080/v3/api-docs ✓

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Sei pronto! Segui i 3 step sopra e il problema sarà risolto. 🚀
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
