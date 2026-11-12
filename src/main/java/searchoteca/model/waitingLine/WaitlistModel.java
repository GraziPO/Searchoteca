package searchoteca.model.waitingLine;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@Table(name = "org_waitlist")
public class WaitlistModel {
    @Id
    @Column("waitlist_id")
    private Long waitlistId;
    private String isbn;
    private Long studentId;
    private String status;          // WaitlistStatus
    private LocalDateTime createdAt;
    private Long copyId;            // exemplar reservado, quando NOTIFIED
    private LocalDateTime notifiedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime closedAt;

    public WaitlistModel() {}
}
