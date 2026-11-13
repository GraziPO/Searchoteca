package searchoteca.DTO.users.account;

import java.time.LocalDateTime;

/** Um pedido de desativação ou reativação, como o aluno e o root veem. */
public record AccountRequestView(
        Long requestId,
        Long userId,
        String username,
        String completeName,
        String ra,
        String type,           // DEACTIVATE | REACTIVATE
        String status,         // PENDING | APPROVED | REJECTED | CANCELLED
        String reason,
        LocalDateTime createdAt,
        LocalDateTime decidedAt,
        String decidedBy,
        String decisionNote
) {
}
