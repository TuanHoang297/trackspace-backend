package com.trackspace.srs.dto;

import lombok.Data;

@Data
public class DocxExportRequest {
    private String htmlContent;
    private String title;
    private String fileName;
}
