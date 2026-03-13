package com.trackspace.srs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SrsUpdateRequest {
    @Size(min = 1, max = 500, message = "Title phải từ 1-500 ký tự nếu được cung cấp")
    private String title; // null = ko đổi, not-null = thay đổi

    @NotBlank
    private String content; //HTML Text
}
