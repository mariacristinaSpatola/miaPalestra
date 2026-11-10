package it.gym.Prenotazioni.web.dto;

import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public final class UtentiDto {

        private UtentiDto() {
        }

        @Schema(description = "Credenziali di login", example = "{\"username\":\"admin\",\"password\":\"admin123\"}")
        public record LoginRequest(
                        @Schema(description = "Username dell'utente", example = "admin") String username,
                        @Schema(description = "Password dell'utente", example = "admin123") String password) {
        }

        @Schema(description = "Dati per registrare un account cliente")
        public record RegistrazioneCliente(
                        String username,
                        String password,
                        String firstName,
                        String lastName,
                        String email,
                        String phone) {
        }

        @Schema(description = "Token CSRF per proteggere le operazioni modificative")
        public record TokenCsrf(
                        @Schema(description = "Token CSRF da includere nell'header X-CSRF-TOKEN", example = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6") String token) {
        }

        @Schema(description = "Dati per creare un nuovo utente", example = "{\"username\":\"mario.rossi\",\"password\":\"Password123!\",\"firstName\":\"Mario\",\"lastName\":\"Rossi\",\"email\":\"mario@example.com\",\"phone\":\"3331234567\",\"role\":\"CUSTOMER\"}")
        public record NuovoUtente(
                        @Schema(description = "Username univoco", example = "mario.rossi", minLength = 3, maxLength = 50) String username,
                        @Schema(description = "Password (min 8 char, contiene maiuscola, minuscola, numero)", example = "Password123!") String password,
                        @Schema(description = "Nome", example = "Mario", maxLength = 100) String firstName,
                        @Schema(description = "Cognome", example = "Rossi", maxLength = 100) String lastName,
                        @Schema(description = "Email", example = "mario@example.com", maxLength = 100) String email,
                        @Schema(description = "Numero di telefono", example = "3331234567", maxLength = 20) String phone,
                        @Schema(description = "Ruolo (ADMIN, SECRETARY, CUSTOMER)", example = "CUSTOMER") UserRole role) {
        }

        @Schema(description = "Dati per aggiornare un utente (solo ADMIN)")
        public record AggiornaUtente(
                        @Schema(description = "Nome", example = "Mario") String firstName,
                        @Schema(description = "Cognome", example = "Rossi") String lastName,
                        @Schema(description = "Email", example = "mario@example.com") String email,
                        @Schema(description = "Numero di telefono", example = "3331234567") String phone,
                        @Schema(description = "Ruolo (ADMIN, SECRETARY, CUSTOMER)", example = "CUSTOMER") UserRole role,
                        @Schema(description = "Utente attivo (true) o disattivato (false)", example = "true") Boolean active,
                        @Schema(description = "Nuova password (opzionale, se fornita sovrascrive quella attuale)") String password) {
        }

        @Schema(description = "Dati per aggiornare il proprio profilo")
        public record AggiornaProfilo(
                        @Schema(description = "Nome", example = "Mario") String firstName,
                        @Schema(description = "Cognome", example = "Rossi") String lastName,
                        @Schema(description = "Email", example = "mario@example.com") String email,
                        @Schema(description = "Numero di telefono", example = "3331234567") String phone) {
        }

        @Schema(description = "Richiesta di cambio password")
        public record CambioPassword(
                        @Schema(description = "Password attuale", example = "OldPassword123!") String currentPassword,
                        @Schema(description = "Nuova password", example = "NewPassword123!") String newPassword) {
        }

        @Schema(description = "Dati di risposta per un utente")
        public record UtenteRisposta(
                        @Schema(description = "ID univoco dell'utente", example = "1") Long id,
                        @Schema(description = "Username", example = "mario.rossi") String username,
                        @Schema(description = "Nome", example = "Mario") String firstName,
                        @Schema(description = "Cognome", example = "Rossi") String lastName,
                        @Schema(description = "Email", example = "mario@example.com") String email,
                        @Schema(description = "Telefono", example = "3331234567") String phone,
                        @Schema(description = "Ruolo (ADMIN, SECRETARY, CUSTOMER)", example = "CUSTOMER") UserRole role,
                        @Schema(description = "Stato dell'utente (true=attivo, false=disattivato)", example = "true") boolean active,
                        @Schema(description = "Data e ora di creazione", example = "2026-11-06T15:00:00") LocalDateTime createdAt) {

                public static UtenteRisposta da(Utenti utente) {
                        return new UtenteRisposta(utente.getId(), utente.getUsername(), utente.getFirstName(),
                                        utente.getLastName(), utente.getEmail(), utente.getPhone(), utente.getRole(),
                                        utente.isActive(), utente.getCreatedAt());
                }
        }
}
