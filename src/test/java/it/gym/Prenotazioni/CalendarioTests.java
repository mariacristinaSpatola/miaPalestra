package it.gym.Prenotazioni;

import it.gym.Prenotazioni.entity.*;
import it.gym.Prenotazioni.enums.*;
import it.gym.Prenotazioni.exception.MiaPalestraException;
import it.gym.Prenotazioni.repository.*;
import it.gym.Prenotazioni.service.CalendarioService;
import it.gym.Prenotazioni.service.PrenotazioniService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:calendario-tests;DB_CLOSE_DELAY=-1")
class CalendarioTests {
    private static final String PASSWORD = "password-di-test";

    @Autowired
    CalendarioService calendario;
    @Autowired
    PrenotazioniService prenotazioniService;
    @Autowired
    CorsiRepository corsi;
    @Autowired
    ProgrammaSettimanaleRepository programmi;
    @Autowired
    AgendaCorsiRepository agenda;
    @Autowired
    PrenotazioniRepository prenotazioni;
    @Autowired
    UtentiRepository utenti;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    WebApplicationContext contesto;
    private MockMvc http;
    private Corsi corso;
    private ProgrammaSettimanale programma;
    private final LocalDate inizio = LocalDate.of(2099, 1, 5);

    @BeforeEach
    void prepara() {
        http = MockMvcBuilders.webAppContextSetup(contesto)
                .addFilters(contesto.getBean(FilterChainProxy.class)).build();
        corso = corsi.save(new Corsi("Corso " + UUID.randomUUID(), "Descrizione pubblica"));
        programma = programmi.save(new ProgrammaSettimanale(corso, inizio.getDayOfWeek().getValue(),
                LocalTime.of(18, 0), 2, inizio, null));
    }

    @Test
    void pubblicareDueVolteNonDuplicaLeLezioniENonSuperaIlTrentesimoGiorno() {
        var prima = calendario.pubblica(inizio);
        var seconda = calendario.pubblica(inizio);
        assertEquals(inizio.plusDays(29), prima.al());
        assertEquals(5, prima.create());
        assertEquals(0, seconda.create());
        assertEquals(5, calendario.elenco(inizio, inizio.plusDays(29)).size());
        assertEquals(60, calendario.elenco(inizio, inizio).getFirst().durataMinuti());
        assertTrue(calendario.elenco(inizio.plusDays(30), inizio.plusDays(35)).isEmpty());
    }

    @Test
    void ripubblicareConservaLaPrenotazioneESegnalaLaLezioneInConflitto() {
        calendario.pubblica(inizio);
        var lezione = agenda.findByLessonDateBetweenOrderByLessonDateAscStartTimeAsc(inizio, inizio).getFirst();
        var cliente = cliente();
        prenotazioni.saveAndFlush(new Prenotazioni(cliente, lezione, PrenotazioniStatus.CONFIRMED,
                PrenotazioniChannel.WEB, null));
        programma.disattiva();
        var risultato = calendario.pubblica(inizio);
        assertEquals(4, risultato.annullate());
        assertEquals(java.util.List.of(lezione.getId()), risultato.conflitti());
        assertEquals(OccurrenceStatus.SCHEDULED, lezione.getStatus());
        assertEquals(1, calendario.elenco(inizio, inizio).getFirst().postiDisponibili());
    }

    @Test
    void ripubblicareNonAnnullaUnaLezioneModificataManualmente() {
        calendario.pubblica(inizio);
        var lezione = agenda.findByLessonDateBetweenOrderByLessonDateAscStartTimeAsc(inizio, inizio).getFirst();

        calendario.modificaLezione(lezione.getId(), LocalTime.of(20, 30), 30, 2);
        programma.disattiva();
        var risultato = calendario.pubblica(inizio);

        assertEquals(4, risultato.annullate());
        assertTrue(risultato.conflitti().contains(lezione.getId()));
        assertEquals(OccurrenceStatus.SCHEDULED, lezione.getStatus());
        assertEquals(LocalTime.of(20, 30), lezione.getStartTime());
        assertTrue(lezione.isModificataManualmente());
    }

    @Test
    void annullareLezioneCancellaLePrenotazioniConTimestampNellaStessaTransazione() {
        calendario.pubblica(inizio);
        var lezione = agenda.findByLessonDateBetweenOrderByLessonDateAscStartTimeAsc(inizio, inizio).getFirst();
        var prima = prenotazioni.save(new Prenotazioni(cliente(), lezione, PrenotazioniStatus.CONFIRMED,
                PrenotazioniChannel.WEB, null));
        var seconda = prenotazioni.save(new Prenotazioni(cliente(), lezione, PrenotazioniStatus.CONFIRMED,
                PrenotazioniChannel.SECRETARY, null));

        calendario.annullaLezione(lezione.getId());

        assertEquals(OccurrenceStatus.CANCELLED, lezione.getStatus());
        assertEquals(PrenotazioniStatus.CANCELLED, prima.getStatus());
        assertEquals(PrenotazioniStatus.CANCELLED, seconda.getStatus());
        assertNotNull(prima.getCancelledAt());
        assertNotNull(seconda.getCancelledAt());
        assertEquals(0, prenotazioni.countByOccurrenceIdAndStatus(lezione.getId(), PrenotazioniStatus.CONFIRMED));
    }

