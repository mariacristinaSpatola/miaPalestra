package it.gym.Prenotazioni;

import it.gym.Prenotazioni.entity.Corsi;
import it.gym.Prenotazioni.entity.ProgrammaSettimanale;
import it.gym.Prenotazioni.exception.MiaPalestraException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgrammaSettimanaleTests {

    private static final LocalDate DAL = LocalDate.of(2026, 11, 2);

    @Test
    void accettaGiorniIsoAiConfiniUnoESetteECapienzaPositivaMinima() {
        ProgrammaSettimanale lunedi = programma(1, LocalTime.MIDNIGHT, 1, DAL, DAL);
        ProgrammaSettimanale domenica = programma(7, LocalTime.MAX, 1, DAL, null);

        assertEquals(1, lunedi.getDayOfWeek());
        assertEquals(7, domenica.getDayOfWeek());
        assertEquals(1, lunedi.getDefaultCapacity());
    }

    @Test
    void rifiutaGiorniIsoFuoriIntervalloZeroEotto() {
        assertThrows(MiaPalestraException.class,
                () -> programma(0, LocalTime.NOON, 10, DAL, null));
        assertThrows(MiaPalestraException.class,
                () -> programma(8, LocalTime.NOON, 10, DAL, null));
    }

    @Test
    void rifiutaOraMancanteECapienzaNullaONegativa() {
        assertThrows(MiaPalestraException.class, () -> programma(1, null, 10, DAL, null));
        assertThrows(MiaPalestraException.class, () -> programma(1, LocalTime.NOON, 0, DAL, null));
        assertThrows(MiaPalestraException.class, () -> programma(1, LocalTime.NOON, -1, DAL, null));
    }

    @Test
    void rifiutaDecorrenzaMancanteOFinePrecedenteAllInizio() {
        assertThrows(MiaPalestraException.class,
                () -> new ProgrammaSettimanale(new Corsi("Yoga", null), 1, LocalTime.NOON, 10, null, null));
        assertThrows(MiaPalestraException.class,
                () -> programma(1, LocalTime.NOON, 10, DAL, DAL.minusDays(1)));
    }

    @Test
    void validitaDellaStessaDataEValida() {
        ProgrammaSettimanale programma = programma(1, LocalTime.NOON, 10, DAL, DAL);

        assertEquals(DAL, programma.getValidFrom());
        assertEquals(DAL, programma.getValidTo());
    }

    @Test
    void rilevaSovrapposizioneInclusivaSoloPerCorsoGiornoEOraUguali() {
        Corsi corso = new Corsi("Yoga", null);
        ProgrammaSettimanale primo = new ProgrammaSettimanale(corso, 1, LocalTime.NOON, 10, DAL,
                DAL.plusDays(14));
        ProgrammaSettimanale sovrappostoSulConfine = new ProgrammaSettimanale(corso, 1, LocalTime.NOON, 20,
                DAL.plusDays(14), null);
        ProgrammaSettimanale successivo = new ProgrammaSettimanale(corso, 1, LocalTime.NOON, 20,
                DAL.plusDays(15), null);
        ProgrammaSettimanale altraOra = new ProgrammaSettimanale(corso, 1, LocalTime.of(13, 0), 10, DAL, null);
        ProgrammaSettimanale altroGiorno = new ProgrammaSettimanale(corso, 2, LocalTime.NOON, 10, DAL, null);

        assertTrue(primo.siSovrapponeA(sovrappostoSulConfine));
        assertFalse(primo.siSovrapponeA(successivo));
        assertFalse(primo.siSovrapponeA(altraOra));
        assertFalse(primo.siSovrapponeA(altroGiorno));
    }

    @Test
    void chiudeLaRegolaIlGiornoPrimaDellaNuovaDecorrenzaSenzaDisattivarla() {
        ProgrammaSettimanale programma = programma(1, LocalTime.NOON, 10, DAL, null);
        LocalDate decorrenzaNuova = DAL.plusDays(14);

        programma.chiudiValidita(decorrenzaNuova.minusDays(1));

        assertEquals(decorrenzaNuova.minusDays(1), programma.getValidTo());
        assertTrue(programma.isActive());
    }

    @Test
    void rifiutaUnaChiusuraChePrecedeLaDecorrenzaDellaRegola() {
        ProgrammaSettimanale programma = programma(1, LocalTime.NOON, 10, DAL, null);

        assertThrows(MiaPalestraException.class, () -> programma.chiudiValidita(DAL.minusDays(1)));
        assertEquals(null, programma.getValidTo());
    }

    @Test
    void laDisattivazioneDellaRegolaNonCancellaLaSuaValiditaStorica() {
        ProgrammaSettimanale programma = programma(1, LocalTime.NOON, 10, DAL, DAL.plusDays(7));

        programma.disattiva();

        assertFalse(programma.isActive());
        assertEquals(DAL.plusDays(7), programma.getValidTo());
    }

    @Test
    void unaRegolaGeneraSoloNelGiornoEIntervalloInclusivoEConCorsoAttivo() {
        Corsi corso = new Corsi("Yoga", null);
        ProgrammaSettimanale programma = new ProgrammaSettimanale(corso, 1, LocalTime.NOON, 10, DAL,
                DAL.plusDays(7));

        assertFalse(programma.eValidoIl(DAL.minusDays(1)));
        assertTrue(programma.eValidoIl(DAL));
        assertFalse(programma.eValidoIl(DAL.plusDays(1)));
        assertTrue(programma.eValidoIl(DAL.plusDays(7)));
        assertFalse(programma.eValidoIl(DAL.plusDays(8)));

        corso.impostaAttivo(false);
        assertFalse(programma.eValidoIl(DAL));
    }

    private ProgrammaSettimanale programma(int giorno, LocalTime ora, int capienza, LocalDate inizio,
            LocalDate fine) {
        return new ProgrammaSettimanale(new Corsi("Yoga", null), giorno, ora, capienza, inizio, fine);
    }
}
