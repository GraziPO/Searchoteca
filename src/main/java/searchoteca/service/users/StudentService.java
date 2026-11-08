package searchoteca.service.users;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.DTO.users.student.StudentRequest;
import searchoteca.model.StudentModel;
import searchoteca.model.UserModel;


import searchoteca.repository.users.StudentRepository;
import searchoteca.repository.users.UserRepository;
import searchoteca.resetPassword.PasswordPolicy;

import java.time.LocalDateTime;

@Service
public class StudentService {
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public StudentService(StudentRepository studentRepository, PasswordEncoder passwordEncoder, UserRepository userRepository, PasswordPolicy passwordPolicy) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.passwordPolicy = passwordPolicy;
    }

    @Transactional
    public StudentModel create(StudentRequest request) {
        UserModel basicUser = new UserModel();
        basicUser.setUsername(request.username());
        basicUser.setCompleteName(request.completeName());
        basicUser.setEmail(request.email());

        passwordPolicy.validatePassword(request.password());
        basicUser.setPassword(passwordEncoder.encode(request.password()));
        
        basicUser.setRole_code("students");
        basicUser.setStatus(true);
        basicUser.setMust_change_password(true);

        basicUser = userRepository.save(basicUser);

        StudentModel student = new StudentModel();
        student.setUserId(basicUser.getUserId());
        student.setRa(request.ra());
        student.setPhone(request.phone());
        student.setLastActivity(LocalDateTime.now());

        return studentRepository.save(student);
    }
}
