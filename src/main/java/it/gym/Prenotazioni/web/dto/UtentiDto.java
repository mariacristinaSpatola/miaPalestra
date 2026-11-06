package it.gym.Prenotazioni.web.dto;

import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.enums.UserRole;

import java.time.LocalDateTime;

public final class UtentiDto {

    private UtentiDto() {
    }

    public record LoginRequest(String username, String password) {
    }

    public record TokenCsrf(String token) {
    }

    public record NuovoUtente(String username, String password, String firstName, String lastName,
            String email, String phone, UserRole role) {
    }

    public record AggiornaUtente(String firstName, String lastName, String email, String phone,
            UserRole role, Boolean active, String password) {
    }

    public record AggiornaProfilo(String firstName, String lastName, String email, String phone) {
    }

    public record CambioPassword(String currentPassword, String newPassword) {
    }

    public record UtenteRisposta(Long id, String username, String firstName, String lastName,
            String email, String phone, UserRole role, boolean active, LocalDateTime createdAt) {

        public static UtenteRisposta da(Utenti utente) {
            return new UtenteRisposta(utente.getId(), utente.getUsername(), utente.getFirstName(),
                    utente.getLastName(), utente.getEmail(), utente.getPhone(), utente.getRole(),
                    utente.isActive(), utente.getCreatedAt());
        }
    }
}
