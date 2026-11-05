# Piano di implementazione: MiaPalestra

## Obiettivo e perimetro

Realizzare un backend REST per la gestione di corsi, programmazione, lezioni e prenotazioni della palestra, con ruoli ADMIN, SECRETARY e CUSTOMER. Il backend usa Java 26, Spring Boot, Maven, Spring MVC, Spring Data JPA/Hibernate, Spring Security, H2 su file e Flyway. Il frontend Angular è previsto come applicazione separata e sarà affrontato dopo il backend.

L'applicazione conserva solo lo stato corrente delle prenotazioni: non introduce audit, cronologie delle modifiche, conferme email o canali chatbot esterni.

## Fasi

### 1. Fondazione

- Creare il progetto Maven e Maven Wrapper con Spring Boot MVC compatibile con Java 26; organizzare i package sotto `it.gym.Prenotazioni`.
- Configurare profili `dev` e `prod`, H2 su file, fuso `Europe/Rome`, `spring.jpa.open-in-view=false` e Hibernate `ddl-auto=validate`.
- Abilitare la console H2 solo in sviluppo e mantenere segreti e credenziali fuori dal repository.
- Introdurre Flyway e la migrazione iniziale per `utenti` e `corsi`, con mapping JPA minimo; inserire Yoga, Pilates e Acquagym.
- Provisionare l'ADMIN iniziale solo tramite bootstrap esplicito e credenziali esterne, usando BCrypt. Il bootstrap non deve duplicare o reimpostare account esistenti, né esporre segreti nei log.

**Uscita:** avvio pulito e migrazione riusciti, Hibernate valida lo schema, corsi iniziali presenti e bootstrap admin sicuro e idempotente.

### 2. Modello dati e repository

- Completare entità, enum, relazioni, repository e successive migrazioni Flyway per `Programma_Settimanale`, `Agenda_Corsi` e `Prenotazioni`.
- Definire chiavi esterne, indici e vincoli: programmazione + data univoci per occorrenza; cliente + occorrenza univoci per prenotazione.
- Modellare ruoli, stati, canali, validità e campi temporali come da requisiti.
- Prevedere conteggio delle sole prenotazioni `CONFIRMED` e accesso con lock pessimista alla lezione.
- Non creare una tabella di audit: `CANCELLED` resta lo stato corrente del record di prenotazione.

**Uscita:** migrazioni applicabili da zero e mapping JPA coerente con Hibernate `validate`.

### 3. Sicurezza e gestione utenti

- Configurare Spring Security, BCrypt, identità autenticata e autorizzazioni server-side su ogni endpoint.
- Implementare gestione utenti per ADMIN e ricerca clienti per SECRETARY; nella prima versione niente registrazione pubblica.
- Consentire al CUSTOMER di operare solo sulla propria identità; ADMIN/SECRETARY possono agire per conto del cliente nei limiti del ruolo.
- Stabilire il meccanismo di autenticazione REST (sessione/cookie oppure token), gestione delle sessioni e protezione CSRF/CORS coerente con Angular futuro.

**Uscita:** test di autorizzazione per tutti i ruoli e nessuna fiducia nell'identità fornita dal client.

### 4. Corsi e programmazione settimanale

- Implementare DTO, servizi e API per consultare e gestire corsi e programmazioni ricorrenti.
- Validare giorno ISO 1-7, orario, capienza, intervalli di validità, duplicati e sovrapposizioni involontarie.
- Per una modifica stabile chiudere la validità della regola precedente e crearne una nuova con decorrenza, senza sovrascrivere occorrenze già generate.
- Un corso disattivato non genera nuove occorrenze, ma non elimina quelle esistenti né le prenotazioni.

**Uscita:** CRUD autorizzato e validazione delle regole ricorrenti.

### 5. Agenda e disponibilità

- Generare in `Europe/Rome` le occorrenze mancanti per oggi e i successivi 14 giorni, all'avvio e quotidianamente; valutare trigger dopo modifiche alla programmazione.
- Rispettare giorno e validità della programmazione. La generazione è idempotente e non sovrascrive orario, capienza o stato personalizzati.
- Implementare consultazione delle lezioni prenotabili e disponibilità calcolata come capienza meno prenotazioni confermate.
- Consentire all'ADMIN di modificare orario e capienza della singola occorrenza e annullare una lezione.
- Impedire duplicati, prenotazioni su lezioni cancellate e riduzioni di capienza sotto le prenotazioni confermate.

