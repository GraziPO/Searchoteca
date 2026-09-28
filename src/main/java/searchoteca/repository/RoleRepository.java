package searchoteca.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchoteca.model.RoleModel;

@Repository
public interface RoleRepository extends CrudRepository<RoleModel, Long> {
    RoleModel findByRoleCode(String roleCode);
}
