package it.gym.Prenotazioni.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.bootstrap-admin")
public record BootstrapAdminProperties(
        String username,
        String password,
        String firstName,
        String lastName,
        String email) {
}