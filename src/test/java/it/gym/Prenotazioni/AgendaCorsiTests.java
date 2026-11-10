package it.gym.Prenotazioni;

import it.gym.Prenotazioni.entity.AgendaCorsi;
import it.gym.Prenotazioni.entity.Corsi;
import it.gym.Prenotazioni.entity.PeriodoCalendario;
import it.gym.Prenotazioni.entity.ProgrammaSettimanale;
import it.gym.Prenotazioni.exception.MiaPalestraException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        @Test
        void ilPeriodoIncludeTrentaGiorniConsecutiviAncheACavalloDelMese() {
                PeriodoCalendario periodo = new PeriodoCalendario(LocalDate.of(2026, 12, 15));
                assertEquals(LocalDate.of(2027, 1, 13), periodo.ultimoGiorno());
                assertEquals(30, periodo.date().size());
                assertThrows(MiaPalestraException.class, () -> new PeriodoCalendario(null));
        }

        @Test
        void laRipubblicazioneConservaLeLezioniPrenotateESegnalaIlConflitto() {
                ProgrammaSettimanale programma = programma();
                AgendaCorsi lezione = new AgendaCorsi(programma, LUNEDI, LocalTime.of(9, 0), 10);
                programma.disattiva();
                assertTrue(lezione.haConflittoConProgramma());
                assertFalse(lezione.riconciliaProgramma(1));
                assertEquals(it.gym.Prenotazioni.enums.OccurrenceStatus.SCHEDULED, lezione.getStatus());
                assertTrue(lezione.riconciliaProgramma(0));
                assertEquals(it.gym.Prenotazioni.enums.OccurrenceStatus.CANCELLED, lezione.getStatus());
        }

        private ProgrammaSettimanale programma() {
                return new ProgrammaSettimanale(new Corsi("Yoga", null), 1, LocalTime.of(9, 0), 10, LUNEDI,
                                LUNEDI.plusDays(7));
        }
}
