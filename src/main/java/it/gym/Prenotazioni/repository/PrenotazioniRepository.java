package it.gym.Prenotazioni.repository;

import it.gym.Prenotazioni.entity.Prenotazioni;
import it.gym.Prenotazioni.enums.PrenotazioniStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

public interface PrenotazioniRepository extends JpaRepository<Prenotazioni, Long> {

    @EntityGraph(attributePaths = { "customer", "occurrence", "occurrence.programmaSettimanale",
            "occurrence.programmaSettimanale.corso", "createdBy" })
    List<Prenotazioni> findByCustomerIdOrderByCreatedAtDesc(Long clienteId);

    long countByOccurrenceIdAndStatus(Long occurrenceId, PrenotazioniStatus status);

    Optional<Prenotazioni> findByCustomerIdAndOccurrenceId(Long customerId, Long occurrenceId);
}