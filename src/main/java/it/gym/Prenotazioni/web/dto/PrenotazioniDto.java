package it.gym.Prenotazioni.web.dto;

import it.gym.Prenotazioni.entity.Prenotazioni;
import it.gym.Prenotazioni.enums.PrenotazioniChannel;
import it.gym.Prenotazioni.enums.PrenotazioniStatus;

import java.time.LocalDateTime;

public final class PrenotazioniDto {

    private PrenotazioniDto() {
    }

    public record NuovaPrenotazione(Long occorrenzaId, Long clienteId) {
    }

    public record PrenotazioneRisposta(
            Long id,
            Long clienteId,
            Long occorrenzaId,
            PrenotazioniStatus stato,
            PrenotazioniChannel canale,
            Long registratoDaId,
            LocalDateTime creatoIl,
            LocalDateTime cancellatoIl) {

        public static PrenotazioneRisposta da(Prenotazioni prenotazione) {
            return new PrenotazioneRisposta(
                    prenotazione.getId(),
                    prenotazione.getCustomer().getId(),
                    prenotazione.getOccurrence().getId(),
                    prenotazione.getStatus(),
                    prenotazione.getChannel(),
                    prenotazione.getCreatedBy() == null ? null : prenotazione.getCreatedBy().getId(),
                    prenotazione.getCreatedAt(),
                    prenotazione.getCancelledAt());
        }
    }
}
