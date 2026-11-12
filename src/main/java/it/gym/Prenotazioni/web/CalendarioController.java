package it.gym.Prenotazioni.web;

import it.gym.Prenotazioni.service.CalendarioService;
import it.gym.Prenotazioni.service.CorsiService;
import it.gym.Prenotazioni.web.dto.CorsiProgrammiDto.CorsoRisposta;
import it.gym.Prenotazioni.web.dto.CalendarioDto.ModificaLezione;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;

@RestController
public class CalendarioController {
    private final CalendarioService calendario;
    private final CorsiService corsi;

    public CalendarioController(CalendarioService calendario, CorsiService corsi) {
        this.calendario = calendario;
        this.corsi = corsi;
    }

    public record PubblicaCalendario(LocalDate dal) {
    }

    @GetMapping("/api/pubblico/corsi")
    public List<CorsoRisposta> corsi() {
        return corsi.trovaAttivi().stream().map(CorsoRisposta::da).toList();
    }

    @GetMapping("/api/pubblico/lezioni")
    public List<CalendarioService.LezioneRisposta> lezioni(@RequestParam LocalDate dal, @RequestParam LocalDate al) {
        return calendario.elenco(dal, al);
    }

    @PostMapping("/api/calendario/pubblica")
    @PreAuthorize("hasRole('ADMIN')")
    public CalendarioService.PubblicazioneRisposta pubblica(@RequestBody PubblicaCalendario richiesta) {
        return calendario.pubblica(richiesta.dal());
    }

    @PutMapping("/api/calendario/lezioni/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    public CalendarioService.LezioneRisposta modificaLezione(@PathVariable Long id,
            @RequestBody ModificaLezione richiesta) {
        return calendario.modificaLezione(id, richiesta.ora(), richiesta.durataMinuti(), richiesta.capienza());
    }

    @PostMapping("/api/calendario/lezioni/{id}/annulla")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECRETARY')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void annullaLezione(@PathVariable Long id) {
        calendario.annullaLezione(id);
    }
}