package it.gym.Prenotazioni.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "programma_settimanale", indexes = {
        @Index(name = "idx_programma_settimanale_corsi_id", columnList = "corsi_id")
})
public class ProgrammaSettimanale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corsi_id", nullable = false, foreignKey = @ForeignKey(name = "fk_programma_settimanale_corsi"))
    private Corsi corso;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "default_capacity", nullable = false)
    private int defaultCapacity;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(nullable = false)
    private boolean active;

    protected ProgrammaSettimanale() {
    }

    public ProgrammaSettimanale(Corsi corso, int dayOfWeek, LocalTime startTime, int defaultCapacity,
            LocalDate validFrom, LocalDate validTo) {
        this.corso = corso;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.defaultCapacity = defaultCapacity;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public Corsi getCorso() {
        return corso;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public int getDefaultCapacity() {
        return defaultCapacity;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public boolean isActive() {
        return active;
    }
}