package it.gym.Prenotazioni;

import it.gym.Prenotazioni.entity.AgendaCorsi;
import it.gym.Prenotazioni.entity.Corsi;
import it.gym.Prenotazioni.entity.Prenotazioni;
import it.gym.Prenotazioni.entity.ProgrammaSettimanale;
import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.enums.PrenotazioniChannel;
import it.gym.Prenotazioni.enums.PrenotazioniStatus;
import it.gym.Prenotazioni.enums.UserRole;
import it.gym.Prenotazioni.exception.CorsoGiaEsistenteException;
import it.gym.Prenotazioni.exception.MiaPalestraException;
import it.gym.Prenotazioni.repository.AgendaCorsiRepository;
import it.gym.Prenotazioni.repository.CorsiRepository;
import it.gym.Prenotazioni.repository.PrenotazioniRepository;
import it.gym.Prenotazioni.repository.ProgrammaSettimanaleRepository;
import it.gym.Prenotazioni.repository.UtentiRepository;
import it.gym.Prenotazioni.service.CorsiService;
import it.gym.Prenotazioni.service.ProgrammiSettimanaliService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CorsiProgrammiServiceTests {

    private static final LocalDate LUNEDI = LocalDate.of(2026, 11, 2);

    @Autowired
    private CorsiService corsiService;

    @Autowired
    private ProgrammiSettimanaliService programmiService;

    @Autowired
    private CorsiRepository corsiRepository;

    @Autowired
    private ProgrammaSettimanaleRepository programmaRepository;

    @Autowired
    private AgendaCorsiRepository agendaRepository;

    @Autowired
    private PrenotazioniRepository prenotazioniRepository;

    @Autowired
    private UtentiRepository utentiRepository;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:corsi-programma;DB_CLOSE_DELAY=-1");
    }

    @Test
    void creaCorsoConNomeNormalizzatoERifiutaDuplicatiSenzaDistinzioneDiMaiuscole() {
        Corsi corso = corsiService.creaCorso("  Danza  ", "Descrizione");

        assertEquals("Danza", corso.getName());
        assertThrows(CorsoGiaEsistenteException.class, () -> corsiService.creaCorso("danza", null));
    }

    @Test
    void rifiutaProgrammaSovrappostoMaAccettaLaDecorrenzaIlGiornoSuccessivoAllaFine() {
        Corsi corso = corsiService.creaCorso("Corso sovrapposizioni", null);
        ProgrammaSettimanale primo = programmiService.crea(corso.getId(), 1, LocalTime.of(9, 0), 10,
                LUNEDI, LUNEDI.plusDays(14));

        assertThrows(MiaPalestraException.class, () -> programmiService.crea(corso.getId(), 1,
                LocalTime.of(9, 0), 15, LUNEDI.plusDays(14), null));
        ProgrammaSettimanale successivo = programmiService.crea(corso.getId(), 1, LocalTime.of(9, 0), 15,
                LUNEDI.plusDays(15), null);

        assertEquals(LUNEDI.plusDays(14), primo.getValidTo());
        assertEquals(LUNEDI.plusDays(15), successivo.getValidFrom());
    }

    @Test
    void modificaProgrammaChiudeLaVecchiaRegolaECreaUnIdNuovoSenzaModificareLezioniPrenotate() {
        Corsi corso = corsiService.creaCorso("Corso versione", null);
        ProgrammaSettimanale precedente = programmiService.crea(corso.getId(), 1, LocalTime.of(9, 0), 10,
                LUNEDI, null);
        AgendaCorsi lezione = agendaRepository.save(new AgendaCorsi(precedente, LUNEDI, LocalTime.of(9, 0), 8));
        Utenti cliente = utentiRepository.save(new Utenti("cliente-versione", "hash", "Test", "Cliente",
                null, null, UserRole.CUSTOMER));
        Prenotazioni prenotazione = prenotazioniRepository.save(new Prenotazioni(cliente, lezione,
                PrenotazioniStatus.CONFIRMED, PrenotazioniChannel.WEB, null));
        LocalDate decorrenza = LUNEDI.plusDays(7);

        ProgrammaSettimanale nuovo = programmiService.modifica(precedente.getId(), corso.getId(), 1,
                LocalTime.of(10, 30), 12, decorrenza, null);

        assertNotEquals(precedente.getId(), nuovo.getId());
        assertEquals(decorrenza.minusDays(1), precedente.getValidTo());
        assertEquals(LUNEDI, agendaRepository.findById(lezione.getId()).orElseThrow().getLessonDate());
        assertEquals(precedente.getId(), agendaRepository.findById(lezione.getId()).orElseThrow()
                .getProgrammaSettimanale().getId());
        assertEquals(PrenotazioniStatus.CONFIRMED,
                prenotazioniRepository.findById(prenotazione.getId()).orElseThrow().getStatus());
        assertEquals(2, programmaRepository.count());
    }

    @Test
    void disattivareUnCorsoBloccaNuoviProgrammiMaConservaLezioniEPrenotazioni() {
        Corsi corso = corsiService.creaCorso("Corso disattivazione", null);
        ProgrammaSettimanale programma = programmiService.crea(corso.getId(), 1, LocalTime.of(9, 0), 10,
                LUNEDI, null);
        AgendaCorsi lezione = agendaRepository.save(new AgendaCorsi(programma, LUNEDI, LocalTime.of(9, 0), 10));
        Utenti cliente = utentiRepository.save(new Utenti("cliente-disattivato-corso", "hash", "Test", "Cliente",
                null, null, UserRole.CUSTOMER));
        Prenotazioni prenotazione = prenotazioniRepository.save(new Prenotazioni(cliente, lezione,
                PrenotazioniStatus.CONFIRMED, PrenotazioniChannel.WEB, null));

        corsiService.disattivaCorso(corso.getId());

        assertFalse(corsiRepository.findById(corso.getId()).orElseThrow().isActive());
        assertTrue(agendaRepository.findById(lezione.getId()).isPresent());
        assertEquals(PrenotazioniStatus.CONFIRMED,
                prenotazioniRepository.findById(prenotazione.getId()).orElseThrow().getStatus());
        assertThrows(MiaPalestraException.class, () -> programmiService.crea(corso.getId(), 2,
                LocalTime.of(9, 0), 10, LUNEDI, null));
        assertThrows(MiaPalestraException.class, () -> new AgendaCorsi(programma, LUNEDI.plusDays(7),
                LocalTime.of(9, 0), 10));
        assertFalse(programma.eValidoIl(LUNEDI.plusDays(7)));
    }
}
