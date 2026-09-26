package searchoteca.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchoteca.model.AuthorizationModel;

import java.util.List;

@Repository
public interface AuthorizationRepository extends CrudRepository<AuthorizationModel,String> {
    AuthorizationModel findByRoleCode(String authcode)
            ;
    @Query("""
            SELECT p.permission_code
            FROM org_users u
            JOIN sys_role_permissions rp ON rp.role_code = u.role_code
            JOIN sys_permissions p ON p.permission_code = rp.permission_code
            WHERE u.username = :username
            """)

    List<String> findPermissionByUsername(@Param("username") String username);

}
