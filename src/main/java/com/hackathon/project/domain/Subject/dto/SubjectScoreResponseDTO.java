package com.hackathon.project.domain.Subject.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SubjectScoreResponseDTO {

    private List<String> matchedSectors;
    private int topN;
    private List<ScoredSubject> subjects;

    @Getter
    @AllArgsConstructor
    public static class ScoredSubject {

        private SubjectSummary subject;
        private int score;
        private List<String> reasons;
    }

    @Getter
    @AllArgsConstructor
    public static class SubjectSummary {

        private String courseCode;
        private String courseName;
        private String courseType;
        private String selectedArea;
        private Double credits;
        private Integer gradeLevel;
        private String offeringDepartmentMajor;
        private String hostDepartment;
        private String lectureLanguage;
        private String courseFormat;
        private String schedule;
        private String classroom;
    }
}
