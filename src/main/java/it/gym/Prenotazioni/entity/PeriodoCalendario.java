package it.gym.Prenotazioni.entity;

import it.gym.Prenotazioni.exception.RegolaDominioNonValidaException;
import java.time.LocalDate;
import java.util.List;

public record PeriodoCalendario(LocalDate primoGiorno) {
    public PeriodoCalendario {
        if (primoGiorno == null) {
            throw new RegolaDominioNonValidaException("La data iniziale del calendario e obbligatoria.");
        }
    }

    public LocalDate ultimoGiorno() {
        return primoGiorno.plusDays(29);
    }

    public List<LocalDate> date() {
        return primoGiorno.datesUntil(ultimoGiorno().plusDays(1)).toList();
    }
}