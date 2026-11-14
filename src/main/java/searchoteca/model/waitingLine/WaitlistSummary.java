package searchoteca.model.waitingLine;

import java.time.LocalDateTime;

public record WaitlistSummary(
        String isbn,
        int queueSize,
        int limit,
        int availableCopies,
        int circulatingCopies,
        boolean joined,
        Integer position,        // null se não está na fila
        String status,           // WAITING / NOTIFIED, ou null
        LocalDateTime expiresAt, // prazo para buscar, quando NOTIFIED
        boolean canJoin          // o frontend usa para mostrar ou não o botão
) {
}
