package com.hackathon.project.domain.Roadmap.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExcelParseDTO {

    private String courseCode;       // 학수번호
    private String courseName;       // 교과목명
    private String courseType;       // 이수구분
    private String teachingArea;     // 교직영역
    private String selectedArea;     // 선택영역
    private double credits;             // 학점
    private String evaluationType;   // 평가방식
    private String grade;            // 등급
    private double gradePoint;       // 평점
    private String departmentCode;   // 개설학과코드
}