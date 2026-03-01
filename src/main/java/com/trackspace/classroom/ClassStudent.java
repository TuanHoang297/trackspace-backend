package com.trackspace.classroom;

import com.trackspace.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ClassStudent Entity
 * Represents the many-to-many relationship between Class and Student (User)
 */
@Entity
@Table(
        name = "class_students",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"class_id", "student_id"},
                name = "uq_class_student"
        )
)
@IdClass(ClassStudentId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassStudent {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Class classroom;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private LocalDateTime enrolledAt;

    @PrePersist
    protected void onCreate() {
        enrolledAt = LocalDateTime.now();
    }
}
