package it.gym.Prenotazioni;

import it.gym.Prenotazioni.entity.AgendaCorsi;
import it.gym.Prenotazioni.entity.Corsi;
import it.gym.Prenotazioni.entity.Prenotazioni;
import it.gym.Prenotazioni.entity.ProgrammaSettimanale;
import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.enums.PrenotazioniChannel;
import it.gym.Prenotazioni.enums.PrenotazioniStatus;
import it.gym.Prenotazioni.enums.UserRole;
import it.gym.Prenotazioni.repository.AgendaCorsiRepository;
import it.gym.Prenotazioni.repository.CorsiRepository;
import it.gym.Prenotazioni.repository.PrenotazioniRepository;
import it.gym.Prenotazioni.repository.ProgrammaSettimanaleRepository;
import it.gym.Prenotazioni.repository.UtentiRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class PrenotazioniControllerTests {

    private static final String PASSWORD = "password-di-test";
    private static final LocalDate LEZIONE_FUTURA = LocalDate.of(2099, 1, 5);

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private UtentiRepository utentiRepository;

    @Autowired
    private CorsiRepository corsiRepository;

    @Autowired
    private ProgrammaSettimanaleRepository programmiRepository;

    @Autowired
    private AgendaCorsiRepository agendaRepository;

    @Autowired
    private PrenotazioniRepository prenotazioniRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Utenti admin;
    private Utenti segretaria;
    private Utenti cliente;
    private Utenti altroCliente;
    private Utenti altroRuolo;
    private AgendaCorsi lezione;
    private AgendaCorsi secondaLezione;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:prenotazioni-controller;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
    }

    @BeforeEach
    void prepara() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .addFilters(applicationContext.getBean(FilterChainProxy.class))
                .build();
        prenotazioniRepository.deleteAll();
        agendaRepository.deleteAll();
        programmiRepository.deleteAll();
        utentiRepository.deleteAll();

        admin = creaUtente("admin-prenotazioni", UserRole.ADMIN);
        segretaria = creaUtente("segretaria-prenotazioni", UserRole.SECRETARY);
        cliente = creaUtente("cliente-prenotazioni", UserRole.CUSTOMER);
        altroCliente = creaUtente("altro-cliente-prenotazioni", UserRole.CUSTOMER);
        altroRuolo = creaUtente("altro-ruolo-prenotazioni", UserRole.SECRETARY);

        Corsi corso = corsiRepository.findAll().getFirst();
        ProgrammaSettimanale programma = programmiRepository.save(new ProgrammaSettimanale(corso,
                LEZIONE_FUTURA.getDayOfWeek().getValue(), LocalTime.of(9, 0), 1,
                LocalDate.of(2026, 1, 1), null));
        lezione = agendaRepository.save(new AgendaCorsi(programma, LEZIONE_FUTURA, LocalTime.of(9, 0), 1));
        secondaLezione = agendaRepository.save(new AgendaCorsi(programma, LEZIONE_FUTURA.plusWeeks(1),
                LocalTime.of(9, 0), 1));
    }

    @Test
    void unClienteNonPuòAvereDueRecordPerLaStessaOccorrenza() throws Exception {
        MockHttpSession sessione = autentica(cliente.getUsername());

        mockMvc.perform(post("/api/prenotazioni")
                        .session(sessione)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessione))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta(lezione.getId(), null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stato").value("CONFIRMED"));

        mockMvc.perform(post("/api/prenotazioni")
                        .session(sessione)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessione))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta(lezione.getId(), null)))
                .andExpect(status().isBadRequest());

        assertThat(prenotazioniRepository.count()).isEqualTo(1);
    }

    @Test
    void unaPrenotazioneCancellataNonOccupaPostiPerUnAltroCliente() throws Exception {
        MockHttpSession sessioneCliente = autentica(cliente.getUsername());
        Long prenotazioneId = prenota(sessioneCliente, lezione.getId(), null);

        mockMvc.perform(delete("/api/prenotazioni/" + prenotazioneId)
                        .session(sessioneCliente)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessioneCliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("CANCELLED"));

        MockHttpSession sessioneAltroCliente = autentica(altroCliente.getUsername());
        mockMvc.perform(post("/api/prenotazioni")
                        .session(sessioneAltroCliente)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessioneAltroCliente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta(lezione.getId(), null)))
                .andExpect(status().isCreated());

        assertThat(prenotazioniRepository.countByOccurrenceIdAndStatus(lezione.getId(),
                PrenotazioniStatus.CONFIRMED)).isEqualTo(1);
        assertThat(prenotazioniRepository.countByOccurrenceIdAndStatus(lezione.getId(),
                PrenotazioniStatus.CANCELLED)).isEqualTo(1);
    }

    @Test
    void ilClientePuòPrenotarePerSeMaNonPerUnAltroCliente() throws Exception {
        MockHttpSession sessione = autentica(cliente.getUsername());

        mockMvc.perform(post("/api/prenotazioni")
                        .session(sessione)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessione))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta(lezione.getId(), altroCliente.getId())))
                .andExpect(status().isForbidden());

        assertThat(prenotazioniRepository.count()).isZero();
    }

    @Test
    void ilClienteNonPuòAnnullareLaPrenotazioneDiUnAltro() throws Exception {
        MockHttpSession sessioneAdmin = autentica(admin.getUsername());
        Long prenotazioneId = prenota(sessioneAdmin, lezione.getId(), cliente.getId());
        MockHttpSession sessioneAltroCliente = autentica(altroCliente.getUsername());

        mockMvc.perform(delete("/api/prenotazioni/" + prenotazioneId)
                        .session(sessioneAltroCliente)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessioneAltroCliente)))
                .andExpect(status().isForbidden());

        assertThat(prenotazioniRepository.findById(prenotazioneId).orElseThrow().getStatus())
                .isEqualTo(PrenotazioniStatus.CONFIRMED);
    }

    @Test
    void adminESegreteriaOperanoPerIlClienteEIlRegistranteVieneDallaSessione() throws Exception {
        MockHttpSession sessioneSegretaria = autentica(segretaria.getUsername());
        Long prenotazioneSegreteriaId = prenota(sessioneSegretaria, lezione.getId(), cliente.getId());
        Prenotazioni prenotazioneSegreteria = prenotazioniRepository.findById(prenotazioneSegreteriaId)
                .orElseThrow();
        assertThat(prenotazioneSegreteria.getCreatedBy().getId()).isEqualTo(segretaria.getId());
        assertThat(prenotazioneSegreteria.getChannel()).isEqualTo(PrenotazioniChannel.SECRETARY);

        MockHttpSession sessioneAdmin = autentica(admin.getUsername());
        Long prenotazioneAdminId = prenota(sessioneAdmin, secondaLezione.getId(), altroCliente.getId());
        Prenotazioni prenotazioneAdmin = prenotazioniRepository.findById(prenotazioneAdminId).orElseThrow();
        assertThat(prenotazioneAdmin.getCreatedBy().getId()).isEqualTo(admin.getId());
        assertThat(prenotazioneAdmin.getChannel()).isEqualTo(PrenotazioniChannel.SECRETARY);

        mockMvc.perform(delete("/api/prenotazioni/" + prenotazioneSegreteriaId)
                        .session(sessioneAdmin)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessioneAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("CANCELLED"));

        MockHttpSession sessioneSegretariaNuova = autentica(segretaria.getUsername());
        mockMvc.perform(delete("/api/prenotazioni/" + prenotazioneAdminId)
                        .session(sessioneSegretariaNuova)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessioneSegretariaNuova)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("CANCELLED"));
    }

    @Test
    void lAnnullamentoAggiornaIlRecordEsistenteSenzaCreareUnoStorico() throws Exception {
        MockHttpSession sessioneSegretaria = autentica(segretaria.getUsername());
        Long idOriginario = prenota(sessioneSegretaria, lezione.getId(), cliente.getId());

        mockMvc.perform(delete("/api/prenotazioni/" + idOriginario)
                        .session(sessioneSegretaria)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessioneSegretaria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idOriginario))
                .andExpect(jsonPath("$.stato").value("CANCELLED"));

        assertThat(prenotazioniRepository.count()).isEqualTo(1);
        Prenotazioni cancellata = prenotazioniRepository.findById(idOriginario).orElseThrow();
        assertThat(cancellata.getId()).isEqualTo(idOriginario);
        assertThat(cancellata.getStatus()).isEqualTo(PrenotazioniStatus.CANCELLED);
        assertThat(cancellata.getCancelledAt()).isNotNull();
    }

    @Test
    void laRiattivazioneRiusaIlRecordAnnullato() throws Exception {
        MockHttpSession sessioneSegretaria = autentica(segretaria.getUsername());
        Long idOriginario = prenota(sessioneSegretaria, lezione.getId(), cliente.getId());
        mockMvc.perform(delete("/api/prenotazioni/" + idOriginario)
                        .session(sessioneSegretaria)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessioneSegretaria)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/prenotazioni")
                        .session(sessioneSegretaria)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessioneSegretaria))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta(lezione.getId(), cliente.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(idOriginario))
                .andExpect(jsonPath("$.stato").value("CONFIRMED"));

        assertThat(prenotazioniRepository.count()).isEqualTo(1);
        assertThat(prenotazioniRepository.findById(idOriginario)).isPresent();
    }

    @Test
    void ilRecordCancellatoRappresentaLoStatoCorrenteEReattivatoNonMantieneLoStatoPassato() throws Exception {
        MockHttpSession sessioneSegretaria = autentica(segretaria.getUsername());
        Long idOriginario = prenota(sessioneSegretaria, lezione.getId(), cliente.getId());
        var dataCreazione = prenotazioniRepository.findById(idOriginario).orElseThrow().getCreatedAt();

        mockMvc.perform(delete("/api/prenotazioni/" + idOriginario)
                        .session(sessioneSegretaria)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessioneSegretaria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("CANCELLED"));

        assertThat(prenotazioniRepository.findById(idOriginario).orElseThrow().getStatus())
                .isEqualTo(PrenotazioniStatus.CANCELLED);

        mockMvc.perform(post("/api/prenotazioni")
                        .session(sessioneSegretaria)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessioneSegretaria))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta(lezione.getId(), cliente.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(idOriginario))
                .andExpect(jsonPath("$.stato").value("CONFIRMED"));

        Prenotazioni riattivata = prenotazioniRepository.findById(idOriginario).orElseThrow();
        assertThat(prenotazioniRepository.count()).isEqualTo(1);
        assertThat(riattivata.getCreatedAt()).isEqualTo(dataCreazione);
        assertThat(riattivata.getCancelledAt()).isNull();
        assertThat(riattivata.getStatus()).isEqualTo(PrenotazioniStatus.CONFIRMED);
    }

    @Test
    void adminEsegretariaNonPossonoPrenotarePerUtentiConRuoloDiversoDaCustomer() throws Exception {
        MockHttpSession sessioneSegretaria = autentica(segretaria.getUsername());
        mockMvc.perform(post("/api/prenotazioni")
                        .session(sessioneSegretaria)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessioneSegretaria))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta(lezione.getId(), altroRuolo.getId())))
                .andExpect(status().isForbidden());

        MockHttpSession sessioneAdmin = autentica(admin.getUsername());
        mockMvc.perform(post("/api/prenotazioni")
                        .session(sessioneAdmin)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessioneAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta(lezione.getId(), admin.getId())))
                .andExpect(status().isForbidden());

        assertThat(prenotazioniRepository.count()).isZero();
    }

    @Test
    void ignoraIlRegistranteIndicatoNelBodyEUsaLaSessioneAutenticata() throws Exception {
        MockHttpSession sessione = autentica(segretaria.getUsername());

        mockMvc.perform(post("/api/prenotazioni")
                        .session(sessione)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessione))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"occorrenzaId":%d,"clienteId":%d,"createdById":%d}
                                """.formatted(lezione.getId(), cliente.getId(), admin.getId())))
                .andExpect(status().isCreated());

        Prenotazioni prenotazione = prenotazioniRepository.findAll().getFirst();
        assertThat(prenotazione.getCreatedBy().getId()).isEqualTo(segretaria.getId());
    }

    private Utenti creaUtente(String username, UserRole role) {
        return utentiRepository.save(new Utenti(username, passwordEncoder.encode(PASSWORD), username, "Test",
                username + "@example.test", null, role));
    }

    private Long prenota(MockHttpSession sessione, Long occorrenzaId, Long clienteId) throws Exception {
        MvcResult risultato = mockMvc.perform(post("/api/prenotazioni")
                        .session(sessione)
                        .header("X-CSRF-TOKEN", tokenCsrf(sessione))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(richiesta(occorrenzaId, clienteId)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(risultato.getResponse().getContentAsString()).get("id").asLong();
    }

    private static String richiesta(Long occorrenzaId, Long clienteId) {
        return clienteId == null
                ? "{\"occorrenzaId\":" + occorrenzaId + "}"
                : "{\"occorrenzaId\":" + occorrenzaId + ",\"clienteId\":" + clienteId + "}";
    }

    private MockHttpSession autentica(String username) throws Exception {
        Sessione anonima = ottieniCsrf(null);
        MvcResult risultato = mockMvc.perform(post("/api/auth/login")
                        .session(anonima.sessione())
                        .header("X-CSRF-TOKEN", anonima.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "username", username, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) risultato.getRequest().getSession(false);
    }

    private String tokenCsrf(HttpSession sessione) throws Exception {
        return ottieniCsrf((MockHttpSession) sessione).token();
    }

    private Sessione ottieniCsrf(MockHttpSession sessione) throws Exception {
        var richiesta = get("/api/auth/csrf");
        if (sessione != null) {
            richiesta.session(sessione);
        }
        MvcResult risultato = mockMvc.perform(richiesta)
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(risultato.getResponse().getContentAsString())
                .get("token").asString();
        return new Sessione((MockHttpSession) risultato.getRequest().getSession(false), token);
    }

    private record Sessione(MockHttpSession sessione, String token) {
    }
}
