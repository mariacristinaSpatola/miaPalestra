package it.gym.Prenotazioni.service;

import it.gym.Prenotazioni.entity.AgendaCorsi;
import it.gym.Prenotazioni.entity.Prenotazioni;
import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.enums.PrenotazioniChannel;
import it.gym.Prenotazioni.enums.PrenotazioniStatus;
import it.gym.Prenotazioni.enums.UserRole;
import it.gym.Prenotazioni.exception.AccessoNonAutorizzatoException;
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
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

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

    public record PrenotazioneDettaglio(Long id, Long clienteId, String nomeCliente, Long occorrenzaId,
            String nomeCorso, LocalDate data, LocalTime ora, PrenotazioniStatus stato,
            boolean cancellabile) {
    }

    @Transactional(readOnly = true)
    public List<PrenotazioneDettaglio> elencoPerUtente(Long clienteId, String usernameAttore) {
        Utenti attore = trovaAttore(usernameAttore);
        Utenti cliente = clienteId == null ? attore
                : trovaUtente(clienteId, "Il cliente della prenotazione non esiste.");
        attore.verificaPermessoPrenotazione(cliente);
        return prenotazioniRepository.findByCustomerIdOrderByCreatedAtDesc(cliente.getId()).stream()
                .map(prenotazione -> new PrenotazioneDettaglio(prenotazione.getId(), cliente.getId(),
                        cliente.getFirstName() + " " + cliente.getLastName(), prenotazione.getOccurrence().getId(),
                        prenotazione.getOccurrence().getProgrammaSettimanale().getCorso().getName(),
                        prenotazione.getOccurrence().getLessonDate(), prenotazione.getOccurrence().getStartTime(),
                        prenotazione.getStatus(), prenotazione.getStatus() == PrenotazioniStatus.CONFIRMED
                                && prenotazione.getOccurrence().operazioniConsentite(LocalDateTime.now(clock))))
                .toList();
    }

    @Transactional
    public Prenotazioni prenotaPerUtente(Long lezioneId, Long clienteId, String usernameAttore) {
        Utenti attore = trovaAttore(usernameAttore);
        Utenti cliente = clienteId == null ? attore
                : trovaUtente(clienteId, "Il cliente della prenotazione non esiste.");
        attore.verificaPermessoPrenotazione(cliente);
        boolean prenotazioneDiretta = attore.getRole() == UserRole.CUSTOMER;
        return prenota(lezioneId, cliente.getId(),
                prenotazioneDiretta ? PrenotazioniChannel.WEB : PrenotazioniChannel.SECRETARY,
                prenotazioneDiretta ? null : attore.getId());
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

    @Transactional
    public Prenotazioni cancellaPerUtente(Long prenotazioneId, String usernameAttore) {
        if (prenotazioneId == null) {
            throw new RegolaDominioNonValidaException("La prenotazione da cancellare è obbligatoria.");
        }
        Prenotazioni prenotazione = prenotazioniRepository.findById(prenotazioneId)
                .orElseThrow(() -> new RegolaDominioNonValidaException("La prenotazione non esiste."));
        trovaAttore(usernameAttore).verificaPermessoCancellazione(prenotazione);
        return cancella(prenotazioneId);
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

    private Utenti trovaAttore(String username) {
        if (username == null || username.isBlank()) {
            throw new AccessoNonAutorizzatoException("L'utente autenticato non è valido.");
        }
        return utentiRepository.findByUsername(username)
                .orElseThrow(() -> new AccessoNonAutorizzatoException("L'utente autenticato non è valido."));
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
