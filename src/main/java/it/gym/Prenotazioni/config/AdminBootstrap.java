package it.gym.Prenotazioni.config;

import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.enums.UserRole;
import it.gym.Prenotazioni.repository.UtentiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UtentiRepository utentiRepository;
    private final BootstrapAdminProperties properties;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(UtentiRepository utentiRepository, BootstrapAdminProperties properties,
            PasswordEncoder passwordEncoder) {
        this.utentiRepository = utentiRepository;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (isBlank(properties.username()) || isBlank(properties.password())
                || isBlank(properties.firstName()) || isBlank(properties.lastName())) {
            throw new IllegalStateException("Credenziali e nominativo admin richiesti per il bootstrap.");
        }

        if (utentiRepository.findByUsername(properties.username()).isPresent()) {
            logger.info("Bootstrap admin ignorato: l'account configurato esiste gia'.");
            return;
        }

        Utenti admin = new Utenti(
                properties.username(),
                passwordEncoder.encode(properties.password()),
                properties.firstName(),
                properties.lastName(),
                isBlank(properties.email()) ? null : properties.email(),
                null,
                UserRole.ADMIN);
        utentiRepository.save(admin);
        logger.info("Account ADMIN iniziale creato.");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}