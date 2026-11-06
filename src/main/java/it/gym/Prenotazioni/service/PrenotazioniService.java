package it.gym.Prenotazioni.service;

import it.gym.Prenotazioni.entity.AgendaCorsi;
import it.gym.Prenotazioni.entity.Prenotazioni;
import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.enums.PrenotazioniChannel;
import it.gym.Prenotazioni.enums.PrenotazioniStatus;
import it.gym.Prenotazioni.exception.RegolaDominioNonValidaException;
import it.gym.Prenotazioni.repository.AgendaCorsiRepository;
import it.gym.Prenotazioni.repository.PrenotazioniRepository;
import it.gym.Prenotazioni.repository.UtentiRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class PrenotazioniService {

    private final AgendaCorsiRepository agendaRepository;
    private final PrenotazioniRepository prenotazioniRepository;
    private final UtentiRepository utentiRepository;
    private final Clock clock;

    public PrenotazioniService(AgendaCorsiRepository agendaRepository,
            PrenotazioniRepository prenotazioniRepository, UtentiRepository utentiRepository, Clock clock) {
        this.agendaRepository = agendaRepository;
        this.prenotazioniRepository = prenotazioniRepository;
        this.utentiRepository = utentiRepository;
        this.clock = clock;
    }

    @Transactional
    public Prenotazioni prenota(Long lezioneId, Long clienteId, PrenotazioniChannel canale, Long creatoDaId) {
        AgendaCorsi lezione = trovaLezioneConBlocco(lezioneId);
        LocalDateTime richiestaIl = LocalDateTime.now(clock);
        lezione.verificaPrenotabilita(richiestaIl);
        if (canale == null) {
            throw new RegolaDominioNonValidaException("Il canale di prenotazione è obbligatorio.");
        }
        Utenti cliente = trovaUtente(clienteId, "Il cliente della prenotazione non esiste.");
        Utenti creatoDa = creatoDaId == null ? null
                : trovaUtente(creatoDaId, "L'utente che registra la prenotazione non esiste.");

        Prenotazioni esistente = prenotazioniRepository.findByCustomerIdAndOccurrenceId(clienteId, lezioneId)
                .orElse(null);
        if (esistente != null) {
            esistente.verificaRiattivabile();
        }

        long prenotazioniConfermate = prenotazioniRepository.countByOccurrenceIdAndStatus(lezioneId,
                PrenotazioniStatus.CONFIRMED);
        lezione.verificaPostiDisponibili(prenotazioniConfermate);

        if (esistente != null) {
            esistente.riattiva(canale, creatoDa);
            return prenotazioniRepository.saveAndFlush(esistente);
        }

        Prenotazioni nuova = new Prenotazioni(cliente, lezione, PrenotazioniStatus.CONFIRMED,
                canale, creatoDa, richiestaIl);
        try {
            return prenotazioniRepository.saveAndFlush(nuova);
        } catch (DataIntegrityViolationException eccezione) {
            if (eViolazioneUnivoca(eccezione)) {
                throw new RegolaDominioNonValidaException(
                        "Il cliente ha già una prenotazione per questa lezione.");
            }
            throw eccezione;
        }
    }

    @Transactional
    public Prenotazioni cancella(Long prenotazioneId) {
        if (prenotazioneId == null) {
            throw new RegolaDominioNonValidaException("La prenotazione da cancellare è obbligatoria.");
        }
        Prenotazioni prenotazione = prenotazioniRepository.findById(prenotazioneId)
                .orElseThrow(() -> new RegolaDominioNonValidaException("La prenotazione non esiste."));
        Long lezioneId = prenotazione.getOccurrence().getId();
        AgendaCorsi lezione = trovaLezioneConBlocco(lezioneId);
        LocalDateTime richiestaIl = LocalDateTime.now(clock);
        lezione.verificaCancellabilita(richiestaIl);
        prenotazione.cancella(richiestaIl);
        return prenotazioniRepository.saveAndFlush(prenotazione);
    }

    private AgendaCorsi trovaLezioneConBlocco(Long lezioneId) {
        if (lezioneId == null) {
            throw new RegolaDominioNonValidaException("La lezione è obbligatoria.");
        }
        return agendaRepository.findByIdForUpdate(lezioneId)
                .orElseThrow(() -> new RegolaDominioNonValidaException("La lezione non esiste."));
    }

    private Utenti trovaUtente(Long utenteId, String messaggio) {
        if (utenteId == null) {
            throw new RegolaDominioNonValidaException(messaggio);
        }
        return utentiRepository.findById(utenteId)
                .orElseThrow(() -> new RegolaDominioNonValidaException(messaggio));
    }

    private boolean eViolazioneUnivoca(Throwable eccezione) {
        for (Throwable causa = eccezione; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) {
                return true;
            }
            String messaggio = causa.getMessage();
            if (messaggio != null
                    && messaggio.toLowerCase(Locale.ROOT).contains("uk_prenotazioni_customer_occurrence")) {
                return true;
            }
        }
        return false;
    }
}
