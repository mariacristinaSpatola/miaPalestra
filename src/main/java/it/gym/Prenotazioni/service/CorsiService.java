package it.gym.Prenotazioni.service;

import it.gym.Prenotazioni.entity.Corsi;
import it.gym.Prenotazioni.exception.CorsoGiaEsistenteException;
import it.gym.Prenotazioni.exception.CorsoNonTrovatoException;
import it.gym.Prenotazioni.exception.RegolaDominioNonValidaException;
import it.gym.Prenotazioni.repository.CorsiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CorsiService {

    private final CorsiRepository corsiRepository;

    public CorsiService(CorsiRepository corsiRepository) {
        this.corsiRepository = corsiRepository;
    }

    @Transactional
    public Corsi creaCorso(String nome, String descrizione) {
        validaNome(nome);
        String nomeNormalizzato = Corsi.chiaveNome(nome);
        if (corsiRepository.existsByNameNormalized(nomeNormalizzato)) {
            throw new CorsoGiaEsistenteException(nome.trim());
        }
        return corsiRepository.save(new Corsi(nome.trim(), descrizione));
    }

    @Transactional(readOnly = true)
    public List<Corsi> trovaTutti() {
        return corsiRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Corsi> trovaAttivi() {
        return corsiRepository.findAll().stream().filter(Corsi::isActive).toList();
    }

    @Transactional(readOnly = true)
    public Corsi trovaPerId(Long id) {
        return corsiRepository.findById(id).orElseThrow(CorsoNonTrovatoException::new);
    }

    @Transactional
    public Corsi aggiornaCorso(Long id, String nome, String descrizione) {
        Corsi corso = trovaPerId(id);
        validaNome(nome);
        String nomeNormalizzato = Corsi.chiaveNome(nome);
        if (corsiRepository.existsByNameNormalizedAndIdNot(nomeNormalizzato, id)) {
            throw new CorsoGiaEsistenteException(nome.trim());
        }
        corso.aggiornaDati(nome.trim(), descrizione);
        return corso;
    }

    @Transactional
    public void disattivaCorso(Long id) {
        trovaPerId(id).impostaAttivo(false);
    }

    private void validaNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegolaDominioNonValidaException("Il nome del corso è obbligatorio.");
        }
    }
}
