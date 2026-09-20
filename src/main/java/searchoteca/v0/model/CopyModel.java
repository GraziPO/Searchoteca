package searchoteca.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table(name="org_copies")
public class CopyModel {
    @Id
    @Column("copy_id")
    private Long copy_id;

    private String isbn;
    private String depart_code;
    private String local_code;
    private int copy_index;
    private String custom_id;
    private String status;

    public CopyModel() {}

}
