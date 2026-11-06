package it.gym.Prenotazioni.entity;

import it.gym.Prenotazioni.enums.PrenotazioniChannel;
import it.gym.Prenotazioni.enums.PrenotazioniStatus;
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

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "prenotazioni", uniqueConstraints = {
        @UniqueConstraint(name = "uk_prenotazioni_customer_occurrence", columnNames = { "customer_id",
                "occurrence_id" })
}, indexes = {
        @Index(name = "idx_prenotazioni_customer_id", columnList = "customer_id"),
        @Index(name = "idx_prenotazioni_occurrence_id", columnList = "occurrence_id"),
        @Index(name = "idx_prenotazioni_created_by", columnList = "created_by"),
        @Index(name = "idx_prenotazioni_occurrence_status", columnList = "occurrence_id, status")
})
public class Prenotazioni {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prenotazioni_customer"))
    private Utenti customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "occurrence_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prenotazioni_occurrence"))
    private AgendaCorsi occurrence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrenotazioniStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrenotazioniChannel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", foreignKey = @ForeignKey(name = "fk_prenotazioni_created_by"))
    private Utenti createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    protected Prenotazioni() {
    }

    public Prenotazioni(Utenti customer, AgendaCorsi occurrence, PrenotazioniStatus status,
            PrenotazioniChannel channel, Utenti createdBy) {
        this(customer, occurrence, status, channel, createdBy,
                LocalDateTime.now(ZoneId.of("Europe/Rome")));
    }

    public Prenotazioni(Utenti customer, AgendaCorsi occurrence, PrenotazioniStatus status,
            PrenotazioniChannel channel, Utenti createdBy, LocalDateTime createdAt) {
        this.customer = customer;
        this.occurrence = occurrence;
        this.status = status;
        this.channel = channel;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public void riattiva(PrenotazioniChannel channel, Utenti createdBy) {
        verificaRiattivabile();
        this.status = PrenotazioniStatus.CONFIRMED;
        this.channel = channel;
        this.createdBy = createdBy;
        this.cancelledAt = null;
    }

    public void verificaRiattivabile() {
        if (status != PrenotazioniStatus.CANCELLED) {
            throw new RegolaDominioNonValidaException("È possibile riattivare solo una prenotazione cancellata.");
        }
    }

    public void cancella(LocalDateTime cancellataIl) {
        if (status != PrenotazioniStatus.CONFIRMED) {
            throw new RegolaDominioNonValidaException("La prenotazione non è attiva.");
        }
        this.status = PrenotazioniStatus.CANCELLED;
        this.cancelledAt = cancellataIl;
    }

    public Long getId() {
        return id;
    }

    public Utenti getCustomer() {
        return customer;
    }

    public AgendaCorsi getOccurrence() {
        return occurrence;
    }

    public PrenotazioniStatus getStatus() {
        return status;
    }

    public PrenotazioniChannel getChannel() {
        return channel;
    }

    public Utenti getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}