package it.gym.Prenotazioni.repository;

import it.gym.Prenotazioni.entity.ProgrammaSettimanale;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgrammaSettimanaleRepository extends JpaRepository<ProgrammaSettimanale, Long> {
}