package searchoteca.DTO;

public record PasswordChangeChallengeResponse(
        boolean passwordChallengeRequired,
        String passwordChangeToken,
        long expiresInSeconds
) {
}
