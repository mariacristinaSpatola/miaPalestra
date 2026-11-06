package it.gym.Prenotazioni.repository;

import it.gym.Prenotazioni.entity.Utenti;
import it.gym.Prenotazioni.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UtentiRepository extends JpaRepository<Utenti, Long> {

    Optional<Utenti> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Utenti> findAllByOrderByLastNameAscFirstNameAsc();

    @Query("""
            select u from Utenti u
            where u.role = :role and (
                lower(u.username) like lower(concat('%', :query, '%'))
                or lower(u.firstName) like lower(concat('%', :query, '%'))
                or lower(u.lastName) like lower(concat('%', :query, '%'))
                or lower(coalesce(u.email, '')) like lower(concat('%', :query, '%'))
            )
            order by u.lastName, u.firstName
            """)
    List<Utenti> cercaPerRuoloENome(@Param("role") UserRole role, @Param("query") String query);
}