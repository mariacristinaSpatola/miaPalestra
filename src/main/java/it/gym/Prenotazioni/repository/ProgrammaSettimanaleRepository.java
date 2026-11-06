package it.gym.Prenotazioni.repository;

import it.gym.Prenotazioni.entity.ProgrammaSettimanale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProgrammaSettimanaleRepository extends JpaRepository<ProgrammaSettimanale, Long> {

    @Override
    @EntityGraph(attributePaths = "corso")
    List<ProgrammaSettimanale> findAll();

    @EntityGraph(attributePaths = "corso")
    List<ProgrammaSettimanale> findByCorsoIdOrderByValidFromDesc(Long corsoId);

    @EntityGraph(attributePaths = "corso")
    Optional<ProgrammaSettimanale> findWithCorsoById(Long id);
}