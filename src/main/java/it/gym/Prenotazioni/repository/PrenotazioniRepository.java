package it.gym.Prenotazioni.repository;

import it.gym.Prenotazioni.entity.Prenotazioni;
import it.gym.Prenotazioni.enums.PrenotazioniStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrenotazioniRepository extends JpaRepository<Prenotazioni, Long> {

    long countByOccurrenceIdAndStatus(Long occurrenceId, PrenotazioniStatus status);
}