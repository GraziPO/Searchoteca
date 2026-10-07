package searchoteca.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@Table(name="org_loan_items")
public class LoanItemModel {

    @Id
    @Column("loanItem_id")
    private String loanItemId;
    private String copy_id;
    private LocalDateTime returned_at;
    private LocalDateTime returned_to;

    public LoanItemModel() {}
}
