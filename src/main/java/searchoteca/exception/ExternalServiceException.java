package searchoteca.exception;

/** Serviço externo (ex.: Open Library) fora do ar, lento ou com resposta inválida (HTTP 503). */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }
}
