---
applyTo: "src/main/java/**"
description: "Regole di architettura da rispettare quando si scrive codice e da controllare in review"
---
# Architettura

Dipendenze ammesse tra i package di `it.gym.Prenotazioni`:

- `web` → `service`, `model`. **Mai** `repository`: un controller non apre il database.
- `mcp` → `service`, `model`. È un ingresso come `web`: i tool MCP chiamano i service e
  restituiscono record propri (mai entità JPA), **mai** `repository`.
- `service` → `model`, `repository`. Un metodo pubblico = un caso d'uso `@Transactional`.
- `model` → nessun altro package dell'applicazione
- `repository` → `model`. Solo interfacce Spring Data, nessuna logica.

Regole:
- Le regole di business stanno nel `model`; i service le orchestrano; controller e
  template non decidono nulla.
- Errori di dominio: sottoclassi di `MiaPalestraException`, messaggio in italiano.

In review segnala sempre:
- un `it.gym.Prenotazioni.repository` dentro `web` o `mcp`;
- un tool MCP che scrive con `readOnlyHint = true`, o che restituisce un'entità JPA;
- un `if` con una regola di business dentro un controller o un template;
- una regola nuova senza un test che ne copra i confini.
