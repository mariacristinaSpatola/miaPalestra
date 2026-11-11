package it.gym.Prenotazioni.entity;

import it.gym.Prenotazioni.exception.RegolaDominioNonValidaException;
import java.time.LocalDate;
import java.util.List;

public record PeriodoCalendario(LocalDate primoGiorno, int numeroGiorni) {
    public PeriodoCalendario(LocalDate primoGiorno) {
        this(primoGiorno, 30);
    }

    public PeriodoCalendario {
        if (primoGiorno == null) {
            throw new RegolaDominioNonValidaException("La data iniziale del calendario e obbligatoria.");
        }
        if (numeroGiorni < 1) {
            throw new RegolaDominioNonValidaException("Il periodo deve comprendere almeno un giorno.");
        }
    }

    public LocalDate ultimoGiorno() {
        return primoGiorno.plusDays(numeroGiorni - 1L);
    }

    public List<LocalDate> date() {
        return primoGiorno.datesUntil(ultimoGiorno().plusDays(1)).toList();
    }
}