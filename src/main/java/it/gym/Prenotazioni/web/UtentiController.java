package it.gym.Prenotazioni.web;

import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.service.UtentiService;
import it.gym.Prenotazioni.web.dto.UtentiDto.AggiornaProfilo;
import it.gym.Prenotazioni.web.dto.UtentiDto.AggiornaUtente;
import it.gym.Prenotazioni.web.dto.UtentiDto.CambioPassword;
import it.gym.Prenotazioni.web.dto.UtentiDto.NuovoUtente;
import it.gym.Prenotazioni.web.dto.UtentiDto.UtenteRisposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/utenti")
@Tag(name = "Gestione Utenti", description = "Endpoint per la gestione degli utenti, loro profili e password")
public class UtentiController {

    private final UtentiService utentiService;

    public UtentiController(UtentiService utentiService) {
        this.utentiService = utentiService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un nuovo utente", 
        description = "Crea un nuovo utente con i dati forniti. Solo gli amministratori possono creare nuovi utenti.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Utente creato con successo", 
            content = @Content(schema = @Schema(implementation = UtenteRisposta.class))),
        @ApiResponse(responseCode = "400", description = "Dati non validi"),
        @ApiResponse(responseCode = "403", description = "Accesso negato - permessi insufficienti")
    })
    public UtenteRisposta crea(@RequestBody NuovoUtente richiesta) {
        return risposta(utentiService.creaUtente(richiesta.username(), richiesta.password(),
                richiesta.firstName(), richiesta.lastName(), richiesta.email(), richiesta.phone(),
                richiesta.role()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Elenca tutti gli utenti", 
        description = "Restituisce l'elenco di tutti gli utenti del sistema. Solo gli amministratori possono accedere a questo endpoint.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Elenco utenti recuperato con successo",
            content = @Content(schema = @Schema(implementation = UtenteRisposta.class))),
        @ApiResponse(responseCode = "403", description = "Accesso negato - permessi insufficienti")
    })
    public List<UtenteRisposta> elenco() {
        return utentiService.trovaTutti().stream().map(UtenteRisposta::da).toList();
    }

    @GetMapping("/clienti")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @Operation(summary = "Cerca clienti", 
        description = "Ricerca i clienti per nome, cognome o email. Amministratori e segretari possono accedere a questo endpoint.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ricerca completata con successo"),
        @ApiResponse(responseCode = "403", description = "Accesso negato - permessi insufficienti")
    })
    public List<UtenteRisposta> cercaClienti(
        @Parameter(description = "Stringa di ricerca (nome, cognome o email)")
        @RequestParam String query) {
        return utentiService.cercaClienti(query).stream().map(UtenteRisposta::da).toList();
    }

    @GetMapping("/me")
    @Operation(summary = "Recupera il profilo dell'utente corrente", 
        description = "Restituisce i dettagli dell'utente autenticato.")
    @ApiResponse(responseCode = "200", description = "Profilo recuperato con successo",
        content = @Content(schema = @Schema(implementation = UtenteRisposta.class)))
    public UtenteRisposta mioProfilo(Authentication authentication) {
        return risposta(utentiService.trovaPerUsername(authentication.getName()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Aggiorna il proprio profilo", 
        description = "Aggiorna i dati personali dell'utente autenticato (nome, cognome, email, telefono).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profilo aggiornato con successo",
            content = @Content(schema = @Schema(implementation = UtenteRisposta.class))),
        @ApiResponse(responseCode = "400", description = "Dati non validi")
    })
    public UtenteRisposta aggiornaMioProfilo(Authentication authentication,
            @RequestBody AggiornaProfilo richiesta) {
        return risposta(utentiService.aggiornaProfilo(authentication.getName(), richiesta.firstName(),
                richiesta.lastName(), richiesta.email(), richiesta.phone()));
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambia la propria password", 
        description = "Consente all'utente autenticato di cambiare la propria password fornendo la password attuale e la nuova.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Password modificata con successo"),
        @ApiResponse(responseCode = "400", description = "Richiesta non valida"),
        @ApiResponse(responseCode = "401", description = "Password attuale non corretta")
    })
    public void cambiaPassword(Authentication authentication, @RequestBody CambioPassword richiesta) {
        if (richiesta.currentPassword() == null || richiesta.newPassword() == null
                || richiesta.newPassword().isBlank()) {
            throw new IllegalArgumentException("La password attuale e quella nuova sono obbligatorie.");
        }
        utentiService.cambiaPassword(authentication.getName(), richiesta.currentPassword(),
                richiesta.newPassword());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Recupera i dettagli di un utente", 
        description = "Restituisce i dettagli completi di un utente specifico. Solo gli amministratori possono accedere a questo endpoint.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dettagli utente recuperati con successo",
            content = @Content(schema = @Schema(implementation = UtenteRisposta.class))),
        @ApiResponse(responseCode = "403", description = "Accesso negato - permessi insufficienti"),
        @ApiResponse(responseCode = "404", description = "Utente non trovato")
    })
    public UtenteRisposta dettaglio(
        @Parameter(description = "ID dell'utente")
        @PathVariable Long id) {
        return risposta(utentiService.trovaPerId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aggiorna i dati di un utente", 
        description = "Aggiorna i dati di un utente (nome, cognome, email, telefono, ruolo, stato e password). Solo gli amministratori possono accedere a questo endpoint.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utente aggiornato con successo",
            content = @Content(schema = @Schema(implementation = UtenteRisposta.class))),
        @ApiResponse(responseCode = "400", description = "Dati non validi"),
        @ApiResponse(responseCode = "403", description = "Accesso negato - permessi insufficienti"),
        @ApiResponse(responseCode = "404", description = "Utente non trovato")
    })
    public UtenteRisposta aggiorna(
        @Parameter(description = "ID dell'utente")
        @PathVariable Long id, 
        @RequestBody AggiornaUtente richiesta) {
        return risposta(utentiService.aggiornaUtente(id, richiesta.firstName(), richiesta.lastName(),
                richiesta.email(), richiesta.phone(), richiesta.role(), richiesta.active(), richiesta.password()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Disattiva un utente", 
        description = "Disattiva un utente (soft delete). L'utente non potrà più accedere al sistema. Solo gli amministratori possono eseguire questa azione.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Utente disattivato con successo"),
        @ApiResponse(responseCode = "403", description = "Accesso negato - permessi insufficienti"),
        @ApiResponse(responseCode = "404", description = "Utente non trovato")
    })
    public void disattiva(
        @Parameter(description = "ID dell'utente")
        @PathVariable Long id) {
        utentiService.disattivaUtente(id);
    }

    private UtenteRisposta risposta(Utenti utente) {
        return UtenteRisposta.da(utente);
    }
}
