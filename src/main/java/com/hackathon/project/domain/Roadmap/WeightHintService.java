package com.hackathon.project.domain.Roadmap;

import com.hackathon.project.domain.Roadmap.dto.WeightHintResponseDTO;
import com.hackathon.project.domain.Roadmap.dto.WeightHintResponseDTO.WeightRule;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WeightHintService {

    private static final Map<String, List<String>> SECTOR_KEYWORDS = buildSectorKeywords();
    private static final List<WeightRule> WEIGHT_RULES = buildWeightRules();
    private static final int DEFAULT_N = 20;
    private static final String NOTES =
        "동일 섹터 키워드가 여러 컬럼에서 중복 매칭될 경우 점수는 합산하되, 동일 규칙은 1회만 적용한다. "
            + "영어 키워드는 한글 키워드와 동등하게 매칭하며 대소문자는 구분하지 않는다.";

    public WeightHintResponseDTO buildWeightHints(String careerText) {
        List<String> matchedSectors = matchSectors(careerText);
        Map<String, List<String>> keywords =
            matchedSectors.isEmpty() ? SECTOR_KEYWORDS : filterKeywords(matchedSectors);
        if (matchedSectors.isEmpty()) {
            matchedSectors = new ArrayList<>(SECTOR_KEYWORDS.keySet());
        }

        return new WeightHintResponseDTO(
            matchedSectors,
            keywords,
            WEIGHT_RULES,
            DEFAULT_N,
            NOTES
        );
    }

    private List<String> matchSectors(String careerText) {
        if (careerText == null || careerText.isBlank()) {
            return List.of();
        }

        String normalized = careerText.toLowerCase(Locale.ROOT);
        List<String> matched = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : SECTOR_KEYWORDS.entrySet()) {
            if (containsAny(normalized, entry.getValue())) {
                matched.add(entry.getKey());
            }
        }
        return matched;
    }

    private boolean containsAny(String normalizedText, List<String> keywords) {
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            if (normalizedText.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private Map<String, List<String>> filterKeywords(List<String> matchedSectors) {
        Map<String, List<String>> filtered = new LinkedHashMap<>();
        for (String sector : matchedSectors) {
            List<String> keywords = SECTOR_KEYWORDS.get(sector);
            if (keywords != null) {
                filtered.put(sector, keywords);
            }
        }
        return filtered;
    }

    private static Map<String, List<String>> buildSectorKeywords() {
        Map<String, List<String>> keywords = new LinkedHashMap<>();
        keywords.put("AI", List.of("인공지능", "AI", "머신러닝", "딥러닝", "신경망", "자연어", "컴퓨터비전", "강화학습", "데이터마이닝"));
        keywords.put("백엔드", List.of("백엔드", "서버", "웹", "API", "스프링", "Spring", "데이터베이스", "DB", "트랜잭션", "분산", "클라우드"));
        keywords.put("데이터", List.of("데이터", "통계", "분석", "빅데이터", "시각화", "확률", "회귀", "데이터사이언스"));
        keywords.put("보안", List.of("보안", "암호", "해킹", "네트워크보안", "시스템보안", "정보보호", "취약점"));
        keywords.put("교육", List.of("교육", "교수법", "학습", "에듀", "교육공학", "수업설계"));
        keywords.put("경영", List.of("경영", "비즈니스", "전략", "마케팅", "회계", "재무", "조직"));
        keywords.put("디자인", List.of("디자인", "UX", "UI", "인터랙션", "시각디자인", "콘텐츠"));
        keywords.put("언론", List.of("언론", "미디어", "저널리즘", "방송", "신문", "커뮤니케이션"));
        return keywords;
    }

    private static List<WeightRule> buildWeightRules() {
        return List.of(
            new WeightRule(
                "교과목명에 섹터 핵심 키워드가 직접 포함된 경우",
                10,
                "과목의 주제가 해당 큰 섹터와 직접적으로 일치함을 가장 강하게 나타냄"
            ),
            new WeightRule(
                "선택영역/이수구분에 섹터 관련 트랙·전공명이 명시된 경우",
                8,
                "학사 구조상 해당 섹터를 목표로 설계된 과목일 가능성이 높음"
            ),
            new WeightRule(
                "수강대상및유의사항에 섹터 관련 키워드(예: 프로젝트, 실무, 응용)가 포함된 경우",
                6,
                "실제 교육 내용이 섹터 중심으로 운영됨을 보조적으로 확인 가능"
            ),
            new WeightRule(
                "개설학과전공 또는 주관학과가 섹터와 직접 연관된 학과인 경우",
                7,
                "학과 단위에서 해당 섹터 전문성을 전제로 개설된 과목일 확률이 높음"
            ),
            new WeightRule(
                "학수번호에 고급/전공 심화 과목을 의미하는 패턴이 포함된 경우",
                4,
                "기초 과목보다 섹터 전문 역량과의 연관성이 커질 가능성이 높음"
            ),
            new WeightRule(
                "학년이 3학년 또는 4학년으로 지정된 경우",
                4,
                "상위 학년 과목은 진로·분야 특화 내용이 포함될 확률이 높음"
            ),
            new WeightRule(
                "이론 대비 실습 비율에서 실습이 높게 편성된 경우",
                5,
                "실무 중심 섹터에서 중요도가 높은 수업 형태"
            ),
            new WeightRule(
                "강좌유형이 프로젝트, 캡스톤, 실험·실습 중심인 경우",
                6,
                "섹터 역량과 직접 연결되는 문제 해결·응용 능력 강화 과목일 가능성"
            ),
            new WeightRule(
                "학점이 3학점 이상인 경우",
                3,
                "충분한 학습 분량과 심도 있는 내용이 다뤄질 가능성이 높음"
            ),
            new WeightRule(
                "강의언어가 영어인 경우",
                2,
                "전공·최신 기술 중심 과목에서 영어 강의 비중이 높으며 글로벌 자료 활용 가능"
            ),
            new WeightRule(
                "사이버강좌 또는 혼합형 수업이면서 섹터 키워드가 포함된 경우",
                2,
                "최신 트렌드나 산업 연계 주제를 다루는 경우가 비교적 많음"
            ),
            new WeightRule(
                "학점교류수강가능 과목이면서 섹터 키워드가 포함된 경우",
                1,
                "타 대학 우수 전공 과목일 가능성을 소폭 반영"
            )
        );
    }
}
