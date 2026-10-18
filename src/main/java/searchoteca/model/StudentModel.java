package searchoteca.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Getter
@Setter
@Table(name="org_students")
public class StudentModel {
    @Id
    @Column("student_id")
    private Long studentId;
    private Long userId;
    private String ra;
    private String phone;
    private LocalDate lastActivity;

    public StudentModel() { }

}