    @Test
    void adminESegreteriaModificanoLeLezioniMaClienteENonAutenticatoSonoRifiutati() throws Exception {
        var lezione = agenda.save(new AgendaCorsi(programma, inizio, LocalTime.of(18, 0), 2));
        Sessione admin = autentica(UserRole.ADMIN);
        Sessione segreteria = autentica(UserRole.SECRETARY);
        Sessione cliente = autentica(UserRole.CUSTOMER);

        http.perform(put("/api/calendario/lezioni/" + lezione.getId())
                .session(admin.sessione())
                .header("X-CSRF-TOKEN", tokenCsrf(admin.sessione()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ora\":\"19:00\",\"durataMinuti\":60,\"capienza\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ora").value("19:00:00"));

        http.perform(post("/api/calendario/lezioni/" + lezione.getId() + "/annulla")
                .session(segreteria.sessione())
                .header("X-CSRF-TOKEN", tokenCsrf(segreteria.sessione())))
                .andExpect(status().isNoContent());

        http.perform(put("/api/calendario/lezioni/" + lezione.getId())
                .session(cliente.sessione())
                .header("X-CSRF-TOKEN", tokenCsrf(cliente.sessione()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ora\":\"19:00\",\"durataMinuti\":60,\"capienza\":3}"))
                .andExpect(status().isForbidden());

        Sessione anonimo = ottieniCsrf();
        http.perform(put("/api/calendario/lezioni/" + lezione.getId())
                .session(anonimo.sessione())
                .header("X-CSRF-TOKEN", anonimo.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ora\":\"19:00\",\"durataMinuti\":60,\"capienza\":3}"))
                .andExpect(status().isUnauthorized());

        http.perform(put("/api/calendario/lezioni/999999")
                .session(admin.sessione())
                .header("X-CSRF-TOKEN", tokenCsrf(admin.sessione()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ora\":\"19:00\",\"durataMinuti\":60,\"capienza\":3}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ilClienteNonPuoLeggereLePrenotazioniAltrui() {
        var cliente = cliente();
        var altro = cliente();
        assertThrows(MiaPalestraException.class,
                () -> prenotazioniService.elencoPerUtente(altro.getId(), cliente.getUsername()));
        assertTrue(prenotazioniService.elencoPerUtente(null, cliente.getUsername()).isEmpty());
    }

    @Test
    void ilCatalogoPubblicoNonRichiedeLoginENonEsponeCorsiDisattivati() throws Exception {
        corso.impostaAttivo(false);
        http.perform(get("/api/pubblico/corsi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.attivo == false)]").isEmpty());
        http.perform(get("/api/pubblico/lezioni").param("dal", "2099-01-05").param("al", "2099-01-11"))
                .andExpect(status().isOk());
        http.perform(get("/api/prenotazioni")).andExpect(status().isUnauthorized());
        http.perform(post("/api/calendario/pubblica").contentType("application/json")
                .content("{\"dal\":\"2099-01-05\"}")).andExpect(status().isForbidden());
    }

    @Test
    void rifiutaPeriodiPassatiEIntervalliDiConsultazioneEccessivi() {
        assertThrows(MiaPalestraException.class, () -> calendario.pubblica(LocalDate.of(2020, 1, 1)));
        assertThrows(MiaPalestraException.class, () -> calendario.elenco(inizio, inizio.plusDays(63)));
        assertThrows(MiaPalestraException.class, () -> calendario.elenco(inizio, inizio.minusDays(1)));
    }

    private Utenti cliente() {
        return utenti.save(new Utenti("cliente-" + UUID.randomUUID(), "hash-test", "Nome", "Cliente",
                null, null, UserRole.CUSTOMER));
    }

    private Sessione autentica(UserRole ruolo) throws Exception {
        String username = "calendario-" + ruolo.name().toLowerCase() + "-" + UUID.randomUUID();
        utenti.save(new Utenti(username, passwordEncoder.encode(PASSWORD), "Utente", "Test", null, null, ruolo));
        Sessione anonima = ottieniCsrf();
        MvcResult risultato = http.perform(post("/api/auth/login")
                .session(anonima.sessione())
                .header("X-CSRF-TOKEN", anonima.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession sessione = (MockHttpSession) risultato.getRequest().getSession(false);
        return ottieniCsrf(sessione);
    }

    private String tokenCsrf(MockHttpSession sessione) throws Exception {
        return ottieniCsrf(sessione).token();
    }

    private Sessione ottieniCsrf() throws Exception {
        MvcResult risultato = http.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(risultato.getResponse().getContentAsString()).get("token").asString();
        return new Sessione((MockHttpSession) risultato.getRequest().getSession(false), token);
    }

    private Sessione ottieniCsrf(MockHttpSession sessione) throws Exception {
        MvcResult risultato = http.perform(get("/api/auth/csrf").session(sessione))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(risultato.getResponse().getContentAsString()).get("token").asString();
        return new Sessione((MockHttpSession) risultato.getRequest().getSession(false), token);
    }

    private record Sessione(MockHttpSession sessione, String token) {
    }
}