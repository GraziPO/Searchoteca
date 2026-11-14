package searchoteca.DTO.users.account;

/** Corpo do pedido de desativação feito pelo aluno. O motivo é opcional. */
public record DeactivationRequest(String reason) {
}
