package com.trackspace.srs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SrsGenerateRequest {
    /** Optional: Additional info for AI (business rules, constraints, notes, etc.) */
    private String additionalInfo;
}
