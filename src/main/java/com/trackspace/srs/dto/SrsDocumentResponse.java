package com.trackspace.srs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SrsDocumentResponse {
    private Long id;
    private Integer versionNumber;
    private String title;
    private String content;
    private Long projectId;
    private String createdByName;
    private LocalDateTime updatedAt;
}
