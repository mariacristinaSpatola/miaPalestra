package it.gym.Prenotazioni.web;

import it.gym.Prenotazioni.service.UtentiService;
import it.gym.Prenotazioni.web.dto.UtentiDto.LoginRequest;
import it.gym.Prenotazioni.web.dto.UtentiDto.RegistrazioneCliente;
import it.gym.Prenotazioni.web.dto.UtentiDto.TokenCsrf;
import it.gym.Prenotazioni.web.dto.UtentiDto.UtenteRisposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticazione", description = "Endpoint per la gestione dell'autenticazione e delle sessioni")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final UtentiService utentiService;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public AuthController(AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository, UtentiService utentiService) {
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.utentiService = utentiService;
    }

    @GetMapping("/csrf")
    @Operation(summary = "Recupera il token CSRF", description = "Restituisce il token CSRF necessario per proteggere le richieste POST, PUT, DELETE e PATCH. Il token deve essere incluso nell'header X-CSRF-TOKEN per tutte le richieste modificative.")
    @ApiResponse(responseCode = "200", description = "Token CSRF recuperato con successo", content = @Content(schema = @Schema(implementation = TokenCsrf.class)))
    public TokenCsrf csrf(@RequestAttribute("_csrf") CsrfToken token) {
        return new TokenCsrf(token.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un cliente", description = "Crea un account CUSTOMER. Il ruolo non è scelto dal client.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account creato"),
            @ApiResponse(responseCode = "400", description = "Dati non validi"),
            @ApiResponse(responseCode = "409", description = "Username o email già utilizzati")
    })
    public UtenteRisposta registra(@RequestBody RegistrazioneCliente richiesta) {
        return UtenteRisposta.da(utentiService.registraCliente(richiesta.username(), richiesta.password(),
                richiesta.firstName(), richiesta.lastName(), richiesta.email(), richiesta.phone()));
    }

    @PostMapping("/login")
    @Operation(summary = "Esegue il login", description = "Autentica un utente con username e password. Se le credenziali sono valide, crea una sessione autenticata e restituisce i dati dell'utente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login effettuato con successo", content = @Content(schema = @Schema(implementation = UtenteRisposta.class))),
            @ApiResponse(responseCode = "400", description = "Username o password non forniti"),
            @ApiResponse(responseCode = "401", description = "Credenziali non valide")
    })
    public UtenteRisposta login(@RequestBody LoginRequest request, HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        if (request == null || request.username() == null || request.password() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username e password obbligatori.");
        }
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        sessionAuthenticationStrategy.onAuthentication(authentication, servletRequest, servletResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return UtenteRisposta.da(utentiService.trovaPerUsername(authentication.getName()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Esegue il logout", description = "Termina la sessione autenticata dell'utente corrente e invalida il contesto di sicurezza.")
    @ApiResponse(responseCode = "204", description = "Logout eseguito con successo")
    public void logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        logoutHandler.logout(request, response, authentication);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
}
