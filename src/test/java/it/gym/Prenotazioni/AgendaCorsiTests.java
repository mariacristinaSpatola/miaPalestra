package it.gym.Prenotazioni;

import it.gym.Prenotazioni.entity.AgendaCorsi;
import it.gym.Prenotazioni.entity.Corsi;
import it.gym.Prenotazioni.entity.ProgrammaSettimanale;
import it.gym.Prenotazioni.exception.MiaPalestraException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgendaCorsiTests {

    private static final LocalDate LUNEDI = LocalDate.of(2026, 11, 2);

    @Test
    void creaOccorrenzeSoloAlleDateValideInclusiEntrambiIEstremi() {
        ProgrammaSettimanale programma = programma();

        assertEquals(LUNEDI, new AgendaCorsi(programma, LUNEDI, LocalTime.of(9, 0), 10).getLessonDate());
        assertEquals(LUNEDI.plusDays(7),
                new AgendaCorsi(programma, LUNEDI.plusDays(7), LocalTime.of(9, 0), 10).getLessonDate());
    }

    @Test
    void rifiutaOccorrenzeInDateNonValidePerProgrammiDisattivatiECorsiInattivi() {
        Corsi corso = new Corsi("Yoga", null);
        ProgrammaSettimanale programma = new ProgrammaSettimanale(corso, 1, LocalTime.of(9, 0), 10, LUNEDI,
                LUNEDI.plusDays(7));

        assertThrows(MiaPalestraException.class,
                () -> new AgendaCorsi(programma, LUNEDI.plusDays(1), LocalTime.of(9, 0), 10));
        programma.disattiva();
        assertThrows(MiaPalestraException.class,
                () -> new AgendaCorsi(programma, LUNEDI, LocalTime.of(9, 0), 10));

        ProgrammaSettimanale altroProgramma = new ProgrammaSettimanale(corso, 1, LocalTime.of(9, 0), 10, LUNEDI,
                null);
        corso.impostaAttivo(false);
        assertThrows(MiaPalestraException.class,
                () -> new AgendaCorsi(altroProgramma, LUNEDI, LocalTime.of(9, 0), 10));
    }

    private ProgrammaSettimanale programma() {
        return new ProgrammaSettimanale(new Corsi("Yoga", null), 1, LocalTime.of(9, 0), 10, LUNEDI,
                LUNEDI.plusDays(7));
    }
}
