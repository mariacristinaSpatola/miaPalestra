# Istruzioni per Copilot — miaPalestra

Applicazione miaPalestra. Java 26, Spring Boot 4.1.2 (Spring MVC, Spring Data JPA),
 H2 in memoria, JUnit 5.

## Architettura
- Il flusso è controller → service → model/repository. Il controller non contiene
  regole: traduce HTTP in chiamate al service e sceglie la pagina.
- Le regole di business stanno nelle entità del package `model`. I service le
  orchestrano in una transazione e non le duplicano.
- Gli errori di dominio estendono `MiaPalestraException`: messaggio in italiano, per l'utente.

## Regole che non si indovinano dal codice
<!-- - Importi sempre in `BigDecimal`, mai `double`: al centesimo, con `RoundingMode.HALF_UP`. -->
- Stati della Prenotazione: CONFIRMED, CANCELLED.
- Stati della AgendaCorsi: SCHEDULED,  CANCELLED.
- I canali di prenotazione:  WEB,    CHATBOT,    SECRETARY.
- I ruoli per gli utenti sono: ADMIN, SECRETARY,  CUSTOMER.
- Niente nuove dipendenze nel `pom.xml` senza chiederlo.
- Nomi di classi, metodi e test in italiano, come nel codice esistente.

## Build e verifica
- `./mvnw test` (Windows: `.\mvnw.cmd test`) resta verde dopo ogni modifica.
- Una feature non è finita senza test che ne verifichino le regole.


## ENvironmentL
OS: Debian GNU/Linux stable senza Z11 o Wayland, solo il minimo delle applicazioni necessarie installate

OpenJDK 25 or later
