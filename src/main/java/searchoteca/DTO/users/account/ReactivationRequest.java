package searchoteca.DTO.users.account;

/** Pedido de reativação: a conta está desativada, então o aluno se identifica com usuário e senha. */
public record ReactivationRequest(String username, String password, String reason) {
}
