package it.gym.Prenotazioni.web;

import it.gym.Prenotazioni.service.UtentiService;
import it.gym.Prenotazioni.web.dto.UtentiDto.LoginRequest;
import it.gym.Prenotazioni.web.dto.UtentiDto.TokenCsrf;
import it.gym.Prenotazioni.web.dto.UtentiDto.UtenteRisposta;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
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
    public TokenCsrf csrf(@RequestAttribute("_csrf") CsrfToken token) {
        return new TokenCsrf(token.getToken());
    }

    @PostMapping("/login")
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
    public void logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        logoutHandler.logout(request, response, authentication);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
}
