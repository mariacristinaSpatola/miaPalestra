package it.gym.Prenotazioni.web;

import it.gym.Prenotazioni.service.PrenotazioniService;
import it.gym.Prenotazioni.web.dto.PrenotazioniDto.NuovaPrenotazione;
import it.gym.Prenotazioni.web.dto.PrenotazioniDto.PrenotazioneRisposta;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@RestController
@RequestMapping("/api/prenotazioni")
@PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY', 'CUSTOMER')")
public class PrenotazioniController {

    private final PrenotazioniService prenotazioniService;

    public PrenotazioniController(PrenotazioniService prenotazioniService) {
        this.prenotazioniService = prenotazioniService;
    }

    @GetMapping
    public List<PrenotazioniService.PrenotazioneDettaglio> elenco(
            @RequestParam(required = false) Long clienteId, Authentication autenticazione) {
        return prenotazioniService.elencoPerUtente(clienteId, autenticazione.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrenotazioneRisposta prenota(@RequestBody NuovaPrenotazione richiesta,
            Authentication autenticazione) {
        return PrenotazioneRisposta.da(prenotazioniService.prenotaPerUtente(
                richiesta.occorrenzaId(), richiesta.clienteId(), autenticazione.getName()));
    }

    @DeleteMapping("/{id}")
    public PrenotazioneRisposta cancella(@PathVariable Long id, Authentication autenticazione) {
        return PrenotazioneRisposta.da(prenotazioniService.cancellaPerUtente(id, autenticazione.getName()));
    }
}
