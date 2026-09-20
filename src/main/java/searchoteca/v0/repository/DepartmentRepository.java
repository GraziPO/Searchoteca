package searchoteca.v0.repository;

import org.springframework.data.repository.CrudRepository;
<<<<<<< Updated upstream:src/main/java/searchoteca/v0/repository/DepartmentRepository.java
import searchoteca.v0.model.DepartmentModel;
=======
import org.springframework.stereotype.Repository;
import searchoteca.model.DepartmentModel;
>>>>>>> Stashed changes:src/main/java/searchoteca/repository/DepartmentRepository.java

@Repository
public interface DepartmentRepository extends CrudRepository<DepartmentModel,String> {

    DepartmentModel findByDepartCode(String departCode);

}
