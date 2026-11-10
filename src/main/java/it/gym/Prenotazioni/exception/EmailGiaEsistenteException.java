package it.gym.Prenotazioni.exception;

public class EmailGiaEsistenteException extends MiaPalestraException {

    public EmailGiaEsistenteException() {
        super("Esiste già un account con questa email.");
    }
}