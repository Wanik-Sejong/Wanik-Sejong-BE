package com.hackathon.project.domain.Roadmap;

import com.hackathon.project.domain.GeminiService;
import com.hackathon.project.domain.Roadmap.dto.ExcelParseDTO;
import com.hackathon.project.domain.Roadmap.dto.ExcelParseResponseDTO;
import com.hackathon.project.domain.Roadmap.dto.RoadmapAiResponseDTO;
import com.hackathon.project.domain.Roadmap.dto.RoadmapCreateRequestDTO;
import com.hackathon.project.domain.Roadmap.dto.WeightHintResponseDTO;
import com.hackathon.project.domain.Subject.SubjectRecommendationService;
import com.hackathon.project.domain.Subject.dto.SubjectScoreRequestDTO;
import com.hackathon.project.domain.Subject.dto.SubjectScoreResponseDTO;
import com.hackathon.project.domain.Subject.dto.SubjectScoreResponseDTO.ScoredSubject;
import com.hackathon.project.domain.Subject.dto.SubjectScoreResponseDTO.SubjectSummary;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RoadmapService {

    private final GeminiService geminiService;
    private final SubjectRecommendationService subjectRecommendationService;
    private final WeightHintService weightHintService;

    public List<ExcelParseDTO> parse(MultipartFile file) {
        List<ExcelParseDTO> results = new ArrayList<>();

        try (InputStream is = file.getInputStream();
            Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0); // 첫 시트

            // 0번째 row는 헤더라고 가정하고 1번째부터 시작
            for (int i = 4; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                ExcelParseDTO dto = ExcelParseDTO.builder()
                    .completedYear(getInt(row.getCell(1)))         // 이수년도
                    .completedSemester(getInt(row.getCell(2)))     // 이수학기
                    .courseCode(getString(row.getCell(3)))         // 학수번호
                    .courseName(getString(row.getCell(4)))         // 교과목명
                    .courseType(getString(row.getCell(5)))         // 이수구분
                    .teachingArea(getString(row.getCell(6)))       // 교직영역
                    .selectedArea(getString(row.getCell(7)))       // 선택영역
                    .credits(getDouble(row.getCell(8)))            // 학점
                    .evaluationType(getString(row.getCell(9)))     // 평가방식
                    .grade(getString(row.getCell(10)))             // 등급
                    .gradePoint(getDouble(row.getCell(11)))        // 평점
                    .departmentCode(getString(row.getCell(12)))    // 개설학과코드
                    .build();

                results.add(dto);
            }

        } catch (IOException e) {
            throw new RuntimeException("엑셀 파싱 실패", e);
        }

        return results;
    }

    private String getString(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }

    private double getDouble(Cell cell) {
        if (cell == null) {
            return 0.0;
        }

        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                String value = cell.getStringCellValue().trim();
                yield value.isEmpty() ? 0.0 : Double.parseDouble(value);
            }
            default -> 0.0;
        };
    }

    private int getInt(Cell cell) {
        if (cell == null) {
            return 0;
        }
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> parseDigits(cell.getStringCellValue());
            default -> 0;
        };
    }

    private int parseDigits(String value) {
        if (value == null) {
            return 0;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(digits);
    }

    public RoadmapAiResponseDTO generateRoadmap(RoadmapCreateRequestDTO requestDTO) {
        RoadmapAiResponseDTO roadmap = geminiService.askRoadMap(requestDTO);
        WeightHintResponseDTO weightHints =
            weightHintService.buildWeightHints(buildCareerText(requestDTO));
        SubjectScoreResponseDTO subjectRecommendations =
            subjectRecommendationService.scoreSubjects(buildSubjectScoreRequest(requestDTO));
        roadmap.setWeightHints(weightHints);
        roadmap.setSubjectRecommendations(subjectRecommendations);
        roadmap.setCoursePlan(
            buildCoursePlanFromSubjects(roadmap.getCoursePlan(), subjectRecommendations));
        return roadmap;
    }

    private SubjectScoreRequestDTO buildSubjectScoreRequest(RoadmapCreateRequestDTO requestDTO) {
        String careerText = buildCareerText(requestDTO);
        List<String> completedCourseCodes = extractCompletedCourseCodes(requestDTO);
        return new SubjectScoreRequestDTO(careerText, null, completedCourseCodes);
    }

    private String buildCareerText(RoadmapCreateRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.getCareerGoal() == null) {
            return "";
        }
        return requestDTO.getCareerGoal();
    }

    private List<String> extractCompletedCourseCodes(RoadmapCreateRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.getTranscript() == null) {
            return List.of();
        }
        List<RoadmapCreateRequestDTO.Course> courses = requestDTO.getTranscript().getCourses();
        if (courses == null) {
            return List.of();
        }
        return courses.stream()
            .map(RoadmapCreateRequestDTO.Course::getCourseCode)
            .filter(code -> code != null && !code.isBlank())
            .collect(Collectors.toList());
    }

    private List<RoadmapAiResponseDTO.CoursePlan> buildCoursePlanFromSubjects(
        List<RoadmapAiResponseDTO.CoursePlan> coursePlan,
        SubjectScoreResponseDTO subjectRecommendations
    ) {
        int periodCount = (coursePlan == null || coursePlan.isEmpty()) ? 1 : coursePlan.size();
        List<ScoredSubject> scoredSubjects = subjectRecommendations.getSubjects();
        if (scoredSubjects == null || scoredSubjects.isEmpty()) {
            return coursePlan;
        }

        List<ScoredSubject> uniqueSubjects = deduplicateSubjects(scoredSubjects);
        uniqueSubjects.sort(Comparator
            .comparingInt((ScoredSubject s) -> gradeLevelOrMax(s.getSubject()))
            .thenComparing(ScoredSubject::getScore, Comparator.reverseOrder()));

        int perPeriod = (int) Math.ceil(uniqueSubjects.size() / (double) periodCount);
        List<List<RoadmapAiResponseDTO.Course>> buckets = new ArrayList<>(periodCount);
        for (int i = 0; i < periodCount; i++) {
            buckets.add(new ArrayList<>());
        }

        for (int i = 0; i < uniqueSubjects.size(); i++) {
            int periodIndex = i / perPeriod;
            if (periodIndex >= periodCount) {
                periodIndex = periodCount - 1;
            }
            buckets.get(periodIndex).add(mapToCourse(uniqueSubjects.get(i)));
        }

        List<RoadmapAiResponseDTO.CoursePlan> result = new ArrayList<>(periodCount);
        for (int i = 0; i < periodCount; i++) {
            RoadmapAiResponseDTO.CoursePlan base =
                coursePlan == null || coursePlan.isEmpty() ? null : coursePlan.get(i);
            String period = base == null ? "다음 학기" : base.getPeriod();
            String goal = base == null ? "추천 교과목 수강" : base.getGoal();
            String effort = base == null ? "주 0시간" : base.getEffort();
            result.add(new RoadmapAiResponseDTO.CoursePlan(
                period,
                goal,
                buckets.get(i),
                effort
            ));
        }
        return result;
    }

    private List<ScoredSubject> deduplicateSubjects(List<ScoredSubject> scoredSubjects) {
        Map<String, ScoredSubject> byKey = new LinkedHashMap<>();
        for (ScoredSubject scored : scoredSubjects) {
            if (scored == null || scored.getSubject() == null) {
                continue;
            }
            SubjectSummary subject = scored.getSubject();
            String code = subject.getCourseCode();
            String name = subject.getCourseName();
            String key;
            if (code != null && !code.isBlank()) {
                key = code.trim();
            } else if (name != null && !name.isBlank()) {
                key = name.trim().toLowerCase(Locale.ROOT);
            } else {
                continue;
            }
            ScoredSubject existing = byKey.get(key);
            if (existing == null || scored.getScore() > existing.getScore()) {
                byKey.put(key, scored);
            }
        }
        return new ArrayList<>(byKey.values());
    }

    private int gradeLevelOrMax(SubjectSummary summary) {
        if (summary == null || summary.getGradeLevel() == null) {
            return Integer.MAX_VALUE;
        }
        return summary.getGradeLevel();
    }

    private RoadmapAiResponseDTO.Course mapToCourse(ScoredSubject scored) {
        SubjectSummary summary = scored.getSubject();
        String reason = scored.getReasons() == null || scored.getReasons().isEmpty()
            ? "DB 추천 점수 기반"
            : String.join(", ", scored.getReasons());
        String priority = "선택";
        if (summary != null && summary.getCourseType() != null) {
            String normalized = summary.getCourseType().toLowerCase(Locale.ROOT);
            if (normalized.contains("필수") || normalized.contains("전필") || normalized.contains(
                "공필")) {
                priority = "필수";
            }
        }
        return new RoadmapAiResponseDTO.Course(
            summary == null ? null : summary.getCourseName(),
            summary == null ? null : summary.getCourseType(),
            reason,
            priority,
            List.of()
        );
    }

    public ExcelParseResponseDTO convertExcelParseResponseDTO(List<ExcelParseDTO> excelParseDTOS) {
        double totalCredits = 0.0;
        double totalMajorCredits = 0.0;
        double totalGradePoints = 0.0;
        double pnp = 0.0;
        double totalF = 0.0;

        for (ExcelParseDTO parseDTO : excelParseDTOS) {
            String grade = parseDTO.getGrade();

            // 3. F 학점은 totalCredits에는 포함되지만 점수는 0점 (GPA를 깎아먹는 요인)
            // startsWith("F") 블록이 비어있어도 아래에서 credits와 gradePoints(0)가 더해지므로 로직상 정상입니다.
            if (grade.startsWith("F")) {
                // 명시적으로 무언가를 할 필요는 없으나, 가독성을 위해 남겨둠
                totalF += parseDTO.getCredits();
            }

            // 1. NP는 학점 계산에서 완전히 제외 (94학점에 미포함)
            if (grade.startsWith("NP")) {
                continue;
            }

            // 2. P 학점은 별도로 합산 (나중에 GPA 분모에서 제외하기 위함)
            if (grade.startsWith("P")) {
                pnp += parseDTO.getCredits();
            }

            // 공통: 학점 합산 (P, F, 일반성적 모두 포함하여 94.0학점)
            totalCredits += parseDTO.getCredits();

            if (parseDTO.getCourseType().contains("전")) {
                totalMajorCredits += parseDTO.getCredits();
            }

            // 성적 총점 합산 (F와 P는 gradePoint가 0이므로 결과에 영향을 주지 않음)
            totalGradePoints += parseDTO.getGradePoint() * parseDTO.getCredits();
        }

// 4. 최종 GPA 계산 (중요: pnp 학점을 제외한 학점으로 나눔)
        double gpaDenominator = totalCredits - pnp; // 94.0 - 6.0 = 88.0
        double averageGPA = (gpaDenominator > 0) ? totalGradePoints / gpaDenominator : 0.0;
        return ExcelParseResponseDTO.builder()
            .courses(excelParseDTOS)
            .totalCredits(totalCredits - totalF)
            .totalMajorCredits(totalMajorCredits)
            .totalGeneralCredits(totalCredits - totalMajorCredits)
            .averageGPA(averageGPA)
            .build();
    }
}
