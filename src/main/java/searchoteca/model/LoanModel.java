package searchoteca.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@Table(name= "org_loans")
public class LoanModel {
    @Id
    @Column("loanID")
    private Long loanId;
    private LocalDateTime loan_Date;
    private LocalDateTime loan_dueAt;
    private String operator;
    private String user;
    private String items;
    private String type_exit;
    private String status;


    public LoanModel() {}
}
