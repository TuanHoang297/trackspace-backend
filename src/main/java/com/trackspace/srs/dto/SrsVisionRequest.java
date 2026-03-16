package com.trackspace.srs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SrsVisionRequest {
    /** Base64 image data URL or public image URL (Cloudinary) */
    @NotNull
    private String image;

    /** Image type: usecase, screenflow, db_schema, mockup */
    @NotBlank
    private String type;

    /** Optional: extra context about the project */
    private String context;
}
