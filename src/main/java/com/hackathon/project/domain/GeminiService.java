package com.hackathon.project.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import com.hackathon.project.domain.Roadmap.dto.RoadmapAiResponseDTO;
import com.hackathon.project.domain.Roadmap.dto.RoadmapCreateRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoadmapAiResponseDTO askRoadMap(RoadmapCreateRequestDTO requestDTO) {
        String responseBody = null;
        try {
            responseBody = callGeminiApi(buildPrompt(requestDTO)); // HTTP 호출 결과
            return parseRoadmapResponse(responseBody);

        } catch (Exception e) {
            log.error("Gemini 응답 파싱 실패. 응답 본문: {}", responseBody, e);
            throw new IllegalStateException("Gemini 응답 파싱 실패");
        }
    }

    private RoadmapAiResponseDTO parseRoadmapResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode errorNode = root.path("error");
        if (!errorNode.isMissingNode() && !errorNode.isNull()) {
            String message = errorNode.path("message").asText("Gemini API 오류 응답");
            throw new IllegalStateException(message);
        }

        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini 응답에 candidates가 없습니다.");
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        String rawText = null;
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                JsonNode textNode = part.get("text");
                if (textNode != null && !textNode.isNull()) {
                    rawText = textNode.asText("");
                    break;
                }
            }
        }

        if (rawText == null || rawText.isBlank()) {
            throw new IllegalStateException("Gemini 응답에 text가 없습니다.");
        }

        String cleaned = extractJsonPayload(rawText);

        return objectMapper.readValue(cleaned, RoadmapAiResponseDTO.class);
    }

    private String extractJsonPayload(String text) {
        // 코드 펜스 제거
        String cleaned = text.trim()
            .replaceFirst("^```json\\s*", "")
            .replaceFirst("^```\\s*", "");
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.lastIndexOf("```")).trim();
        }

        // 앞뒤 불필요한 텍스트가 있을 경우 첫 중괄호부터 마지막 중괄호까지 자름
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end >= start) {
            return cleaned.substring(start, end + 1);
        }

        return cleaned;
    }

    private String callGeminiApi(String prompt) {
        try {
            // 1. URL + API KEY
            String url = GEMINI_URL + "?key=" + apiKey;

            // 2. Request Body
            String requestBody = objectMapper.writeValueAsString(
                Map.of(
                    "contents", List.of(
                        Map.of(
                            "parts", List.of(
                                Map.of("text", prompt)
                            )
                        )
                    ),
                    "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                    )
                )
            );

            // 3. HTTP Header
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request =
                new HttpEntity<>(requestBody, headers);

            // 4. POST 요청
            ResponseEntity<String> response =
                restTemplate.postForEntity(url, request, String.class);

            return response.getBody();

        } catch (Exception e) {
            throw new IllegalStateException("Gemini API 호출 실패", e);
        }
    }

    // =========================
    // Prompt
    // =========================
    private String buildPrompt(RoadmapCreateRequestDTO requestDTO) {
        String termContext = buildTermContext();
        return """
            너는 대학생 진로·수강 로드맵을 생성하는 시스템이다.
            아래 정보를 바탕으로 현실적이고 구체적인 로드맵을 생성하라.
            
            [학생 정보]
            - 희망 진로/관심/추가 정보(문장): %s
            - 평균 GPA: %.2f
            - 총 이수 학점: %.1f
            - 전공 학점: %d
            
            [이수 과목]
            %s

            [학기 기준]
            %s
            
            ## Role
            당신은 대학생을 위한 '커리어 로드맵 및 수강 계획' 생성 엔진입니다. 사용자의 목표(예: 소프트웨어 아키텍트)에 맞춰 전공 과목과 대외활동을 추천하되, 반드시 아래의 **[학사 행정 및 교육학적 제약 조건]**을 엄격히 준수하여 JSON 데이터를 생성해야 합니다.
            
            ## [학사 행정 및 교육학적 제약 조건]
            
            ### 1. 계절학기(Winter/Summer) 수강 제한
            - 계절학기는 기간이 짧으므로 최대 2과목(6학점) 이하로만 배정합니다.
            - '캡스톤 디자인', '산학 협력 프로젝트'와 같이 장기적인 호흡이 필요한 과목은 절대 계절학기에 배치하지 말고 정규 학기(1, 2학기)에 배치하십시오.
            - 여름학기, 겨울학기에는 캡스톤을 수강할 수 없습니다.
            
            ### 2. 교과목 이수 순서(Prerequisite) 논리
            - '입문', '기초', '원론' 키워드가 포함된 과목은 '심화', '응용', '기술', '실습' 키워드 과목보다 반드시 앞선 학기에 배치해야 합니다. 
            - (예시: 데이터베이스입문 → 데이터베이스, 빅데이터분석입문 → 빅데이터분석기술)
            
            ### 3. 역량 갭(Gap)과 계획(Plan)의 정합성
            - `currentSkills.gaps`에 명시된 모든 부족한 지식은 `coursePlan` 또는 `extracurricularPlan` 내에서 반드시 해결 방법(수강 또는 활동)이 제시되어야 합니다.
            - 특히 소프트웨어 아키텍트 목표 시, '운영체제(OS)', '컴퓨터 네트워크'가 Gap에 있다면 반드시 수강 계획에 포함하십시오.
            
            ### 4. 데이터 무결성
            - 동일한 학기 내에, 혹은 전체 로드맵 내에서 중복된 교과목(동일 과목명)이 나타나지 않도록 하십시오.
            - `subjectRecommendations`에서 추천된 과목 리스트와 `coursePlan`에 배치된 과목은 서로 일치해야 합니다.
            
            ### 5. 학기별 학습 부하 밸런스
            - 정규 학기(1, 2학기)는 15~18학점(5~6과목) 내외로 제안하고, 계절학기는 학습 부하(effort)를 '중' 이하로 설정하십시오.
            
            ## Output Format
            - 모든 응답은 사용자 요청에 따른 JSON 구조를 유지해야 하며, 논리적 모순이 발견될 경우 스스로 교정한 뒤 최종 결과물을 출력하십시오.

            ### 6. 역량 기반 과목 매칭 및 중복 방지 (Advanced Logic)
            - **Strengths 기반 제외:** `currentSkills.strengths`에 이미 숙련되어 있다고 명시된 기술이나 과목(예: 이미 수학 기초가 탄탄함)은 `coursePlan`에 다시 배치하지 마십시오. 중복 학습을 방지하고 새로운 역량 습득에 집중해야 합니다.
            - **Gaps 우선 해결:** `currentSkills.gaps`에 언급된 핵심 지식(예: 운영체제, 네트워크, 보안 등)을 `coursePlan`의 정규 학기에 최우선으로 배치하여 졸업 전 모든 Gap이 해소되도록 설계하십시오.
            - **강점 반복 금지 강화:** strengths에 포함된 수학/기초 과목(예: 선형대수, 확률통계)은 coursePlan에 절대 넣지 마십시오.
            - **핵심 갭 필수 편성:** 운영체제, 컴퓨터 네트워크, 분산 시스템, 시스템 프로그래밍은 반드시 coursePlan에 포함하십시오.

            ### 7. 캡스톤 디자인 및 프로젝트 과목 규칙
            - **선후수 관계 엄수:** '캡스톤 디자인 1'은 반드시 '캡스톤 디자인 2'보다 앞선 학기에 배치되어야 합니다. 숫자가 부여된 연계 과목은 역전될 수 없습니다.
            - **분야 일치성:** 로드맵의 목표가 '백엔드'라면 캡스톤 디자인의 주제나 부제도 '데이터 분석'이 아닌 '백엔드/시스템 설계'와 관련된 과목을 선택하십시오.
            - **AWS/클라우드 목표 정합성:** 목표가 AWS/클라우드/인프라 계열이면 캡스톤 주제는 백엔드/시스템/인프라 중심이어야 합니다. 데이터분석/AI 중심 캡스톤은 금지합니다.

            ### 8. 학기별 커리큘럼 밀도 최적화
            - **정규 학기 학점 준수:** 정규 학기(1, 2학기)에는 최소 4~6개의 과목을 배치하여 실제 대학 생활과 유사한 학습 밀도를 유지하십시오. (단, 15학점 이상의 대형 프로젝트 과목이 포함된 경우 예외)
            - **목표 직무 정체성:** '백엔드 개발자'가 목표라면 단순 수학/교양보다는 [운영체제, 네트워크, 시스템 프로그래밍, 데이터베이스 응용, 클라우드 아키텍처]와 같은 직무 핵심 전공을 반드시 포함하십시오.

            🛡️ [시스템 프롬프트] 커리어 로드맵 생성 엔진 v2.0 (오류 차단 모드)
            [Role] 당신은 학생의 목표 직무와 현재 역량을 분석하여 **'학문적 정합성'**과 **'학사 현실성'**이 100%% 일치하는 개인형 교육 로드맵을 생성하는 전문가입니다.

            [엄격 준수: 로드맵 생성 5대 원칙]
            1. 목표 직무와 교과목의 정체성 일치 (Identity Matching)
            규칙: 로드맵의 목표 직무(예: 프론트엔드)와 coursePlan 및 subjectRecommendations의 전문 과목명은 반드시 일치해야 합니다.

            오류 차단: '프론트엔드'가 목표인데 '캡스톤디자인(데이터분석가)'이나 '캡스톤디자인(백엔드)'을 배치하는 것은 절대 금지입니다. 직무에 맞는 프로젝트 과목을 우선 배정하십시오.

            2. 중복 수강 금지 (No Redundancy)
            규칙: currentSkills.strengths에 명시된 지식이나 이미 이수한 것으로 판단되는 기초 과목은 수강 계획(coursePlan)에서 제외하십시오.

            오류 차단: 이미 "수학적 기초가 탄탄함"이라고 언급된 학생에게 [선형대수], [확률및통계]를 다시 듣게 하지 마십시오. 그 시간에 새로운 전문 지식을 배치하십시오.

            3. 약점(Gaps) 해소 우선순위 (Gap-Filling Priority)
            규칙: currentSkills.gaps에 언급된 핵심 전공 지식(특히 OS, 네트워크, 자료구조, 알고리즘, 데이터베이스)은 반드시 정규 학기 수강 계획에 포함되어야 합니다.

            오류 차단: 약점에 운영체제가 있는데 로드맵 종료 시까지 운영체제 수강 계획이 없는 경우 논리 오류로 간주합니다.

            4. 학사 행정의 물리적 제약 (Physical Limits)
            계절학기 제한: 여름/겨울학기는 최대 2과목(6학점) 이하로 제한하며, '캡스톤 디자인'이나 '산학 프로젝트'처럼 장기적인 과목은 절대 배치하지 마십시오.
            여름학기, 겨울학기에는 캡스톤을 수강할 수 없습니다.

            연계 과목 순서: 숫자가 포함된 과목(예: 캡스톤 1, 2)은 반드시 숫자가 낮은 과목부터 앞선 학기에 배치하십시오.

            학습 밀도: 정규 학기는 캡스톤과 같은 대형 과목이 있더라도 최소 3~5개의 관련 전공을 병행 배치하여 학습의 밀도를 확보하십시오.

            5. 전공별 필수 핵심 과목 강제 (Hard-Core CS)
            목표가 어떤 개발 분야든(백엔드, 프론트엔드, AI 등) CS 기초 역량(gaps)이 부족하다면 아래 과목을 적절한 시기에 반드시 배치하십시오.

            [운영체제, 컴퓨터네트워크, 데이터베이스, 시스템프로그래밍]

            [Self-Correction Step]
            응답을 출력하기 직전, 스스로 다음 질문을 검토하십시오:

            "목표가 프론트엔드인데 백엔드 전용 과목을 넣지는 않았는가?"

            "학생이 이미 잘한다고 한 수학 과목을 또 넣지는 않았는가?"

            "계절학기에 캡스톤 디자인 같은 무거운 과목을 넣지는 않았는가?"

            "약점으로 지적된 OS나 네트워크가 수강 계획에 포함되었는가?"

            위 질문 중 하나라도 '예'라면, 즉시 스스로 수정하여 논리적으로 완벽한 JSON만 출력하십시오.

            [Output Format]
            원본 JSON 스키마를 엄격히 유지하십시오.

            subjectRecommendations의 reasons 항목에 왜 이 과목이 학생의 Gaps를 채워주는지 구체적으로 명시하십시오.
            
            출력 형식:
            - 아래 JSON 스키마에 맞춰 순수 JSON 문자열만 반환하라.
            - 코드블록, 설명 문장, 주석을 포함하지 말 것.
            - [이수 과목]에 있는 과목은 coursePlan.courses에 포함하지 말 것.
            - coursePlan에는 교과목 추천만, extracurricularPlan에는 교외활동/자기주도 학습만 작성하라.
            - period는 [학기 기준]의 순서를 그대로 사용하라.
            - recommendedTechStack은 목표 직무에 맞는 핵심 기술 스택을 6~10개 내외로 제시하라.
            {
              "careerSummary": "string",
              "currentSkills": {
                "strengths": ["string"],
                "gaps": ["string"]
              },
              "coursePlan": [
                {
                  "period": "string",
                  "goal": "string",
                  "courses": [
                    {"name": "string", "type": "string", "reason": "string", "priority": "필수/선택", "prerequisites": ["string"]}
                  ],
                  "effort": "string"
                }
              ],
              "extracurricularPlan": [
                {
                  "period": "string",
                  "goal": "string",
                  "activities": ["string"],
                  "effort": "string"
                }
              ],
              "recommendedTechStack": ["string"],
              "advice": "string",
              "generatedAt": "YYYY-MM-DD"
            }
            """.formatted(
            requestDTO.getCareerGoal(),
            requestDTO.getTranscript().getAverageGPA(),
            requestDTO.getTranscript().getTotalCredits(),
            requestDTO.getTranscript().getTotalMajorCredits(),
            buildCompletedCoursesText(requestDTO),
            termContext
        );
    }

    private String buildCompletedCoursesText(RoadmapCreateRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.getTranscript() == null) {
            return "- 없음";
        }
        List<RoadmapCreateRequestDTO.Course> courses = requestDTO.getTranscript().getCourses();
        if (courses == null || courses.isEmpty()) {
            return "- 없음";
        }
        StringBuilder sb = new StringBuilder();
        for (RoadmapCreateRequestDTO.Course course : courses) {
            if (course == null) {
                continue;
            }
            String code = course.getCourseCode();
            String name = course.getCourseName();
            if ((code == null || code.isBlank()) && (name == null || name.isBlank())) {
                continue;
            }
            sb.append("- ");
            if (name != null && !name.isBlank()) {
                sb.append(name.trim());
            }
            if (code != null && !code.isBlank()) {
                if (sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
                sb.append('(').append(code.trim()).append(')');
            }
            sb.append('\n');
        }
        if (sb.length() == 0) {
            return "- 없음";
        }
        return sb.toString().trim();
    }

    private String buildTermContext() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        int year = today.getYear();
        int month = today.getMonthValue();

        List<String> terms;
        if (month >= 3 && month <= 4) {
            terms = List.of(
                "%d 1학기".formatted(year),
                "%d 여름학기".formatted(year),
                "%d 2학기".formatted(year),
                "%d 겨울학기".formatted(year)
            );
        } else if (month >= 5 && month <= 6) {
            terms = List.of(
                "%d 여름학기".formatted(year),
                "%d 2학기".formatted(year),
                "%d 겨울학기".formatted(year),
                "%d 1학기".formatted(year + 1)
            );
        } else if (month >= 7 && month <= 8) {
            terms = List.of(
                "%d 2학기".formatted(year),
                "%d 겨울학기".formatted(year),
                "%d 1학기".formatted(year + 1),
                "%d 여름학기".formatted(year + 1)
            );
        } else if (month >= 9) {
            terms = List.of(
                "%d 겨울학기".formatted(year + 1),
                "%d 1학기".formatted(year + 1),
                "%d 여름학기".formatted(year + 1),
                "%d 2학기".formatted(year + 1)
            );
        } else {
            terms = List.of(
                "%d 1학기".formatted(year),
                "%d 여름학기".formatted(year),
                "%d 2학기".formatted(year),
                "%d 겨울학기".formatted(year)
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append("- 오늘 날짜: ").append(today).append('\n');
        sb.append("- 다음 학기 순서:\n");
        for (String term : terms) {
            sb.append("  - ").append(term).append('\n');
        }
        return sb.toString().trim();
    }
}
