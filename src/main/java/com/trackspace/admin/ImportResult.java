package com.trackspace.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {

    private int totalRows;
    private int successCount;
    private int failedCount;

    @Builder.Default
    private List<SuccessEntry> successes = new ArrayList<>();

    @Builder.Default
    private List<ImportError> errors = new ArrayList<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SuccessEntry {
        private int row;
        private String email;
        private String fullName;
        private String role;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImportError {
        private int row;
        private String email;
        private String reason;
    }
}
