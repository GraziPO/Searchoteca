package searchoteca.DTO.security;

public record MfaChallengeResponse(boolean mfaRequired, String mfaToken, String email, long expiresInSeconds) {

}
