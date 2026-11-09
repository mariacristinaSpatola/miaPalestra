package it.gym.Prenotazioni.web;

import it.gym.Prenotazioni.exception.PasswordCorrenteNonValidaException;
import it.gym.Prenotazioni.exception.AccessoNonAutorizzatoException;
import it.gym.Prenotazioni.exception.CorsoGiaEsistenteException;
import it.gym.Prenotazioni.exception.CorsoNonTrovatoException;
import it.gym.Prenotazioni.exception.MiaPalestraException;
import it.gym.Prenotazioni.exception.ProgrammaSettimanaleNonTrovatoException;
import it.gym.Prenotazioni.exception.UtenteGiaEsistenteException;
import it.gym.Prenotazioni.exception.UtenteNonTrovatoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class ErroreController {

    @ExceptionHandler(UtenteNonTrovatoException.class)
    public ResponseEntity<Map<String, String>> nonTrovato(UtenteNonTrovatoException exception) {
        return errore(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({CorsoNonTrovatoException.class, ProgrammaSettimanaleNonTrovatoException.class})
    public ResponseEntity<Map<String, String>> risorsaNonTrovata(MiaPalestraException exception) {
        return errore(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(UtenteGiaEsistenteException.class)
    public ResponseEntity<Map<String, String>> giaEsistente(UtenteGiaEsistenteException exception) {
        return errore(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(AccessoNonAutorizzatoException.class)
    public ResponseEntity<Map<String, String>> accessoNonAutorizzato(AccessoNonAutorizzatoException exception) {
        return errore(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(CorsoGiaEsistenteException.class)
    public ResponseEntity<Map<String, String>> corsoGiaEsistente(CorsoGiaEsistenteException exception) {
        return errore(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(MiaPalestraException.class)
    public ResponseEntity<Map<String, String>> regolaNonValida(MiaPalestraException exception) {
        return errore(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(PasswordCorrenteNonValidaException.class)
    public ResponseEntity<Map<String, String>> passwordNonValida(PasswordCorrenteNonValidaException exception) {
        return errore(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> richiestaNonValida(IllegalArgumentException exception) {
        return errore(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> credenzialiNonValide(AuthenticationException exception) {
        return errore(HttpStatus.UNAUTHORIZED, "Credenziali non valide.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> statoHttp(ResponseStatusException exception) {
        return errore(HttpStatus.valueOf(exception.getStatusCode().value()), exception.getReason());
    }

    private ResponseEntity<Map<String, String>> errore(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("errore", message == null ? status.getReasonPhrase() : message));
    }
}
