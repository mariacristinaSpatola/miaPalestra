package it.gym.Prenotazioni.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableConfigurationProperties(BootstrapAdminProperties.class)
public class ApplicationConfiguration {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Europe/Rome"));
    }
}