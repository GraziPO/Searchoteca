package searchoteca.service;

import org.springframework.stereotype.Service;
import searchoteca.model.StudentModel;
import searchoteca.repository.StudentRepository;

import java.time.LocalDate;

@Service
public class StudentService {
    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public StudentModel create(StudentModel student){
        return studentRepository.save(student);
    }

    public StudentModel lastActivity(String ra){
        LocalDate now = LocalDate.now();
        StudentModel student = studentRepository.findByRa(ra);
        student.setLastActivity(now);
        return studentRepository.save(student);
    }
}
