package searchoteca.DTO;

/** As versões enviadas devem ser as que o usuário leu na tela. */
public record TermsAcceptRequest(String termsToken, String termsVersion, String privacyVersion) {
}
