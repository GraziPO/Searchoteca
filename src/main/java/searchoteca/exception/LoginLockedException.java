package searchoteca.exception;

/** Login recusado porque o usuário está temporariamente bloqueado (HTTP 429). */
public class LoginLockedException extends RuntimeException {

    private final long retryAfterSeconds;

    public LoginLockedException(long retryAfterSeconds) {
        super(message(retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    private static String message(long seconds) {
        long minutes = Math.max(1, (seconds + 59) / 60);   // arredonda para cima: 61 s -> 2 min
        return "Muitas tentativas de login sem sucesso. Tente novamente em "
                + minutes + (minutes == 1 ? " minuto." : " minutos.");
    }
}