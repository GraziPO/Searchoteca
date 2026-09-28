package searchoteca.DTO;

public record MfaChallengeResponse(boolean mfaRequired, String mfaToken, String email, long expiresInSeconds) {

}
