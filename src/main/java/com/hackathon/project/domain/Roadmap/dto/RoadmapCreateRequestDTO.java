package com.hackathon.project.domain.Roadmap.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoadmapCreateRequestDTO {

    private Transcript transcript;
    private CareerGoal careerGoal;

    // =====================
    // Transcript
    // =====================
    @Getter
    @NoArgsConstructor
    public static class Transcript {

        private List<Course> courses;
        private int totalCredits;
        private int totalMajorCredits;
        private int totalGeneralCredits;
        private double averageGPA;
    }

    // =====================
    // Course
    // =====================
    @Getter
    @NoArgsConstructor
    public static class Course {

        private String courseCode;       // 학수번호
        private String courseName;       // 교과목명
        private String courseType;       // 이수구분
        private String teachingArea;     // 교직영역
        private String selectedArea;     // 선택영역
        private int credits;             // 학점
        private String evaluationType;   // 평가방식
        private String grade;             // 등급
        private double gradePoint;        // 평점
        private String departmentCode;    // 개설학과코드
    }

    // =====================
    // CareerGoal
    // =====================
    @Getter
    @NoArgsConstructor
    public static class CareerGoal {

        private String careerPath;
        private List<String> interests;
        private String additionalInfo;
    }
}