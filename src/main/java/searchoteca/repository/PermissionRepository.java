package searchoteca.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchoteca.model.PermissionModel;

@Repository
public interface PermissionRepository extends CrudRepository<PermissionModel, Long> {
    PermissionModel findByPermissionId(String name);
}
