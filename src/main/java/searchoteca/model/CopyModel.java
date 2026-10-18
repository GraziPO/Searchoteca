package searchoteca.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table(name ="org_copies")
public class CopyModel {
    @Id
    @Column("copyId")
    private Long copyId;

    private String isbn;

    @Column("depart_code")
    private String departCode;

    @Column("local_code")
    private String localCode;

    @Column("copy_index")
    private int copy_index;

    // Coluna calculada pelo banco (GENERATED ALWAYS): só leitura
    @ReadOnlyProperty
    @Column("copy_custom_code")
    private String copy_custom_code;

    private String status;

    public CopyModel(){}
}
