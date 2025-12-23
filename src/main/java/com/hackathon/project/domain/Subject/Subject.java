package com.hackathon.project.domain.Subject;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subjects")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offering_department_major", nullable = false)
    private String offeringDepartmentMajor;

    @Column(name = "course_code", nullable = false)
    private String courseCode;

    @Column(name = "section", nullable = false)
    private String section;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "lecture_language")
    private String lectureLanguage;

    @Column(name = "course_type", nullable = false)
    private String courseType;

    @Column(name = "selected_area")
    private String selectedArea;

    @Column(name = "credits", nullable = false)
    private Double credits;

    @Column(name = "theory_hours", nullable = false)
    private Integer theoryHours;

    @Column(name = "practice_hours", nullable = false)
    private Integer practiceHours;

    @Column(name = "grade_level", nullable = false)
    private Integer gradeLevel;

    @Column(name = "target_program", nullable = false)
    private String targetProgram;

    @Column(name = "host_department", nullable = false)
    private String hostDepartment;

    @Column(name = "professor_name", nullable = false)
    private String professorName;

    @Column(name = "schedule", nullable = false)
    private String schedule;

    @Column(name = "classroom")
    private String classroom;

    @Column(name = "cyber_lecture")
    private String cyberLecture;

    @Column(name = "course_format")
    private String courseFormat;

    @Column(name = "credit_exchange_availability")
    private String creditExchangeAvailability;

    @Column(name = "notes")
    private String notes;
}
