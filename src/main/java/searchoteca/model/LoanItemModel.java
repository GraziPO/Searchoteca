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
    @Column("item_id")
    private Long itemId;
    private Long loanId;
    private Long copyId;
    private LocalDateTime returned_at;
    private Long returned_to;

    public LoanItemModel() {}
}
