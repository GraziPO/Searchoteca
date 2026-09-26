package searchoteca.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table(name="sys_permissions")
public class PermissionModel {
    @Id
    @Column("permission_id")
    private Long permissionId;

    private String permissionCode;
    private String resource;
    private String action;
    private String description;

    public PermissionModel(){}

}
