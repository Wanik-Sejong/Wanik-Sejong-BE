package com.hackathon.project.domain.Roadmap.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExcelParseResponseDTO {

    private List<ExcelParseDTO> courses;
}