**Uscita:** scheduler deterministico, idempotente e verificato sui confini temporali.

### 6. Prenotazioni

- Implementare un unico `PrenotazioniService` per i canali WEB, CHATBOT e SECRETARY.
- In transazione bloccare la lezione con `PESSIMISTIC_WRITE`; verificare stato, finestra temporale, prenotazione esistente e posti disponibili; infine creare o riattivare la prenotazione.
- Una prenotazione annullata non occupa posto; il record può essere riattivato. Gestire anche il vincolo univoco in caso di concorrenza.
- Consentire più lezioni al giorno e lo stesso corso in date diverse, mantenendo un solo record cliente/occorrenza.
- Applicare la stessa regola temporale a prenotazione e annullamento: inizio prima delle 12:00, entro il giorno precedente; inizio alle 12:00 o dopo, fino a 2 ore prima.

**Uscita:** correttezza con ultimo posto conteso, stato CANCELLED e rispetto dei cutoff.

### 7. API REST e chatbot

- Esporre gli endpoint del requisito tramite DTO, validazione e status HTTP coerenti, includendo quelli mancanti per segreteria e gestione utenti.
- Endpoint di riferimento: corsi, programmazioni admin, occorrenze e disponibilità, prenotazioni, annullamento, prenotazioni cliente e messaggi chatbot.
- Integrare Spring AI con provider configurabile da deployment; definire strumenti applicativi per cercare corsi/lezioni, controllare disponibilità e consultare/prenotare/annullare.
- L'AI non accede direttamente al database. L'identità deriva sempre dal principal autenticato; per prenotare o annullare serve conferma esplicita e il risultato è comunicato solo dopo esito positivo del servizio.
- Richiedere chiarimenti per input ambiguo e non inventare disponibilità.

**Uscita:** API coperte da test di sicurezza e contratto; chatbot testato con provider mock.

### 8. Hardening, documentazione e rilascio backend

- Completare README con setup, profili, provisioning admin, API, ruoli e regole operative.
- Aggiungere pipeline CI per build e test Maven; verificare configurazione dei segreti e console H2 disabilitata fuori da `dev`.
- Documentare la scelta del meccanismo auth e la configurazione del provider AI.
- Rivalutare H2 su file prima di un'esposizione produttiva e definire backup, ripristino e migrazione a un DB server qualora richiesto.

**Uscita:** backend riproducibile, verificabile e configurabile senza segreti versionati.

### 9. Frontend Angular (fase successiva)

- Analizzare separatamente architettura, autenticazione, flussi customer e strumenti operativi per segreteria/admin.
- Consumare le API REST senza incorporare template o asset Angular nel backend.

**Uscita:** piano e implementazione frontend da approvare dopo la consegna backend.

## Verifiche trasversali

- `mvnw.cmd verify` con Java 26, eseguito localmente e in CI.
- Test repository e integrazione per schema, vincoli univoci, query disponibilità e lock.
- Test servizi per cutoff prima/dalle 12:00, capienza piena, lezione cancellata, riduzione capienza, riattivazione e autorizzazioni.
- Test concorrente sull'ultimo posto: una sola prenotazione deve essere confermata.
- Test scheduler per intervallo oggi-oggi+14, fuso Europe/Rome, validità, doppia esecuzione e preservazione delle personalizzazioni.
- Test API per DTO, validazione, status e permessi; test chatbot mock per identità, ambiguità, conferma esplicita ed esito del backend.
- Test di avvio da database vuoto e riavvio su H2 persistente.

## Decisioni già concordate

- La prima implementazione riguarda il backend; Angular viene affrontato in una fase successiva.
- La finestra prenotabile include oggi e i successivi 14 giorni.
- Alle 12:00 si applica il cutoff di 2 ore prima; prima delle 12:00 il cutoff è il giorno precedente, interpretato come fine della giornata precedente in `Europe/Rome`.
- Gli account CUSTOMER sono creati da ADMIN/SECRETARY; non è prevista registrazione pubblica nella prima versione.
- Flyway gestisce lo schema e Hibernate usa `validate`; admin iniziale tramite segreti esterni e BCrypt.
- Nessuna tabella audit, storico modifiche, conferma email o chatbot su canali esterni.
- Il meccanismo auth REST e i dettagli di deployment H2/produzione vanno fissati prima della rispettiva implementazione.