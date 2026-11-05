package searchoteca.DTO;

public record ResetPasswordRequest(String login, String code, String newPassword) {
}
