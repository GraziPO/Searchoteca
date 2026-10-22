package searchoteca.repository;

import org.springframework.data.repository.CrudRepository;
import searchoteca.model.DepartmentModel;

public interface DepartmentRepository extends CrudRepository<DepartmentModel,Long> {

    DepartmentModel findByDepartCode(String departCode);

}
