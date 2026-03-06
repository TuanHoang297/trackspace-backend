package com.trackspace.classroom;

import com.trackspace.common.BadRequestException;
import com.trackspace.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterRepository semesterRepository;

    @Transactional(readOnly = true)
    public List<SemesterResponse> getAllActiveSemesters() {
        return semesterRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SemesterResponse> getAllSemesters() {
        return semesterRepository.findAll()
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public SemesterResponse createSemester(SemesterRequest request) {
        if (semesterRepository.existsByName(request.getName())) {
            throw new BadRequestException("Học kỳ '" + request.getName() + "' đã tồn tại");
        }
        Semester semester = Semester.builder()
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(true)
                .build();
        return toResponse(semesterRepository.save(semester));
    }

    @Transactional
    public SemesterResponse updateSemester(Long id, SemesterRequest request) {
        Semester semester = findById(id);
        if (!semester.getName().equals(request.getName()) && semesterRepository.existsByName(request.getName())) {
            throw new BadRequestException("Học kỳ '" + request.getName() + "' đã tồn tại");
        }
        semester.setName(request.getName());
        semester.setStartDate(request.getStartDate());
        semester.setEndDate(request.getEndDate());
        return toResponse(semesterRepository.save(semester));
    }

    @Transactional
    public void deleteSemester(Long id) {
        Semester semester = findById(id);
        semesterRepository.delete(semester);
    }

    private Semester findById(Long id) {
        return semesterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học kỳ với ID: " + id));
    }

    private SemesterResponse toResponse(Semester s) {
        return SemesterResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .active(s.getActive())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
