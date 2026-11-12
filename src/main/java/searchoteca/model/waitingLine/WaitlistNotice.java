package searchoteca.model.waitingLine;

import java.time.LocalDateTime;

public record WaitlistNotice(
        String email,
        String studentName,
        String title,
        String copyCode,
        String localName,
        LocalDateTime expiresAt
) {
}
