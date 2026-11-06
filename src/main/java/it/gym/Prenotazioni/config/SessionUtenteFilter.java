package it.gym.Prenotazioni.config;

import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.repository.UtentiRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

class SessionUtenteFilter extends OncePerRequestFilter {

    private final UtentiRepository utentiRepository;

    SessionUtenteFilter(UtentiRepository utentiRepository) {
        this.utentiRepository = utentiRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && !sessioneValida(authentication)) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean sessioneValida(Authentication authentication) {
        return utentiRepository.findByUsername(authentication.getName())
                .filter(Utenti::isActive)
                .filter(utente -> authentication.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority()
                                .equals("ROLE_" + utente.getRole().name())))
                .isPresent();
    }
}
