package it.gym.Prenotazioni.exception;

public class UtenteGiaEsistenteException extends MiaPalestraException {

    public UtenteGiaEsistenteException(String username) {
        super("Esiste già un utente con username " + username + ".");
    }
}
