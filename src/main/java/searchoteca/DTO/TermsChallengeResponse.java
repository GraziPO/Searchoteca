package searchoteca.DTO;

/** Resposta do login quando o usuário ainda não aceitou a versão atual dos documentos. */
public record TermsChallengeResponse(boolean termsRequired,
                                     String termsToken,
                                     String termsVersion,
                                     String privacyVersion) {
}
