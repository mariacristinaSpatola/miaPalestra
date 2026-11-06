package it.gym.Prenotazioni.entity;

import it.gym.Prenotazioni.exception.RegolaDominioNonValidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Locale;

@Entity
@Table(name = "corsi")
public class Corsi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "name_normalized", nullable = false, unique = true, length = 100)
    private String nameNormalized;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean active;

    protected Corsi() {
    }

    public Corsi(String name, String description) {
        validaNome(name);
        this.name = name.trim();
        this.nameNormalized = chiaveNome(name);
        validaDescrizione(description);
        this.description = description;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public void aggiornaDati(String name, String description) {
        validaNome(name);
        validaDescrizione(description);
        this.name = name.trim();
        this.nameNormalized = chiaveNome(name);
        this.description = description;
    }

    public void impostaAttivo(boolean active) {
        this.active = active;
    }

    private static void validaNome(String name) {
        if (name == null || name.isBlank()) {
            throw new RegolaDominioNonValidaException("Il nome del corso è obbligatorio.");
        }
        if (name.length() > 100) {
            throw new RegolaDominioNonValidaException("Il nome del corso non può superare 100 caratteri.");
        }
    }

    private static void validaDescrizione(String description) {
        if (description != null && description.length() > 1000) {
            throw new RegolaDominioNonValidaException(
                    "La descrizione del corso non può superare 1000 caratteri.");
        }
    }

    public static String chiaveNome(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}