package it.gym.Prenotazioni.service;

import it.gym.Prenotazioni.entity.AgendaCorsi;
import it.gym.Prenotazioni.entity.PeriodoCalendario;
import it.gym.Prenotazioni.enums.OccurrenceStatus;
import it.gym.Prenotazioni.enums.PrenotazioniStatus;
import it.gym.Prenotazioni.exception.LezioneNonTrovataException;
import it.gym.Prenotazioni.exception.RegolaDominioNonValidaException;
import it.gym.Prenotazioni.repository.AgendaCorsiRepository;
import it.gym.Prenotazioni.repository.CorsiRepository;
import it.gym.Prenotazioni.repository.PrenotazioniRepository;
import it.gym.Prenotazioni.repository.ProgrammaSettimanaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Service
public class CalendarioService {
    private final AgendaCorsiRepository agenda;
    private final ProgrammaSettimanaleRepository programmi;
    private final CorsiRepository corsi;
    private final PrenotazioniRepository prenotazioni;
    private final Clock clock;

    public CalendarioService(AgendaCorsiRepository agenda, ProgrammaSettimanaleRepository programmi,
            CorsiRepository corsi, PrenotazioniRepository prenotazioni, Clock clock) {
        this.agenda = agenda;
        this.programmi = programmi;
        this.corsi = corsi;
        this.prenotazioni = prenotazioni;
        this.clock = clock;
    }

    public record LezioneRisposta(Long id, Long corsoId, String nomeCorso, LocalDate data, LocalTime ora,
            int capienza, long postiDisponibili, OccurrenceStatus stato, LocalDateTime scadenza,
            boolean operazioniConsentite, boolean conflittoProgramma, int durataMinuti) {
    }

    public record PubblicazioneRisposta(LocalDate dal, LocalDate al, int create, int annullate,
            List<Long> conflitti) {
    }

    @Transactional(readOnly = true)
    public List<LezioneRisposta> elenco(LocalDate dal, LocalDate al) {
        if (dal == null || al == null || al.isBefore(dal) || al.isAfter(dal.plusDays(62))) {
            throw new RegolaDominioNonValidaException("Selezionare un intervallo da 1 a 63 giorni.");
        }
        return agenda.findByLessonDateBetweenOrderByLessonDateAscStartTimeAsc(dal, al).stream()
                .map(this::dettaglio)
                .toList();
    }

    @Transactional
    public LezioneRisposta modificaLezione(Long lezioneId, LocalTime ora, int durataMinuti, int capienza) {
        AgendaCorsi lezione = trovaLezioneConBlocco(lezioneId);
        long confermate = prenotazioni.countByOccurrenceIdAndStatus(lezioneId, PrenotazioniStatus.CONFIRMED);
        lezione.modifica(ora, durataMinuti, capienza, confermate, LocalDateTime.now(clock));
        return dettaglio(lezione);
    }

    @Transactional
    public void annullaLezione(Long lezioneId) {
        AgendaCorsi lezione = trovaLezioneConBlocco(lezioneId);
        LocalDateTime annullataIl = LocalDateTime.now(clock);
        lezione.annulla(annullataIl);
        prenotazioni.findByOccurrenceIdAndStatus(lezioneId, PrenotazioniStatus.CONFIRMED)
                .forEach(prenotazione -> prenotazione.cancella(annullataIl));
    }

    @Transactional
    public PubblicazioneRisposta pubblica(LocalDate dal) {
        PeriodoCalendario periodo = new PeriodoCalendario(dal);
        if (dal.isBefore(LocalDate.now(clock))) {
            throw new RegolaDominioNonValidaException("Non e possibile pubblicare un periodo passato.");
        }
        corsi.findAll().stream().sorted(Comparator.comparing(it.gym.Prenotazioni.entity.Corsi::getId))
                .forEach(corso -> corsi.findByIdForUpdate(corso.getId()).orElseThrow());
        var esistenti = agenda.findByLessonDateBetweenOrderByLessonDateAscStartTimeAsc(dal, periodo.ultimoGiorno());
        var chiavi = new HashSet<String>();
        var conflitti = new ArrayList<Long>();
        int annullate = 0;
        for (var esistente : esistenti) {
            var lezione = agenda.findByIdForUpdate(esistente.getId()).orElseThrow();
            chiavi.add(lezione.getProgrammaSettimanale().getId() + ":" + lezione.getLessonDate());
            long confermate = prenotazioni.countByOccurrenceIdAndStatus(lezione.getId(), PrenotazioniStatus.CONFIRMED);
            if (lezione.riconciliaProgramma(confermate)) {
                annullate++;
            } else if (lezione.haConflittoConProgramma()) {
                conflitti.add(lezione.getId());
            }
        }
        int create = 0;
        for (var programma : programmi.findAll()) {
            for (var data : periodo.date()) {
                if (programma.eValidoIl(data) && chiavi.add(programma.getId() + ":" + data)) {
                    agenda.save(
                            new AgendaCorsi(programma, data, programma.getStartTime(), programma.getDefaultCapacity()));
                    create++;
                }
            }
        }
        return new PubblicazioneRisposta(dal, periodo.ultimoGiorno(), create, annullate, List.copyOf(conflitti));
    }

    private LezioneRisposta dettaglio(AgendaCorsi lezione) {
        long confermate = prenotazioni.countByOccurrenceIdAndStatus(lezione.getId(), PrenotazioniStatus.CONFIRMED);
        return new LezioneRisposta(lezione.getId(), lezione.getProgrammaSettimanale().getCorso().getId(),
                lezione.getProgrammaSettimanale().getCorso().getName(), lezione.getLessonDate(),
                lezione.getStartTime(), lezione.getCapacity(), Math.max(0, lezione.getCapacity() - confermate),
                lezione.getStatus(), lezione.scadenzaOperazioni(),
                lezione.operazioniConsentite(LocalDateTime.now(clock)), lezione.haConflittoConProgramma(),
                lezione.getDurataMinuti());
    }

    private AgendaCorsi trovaLezioneConBlocco(Long lezioneId) {
        if (lezioneId == null) {
            throw new RegolaDominioNonValidaException("La lezione è obbligatoria.");
        }
        return agenda.findByIdForUpdate(lezioneId).orElseThrow(LezioneNonTrovataException::new);
    }
}