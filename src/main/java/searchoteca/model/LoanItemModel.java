package searchoteca.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

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
