package it.gym.Prenotazioni.exception;

public class CorsoGiaEsistenteException extends MiaPalestraException {

    public CorsoGiaEsistenteException(String nome) {
        super("Esiste già un corso con nome \"" + nome + "\".");
    }
}
