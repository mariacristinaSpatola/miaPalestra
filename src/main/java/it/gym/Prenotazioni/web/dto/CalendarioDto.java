package it.gym.Prenotazioni.web.dto;

import java.time.LocalTime;

public final class CalendarioDto {

    private CalendarioDto() {
    }

    public record ModificaLezione(LocalTime ora, int durataMinuti, int capienza) {
    }
}