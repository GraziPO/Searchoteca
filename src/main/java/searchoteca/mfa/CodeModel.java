package searchoteca.mfa;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@Table(name = "sys_mfa")
public class CodeModel {
    @Id
    private Long id;

    private Long userId;
    private String codeHash;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private int attempts;
    private LocalDateTime usedAt;

    public CodeModel() {}
}
