package searchoteca.DTO.security;

public record MfaVerifyRequest(String mfaToken, String code) {
}
