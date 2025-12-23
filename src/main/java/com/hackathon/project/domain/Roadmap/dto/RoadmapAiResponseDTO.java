package com.hackathon.project.domain.Roadmap.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoadmapAiResponseDTO {

    private String careerSummary;
    private CurrentSkills currentSkills;
    private List<LearningPath> learningPath;
    private String advice;
    private String generatedAt;

    @Getter
    @NoArgsConstructor
    public static class CurrentSkills {

        private List<String> strengths;
        private List<String> gaps;
    }

    @Getter
    @NoArgsConstructor
    public static class LearningPath {

        private String period;
        private String goal;
        private List<Course> courses;
        private List<String> activities;
        private String effort;
    }

    @Getter
    @NoArgsConstructor
    public static class Course {

        private String name;
        private String type;
        private String reason;
        private String priority;
        private List<String> prerequisites;
    }
}