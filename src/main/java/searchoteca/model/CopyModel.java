package searchoteca.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table(name ="org_copies")
public class CopyModel {
    @Id
    @Column("copy_id")
    private String id;

    private String isbn;

    @Column("depart_code")
    private String departCode;

    @Column("local_code")
    private String localCode;

    private int index;

    private String customCode;

    public CopyModel(){}
}
