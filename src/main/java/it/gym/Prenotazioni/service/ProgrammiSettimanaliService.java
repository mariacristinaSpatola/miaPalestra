package it.gym.Prenotazioni.service;

import it.gym.Prenotazioni.entity.Corsi;
import it.gym.Prenotazioni.entity.ProgrammaSettimanale;
import it.gym.Prenotazioni.exception.CorsoNonTrovatoException;
import it.gym.Prenotazioni.exception.ProgrammaSettimanaleNonTrovatoException;
import it.gym.Prenotazioni.exception.RegolaDominioNonValidaException;
import it.gym.Prenotazioni.repository.CorsiRepository;
import it.gym.Prenotazioni.repository.ProgrammaSettimanaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ProgrammiSettimanaliService {

    private final ProgrammaSettimanaleRepository programmaRepository;
    private final CorsiRepository corsiRepository;

    public ProgrammiSettimanaliService(ProgrammaSettimanaleRepository programmaRepository,
            CorsiRepository corsiRepository) {
        this.programmaRepository = programmaRepository;
        this.corsiRepository = corsiRepository;
    }

    @Transactional
    public ProgrammaSettimanale crea(Long corsoId, int giorno, LocalTime ora, int capienza,
            LocalDate validoDal, LocalDate validoAl) {
        return crea(corsoId, giorno, ora, capienza, validoDal, validoAl, null);
    }

    @Transactional
    public ProgrammaSettimanale crea(Long corsoId, int giorno, LocalTime ora, int capienza,
            LocalDate validoDal, LocalDate validoAl, Integer durataMinuti) {
        Corsi corso = trovaCorsoForUpdate(corsoId);
        validaCorsoAttivo(corso);
        ProgrammaSettimanale programma = new ProgrammaSettimanale(corso, giorno, ora, capienza, validoDal, validoAl,
                durataMinuti == null ? 60 : durataMinuti);
        validaSovrapposizioni(programma, null);
        return programmaRepository.save(programma);
    }

    @Transactional(readOnly = true)
    public List<ProgrammaSettimanale> trovaTutti(Long corsoId) {
        if (corsoId == null) {
            return programmaRepository.findAll();
        }
        trovaCorso(corsoId);
        return programmaRepository.findByCorsoIdOrderByValidFromDesc(corsoId);
    }

    @Transactional(readOnly = true)
    public ProgrammaSettimanale trovaPerId(Long id) {
        return programmaRepository.findWithCorsoById(id)
                .orElseThrow(ProgrammaSettimanaleNonTrovatoException::new);
    }

    @Transactional
    public ProgrammaSettimanale modifica(Long id, Long corsoId, int giorno, LocalTime ora, int capienza,
            LocalDate validoDal, LocalDate validoAl) {
        return modifica(id, corsoId, giorno, ora, capienza, validoDal, validoAl, null);
    }

    @Transactional
    public ProgrammaSettimanale modifica(Long id, Long corsoId, int giorno, LocalTime ora, int capienza,
            LocalDate validoDal, LocalDate validoAl, Integer durataMinuti) {
        Corsi corso = trovaCorsoForUpdate(corsoId);
        ProgrammaSettimanale precedente = trovaPerId(id);
        if (!precedente.isActive() || precedente.getValidTo() != null) {
            throw new RegolaDominioNonValidaException(
                    "Si può modificare solo un programma attivo e senza fine di validità.");
        }
        if (validoDal == null || !validoDal.isAfter(precedente.getValidFrom())) {
            throw new RegolaDominioNonValidaException(
                    "La nuova decorrenza deve essere successiva a quella del programma precedente.");
        }
        validaCorsoAttivo(corso);
        ProgrammaSettimanale nuovo = new ProgrammaSettimanale(corso, giorno, ora, capienza, validoDal, validoAl,
                durataMinuti == null ? precedente.getDurataMinuti() : durataMinuti);
        validaSovrapposizioni(nuovo, precedente.getId());

        precedente.chiudiValidita(validoDal.minusDays(1));
        programmaRepository.save(precedente);
        return programmaRepository.save(nuovo);
    }

    @Transactional
    public void disattiva(Long id) {
        trovaPerId(id).disattiva();
    }

    private Corsi trovaCorso(Long id) {
        if (id == null) {
            throw new RegolaDominioNonValidaException("Il corso del programma è obbligatorio.");
        }
        return corsiRepository.findById(id).orElseThrow(CorsoNonTrovatoException::new);
    }

    private Corsi trovaCorsoForUpdate(Long id) {
        if (id == null) {
            throw new RegolaDominioNonValidaException("Il corso del programma è obbligatorio.");
        }
        return corsiRepository.findByIdForUpdate(id).orElseThrow(CorsoNonTrovatoException::new);
    }

    private void validaCorsoAttivo(Corsi corso) {
        if (!corso.isActive()) {
            throw new RegolaDominioNonValidaException(
                    "Non è possibile pianificare un programma per un corso disattivato.");
        }
    }

    private void validaSovrapposizioni(ProgrammaSettimanale nuovo, Long idEscluso) {
        for (ProgrammaSettimanale esistente : programmaRepository.findByCorsoIdOrderByValidFromDesc(
                nuovo.getCorso().getId())) {
            if (esistente.isActive() && !esistente.getId().equals(idEscluso) && nuovo.siSovrapponeA(esistente)) {
                throw new RegolaDominioNonValidaException(
                        "Il programma si sovrappone a un'altra regola attiva dello stesso corso.");
            }
        }
    }
}
