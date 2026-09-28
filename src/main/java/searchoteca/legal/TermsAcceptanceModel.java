package searchoteca.legal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@Table(name = "sys_terms_acceptance")
public class TermsAcceptanceModel {
    @Id
    private Long id;

    private Long userId;
    private String termsVersion;
    private String privacyVersion;
    private LocalDateTime acceptedAt;

    public TermsAcceptanceModel() {}
}
