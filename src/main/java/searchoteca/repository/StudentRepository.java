package searchoteca.repository;

import org.springframework.data.repository.CrudRepository;
import searchoteca.model.StudentModel;

public interface StudentRepository extends CrudRepository<StudentModel, Long> {
    StudentModel findByStudentId(Long student_id);
    StudentModel findByRa(String ra);
}
