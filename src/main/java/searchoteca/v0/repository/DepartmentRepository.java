package searchoteca.v0.repository;

import org.springframework.data.repository.CrudRepository;
import searchoteca.v0.model.DepartmentModel;

public interface DepartmentRepository extends CrudRepository<DepartmentModel,String> {

    DepartmentModel findByDepartCode(String departCode);

}
