package it.gym.Prenotazioni.service;

import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.enums.UserRole;
import it.gym.Prenotazioni.exception.PasswordCorrenteNonValidaException;
import it.gym.Prenotazioni.exception.UtenteGiaEsistenteException;
import it.gym.Prenotazioni.exception.UtenteNonTrovatoException;
import it.gym.Prenotazioni.repository.UtentiRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        if (utentiRepository.existsByUsername(username)) {
            throw new UtenteGiaEsistenteException(username);
        }
        Utenti utente = new Utenti(username, passwordEncoder.encode(password), firstName, lastName,
                email, phone, role);
        return utentiRepository.save(utente);
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
}
