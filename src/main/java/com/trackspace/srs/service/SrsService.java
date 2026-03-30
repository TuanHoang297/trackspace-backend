package com.trackspace.srs.service;

import com.trackspace.srs.dto.SrsDocumentResponse;
import com.trackspace.srs.dto.SrsGenerateRequest;
import com.trackspace.srs.dto.SrsUpdateRequest;

import java.util.List;

public interface SrsService {
    SrsDocumentResponse generateSrs(Long projectId, Long currentUserId, SrsGenerateRequest request);
    SrsDocumentResponse getLatestSrs(Long projectId);
    List<SrsDocumentResponse> getAllVersions(Long projectId);
    SrsDocumentResponse updateSrs(Long srsId, SrsUpdateRequest request, Long currentUserId);
    void deleteSrsVersion(Long srsId, Long projectId);
}

