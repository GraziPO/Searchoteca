package searchoteca.model;

import org.springframework.data.annotation.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

@Getter
@Setter
@Table(name ="org_local")
public class LocationModel {
    @Id
    @Column("local_id")
    private Long id;

    private String localCode;
    private String localName;
    private String localDesc;

    @Column("depart_code")
    private String departCode;

    public LocationModel() {}
}
