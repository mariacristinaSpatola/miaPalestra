package it.gym.Prenotazioni.web.dto;

import it.gym.Prenotazioni.entity.Corsi;
import it.gym.Prenotazioni.entity.ProgrammaSettimanale;

import java.time.LocalDate;
import java.time.LocalTime;

public final class CorsiProgrammiDto {

    private CorsiProgrammiDto() {
    }

    public record NuovoCorso(String nome, String descrizione) {
    }

    public record CorsoRisposta(Long id, String nome, String descrizione, boolean attivo) {
        public static CorsoRisposta da(Corsi corso) {
            return new CorsoRisposta(corso.getId(), corso.getName(), corso.getDescription(), corso.isActive());
        }
    }

    public record NuovoProgramma(Long corsoId, int giorno, LocalTime ora, int capienzaPredefinita,
            LocalDate validoDal, LocalDate validoAl, Integer durataMinuti) {
    }

    public record ProgrammaRisposta(Long id, Long corsoId, String nomeCorso, int giorno, LocalTime ora,
            int capienzaPredefinita, LocalDate validoDal, LocalDate validoAl, boolean attivo, int durataMinuti) {
        public static ProgrammaRisposta da(ProgrammaSettimanale programma) {
            return new ProgrammaRisposta(programma.getId(), programma.getCorso().getId(),
                    programma.getCorso().getName(), programma.getDayOfWeek(), programma.getStartTime(),
                    programma.getDefaultCapacity(), programma.getValidFrom(), programma.getValidTo(),
                    programma.isActive(), programma.getDurataMinuti());
        }
    }
}
