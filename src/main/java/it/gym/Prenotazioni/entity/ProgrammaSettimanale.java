package it.gym.Prenotazioni.entity;

import it.gym.Prenotazioni.exception.RegolaDominioNonValidaException;
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
        valida(corso, dayOfWeek, startTime, defaultCapacity, validFrom, validTo);
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

    public void chiudiValidita(LocalDate ultimoGiorno) {
        if (ultimoGiorno == null || ultimoGiorno.isBefore(validFrom)
                || (validTo != null && ultimoGiorno.isAfter(validTo))) {
            throw new RegolaDominioNonValidaException(
                    "La fine della validità deve essere compresa nell'intervallo del programma.");
        }
        this.validTo = ultimoGiorno;
    }

    public void disattiva() {
        this.active = false;
    }

    public boolean siSovrapponeA(ProgrammaSettimanale altro) {
        if (altro == null || !stessoCorso(altro) || dayOfWeek != altro.dayOfWeek
                || !startTime.equals(altro.startTime)) {
            return false;
        }
        LocalDate fineQuesta = validTo == null ? LocalDate.MAX : validTo;
        LocalDate fineAltra = altro.validTo == null ? LocalDate.MAX : altro.validTo;
        return !validFrom.isAfter(fineAltra) && !altro.validFrom.isAfter(fineQuesta);
    }

    public boolean eValidoIl(LocalDate data) {
        return data != null && active && corso.isActive() && !data.isBefore(validFrom)
                && (validTo == null || !data.isAfter(validTo))
                && data.getDayOfWeek().getValue() == dayOfWeek;
    }

    private boolean stessoCorso(ProgrammaSettimanale altro) {
        Long idCorso = corso.getId();
        Long altroIdCorso = altro.corso.getId();
        return corso == altro.corso || (idCorso != null && idCorso.equals(altroIdCorso));
    }

    private static void valida(Corsi corso, int dayOfWeek, LocalTime startTime, int defaultCapacity,
            LocalDate validFrom, LocalDate validTo) {
        if (corso == null) {
            throw new RegolaDominioNonValidaException("Il corso del programma è obbligatorio.");
        }
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new RegolaDominioNonValidaException("Il giorno deve essere ISO da 1 (lunedì) a 7 (domenica).");
        }
        if (startTime == null) {
            throw new RegolaDominioNonValidaException("L'ora del programma è obbligatoria.");
        }
        if (defaultCapacity < 1) {
            throw new RegolaDominioNonValidaException("La capienza predefinita deve essere almeno 1.");
        }
        if (validFrom == null) {
            throw new RegolaDominioNonValidaException("La data di decorrenza è obbligatoria.");
        }
        if (validTo != null && validTo.isBefore(validFrom)) {
            throw new RegolaDominioNonValidaException(
                    "La fine della validità non può precedere la decorrenza.");
        }
    }
}