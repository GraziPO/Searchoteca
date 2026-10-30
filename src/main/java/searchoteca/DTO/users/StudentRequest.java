package searchoteca.DTO.users;

public record StudentRequest(
        String username,
        String completeName,
        String email,
        String password,
        String ra,
        String phone
) {
}
