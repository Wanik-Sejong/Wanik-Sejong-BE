package com.hackathon.project.domain.Subject;

import com.hackathon.project.domain.Subject.dto.SubjectScoreRequestDTO;
import com.hackathon.project.domain.Subject.dto.SubjectScoreResponseDTO;
import com.hackathon.project.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectRecommendationService subjectRecommendationService;

    @PostMapping("/score")
    public ResponseEntity<ApiResponse<SubjectScoreResponseDTO>> scoreSubjects(
        @RequestBody SubjectScoreRequestDTO requestDTO) {
        SubjectScoreResponseDTO response = subjectRecommendationService.scoreSubjects(requestDTO);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(ApiResponse.success(response));
    }
}
