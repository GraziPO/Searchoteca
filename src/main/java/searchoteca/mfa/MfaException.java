package searchoteca.mfa;

import org.springframework.http.HttpStatus;

public class MfaException extends RuntimeException {
    private final HttpStatus status;

    private MfaException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** Código errado, expirado ou mfaToken inválido. */
    public static MfaException invalid(String message) {
        return new MfaException(HttpStatus.UNAUTHORIZED, message);
    }

    /** Pedidos demais (reenvio antes do prazo, limite por hora). */
    public static MfaException tooManyRequests(String message) {
        return new MfaException(HttpStatus.TOO_MANY_REQUESTS, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
