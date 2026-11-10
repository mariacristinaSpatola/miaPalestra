package it.gym.Prenotazioni.entity;

import it.gym.Prenotazioni.enums.UserRole;
import it.gym.Prenotazioni.exception.AccessoNonAutorizzatoException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Entity
@Table(name = "utenti")
public class Utenti {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String password;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(unique = true, length = 254)
    private String email;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Utenti() {
    }

    public Utenti(String username, String password, String firstName, String lastName,
            String email, String phone, UserRole role) {
        validaTestoObbligatorio(username, "Username");
        validaTestoObbligatorio(password, "Password");
        validaTestoObbligatorio(firstName, "Nome");
        validaTestoObbligatorio(lastName, "Cognome");
        if (role == null) {
            throw new IllegalArgumentException("Il ruolo dell'utente è obbligatorio.");
        }
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.active = true;
        this.createdAt = LocalDateTime.now(ZoneId.of("Europe/Rome"));
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void aggiornaDati(String firstName, String lastName, String email, String phone) {
        validaTestoObbligatorio(firstName, "Nome");
        validaTestoObbligatorio(lastName, "Cognome");
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

    public void cambiaPassword(String password) {
        validaTestoObbligatorio(password, "Password");
        this.password = password;
    }

    public void cambiaRuolo(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Il ruolo dell'utente è obbligatorio.");
        }
        this.role = role;
    }

    public void impostaAttivo(boolean active) {
        this.active = active;
    }

    public void verificaPermessoPrenotazione(Utenti cliente) {
        if (cliente == null || cliente.getRole() != UserRole.CUSTOMER) {
            throw new AccessoNonAutorizzatoException("Le prenotazioni sono consentite solo per i clienti.");
        }
        if (role == UserRole.CUSTOMER) {
            if (!Objects.equals(id, cliente.getId())) {
                throw new AccessoNonAutorizzatoException(
                        "Un cliente può gestire solo le proprie prenotazioni.");
            }
            return;
        }
        if (role != UserRole.ADMIN && role != UserRole.SECRETARY) {
            throw new AccessoNonAutorizzatoException("Non hai i permessi per gestire le prenotazioni.");
        }
    }

    public void verificaPermessoCancellazione(Prenotazioni prenotazione) {
        if (prenotazione == null) {
            throw new AccessoNonAutorizzatoException("La prenotazione non è accessibile.");
        }
        verificaPermessoPrenotazione(prenotazione.getCustomer());
    }

    private static void validaTestoObbligatorio(String value, String campo) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(campo + " obbligatorio.");
        }
    }
}