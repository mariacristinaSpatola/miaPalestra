package it.gym.Prenotazioni.exception;

public class PasswordCorrenteNonValidaException extends MiaPalestraException {

    public PasswordCorrenteNonValidaException() {
        super("La password attuale non è corretta.");
    }
}
