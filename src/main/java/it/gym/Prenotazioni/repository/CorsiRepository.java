package it.gym.Prenotazioni.repository;

import it.gym.Prenotazioni.entity.Corsi;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CorsiRepository extends JpaRepository<Corsi, Long> {

    boolean existsByNameNormalized(String nameNormalized);

    boolean existsByNameNormalizedAndIdNot(String nameNormalized, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select corso from Corsi corso where corso.id = :id")
    Optional<Corsi> findByIdForUpdate(@Param("id") Long id);
}