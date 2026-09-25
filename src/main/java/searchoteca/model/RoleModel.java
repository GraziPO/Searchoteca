package searchoteca.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table(name="sys_roles")
public class RoleModel {
    @Id
    @Column("role_id")
    private Long role_id;

    @Column("role_code")
    private String roleCode;

    private String role_name;
    private String role_desc;

    @Column("is_root")
    private boolean root;

    public RoleModel() {}

}
