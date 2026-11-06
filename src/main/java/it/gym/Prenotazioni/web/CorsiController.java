package it.gym.Prenotazioni.web;

import it.gym.Prenotazioni.service.CorsiService;
import it.gym.Prenotazioni.web.dto.CorsiProgrammiDto.CorsoRisposta;
import it.gym.Prenotazioni.web.dto.CorsiProgrammiDto.NuovoCorso;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/corsi")
@PreAuthorize("hasRole('ADMIN')")
public class CorsiController {

    private final CorsiService corsiService;

    public CorsiController(CorsiService corsiService) {
        this.corsiService = corsiService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CorsoRisposta crea(@RequestBody NuovoCorso richiesta) {
        return CorsoRisposta.da(corsiService.creaCorso(richiesta.nome(), richiesta.descrizione()));
    }

    @GetMapping
    public List<CorsoRisposta> elenco() {
        return corsiService.trovaTutti().stream().map(CorsoRisposta::da).toList();
    }

    @GetMapping("/{id}")
    public CorsoRisposta dettaglio(@PathVariable Long id) {
        return CorsoRisposta.da(corsiService.trovaPerId(id));
    }

    @PutMapping("/{id}")
    public CorsoRisposta aggiorna(@PathVariable Long id, @RequestBody NuovoCorso richiesta) {
        return CorsoRisposta.da(corsiService.aggiornaCorso(id, richiesta.nome(), richiesta.descrizione()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disattiva(@PathVariable Long id) {
        corsiService.disattivaCorso(id);
    }
}
