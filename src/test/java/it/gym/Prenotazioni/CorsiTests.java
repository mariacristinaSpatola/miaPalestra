package it.gym.Prenotazioni;

import it.gym.Prenotazioni.entity.Corsi;
import it.gym.Prenotazioni.exception.MiaPalestraException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsiTests {

    @Test
    void ilNomeObbligatorioNonAccettaValoriNulliVuotiONonSignificativi() {
        assertThrows(MiaPalestraException.class, () -> new Corsi(null, null));
        assertThrows(MiaPalestraException.class, () -> new Corsi("", null));
        assertThrows(MiaPalestraException.class, () -> new Corsi("   ", null));
    }

    @Test
    void unCorsoNuovoEAttivoEpuoEssereDisattivatoSenzaEssereCancellato() {
        Corsi corso = new Corsi("Yoga", "Lezione dolce");

        assertTrue(corso.isActive());
        corso.impostaAttivo(false);
        assertFalse(corso.isActive());
    }

    @Test
    void laModificaDelNomeMantieneObbligatorieta() {
        Corsi corso = new Corsi("Yoga", null);

        assertThrows(MiaPalestraException.class, () -> corso.aggiornaDati(" ", "Nuova descrizione"));
        corso.aggiornaDati("Pilates", "Nuova descrizione");

        assertTrue(corso.getName().equals("Pilates"));
    }
}
