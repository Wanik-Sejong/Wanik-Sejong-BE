package com.hackathon.project.domain.Roadmap.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.hackathon.project.domain.Subject.dto.SubjectScoreResponseDTO;

@Getter
@NoArgsConstructor
public class RoadmapAiResponseDTO {

    private String careerSummary;
    private CurrentSkills currentSkills;
    private List<CoursePlan> coursePlan;
    private List<ExtracurricularPlan> extracurricularPlan;
    private List<String> recommendedTechStack;
    private String advice;
    private String generatedAt;
    private SubjectScoreResponseDTO subjectRecommendations;
    private WeightHintResponseDTO weightHints;

    public void setSubjectRecommendations(SubjectScoreResponseDTO subjectRecommendations) {
        this.subjectRecommendations = subjectRecommendations;
    }

    public void setWeightHints(WeightHintResponseDTO weightHints) {
        this.weightHints = weightHints;
    }

    public void setCoursePlan(List<CoursePlan> coursePlan) {
        this.coursePlan = coursePlan;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentSkills {

        private List<String> strengths;
        private List<String> gaps;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoursePlan {

        private String period;
        private String goal;
        private List<Course> courses;
        private String effort;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtracurricularPlan {

        private String period;
        private String goal;
        private List<String> activities;
        private String effort;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Course {

        private String name;
        private String type;
        private String reason;
        private String priority;
        private List<String> prerequisites;
    }
}
