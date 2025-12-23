package com.hackathon.project.domain.Roadmap.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoadmapCreateRequestDTO {

    private Transcript transcript;
    private String careerGoal;

    // =====================
    // Transcript
    // =====================
    @Getter
    @NoArgsConstructor
    public static class Transcript {

        private List<Course> courses;
        private double totalCredits;
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

        private int completedYear;        // 이수년도
        private int completedSemester;    // 이수학기
        private String courseCode;       // 학수번호
        private String courseName;       // 교과목명
        private String courseType;       // 이수구분
        private String teachingArea;     // 교직영역
        private String selectedArea;     // 선택영역
        private double credits;          // 학점
        private String evaluationType;   // 평가방식
        private String grade;            // 등급
        private double gradePoint;       // 평점
        private String departmentCode;    // 개설학과코드
    }

    // careerGoal: 학생의 희망 진로/관심사/추가 정보를 문장형으로 전달
}
