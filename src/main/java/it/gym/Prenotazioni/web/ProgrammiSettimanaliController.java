package it.gym.Prenotazioni.web;

import it.gym.Prenotazioni.service.ProgrammiSettimanaliService;
import it.gym.Prenotazioni.web.dto.CorsiProgrammiDto.NuovoProgramma;
import it.gym.Prenotazioni.web.dto.CorsiProgrammiDto.ProgrammaRisposta;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/programmi-settimanali")
@PreAuthorize("hasRole('ADMIN')")
public class ProgrammiSettimanaliController {

    private final ProgrammiSettimanaliService programmiService;

    public ProgrammiSettimanaliController(ProgrammiSettimanaliService programmiService) {
        this.programmiService = programmiService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProgrammaRisposta crea(@RequestBody NuovoProgramma richiesta) {
        return ProgrammaRisposta.da(programmiService.crea(richiesta.corsoId(), richiesta.giorno(), richiesta.ora(),
                richiesta.capienzaPredefinita(), richiesta.validoDal(), richiesta.validoAl(),
                richiesta.durataMinuti()));
    }

    @GetMapping
    public List<ProgrammaRisposta> elenco(@RequestParam(required = false) Long corsoId) {
        return programmiService.trovaTutti(corsoId).stream().map(ProgrammaRisposta::da).toList();
    }

    @GetMapping("/{id}")
    public ProgrammaRisposta dettaglio(@PathVariable Long id) {
        return ProgrammaRisposta.da(programmiService.trovaPerId(id));
    }

    @PutMapping("/{id}")
    public ProgrammaRisposta modifica(@PathVariable Long id, @RequestBody NuovoProgramma richiesta) {
        return ProgrammaRisposta.da(programmiService.modifica(id, richiesta.corsoId(), richiesta.giorno(),
                richiesta.ora(), richiesta.capienzaPredefinita(), richiesta.validoDal(), richiesta.validoAl(),
                richiesta.durataMinuti()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disattiva(@PathVariable Long id) {
        programmiService.disattiva(id);
    }
}
