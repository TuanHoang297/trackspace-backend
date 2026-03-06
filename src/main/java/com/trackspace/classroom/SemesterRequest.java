package com.trackspace.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Semester request payload")
public class SemesterRequest {

    @NotBlank(message = "Tên học kỳ không được để trống")
    @Size(max = 50, message = "Tên học kỳ không được vượt quá 50 ký tự")
    @Schema(description = "Semester name", example = "Spring 2026")
    private String name;

    @Schema(description = "Start date", example = "2026-01-01")
    private LocalDate startDate;

    @Schema(description = "End date", example = "2026-05-31")
    private LocalDate endDate;
}
