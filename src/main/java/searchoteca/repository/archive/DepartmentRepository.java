package searchoteca.repository.archive;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.CrudRepository;
import searchoteca.model.DepartmentModel;

public interface DepartmentRepository extends CrudRepository<DepartmentModel,Long>, PagingAndSortingRepository<DepartmentModel,Long> {

    DepartmentModel findByDepartCode(String departCode);

}
