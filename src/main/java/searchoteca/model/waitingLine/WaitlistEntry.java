package searchoteca.model.waitingLine;

import java.time.LocalDateTime;

public record WaitlistEntry(
        Long waitlistId,
        String isbn,
        String title,
        String author,
        String status,
        int position,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        String copyCode, String localCode, //notified
        String studentName, String ra //só pro operador
) {
}
