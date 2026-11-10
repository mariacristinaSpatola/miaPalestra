package it.gym.Prenotazioni.service;

import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.enums.UserRole;
import it.gym.Prenotazioni.exception.PasswordCorrenteNonValidaException;
import it.gym.Prenotazioni.exception.EmailGiaEsistenteException;
import it.gym.Prenotazioni.exception.UtenteGiaEsistenteException;
import it.gym.Prenotazioni.exception.UtenteNonTrovatoException;
import it.gym.Prenotazioni.repository.UtentiRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.nio.charset.StandardCharsets;

@Service
public class UtentiService {

    private final UtentiRepository utentiRepository;
    private final PasswordEncoder passwordEncoder;

    public UtentiService(UtentiRepository utentiRepository, PasswordEncoder passwordEncoder) {
        this.utentiRepository = utentiRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Utenti creaUtente(String username, String password, String firstName, String lastName,
            String email, String phone, UserRole role) {
        validaTestoObbligatorio(username, "Username");
        validaTestoObbligatorio(password, "Password");
        if (utentiRepository.existsByUsername(username)) {
            throw new UtenteGiaEsistenteException(username);
        }
        Utenti utente = new Utenti(username, passwordEncoder.encode(password), firstName, lastName,
                email, phone, role);
        return utentiRepository.save(utente);
    }

    @Transactional
    public Utenti registraCliente(String username, String password, String firstName, String lastName,
            String email, String phone) {
        String usernameValido = username == null ? null : username.trim();
        validaTestoObbligatorio(usernameValido, "Username");
        if (usernameValido.length() < 3 || usernameValido.length() > 80) {
            throw new IllegalArgumentException("Lo username deve contenere da 3 a 80 caratteri.");
        }
        validaTestoObbligatorio(firstName, "Nome");
        validaTestoObbligatorio(lastName, "Cognome");
        if (firstName.trim().length() > 100 || lastName.trim().length() > 100) {
            throw new IllegalArgumentException("Nome e cognome non possono superare 100 caratteri.");
        }
        validaPasswordRegistrazione(password);

        String emailValida = email == null || email.isBlank() ? null : email.trim();
        String telefonoValido = phone == null || phone.isBlank() ? null : phone.trim();
        if (emailValida != null
                && (emailValida.length() > 254 || !emailValida.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            throw new IllegalArgumentException("Inserire un indirizzo email valido.");
        }
        if (telefonoValido != null && telefonoValido.length() > 30) {
            throw new IllegalArgumentException("Il numero di telefono non può superare 30 caratteri.");
        }
        if (utentiRepository.existsByUsername(usernameValido)) {
            throw new UtenteGiaEsistenteException(usernameValido);
        }
        if (emailValida != null && utentiRepository.existsByEmail(emailValida)) {
            throw new EmailGiaEsistenteException();
        }
        return creaUtente(usernameValido, password, firstName.trim(), lastName.trim(), emailValida,
                telefonoValido, UserRole.CUSTOMER);
    }

    @Transactional(readOnly = true)
    public List<Utenti> trovaTutti() {
        return utentiRepository.findAllByOrderByLastNameAscFirstNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Utenti> cercaClienti(String query) {
        if (query == null || query.isBlank() || query.trim().length() < 2) {
            throw new IllegalArgumentException("La ricerca deve contenere almeno due caratteri.");
        }
        return utentiRepository.cercaPerRuoloENome(UserRole.CUSTOMER, query.trim());
    }

    @Transactional(readOnly = true)
    public Utenti trovaPerId(Long id) {
        return utentiRepository.findById(id).orElseThrow(UtenteNonTrovatoException::new);
    }

    @Transactional(readOnly = true)
    public Utenti trovaPerUsername(String username) {
        return utentiRepository.findByUsername(username).orElseThrow(UtenteNonTrovatoException::new);
    }

    @Transactional
    public Utenti aggiornaUtente(Long id, String firstName, String lastName, String email,
            String phone, UserRole role, Boolean active, String password) {
        Utenti utente = trovaPerId(id);
        utente.aggiornaDati(firstName, lastName, email, phone);
        if (role != null) {
            utente.cambiaRuolo(role);
        }
        if (active != null) {
            utente.impostaAttivo(active);
        }
        if (password != null && !password.isBlank()) {
            utente.cambiaPassword(passwordEncoder.encode(password));
        }
        return utente;
    }

    @Transactional
    public Utenti aggiornaProfilo(String username, String firstName, String lastName,
            String email, String phone) {
        Utenti utente = trovaPerUsername(username);
        utente.aggiornaDati(firstName, lastName, email, phone);
        return utente;
    }

    @Transactional
    public void cambiaPassword(String username, String currentPassword, String newPassword) {
        Utenti utente = trovaPerUsername(username);
        if (!passwordEncoder.matches(currentPassword, utente.getPassword())) {
            throw new PasswordCorrenteNonValidaException();
        }
        utente.cambiaPassword(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void disattivaUtente(Long id) {
        trovaPerId(id).impostaAttivo(false);
    }

    private void validaTestoObbligatorio(String value, String campo) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(campo + " obbligatorio.");
        }
    }

    private void validaPasswordRegistrazione(String password) {
        if (password == null || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException(
                    "La password deve contenere almeno 8 caratteri, una maiuscola, una minuscola e un numero.");
        }
    }
}
