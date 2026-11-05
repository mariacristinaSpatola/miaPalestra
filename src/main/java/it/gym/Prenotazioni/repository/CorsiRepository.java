package it.gym.Prenotazioni.repository;

import it.gym.Prenotazioni.entity.Corsi;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorsiRepository extends JpaRepository<Corsi, Long> {
}