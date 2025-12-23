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
            
            출력 형식:
            - 아래 JSON 스키마에 맞춰 순수 JSON 문자열만 반환하라.
            - 코드블록, 설명 문장, 주석을 포함하지 말 것.
            - [이수 과목]에 있는 과목은 coursePlan.courses에 포함하지 말 것.
            - coursePlan에는 교과목 추천만, extracurricularPlan에는 교외활동/자기주도 학습만 작성하라.
            - period는 [학기 기준]의 순서를 그대로 사용하라.
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
