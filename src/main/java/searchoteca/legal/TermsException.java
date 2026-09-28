package searchoteca.legal;

import org.springframework.http.HttpStatus;

public class TermsException extends RuntimeException {
    private final HttpStatus status;

    private TermsException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** termsToken ausente, expirado ou inválido. */
    public static TermsException invalid(String message) {
        return new TermsException(HttpStatus.UNAUTHORIZED, message);
    }

    /** O usuário aceitou uma versão que não é mais a atual. */
    public static TermsException outdated(String message) {
        return new TermsException(HttpStatus.CONFLICT, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
