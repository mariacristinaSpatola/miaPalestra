package it.gym.Prenotazioni.repository;

import it.gym.Prenotazioni.entity.AgendaCorsi;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AgendaCorsiRepository extends JpaRepository<AgendaCorsi, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select agenda from AgendaCorsi agenda where agenda.id = :id")
    Optional<AgendaCorsi> findByIdForUpdate(@Param("id") Long id);
}