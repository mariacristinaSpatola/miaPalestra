package it.gym.Prenotazioni;

import it.gym.Prenotazioni.entity.AgendaCorsi;
import it.gym.Prenotazioni.entity.Corsi;
import it.gym.Prenotazioni.entity.Prenotazioni;
import it.gym.Prenotazioni.entity.ProgrammaSettimanale;
import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.enums.PrenotazioniChannel;
import it.gym.Prenotazioni.enums.PrenotazioniStatus;
import it.gym.Prenotazioni.enums.UserRole;
import it.gym.Prenotazioni.exception.RegolaDominioNonValidaException;
import it.gym.Prenotazioni.repository.AgendaCorsiRepository;
import it.gym.Prenotazioni.repository.PrenotazioniRepository;
import it.gym.Prenotazioni.repository.UtentiRepository;
import it.gym.Prenotazioni.service.PrenotazioniService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrenotazioniServiceTests {

    private static final ZoneId FUSO_ITALIANO = ZoneId.of("Europe/Rome");
    private static final LocalDate LUNEDI = LocalDate.of(2026, 11, 9);
    private static final Long ID_LEZIONE = 101L;
    private static final Long ID_CLIENTE = 202L;
    private static final Long ID_SEGRETARIO = 303L;

    private AgendaCorsiRepository agendaRepository;
    private PrenotazioniRepository prenotazioniRepository;
    private UtentiRepository utentiRepository;
    private PrenotazioniService servizio;
    private AgendaCorsi lezione;
    private Utenti cliente;
    private Utenti segretario;

    @BeforeEach
    void prepara() throws Exception {
        agendaRepository = mock(AgendaCorsiRepository.class);
        prenotazioniRepository = mock(PrenotazioniRepository.class);
        utentiRepository = mock(UtentiRepository.class);
        servizio = new PrenotazioniService(agendaRepository, prenotazioniRepository, utentiRepository,
                orologioAlle(LUNEDI.minusDays(1), LocalTime.of(9, 0)));

        lezione = lezione(LocalDate.of(2026, 11, 9), LocalTime.of(9, 0), 2);
        impostaId(lezione, ID_LEZIONE);
        cliente = utente("cliente-prenotazione", UserRole.CUSTOMER);
        segretario = utente("segretario-prenotazione", UserRole.SECRETARY);
        when(agendaRepository.findByIdForUpdate(ID_LEZIONE)).thenReturn(Optional.of(lezione));
        when(utentiRepository.findById(ID_CLIENTE)).thenReturn(Optional.of(cliente));
        when(utentiRepository.findById(ID_SEGRETARIO)).thenReturn(Optional.of(segretario));
        when(prenotazioniRepository.findByCustomerIdAndOccurrenceId(ID_CLIENTE, ID_LEZIONE))
                .thenReturn(Optional.empty());
        when(prenotazioniRepository.countByOccurrenceIdAndStatus(ID_LEZIONE, PrenotazioniStatus.CONFIRMED))
                .thenReturn(0L);
        when(prenotazioniRepository.saveAndFlush(any(Prenotazioni.class)))
                .thenAnswer(invocazione -> invocazione.getArgument(0));
    }

    @Test
    void prenotaConTuttiICanaliEBloccaLaLezionePerAggiornamento() {
        for (PrenotazioniChannel canale : PrenotazioniChannel.values()) {
            when(prenotazioniRepository.findByCustomerIdAndOccurrenceId(ID_CLIENTE, ID_LEZIONE))
                    .thenReturn(Optional.empty());

            Prenotazioni prenotazione = servizio.prenota(ID_LEZIONE, ID_CLIENTE, canale, ID_SEGRETARIO);

            assertEquals(PrenotazioniStatus.CONFIRMED, prenotazione.getStatus());
            assertEquals(canale, prenotazione.getChannel());
            assertEquals(cliente, prenotazione.getCustomer());
            assertEquals(lezione, prenotazione.getOccurrence());
            assertEquals(segretario, prenotazione.getCreatedBy());
        }
        verify(agendaRepository, org.mockito.Mockito.times(PrenotazioniChannel.values().length))
                .findByIdForUpdate(ID_LEZIONE);
    }

    @Test
    void lezionePrimaDiMezzogiornoAccettaFinoAlle235959DelGiornoPrima() {
        servizio = servizioAlle(LUNEDI.minusDays(1), LocalTime.MAX);

        assertEquals(PrenotazioniStatus.CONFIRMED,
                servizio.prenota(ID_LEZIONE, ID_CLIENTE, PrenotazioniChannel.WEB, null).getStatus());
    }

    @Test
    void lezionePrimaDiMezzogiornoRifiutaDallInizioDelGiornoDellaLezione() {
        servizio = servizioAlle(LUNEDI, LocalTime.MIDNIGHT);

        assertThrows(RegolaDominioNonValidaException.class,
                () -> servizio.prenota(ID_LEZIONE, ID_CLIENTE, PrenotazioniChannel.WEB, null));
    }

    @Test
    void lezioneAlleUndiciECinquantanoveSegueLaScadenzaPrimaDellInizioDelGiorno() throws Exception {
        lezione = lezione(LUNEDI, LocalTime.of(11, 59), 2);
        impostaId(lezione, ID_LEZIONE);
        when(agendaRepository.findByIdForUpdate(ID_LEZIONE)).thenReturn(Optional.of(lezione));
        servizio = servizioAlle(LUNEDI, LocalTime.MIDNIGHT);

        assertThrows(RegolaDominioNonValidaException.class,
                () -> servizio.prenota(ID_LEZIONE, ID_CLIENTE, PrenotazioniChannel.WEB, null));
    }

    @Test
    void lezioneAlleDodiciAccettaEsattamenteDueOrePrima() throws Exception {
        lezione = lezione(LUNEDI, LocalTime.NOON, 2);
        impostaId(lezione, ID_LEZIONE);
        when(agendaRepository.findByIdForUpdate(ID_LEZIONE)).thenReturn(Optional.of(lezione));
        servizio = servizioAlle(LUNEDI, LocalTime.of(10, 0));

        assertEquals(PrenotazioniStatus.CONFIRMED,
                servizio.prenota(ID_LEZIONE, ID_CLIENTE, PrenotazioniChannel.WEB, null).getStatus());
    }

    @Test
    void lezioneAlleDodiciRifiutaUnIstanteDopoLaScadenza() throws Exception {
        lezione = lezione(LUNEDI, LocalTime.NOON, 2);
        impostaId(lezione, ID_LEZIONE);
        when(agendaRepository.findByIdForUpdate(ID_LEZIONE)).thenReturn(Optional.of(lezione));
        servizio = servizioAlle(LUNEDI, LocalTime.of(10, 0, 0, 1));

        assertThrows(RegolaDominioNonValidaException.class,
                () -> servizio.prenota(ID_LEZIONE, ID_CLIENTE, PrenotazioniChannel.WEB, null));
    }

    @Test
    void rifiutaPrenotazioneDiLezioneAnnullata() {
        lezione.annulla();

        assertThrows(RegolaDominioNonValidaException.class,
                () -> servizio.prenota(ID_LEZIONE, ID_CLIENTE, PrenotazioniChannel.WEB, null));
    }

    @Test
    void rifiutaPrenotazioneQuandoTuttiIPostiSonoOccupati() {
        when(prenotazioniRepository.countByOccurrenceIdAndStatus(ID_LEZIONE, PrenotazioniStatus.CONFIRMED))
                .thenReturn(2L);

        assertThrows(RegolaDominioNonValidaException.class,
                () -> servizio.prenota(ID_LEZIONE, ID_CLIENTE, PrenotazioniChannel.WEB, null));
        verify(prenotazioniRepository, never()).saveAndFlush(any(Prenotazioni.class));
    }

    @Test
    void prenotazioneCancellataNonOccupaPostiERiattivaLoStessoRecord() {
        Prenotazioni esistente = new Prenotazioni(cliente, lezione, PrenotazioniStatus.CANCELLED,
                PrenotazioniChannel.CHATBOT, null);
        when(prenotazioniRepository.findByCustomerIdAndOccurrenceId(ID_CLIENTE, ID_LEZIONE))
                .thenReturn(Optional.of(esistente));
        when(prenotazioniRepository.countByOccurrenceIdAndStatus(ID_LEZIONE, PrenotazioniStatus.CONFIRMED))
                .thenReturn(1L);
        servizio = servizioAlle(LUNEDI.minusDays(1), LocalTime.of(9, 0));

        Prenotazioni riattivata = servizio.prenota(ID_LEZIONE, ID_CLIENTE, PrenotazioniChannel.SECRETARY,
                ID_SEGRETARIO);

        assertEquals(PrenotazioniStatus.CONFIRMED, riattivata.getStatus());
        assertEquals(PrenotazioniChannel.SECRETARY, riattivata.getChannel());
        assertSame(esistente, riattivata);
        verify(prenotazioniRepository).saveAndFlush(esistente);
        verify(prenotazioniRepository).countByOccurrenceIdAndStatus(ID_LEZIONE, PrenotazioniStatus.CONFIRMED);
    }

    @Test
    void nonRiattivaUnaPrenotazioneCancellataQuandoNonCiSonoPosti() {
        Prenotazioni esistente = new Prenotazioni(cliente, lezione, PrenotazioniStatus.CANCELLED,
                PrenotazioniChannel.CHATBOT, null);
        when(prenotazioniRepository.findByCustomerIdAndOccurrenceId(ID_CLIENTE, ID_LEZIONE))
                .thenReturn(Optional.of(esistente));
        when(prenotazioniRepository.countByOccurrenceIdAndStatus(ID_LEZIONE, PrenotazioniStatus.CONFIRMED))
                .thenReturn(2L);

        assertThrows(RegolaDominioNonValidaException.class,
                () -> servizio.prenota(ID_LEZIONE, ID_CLIENTE, PrenotazioniChannel.WEB, null));
        assertEquals(PrenotazioniStatus.CANCELLED, esistente.getStatus());
        verify(prenotazioniRepository, never()).saveAndFlush(any(Prenotazioni.class));
    }

    @Test
    void rifiutaUnSecondoRecordQuandoLaPrenotazioneEsisteGiaConfermata() {
        Prenotazioni esistente = new Prenotazioni(cliente, lezione, PrenotazioniStatus.CONFIRMED,
                PrenotazioniChannel.WEB, null);
        when(prenotazioniRepository.findByCustomerIdAndOccurrenceId(ID_CLIENTE, ID_LEZIONE))
                .thenReturn(Optional.of(esistente));

        assertThrows(RegolaDominioNonValidaException.class,
                () -> servizio.prenota(ID_LEZIONE, ID_CLIENTE, PrenotazioniChannel.CHATBOT, null));
        verify(prenotazioniRepository, never()).saveAndFlush(any(Prenotazioni.class));
    }

    @Test
    void laRaceSulVincoloUnivocoDiventaErroreDiDominio() {
        when(prenotazioniRepository.saveAndFlush(any(Prenotazioni.class)))
                .thenThrow(new DataIntegrityViolationException("vincolo univoco",
                        new SQLException("chiave duplicata", "23505")));

        assertThrows(RegolaDominioNonValidaException.class,
                () -> servizio.prenota(ID_LEZIONE, ID_CLIENTE, PrenotazioniChannel.WEB, null));
    }

    @Test
    void accettaPiuOccorrenzeDelloStessoClienteNelloStessoGiorno() throws Exception {
        AgendaCorsi secondaLezione = lezione(LUNEDI, LocalTime.of(10, 0), 2);
        impostaId(secondaLezione, 102L);
        when(agendaRepository.findByIdForUpdate(102L)).thenReturn(Optional.of(secondaLezione));
        when(prenotazioniRepository.findByCustomerIdAndOccurrenceId(ID_CLIENTE, 102L))
                .thenReturn(Optional.empty());
        servizio = servizioAlle(LUNEDI.minusDays(1), LocalTime.of(9, 0));

        Prenotazioni prima = servizio.prenota(ID_LEZIONE, ID_CLIENTE, PrenotazioniChannel.WEB, null);
        Prenotazioni seconda = servizio.prenota(102L, ID_CLIENTE, PrenotazioniChannel.WEB, null);

        assertNotSame(prima, seconda);
        assertEquals(LUNEDI, prima.getOccurrence().getLessonDate());
        assertEquals(LUNEDI, seconda.getOccurrence().getLessonDate());
    }

    @Test
    void accettaLoStessoCorsoInDateDiverse() throws Exception {
        Corsi corso = new Corsi("Corso stesso", null);
        ProgrammaSettimanale programma = new ProgrammaSettimanale(corso, LUNEDI.getDayOfWeek().getValue(),
                LocalTime.of(9, 0), 2,
                LUNEDI.minusDays(7), null);
        lezione = new AgendaCorsi(programma, LUNEDI, LocalTime.of(9, 0), 2);
        impostaId(lezione, ID_LEZIONE);
        when(agendaRepository.findByIdForUpdate(ID_LEZIONE)).thenReturn(Optional.of(lezione));
        AgendaCorsi lezioneSeguente = new AgendaCorsi(programma, LUNEDI.plusDays(7), LocalTime.of(9, 0), 2);
        impostaId(lezioneSeguente, 103L);
        when(agendaRepository.findByIdForUpdate(103L)).thenReturn(Optional.of(lezioneSeguente));
        when(prenotazioniRepository.findByCustomerIdAndOccurrenceId(ID_CLIENTE, 103L))
                .thenReturn(Optional.empty());
        when(prenotazioniRepository.countByOccurrenceIdAndStatus(103L, PrenotazioniStatus.CONFIRMED))
                .thenReturn(0L);
        servizio = servizioAlle(LUNEDI.minusDays(1), LocalTime.of(9, 0));

        Prenotazioni prima = servizio.prenota(ID_LEZIONE, ID_CLIENTE, PrenotazioniChannel.WEB, null);
        Prenotazioni seconda = servizio.prenota(103L, ID_CLIENTE, PrenotazioniChannel.WEB, null);

        assertNotSame(prima, seconda);
        assertEquals(LUNEDI, prima.getOccurrence().getLessonDate());
        assertEquals(LUNEDI.plusDays(7), seconda.getOccurrence().getLessonDate());
    }

    @Test
    void lezionePrimaDiMezzogiornoSiCancellaSoloPrimaDelGiornoDellaLezione() throws Exception {
        Prenotazioni prenotazione = prenotazioneConfermata(LocalTime.of(9, 0));
        servizio = servizioAlle(LUNEDI.minusDays(1), LocalTime.MAX);

        assertEquals(PrenotazioniStatus.CANCELLED, servizio.cancella(prenotazione.getId()).getStatus());

        prenotazione = prenotazioneConfermata(LocalTime.of(9, 0));
        Prenotazioni idPrenotazione = prenotazione;
        servizio = servizioAlle(LUNEDI, LocalTime.MIDNIGHT);
        assertThrows(RegolaDominioNonValidaException.class,
                () -> servizio.cancella(idPrenotazione.getId()));
    }

    @Test
    void lezioneDalleDodiciSiCancellaFinoAlleDueOrePrimaIncluse() throws Exception {
        Prenotazioni prenotazione = prenotazioneConfermata(LocalTime.NOON);
        servizio = servizioAlle(LUNEDI, LocalTime.of(10, 0));

        assertEquals(PrenotazioniStatus.CANCELLED, servizio.cancella(prenotazione.getId()).getStatus());

        prenotazione = prenotazioneConfermata(LocalTime.NOON);
        Prenotazioni idPrenotazione = prenotazione;
        servizio = servizioAlle(LUNEDI, LocalTime.of(10, 0, 0, 1));
        assertThrows(RegolaDominioNonValidaException.class,
                () -> servizio.cancella(idPrenotazione.getId()));
    }

    @Test
    void rifiutaCancellazioneDiLezioneAnnullata() throws Exception {
        Prenotazioni prenotazione = prenotazioneConfermata(LocalTime.of(9, 0));
        prenotazione.getOccurrence().annulla();

        assertThrows(RegolaDominioNonValidaException.class,
                () -> servizio.cancella(prenotazione.getId()));
    }

    private Prenotazioni prenotazioneConfermata(LocalTime ora) throws Exception {
        AgendaCorsi lezioneCorrispondente = lezione(LUNEDI, ora, 2);
        impostaId(lezioneCorrispondente, ID_LEZIONE);
        when(agendaRepository.findByIdForUpdate(ID_LEZIONE)).thenReturn(Optional.of(lezioneCorrispondente));
        Prenotazioni prenotazione = new Prenotazioni(cliente, lezioneCorrispondente, PrenotazioniStatus.CONFIRMED,
                PrenotazioniChannel.WEB, null);
        impostaId(prenotazione, 404L);
        when(prenotazioniRepository.findById(404L)).thenReturn(Optional.of(prenotazione));
        return prenotazione;
    }

    private PrenotazioniService servizioAlle(LocalDate data, LocalTime ora) {
        return new PrenotazioniService(agendaRepository, prenotazioniRepository, utentiRepository,
                orologioAlle(data, ora));
    }

    private static Clock orologioAlle(LocalDate data, LocalTime ora) {
        return Clock.fixed(data.atTime(ora).atZone(FUSO_ITALIANO).toInstant(), FUSO_ITALIANO);
    }

    private static AgendaCorsi lezione(LocalDate data, LocalTime ora, int capienza) {
        Corsi corso = new Corsi("Corso prenotazioni", null);
        ProgrammaSettimanale programma = new ProgrammaSettimanale(corso, data.getDayOfWeek().getValue(), ora,
                capienza, data.minusDays(7), null);
        return new AgendaCorsi(programma, data, ora, capienza);
    }

    private static Utenti utente(String username, UserRole ruolo) {
        return new Utenti(username, "hash", "Test", "Utente", null, null, ruolo);
    }

    private static void impostaId(Object entita, Long id) throws Exception {
        Field campo = entita.getClass().getDeclaredField("id");
        campo.setAccessible(true);
        campo.set(entita, id);
    }
}
