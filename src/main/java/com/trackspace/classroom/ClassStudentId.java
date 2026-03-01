package com.trackspace.classroom;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for ClassStudent entity
 */
public class ClassStudentId implements Serializable {

    private Long classroom;
    private Long student;

    public ClassStudentId() {}

    public ClassStudentId(Long classroom, Long student) {
        this.classroom = classroom;
        this.student = student;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassStudentId that)) return false;
        return Objects.equals(classroom, that.classroom) && Objects.equals(student, that.student);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classroom, student);
    }
}
