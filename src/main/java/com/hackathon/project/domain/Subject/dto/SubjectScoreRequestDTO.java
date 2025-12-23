package com.hackathon.project.domain.Subject.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubjectScoreRequestDTO {

    private String careerText;
    private Integer topN;
    private List<String> completedCourseCodes;
}
