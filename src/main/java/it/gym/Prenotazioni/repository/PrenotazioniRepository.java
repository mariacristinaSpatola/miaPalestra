package it.gym.Prenotazioni.repository;

import it.gym.Prenotazioni.entity.Prenotazioni;
import it.gym.Prenotazioni.enums.PrenotazioniStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrenotazioniRepository extends JpaRepository<Prenotazioni, Long> {

    long countByOccurrenceIdAndStatus(Long occurrenceId, PrenotazioniStatus status);

    Optional<Prenotazioni> findByCustomerIdAndOccurrenceId(Long customerId, Long occurrenceId);
}