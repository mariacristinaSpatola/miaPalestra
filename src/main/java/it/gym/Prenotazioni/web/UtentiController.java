package it.gym.Prenotazioni.web;

import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.service.UtentiService;
import it.gym.Prenotazioni.web.dto.UtentiDto.AggiornaProfilo;
import it.gym.Prenotazioni.web.dto.UtentiDto.AggiornaUtente;
import it.gym.Prenotazioni.web.dto.UtentiDto.CambioPassword;
import it.gym.Prenotazioni.web.dto.UtentiDto.NuovoUtente;
import it.gym.Prenotazioni.web.dto.UtentiDto.UtenteRisposta;
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
public class UtentiController {

    private final UtentiService utentiService;

    public UtentiController(UtentiService utentiService) {
        this.utentiService = utentiService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UtenteRisposta crea(@RequestBody NuovoUtente richiesta) {
        return risposta(utentiService.creaUtente(richiesta.username(), richiesta.password(),
                richiesta.firstName(), richiesta.lastName(), richiesta.email(), richiesta.phone(),
                richiesta.role()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UtenteRisposta> elenco() {
        return utentiService.trovaTutti().stream().map(UtenteRisposta::da).toList();
    }

    @GetMapping("/clienti")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    public List<UtenteRisposta> cercaClienti(@RequestParam String query) {
        return utentiService.cercaClienti(query).stream().map(UtenteRisposta::da).toList();
    }

    @GetMapping("/me")
    public UtenteRisposta mioProfilo(Authentication authentication) {
        return risposta(utentiService.trovaPerUsername(authentication.getName()));
    }

    @PatchMapping("/me")
    public UtenteRisposta aggiornaMioProfilo(Authentication authentication,
            @RequestBody AggiornaProfilo richiesta) {
        return risposta(utentiService.aggiornaProfilo(authentication.getName(), richiesta.firstName(),
                richiesta.lastName(), richiesta.email(), richiesta.phone()));
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
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
    public UtenteRisposta dettaglio(@PathVariable Long id) {
        return risposta(utentiService.trovaPerId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UtenteRisposta aggiorna(@PathVariable Long id, @RequestBody AggiornaUtente richiesta) {
        return risposta(utentiService.aggiornaUtente(id, richiesta.firstName(), richiesta.lastName(),
                richiesta.email(), richiesta.phone(), richiesta.role(), richiesta.active(), richiesta.password()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disattiva(@PathVariable Long id) {
        utentiService.disattivaUtente(id);
    }

    private UtenteRisposta risposta(Utenti utente) {
        return UtenteRisposta.da(utente);
    }
}
