package com.hackathon.project.domain.Roadmap;

import com.hackathon.project.domain.Roadmap.dto.ExcelParseDTO;
import com.hackathon.project.domain.Roadmap.dto.ExcelParseResponseDTO;
import com.hackathon.project.domain.Roadmap.dto.RoadmapAiResponseDTO;
import com.hackathon.project.domain.Roadmap.dto.RoadmapCreateRequestDTO;
import com.hackathon.project.domain.Roadmap.dto.WeightHintRequestDTO;
import com.hackathon.project.domain.Roadmap.dto.WeightHintResponseDTO;
import com.hackathon.project.global.dto.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RoadmapController {

    private final RoadmapService roadmapService;
    private final WeightHintService weightHintService;

    @PostMapping(
        value = "/parse-excel",
        consumes = "multipart/form-data",
        produces = "application/json"
    )
    public ResponseEntity<ApiResponse<ExcelParseResponseDTO>> uploadExcel(
        @RequestParam("file") MultipartFile file) {
        List<ExcelParseDTO> excelParseDTOS = roadmapService.parse(file);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(ApiResponse.success(roadmapService.convertExcelParseResponseDTO(excelParseDTOS)));
    }

    @PostMapping("/generate-roadmap")
    public ResponseEntity<ApiResponse<RoadmapAiResponseDTO>> generateRoadmap(
        @RequestBody RoadmapCreateRequestDTO requestDTO) {
        RoadmapAiResponseDTO roadmap = roadmapService.generateRoadmap(requestDTO);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(ApiResponse.success(roadmap));
    }

    @PostMapping("/weight-hints")
    public ResponseEntity<ApiResponse<WeightHintResponseDTO>> weightHints(
        @RequestBody WeightHintRequestDTO requestDTO) {
        WeightHintResponseDTO response =
            weightHintService.buildWeightHints(requestDTO.getCareerText());
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(ApiResponse.success(response));
    }
}
