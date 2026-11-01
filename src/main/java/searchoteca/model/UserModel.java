package searchoteca.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private Long userId;
    @Column("custom_id")
    private String customId;

    private String username;
    private String completeName;
    private String email;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)   // aceita no cadastro, nunca devolve o hash
    private String password;
    private String role_code;
    private Boolean status;

    public UserModel() {}

}
