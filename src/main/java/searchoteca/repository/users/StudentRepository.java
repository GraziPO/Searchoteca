package searchoteca.repository.users;

import org.springframework.data.repository.CrudRepository;
import searchoteca.model.StudentModel;

public interface StudentRepository extends CrudRepository<StudentModel, Long> {
    StudentModel findByStudentId(Long student_id);
    StudentModel findByUserId(Long userId);
    StudentModel findByRa(String ra);
}
