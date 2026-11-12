package it.gym.Prenotazioni.exception;

public class LezioneNonTrovataException extends MiaPalestraException {

    public LezioneNonTrovataException() {
        super("Lezione non trovata.");
    }
}