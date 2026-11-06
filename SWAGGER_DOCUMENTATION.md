# Documentazione Swagger - miaPalestra

## Sommario

Sono state aggiunte **descrizioni dettagliate** a tutti i servizi REST implementati utilizzando le annotazioni **OpenAPI 3.0** di springdoc-openapi. La documentazione è ora visibile in **Swagger UI** e nell'**OpenAPI specification JSON**.

## Modifiche apportate

### 1. AuthController (`/api/auth`)

**Tag**: "Autenticazione"
**Descrizione**: Endpoint per la gestione dell'autenticazione e delle sessioni

#### Endpoint documentati:

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| `GET` | `/api/auth/csrf` | Recupera il token CSRF necessario per le richieste modificative |
| `POST` | `/api/auth/login` | Autentica un utente e crea una sessione |
| `POST` | `/api/auth/logout` | Termina la sessione dell'utente |

**Dettagli**:
- ✅ Ogni endpoint ha una descrizione esplicita in italiano
- ✅ Ogni endpoint ha una lista di risposte possibili (200, 400, 401, 403, 404, etc.)
- ✅ Ogni risposta è documentata con il suo significato
- ✅ I parametri di input (dove presenti) sono documentati

### 2. UtentiController (`/api/utenti`)

**Tag**: "Gestione Utenti"
**Descrizione**: Endpoint per la gestione degli utenti, loro profili e password

#### Endpoint documentati:

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| `POST` | `/api/utenti` | Crea un nuovo utente (solo ADMIN) |
| `GET` | `/api/utenti` | Elenca tutti gli utenti (solo ADMIN) |
| `GET` | `/api/utenti/clienti` | Cerca clienti per nome/email (ADMIN, SECRETARY) |
| `GET` | `/api/utenti/me` | Recupera il profilo dell'utente corrente |
| `PATCH` | `/api/utenti/me` | Aggiorna il proprio profilo |
| `PUT` | `/api/utenti/me/password` | Cambia la propria password |
| `GET` | `/api/utenti/{id}` | Recupera i dettagli di un utente (solo ADMIN) |
| `PUT` | `/api/utenti/{id}` | Aggiorna i dati di un utente (solo ADMIN) |
| `DELETE` | `/api/utenti/{id}` | Disattiva un utente (solo ADMIN) |

**Dettagli per ogni operazione**:
- ✅ Summary breve e descrizione dettagliata
- ✅ Codici di risposta HTTP: 200, 201, 204, 400, 401, 403, 404
- ✅ Descrizione di ogni codice di risposta
- ✅ Parametri path e query documentati
- ✅ Schema di request/response

## Annotazioni utilizzate

### Import aggiunti:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
```

### Esempio di utilizzo:

```java
@GetMapping("/clienti")
@PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
@Operation(summary = "Cerca clienti", 
    description = "Ricerca i clienti per nome, cognome o email...")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Ricerca completata..."),
    @ApiResponse(responseCode = "403", description = "Accesso negato...")
})
public List<UtenteRisposta> cercaClienti(
    @Parameter(description = "Stringa di ricerca...")
    @RequestParam String query) {
    // ...
}
```

## Accesso alla documentazione

### Swagger UI
Avvia l'applicazione:
```bash
mvn spring-boot:run
```

Accedi a:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

### Struttura della documentazione

La documentazione è organizzata per **tag** (gruppi logici):

1. **Autenticazione** - Gestione login, logout e CSRF
2. **Gestione Utenti** - CRUD utenti e gestione profili

## File modificati

- ✅ `src/main/java/it/gym/Prenotazioni/web/AuthController.java`
- ✅ `src/main/java/it/gym/Prenotazioni/web/UtentiController.java`

## Dipendenza utilizzata

La documentazione si basa su:
- **springdoc-openapi-starter-webmvc-ui** 3.2.0
- Supporta OpenAPI 3.0 standard

## Note di implementazione

1. ✅ Tutte le annotazioni usano testi in **italiano**
2. ✅ Ogni operazione include summary breve e descrizione dettagliata
3. ✅ Sono documentati tutti i codici HTTP possibili
4. ✅ I parametri path e query includono descrizioni
5. ✅ Gli endpoint sono raggruppati per tag tematico
6. ✅ Le risposte includono lo schema esperato
7. ✅ Non sono state apportate modifiche alla logica del codice

## Test

Il build continua a passare con **16 test verdi** e **0 errori**:

```
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Prossimi step

Quando verranno implementati ulteriori controller (ad es. per Corsi, Programmazione, Prenotazioni), seguire lo stesso pattern:
1. Aggiungere `@Tag` sulla classe
2. Aggiungere `@Operation` su ogni metodo
3. Aggiungere `@ApiResponses` con i possibili esiti
4. Documentare i parametri con `@Parameter`
