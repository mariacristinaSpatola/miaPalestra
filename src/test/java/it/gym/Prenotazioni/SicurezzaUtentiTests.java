package it.gym.Prenotazioni;

import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.enums.UserRole;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SicurezzaUtentiTests {

        private static final String PASSWORD = "password-di-test";

        @Autowired
        private WebApplicationContext applicationContext;

        private MockMvc mockMvc;

        @Autowired
        private UtentiRepository utentiRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private ObjectMapper objectMapper;

        @DynamicPropertySource
        static void databaseProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", () -> "jdbc:h2:mem:sicurezza;DB_CLOSE_DELAY=-1");
                registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
                registry.add("spring.datasource.username", () -> "sa");
                registry.add("spring.datasource.password", () -> "");
                registry.add("app.cors.allowed-origins", () -> "https://angular.example.test");
        }

        @BeforeEach
        void preparaUtenti() {
                mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                                .addFilters(applicationContext.getBean(FilterChainProxy.class))
                                .build();
                utentiRepository.deleteAll();
                creaUtente("admin", UserRole.ADMIN);
                creaUtente("segretaria", UserRole.SECRETARY);
                creaUtente("cliente", UserRole.CUSTOMER);
                creaUtente("altro-cliente", UserRole.CUSTOMER);
        }

        @Test
        void richiedeAutenticazionePerLeApiProtette() throws Exception {
                mockMvc.perform(get("/api/utenti/me"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void swaggerEAccessibileSenzaAutenticazione() throws Exception {
                mockMvc.perform(get("/v3/api-docs"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/swagger-ui/index.html"))
                                .andExpect(status().isOk());
        }

        @Test
        void soloAdminGestisceGliUtentiELeRisposteNonEspongonoLaPassword() throws Exception {
                Sessione admin = autentica("admin");
                MockHttpSession sessione = admin.sessione();

                mockMvc.perform(post("/api/utenti")
                                .session(sessione)
                                .header("X-CSRF-TOKEN", tokenCsrf(sessione))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"username":"nuovo-cliente","password":"password-nuova",
                                                 "firstName":"Nuovo","lastName":"Cliente","role":"CUSTOMER"}
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.password").doesNotExist())
                                .andExpect(jsonPath("$.role").value("CUSTOMER"));

                mockMvc.perform(post("/api/utenti")
                                .session(sessione)
                                .header("X-CSRF-TOKEN", tokenCsrf(sessione))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"username":"nuovo-cliente","password":"password-nuova",
                                                 "firstName":"Duplicato","lastName":"Cliente","role":"CUSTOMER"}
                                                """))
                                .andExpect(status().isConflict());

                mockMvc.perform(get("/api/utenti")
                                .session(sessione))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].password").doesNotExist());

                Long clienteId = utentiRepository.findByUsername("cliente").orElseThrow().getId();
                mockMvc.perform(put("/api/utenti/" + clienteId)
                                .session(sessione)
                                .header("X-CSRF-TOKEN", tokenCsrf(sessione))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"firstName":"Cliente","lastName":"Aggiornato",
                                                 "role":"SECRETARY","active":false}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.role").value("SECRETARY"))
                                .andExpect(jsonPath("$.active").value(false));
        }

        @Test
        void secretarySearchesOnlyCustomersAndCannotManageUsers() throws Exception {
                MockHttpSession sessione = autentica("segretaria").sessione();

                mockMvc.perform(get("/api/utenti/clienti")
                                .param("query", "cliente")
                                .session(sessione))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[*].role").value(org.hamcrest.Matchers.everyItem(
                                                org.hamcrest.Matchers.is("CUSTOMER"))));

                mockMvc.perform(get("/api/utenti").session(sessione))
                                .andExpect(status().isForbidden());

                mockMvc.perform(post("/api/utenti")
                                .session(sessione)
                                .header("X-CSRF-TOKEN", tokenCsrf(sessione))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"username":"non-autorizzato","password":"password",
                                                 "firstName":"No","lastName":"No","role":"CUSTOMER"}
                                                """))
                                .andExpect(status().isForbidden());
        }

        @Test
        void ricercaClientiSenzaTestoRestituisceTuttiIClienti() throws Exception {
                MockHttpSession sessione = autentica("segretaria").sessione();

                mockMvc.perform(get("/api/utenti/clienti").param("query", "").session(sessione))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2));

                mockMvc.perform(get("/api/utenti/clienti").param("query", "  ").session(sessione))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2));

                mockMvc.perform(get("/api/utenti/clienti").param("query", "c").session(sessione))
                                .andExpect(status().isBadRequest());

                mockMvc.perform(get("/api/utenti/clienti").param("query", "cl").session(sessione))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        void corsiEProgrammiRichiedonoIlRuoloAdmin() throws Exception {
                MockHttpSession sessioneAdmin = autentica("admin").sessione();
                MockHttpSession sessioneCliente = autentica("cliente").sessione();

                mockMvc.perform(get("/api/corsi").session(sessioneAdmin))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/corsi").session(sessioneCliente))
                                .andExpect(status().isForbidden());

                mockMvc.perform(post("/api/corsi")
                                .session(sessioneCliente)
                                .header("X-CSRF-TOKEN", tokenCsrf(sessioneCliente))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"nome":"Corso non autorizzato","descrizione":null}
                                                """))
                                .andExpect(status().isForbidden());

                mockMvc.perform(get("/api/programmi-settimanali").session(sessioneCliente))
                                .andExpect(status().isForbidden());

                mockMvc.perform(post("/api/programmi-settimanali")
                                .session(sessioneAdmin)
                                .header("X-CSRF-TOKEN", tokenCsrf(sessioneAdmin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"corsoId":1,"giorno":1,"ora":"09:00:00",
                                                 "capienzaPredefinita":10,"validoDal":"2026-11-02"}
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.giorno").value(1));

                mockMvc.perform(get("/api/programmi-settimanali").session(sessioneAdmin))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].nomeCorso").exists());
        }

        @Test
        void customerAccedeSoloAiPropriDatiEIlClientNonPuoCambiareIdentita() throws Exception {
                MockHttpSession sessione = autentica("cliente").sessione();
                mockMvc.perform(get("/api/utenti/me").session(sessione))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("cliente"))
                                .andExpect(jsonPath("$.role").value("CUSTOMER"));

                Long adminId = utentiRepository.findByUsername("admin").orElseThrow().getId();
                mockMvc.perform(get("/api/utenti/" + adminId).session(sessione))
                                .andExpect(status().isForbidden());

                mockMvc.perform(patch("/api/utenti/me")
                                .session(sessione)
                                .header("X-CSRF-TOKEN", tokenCsrf(sessione))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"firstName":"NomeNuovo","lastName":"Cliente",
                                                 "username":"admin","userId":1,"role":"ADMIN","active":true}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("cliente"))
                                .andExpect(jsonPath("$.firstName").value("NomeNuovo"))
                                .andExpect(jsonPath("$.role").value("CUSTOMER"));

                mockMvc.perform(get("/api/utenti/1").session(sessione))
                                .andExpect(status().isForbidden());
        }

        @Test
        void leMutazioniRichiedonoCsrfELeOriginiCorsSonoEsplicite() throws Exception {
                MockHttpSession sessione = autentica("admin").sessione();

                mockMvc.perform(post("/api/utenti")
                                .session(sessione)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isForbidden());

                mockMvc.perform(options("/api/utenti")
                                .header("Origin", "https://angular.example.test")
                                .header("Access-Control-Request-Method", "PATCH"))
                                .andExpect(status().isOk())
                                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                                .header()
                                                .string("Access-Control-Allow-Origin", "https://angular.example.test"))
                                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                                .header().string("Access-Control-Allow-Credentials", "true"));

                mockMvc.perform(options("/api/utenti")
                                .header("Origin", "https://attacker.example.test")
                                .header("Access-Control-Request-Method", "PATCH"))
                                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                                .header().doesNotExist("Access-Control-Allow-Origin"));
        }

        @Test
        void laModificaDelRuoloRevocaLaSessioneConIPrivilegiPrecedenti() throws Exception {
                MockHttpSession sessione = autentica("admin").sessione();
                Long adminId = utentiRepository.findByUsername("admin").orElseThrow().getId();

                mockMvc.perform(put("/api/utenti/" + adminId)
                                .session(sessione)
                                .header("X-CSRF-TOKEN", tokenCsrf(sessione))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"firstName":"admin","lastName":"Test","role":"SECRETARY"}
                                                """))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/utenti").session(sessione))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void disattivareUnUtenteRevocaLaSuaSessioneAttiva() throws Exception {
                MockHttpSession sessioneCliente = autentica("cliente").sessione();
                MockHttpSession sessioneAdmin = autentica("admin").sessione();
                Long clienteId = utentiRepository.findByUsername("cliente").orElseThrow().getId();

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/utenti/" + clienteId)
                                .session(sessioneAdmin)
                                .header("X-CSRF-TOKEN", tokenCsrf(sessioneAdmin)))
                                .andExpect(status().isNoContent());

                mockMvc.perform(get("/api/utenti/me").session(sessioneCliente))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void cambioPasswordVerificaLaPasswordCorrente() throws Exception {
                MockHttpSession sessione = autentica("cliente").sessione();

                mockMvc.perform(put("/api/utenti/me/password")
                                .session(sessione)
                                .header("X-CSRF-TOKEN", tokenCsrf(sessione))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"currentPassword":"errata","newPassword":"nuova-password-sicura"}
                                                """))
                                .andExpect(status().isBadRequest());

                mockMvc.perform(put("/api/utenti/me/password")
                                .session(sessione)
                                .header("X-CSRF-TOKEN", tokenCsrf(sessione))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"currentPassword":"password-di-test","newPassword":"nuova-password-sicura"}
                                                """))
                                .andExpect(status().isNoContent());
        }

        @Test
        void sessioneAutenticataPuoUscireEdUnaPasswordErrataNonAutentica() throws Exception {
                Sessione anonima = ottieniCsrf();
                mockMvc.perform(post("/api/utenti")
                                .session(anonima.sessione())
                                .header("X-CSRF-TOKEN", anonima.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"username":"registrazione-pubblica","password":"password",
                                                 "firstName":"No","lastName":"No","role":"CUSTOMER"}
                                                """))
                                .andExpect(status().isUnauthorized());

                mockMvc.perform(post("/api/auth/login")
                                .session(anonima.sessione())
                                .header("X-CSRF-TOKEN", anonima.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"username":"cliente","password":"errata"}
                                                """))
                                .andExpect(status().isUnauthorized());

                MockHttpSession sessione = autentica("cliente").sessione();
                mockMvc.perform(post("/api/auth/logout")
                                .session(sessione)
                                .header("X-CSRF-TOKEN", tokenCsrf(sessione)))
                                .andExpect(status().isNoContent());
                mockMvc.perform(get("/api/utenti/me").session(sessione))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void registrazionePubblicaCreaSoloClienteEConsenteAccesso() throws Exception {
                Sessione anonima = ottieniCsrf();
                mockMvc.perform(post("/api/auth/register")
                                .session(anonima.sessione())
                                .header("X-CSRF-TOKEN", anonima.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"username":"nuovo-cliente","password":"Forza123",
                                                 "firstName":"Nuovo","lastName":"Cliente",
                                                 "role":"ADMIN"}
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                                .andExpect(jsonPath("$.password").doesNotExist());

                org.junit.jupiter.api.Assertions.assertEquals(UserRole.CUSTOMER,
                                utentiRepository.findByUsername("nuovo-cliente").orElseThrow().getRole());

                mockMvc.perform(post("/api/auth/register")
                                .session(anonima.sessione())
                                .header("X-CSRF-TOKEN", anonima.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"username":"nuovo-cliente","password":"Forza123",
                                                 "firstName":"Nuovo","lastName":"Cliente"}
                                                """))
                                .andExpect(status().isConflict());

                Sessione csrfAggiornato = ottieniCsrf(anonima.sessione());
                mockMvc.perform(post("/api/auth/login")
                                .session(csrfAggiornato.sessione())
                                .header("X-CSRF-TOKEN", csrfAggiornato.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"username":"nuovo-cliente","password":"Forza123"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.role").value("CUSTOMER"));
        }

        @Test
        void registrazioneRifiutaPasswordDeboleEEmailGiaUsata() throws Exception {
                Sessione anonima = ottieniCsrf();
                mockMvc.perform(post("/api/auth/register")
                                .session(anonima.sessione())
                                .header("X-CSRF-TOKEN", anonima.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"username":"password-debole","password":"forza123",
                                                 "firstName":"Nome","lastName":"Cliente"}
                                                """))
                                .andExpect(status().isBadRequest());

                mockMvc.perform(post("/api/auth/register")
                                .session(anonima.sessione())
                                .header("X-CSRF-TOKEN", anonima.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"username":"email-duplicata","password":"Forza123",
                                                 "firstName":"Nome","lastName":"Cliente",
                                                 "email":"admin@example.test"}
                                                """))
                                .andExpect(status().isConflict());
        }

        private void creaUtente(String username, UserRole role) {
                utentiRepository.save(new Utenti(username, passwordEncoder.encode(PASSWORD), username, "Test",
                                username + "@example.test", null, role));
        }

        private Sessione autentica(String username) throws Exception {
                Sessione anonima = ottieniCsrf();
                MvcResult risultato = mockMvc.perform(post("/api/auth/login")
                                .session(anonima.sessione())
                                .header("X-CSRF-TOKEN", anonima.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                java.util.Map.of("username", username, "password", PASSWORD))))
                                .andExpect(status().isOk())
                                .andReturn();

                return new Sessione((MockHttpSession) risultato.getRequest().getSession(false), null);
        }

        private String tokenCsrf(HttpSession sessione) throws Exception {
                return ottieniCsrf(sessione).token();
        }

        private Sessione ottieniCsrf() throws Exception {
                return ottieniCsrf(null);
        }

        private Sessione ottieniCsrf(HttpSession sessione) throws Exception {
                var richiesta = get("/api/auth/csrf");
                if (sessione != null) {
                        richiesta.session((MockHttpSession) sessione);
                }
                MvcResult risultato = mockMvc.perform(richiesta)
                                .andExpect(status().isOk())
                                .andReturn();
                String token = objectMapper.readTree(risultato.getResponse().getContentAsString()).get("token")
                                .asString();
                return new Sessione((MockHttpSession) risultato.getRequest().getSession(false), token);
        }

        private record Sessione(MockHttpSession sessione, String token) {
        }
}
