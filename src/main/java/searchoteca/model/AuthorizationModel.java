package searchoteca.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table(name="sys_role_permissions")
public class AuthorizationModel {
    @Id
    @Column("role_code")
    private String roleCode;

    @Column("permission_code")
    private String PermissionCode;

    public AuthorizationModel(){}
}
