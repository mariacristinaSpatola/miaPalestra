package it.gym.Prenotazioni.entity;

import it.gym.Prenotazioni.enums.OccurrenceStatus;
import it.gym.Prenotazioni.exception.RegolaDominioNonValidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "agenda_corsi", uniqueConstraints = {
        @UniqueConstraint(name = "uk_agenda_corsi_programma_data", columnNames = { "programma_settimanale_id",
                "lesson_date" })
}, indexes = {
        @Index(name = "idx_agenda_corsi_programma_id", columnList = "programma_settimanale_id")
})
public class AgendaCorsi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programma_settimanale_id", nullable = false, foreignKey = @ForeignKey(name = "fk_agenda_corsi_programma"))
    private ProgrammaSettimanale programmaSettimanale;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OccurrenceStatus status;

    protected AgendaCorsi() {
    }

    public AgendaCorsi(ProgrammaSettimanale programmaSettimanale, LocalDate lessonDate, LocalTime startTime,
            int capacity) {
        if (programmaSettimanale == null || !programmaSettimanale.eValidoIl(lessonDate)) {
            throw new RegolaDominioNonValidaException(
                    "La lezione non può essere creata fuori dalla validità di un programma attivo.");
        }
        if (startTime == null || capacity < 1) {
            throw new RegolaDominioNonValidaException("Ora e capienza della lezione sono obbligatorie.");
        }
        this.programmaSettimanale = programmaSettimanale;
        this.lessonDate = lessonDate;
        this.startTime = startTime;
        this.capacity = capacity;
        this.status = OccurrenceStatus.SCHEDULED;
    }

    public Long getId() {
        return id;
    }

    public ProgrammaSettimanale getProgrammaSettimanale() {
        return programmaSettimanale;
    }

    public LocalDate getLessonDate() {
        return lessonDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public OccurrenceStatus getStatus() {
        return status;
    }

    public void verificaPrenotabilita(LocalDateTime richiestaIl) {
        if (status != OccurrenceStatus.SCHEDULED) {
            throw new RegolaDominioNonValidaException("Non è possibile prenotare una lezione annullata.");
        }
        verificaFinestraOperativa(richiestaIl);
    }

    public void verificaCancellabilita(LocalDateTime richiestaIl) {
        if (status != OccurrenceStatus.SCHEDULED) {
            throw new RegolaDominioNonValidaException("Non è possibile cancellare la prenotazione di una lezione annullata.");
        }
        verificaFinestraOperativa(richiestaIl);
    }

    public void verificaPostiDisponibili(long prenotazioniConfermate) {
        if (prenotazioniConfermate >= capacity) {
            throw new RegolaDominioNonValidaException("Non ci sono posti disponibili per questa lezione.");
        }
    }

    public void annulla() {
        status = OccurrenceStatus.CANCELLED;
    }

    private void verificaFinestraOperativa(LocalDateTime richiestaIl) {
        if (richiestaIl == null) {
            throw new RegolaDominioNonValidaException("La data della richiesta è obbligatoria.");
        }
        LocalDateTime scadenza = startTime.isBefore(LocalTime.NOON)
                ? lessonDate.atStartOfDay()
                : lessonDate.atTime(startTime).minusHours(2);
        boolean richiestaConsentita = startTime.isBefore(LocalTime.NOON)
                ? richiestaIl.isBefore(scadenza)
                : !richiestaIl.isAfter(scadenza);
        if (!richiestaConsentita) {
            throw new RegolaDominioNonValidaException(
                    "La finestra per prenotare o cancellare questa lezione è terminata.");
        }
    }
}