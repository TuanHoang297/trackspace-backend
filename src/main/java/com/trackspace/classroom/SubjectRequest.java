package com.trackspace.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Subject request payload")
public class SubjectRequest {

    @NotBlank(message = "Mã môn không được để trống")
    @Size(max = 50, message = "Mã môn không được vượt quá 50 ký tự")
    @Schema(description = "Subject code", example = "SE101")
    private String subjectCode;

    @NotBlank(message = "Tên môn không được để trống")
    @Size(max = 255, message = "Tên môn không được vượt quá 255 ký tự")
    @Schema(description = "Subject name", example = "Software Engineering")
    private String subjectName;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    @Schema(description = "Subject description")
    private String description;
}
