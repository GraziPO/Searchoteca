package searchoteca.exception;

public class LoginLockedException extends RuntimeException {
    private final long retryAfterSeconds;

    public LoginLockedException(long retryAfterSeconds) {
        super(message(retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
    }

    private static String message(long seconds) {
        long minutes = Math.max(1, (seconds + 59) / 60);   // arredonda para cima: 61 s -> 2 min
        return "Muitas tentativas de login sem sucesso. Tente novamente em "
                + minutes + (minutes == 1 ? " minuto." : " minutos.");
    }
}