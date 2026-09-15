package searchoteca.v0.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(name= "org_depart")
public class DepartmentModel {
    @Id
    @Column("depart_id")
    private Long Id;

    private String departCode;
    private String departName;
    private String departDesc;

    public DepartmentModel() {}
}
