package searchoteca.auditTrail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@Table(name= "audittrail_logs")
public class AuditLogModel {
    @Id
    private Long id;

    private LocalDateTime time_stamp;
    private String username;
    private String action;
    private String details;

    public AuditLogModel() {}
}
