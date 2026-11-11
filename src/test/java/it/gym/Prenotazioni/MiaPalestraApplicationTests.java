package it.gym.Prenotazioni;

import it.gym.Prenotazioni.entity.Corsi;
import it.gym.Prenotazioni.entity.AgendaCorsi;
import it.gym.Prenotazioni.entity.Prenotazioni;
import it.gym.Prenotazioni.entity.ProgrammaSettimanale;
import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.config.AdminBootstrap;
import it.gym.Prenotazioni.enums.PrenotazioniChannel;
import it.gym.Prenotazioni.enums.PrenotazioniStatus;
import it.gym.Prenotazioni.enums.UserRole;
import it.gym.Prenotazioni.repository.AgendaCorsiRepository;
import it.gym.Prenotazioni.repository.CorsiRepository;
import it.gym.Prenotazioni.repository.PrenotazioniRepository;
import it.gym.Prenotazioni.repository.ProgrammaSettimanaleRepository;
import it.gym.Prenotazioni.repository.UtentiRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class MiaPalestraApplicationTests {

    @Autowired
    private CorsiRepository corsiRepository;

    @Autowired
    private UtentiRepository utentiRepository;

    @Autowired
    private ProgrammaSettimanaleRepository programmaSettimanaleRepository;

    @Autowired
    private AgendaCorsiRepository agendaCorsiRepository;

    @Autowired
    private PrenotazioniRepository prenotazioniRepository;

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
    void bootstrapsAdminWithDefaultCredentialsAndDoesNotResetExistingAccount() {
        Utenti admin = utentiRepository.findByUsername("admin").orElseThrow();
        String originalPasswordHash = admin.getPassword();

        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(new BCryptPasswordEncoder().matches("12345678", originalPasswordHash)).isTrue();

        adminBootstrap.run();

        assertThat(utentiRepository.count()).isEqualTo(1);
        assertThat(utentiRepository.findByUsername("admin").orElseThrow().getPassword())
                .isEqualTo(originalPasswordHash);
    }

    @Test
    @Transactional
    void countsOnlyConfirmedBookingsAndLocksOccurrenceForUpdate() {
        Corsi course = corsiRepository.findAll().get(0);
        Utenti confirmedCustomer = utentiRepository.save(new Utenti("phase2-customer", "hash", "Test", "Customer",
                null, null, UserRole.CUSTOMER));
        Utenti cancelledCustomer = utentiRepository.save(new Utenti("phase2-customer-cancelled", "hash", "Test",
                "Customer", null, null, UserRole.CUSTOMER));
        ProgrammaSettimanale schedule = programmaSettimanaleRepository.save(new ProgrammaSettimanale(course, 1,
                LocalTime.of(9, 0), 10, LocalDate.of(2026, 1, 1), null));
        AgendaCorsi occurrence = agendaCorsiRepository.save(new AgendaCorsi(schedule, LocalDate.of(2026, 11, 9),
                LocalTime.of(9, 0), 10));

        prenotazioniRepository.save(new Prenotazioni(confirmedCustomer, occurrence, PrenotazioniStatus.CONFIRMED,
                PrenotazioniChannel.WEB, null));
        prenotazioniRepository.save(new Prenotazioni(cancelledCustomer, occurrence, PrenotazioniStatus.CANCELLED,
                PrenotazioniChannel.SECRETARY, null));

        assertThat(prenotazioniRepository.countByOccurrenceIdAndStatus(occurrence.getId(),
                PrenotazioniStatus.CONFIRMED)).isEqualTo(1);
        assertThat(prenotazioniRepository.countByOccurrenceIdAndStatus(occurrence.getId(),
                PrenotazioniStatus.CANCELLED)).isEqualTo(1);
        assertThat(agendaCorsiRepository.findByIdForUpdate(occurrence.getId()).orElseThrow().getId())
                .isEqualTo(occurrence.getId());
    }

    @Test
    @Transactional
    void enforcesUniqueOccurrencePerScheduleAndDate() {
        Corsi course = corsiRepository.findAll().get(0);
        ProgrammaSettimanale schedule = programmaSettimanaleRepository.save(new ProgrammaSettimanale(course, 1,
                LocalTime.of(9, 0), 10, LocalDate.of(2026, 1, 1), null));
        LocalDate lessonDate = LocalDate.of(2026, 11, 9);
        agendaCorsiRepository.saveAndFlush(new AgendaCorsi(schedule, lessonDate, LocalTime.of(9, 0), 10));

        assertThatThrownBy(() -> agendaCorsiRepository.saveAndFlush(
                new AgendaCorsi(schedule, lessonDate, LocalTime.of(10, 0), 10)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void enforcesUniqueCustomerAndOccurrenceBooking() {
        Corsi course = corsiRepository.findAll().get(0);
        Utenti customer = utentiRepository.save(new Utenti("phase2-unique-customer", "hash", "Test", "Customer",
                null, null, UserRole.CUSTOMER));
        ProgrammaSettimanale schedule = programmaSettimanaleRepository.save(new ProgrammaSettimanale(course, 1,
                LocalTime.of(9, 0), 10, LocalDate.of(2026, 1, 1), null));
        AgendaCorsi occurrence = agendaCorsiRepository.save(new AgendaCorsi(schedule, LocalDate.of(2026, 11, 9),
                LocalTime.of(9, 0), 10));
        prenotazioniRepository.saveAndFlush(new Prenotazioni(customer, occurrence, PrenotazioniStatus.CONFIRMED,
                PrenotazioniChannel.WEB, null));

        assertThatThrownBy(() -> prenotazioniRepository.saveAndFlush(new Prenotazioni(customer, occurrence,
                PrenotazioniStatus.CANCELLED, PrenotazioniChannel.WEB, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}