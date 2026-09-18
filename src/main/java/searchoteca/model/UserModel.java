package searchoteca.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

@Getter
@Setter
@Table(name = "org_users")
public class UserModel {
    @Id
    @Column("user_id")
    private Long id;
    private String custom_id;
    private String username;
    private String completeName;
    private String email;
    private String password;
    private String role;
    private String accessLevel;

    public UserModel() {}

}
