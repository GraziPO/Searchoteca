package searchoteca.DTO;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}
