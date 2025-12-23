package com.hackathon.project.domain.Subject;

import com.hackathon.project.domain.Roadmap.WeightHintService;
import com.hackathon.project.domain.Roadmap.dto.WeightHintResponseDTO;
import com.hackathon.project.domain.Subject.dto.SubjectScoreRequestDTO;
import com.hackathon.project.domain.Subject.dto.SubjectScoreResponseDTO;
import com.hackathon.project.domain.Subject.dto.SubjectScoreResponseDTO.ScoredSubject;
import com.hackathon.project.domain.Subject.dto.SubjectScoreResponseDTO.SubjectSummary;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubjectRecommendationService {

    private static final List<String> PROJECT_KEYWORDS = List.of("프로젝트", "실무", "응용");
    private static final List<String> PRACTICE_KEYWORDS = List.of("프로젝트", "캡스톤", "실험", "실습");

    private final SubjectRepository subjectRepository;
    private final WeightHintService weightHintService;

    public SubjectScoreResponseDTO scoreSubjects(SubjectScoreRequestDTO requestDTO) {
        WeightHintResponseDTO hints =
            weightHintService.buildWeightHints(requestDTO.getCareerText());
        Set<String> keywords = flattenKeywords(hints.getSectorKeywords());
        Set<String> completedCodes = normalizeCodes(requestDTO.getCompletedCourseCodes());

        List<ScoredSubject> scored = new ArrayList<>();
        for (Subject subject : subjectRepository.findAll()) {
            if (completedCodes.contains(normalizeCode(subject.getCourseCode()))) {
                continue;
            }
            scored.add(scoreSubject(subject, keywords));
        }

        scored.sort(Comparator
            .comparingInt(ScoredSubject::getScore).reversed()
            .thenComparing(s -> s.getSubject().getCourseName(), Comparator.nullsLast(String::compareTo)));

        int topN = requestDTO.getTopN() == null ? hints.getDefaultN() : requestDTO.getTopN();
        if (topN < 1) {
            topN = hints.getDefaultN();
        }

        List<ScoredSubject> topScored =
            scored.size() > topN ? scored.subList(0, topN) : scored;

        return new SubjectScoreResponseDTO(
            hints.getMatchedSectors(),
            topN,
            topScored
        );
    }

    private ScoredSubject scoreSubject(Subject subject, Set<String> keywords) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (containsAny(subject.getCourseName(), keywords)) {
            score += 10;
            reasons.add("과목명 키워드 매칭");
        }

        if (containsAny(subject.getSelectedArea(), keywords) || containsAny(subject.getCourseType(), keywords)) {
            score += 8;
            reasons.add("선택영역/이수구분 키워드 매칭");
        }

        if (containsAny(subject.getNotes(), keywords) || containsAny(subject.getNotes(), PROJECT_KEYWORDS)) {
            score += 6;
            reasons.add("유의사항 키워드 매칭");
        }

        if (containsAny(subject.getOfferingDepartmentMajor(), keywords)
            || containsAny(subject.getHostDepartment(), keywords)) {
            score += 7;
            reasons.add("개설/주관학과 매칭");
        }

        if (isAdvancedCourseCode(subject.getCourseCode())) {
            score += 4;
            reasons.add("전공 심화 학수번호");
        }

        if (subject.getGradeLevel() != null && (subject.getGradeLevel() == 3 || subject.getGradeLevel() == 4)) {
            score += 4;
            reasons.add("상위 학년 과목");
        }

        if (subject.getPracticeHours() != null && subject.getTheoryHours() != null
            && subject.getPracticeHours() >= subject.getTheoryHours() && subject.getPracticeHours() > 0) {
            score += 5;
            reasons.add("실습 비중 높음");
        }

        if (containsAny(subject.getCourseFormat(), PRACTICE_KEYWORDS)) {
            score += 6;
            reasons.add("실습/프로젝트형 강좌");
        }

        if (subject.getCredits() != null && subject.getCredits() >= 3.0) {
            score += 3;
            reasons.add("3학점 이상");
        }

        if (containsAny(subject.getLectureLanguage(), List.of("영어", "English"))) {
            score += 2;
            reasons.add("영어 강의");
        }

        boolean isOnline = subject.getCyberLecture() != null
            || containsAny(subject.getCourseFormat(), List.of("혼합", "블렌디드"));
        if (isOnline && containsAnyAnyField(subject, keywords)) {
            score += 2;
            reasons.add("사이버/혼합형 + 키워드 매칭");
        }

        if (subject.getCreditExchangeAvailability() != null && containsAnyAnyField(subject, keywords)) {
            score += 1;
            reasons.add("학점교류 + 키워드 매칭");
        }

        SubjectSummary summary = new SubjectSummary(
            subject.getCourseCode(),
            subject.getCourseName(),
            subject.getCourseType(),
            subject.getSelectedArea(),
            subject.getCredits(),
            subject.getGradeLevel(),
            subject.getOfferingDepartmentMajor(),
            subject.getHostDepartment(),
            subject.getLectureLanguage(),
            subject.getCourseFormat(),
            subject.getSchedule(),
            subject.getClassroom()
        );

        return new ScoredSubject(summary, score, reasons);
    }

    private boolean containsAnyAnyField(Subject subject, Set<String> keywords) {
        return containsAny(subject.getCourseName(), keywords)
            || containsAny(subject.getSelectedArea(), keywords)
            || containsAny(subject.getNotes(), keywords);
    }

    private boolean containsAny(String text, Iterable<String> keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean isAdvancedCourseCode(String courseCode) {
        if (courseCode == null || courseCode.isBlank()) {
            return false;
        }
        String digits = courseCode.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return false;
        }
        char first = digits.charAt(0);
        return first == '3' || first == '4';
    }

    private Set<String> flattenKeywords(Map<String, List<String>> sectorKeywords) {
        Set<String> keywords = new LinkedHashSet<>();
        for (List<String> values : sectorKeywords.values()) {
            keywords.addAll(values);
        }
        return keywords;
    }

    private Set<String> normalizeCodes(List<String> codes) {
        Set<String> normalized = new HashSet<>();
        if (codes == null) {
            return normalized;
        }
        for (String code : codes) {
            String norm = normalizeCode(code);
            if (!norm.isEmpty()) {
                normalized.add(norm);
            }
        }
        return normalized;
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return "";
        }
        return code.trim();
    }
}
