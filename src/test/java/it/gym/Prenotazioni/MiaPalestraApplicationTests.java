package it.gym.Prenotazioni;

import it.gym.Prenotazioni.entity.Corsi;
import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.config.AdminBootstrap;
import it.gym.Prenotazioni.repository.CorsiRepository;
import it.gym.Prenotazioni.repository.UtentiRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.bootstrap-admin.enabled=true",
        "app.bootstrap-admin.username=phase1-admin",
        "app.bootstrap-admin.password=test-bootstrap-password",
        "app.bootstrap-admin.first-name=Test",
        "app.bootstrap-admin.last-name=Admin"
})
@ActiveProfiles("test")
class MiaPalestraApplicationTests {

    @Autowired
    private CorsiRepository corsiRepository;

    @Autowired
    private UtentiRepository utentiRepository;

    @Autowired
    private AdminBootstrap adminBootstrap;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:phase1;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
    }

    @Test
    void migratesSchemaAndSeedsInitialCourses() {
        assertThat(corsiRepository.findAll())
                .extracting(Corsi::getName)
                .containsExactlyInAnyOrder("Yoga", "Pilates", "Acquagym");
    }

    @Test
    void bootstrapsAdminWithBcryptAndDoesNotResetExistingAccount() {
        Utenti admin = utentiRepository.findByUsername("phase1-admin").orElseThrow();
        String originalPasswordHash = admin.getPassword();

        assertThat(new BCryptPasswordEncoder().matches("test-bootstrap-password", originalPasswordHash)).isTrue();

        adminBootstrap.run();

        assertThat(utentiRepository.count()).isEqualTo(1);
        assertThat(utentiRepository.findByUsername("phase1-admin").orElseThrow().getPassword())
                .isEqualTo(originalPasswordHash);
    }
}