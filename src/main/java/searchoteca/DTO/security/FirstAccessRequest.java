package searchoteca.DTO.security;

public record FirstAccessRequest(String passwordChangeToken, String newPassword) {}
