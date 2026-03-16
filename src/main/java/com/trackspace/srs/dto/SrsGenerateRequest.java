package com.trackspace.srs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SrsGenerateRequest {
    /** Optional: Business Rules for SRS section I.3 */
    private String businessRules;
    /** Optional: Non-Screen Functions for SRS section I.5.4 */
    private String nonScreenFunctions;
    /** Optional: Extra notes for AI */
    private String notes;
}
