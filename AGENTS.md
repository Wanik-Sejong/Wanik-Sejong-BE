# AGENTS.md

## 프로젝트 개요
- Spring Boot 3.5.3 + Java 21 기반 백엔드.
- 학생 이수 내역을 엑셀로 파싱하고, Gemini API로 진로/수강 로드맵을 생성.
- 과목 DB(subjects 테이블)를 기반으로 진로 키워드와 매칭해 추천 점수를 산출.

## 핵심 도메인 흐름
- 로드맵 생성: `/api/generate-roadmap` → `RoadmapService` → `GeminiService`
- 엑셀 파싱: `/api/parse-excel` → `RoadmapService.parse` (Apache POI)
- 가중치 힌트: `/api/weight-hints` → `WeightHintService`
- 과목 추천: `/api/subjects/score` → `SubjectRecommendationService` (JPA)

## 주요 설정
- 프로필: `application-dev.yml`, `application-prod.yml`
- DB: MySQL (dev 기준 `HACKATHON` DB)
- CORS 허용: `https://hackathon.yeo-li.com`, `http://localhost:3000`
- 보안: 모든 요청 허용(세션/CSRF 비활성화)

## Gemini API 연동
- `GeminiService`에서 `gemini.api-key` 사용.
- 프롬프트는 JSON 스키마만 반환하도록 강제.
- 응답에서 code fence 제거 후 JSON 파싱.
- 키는 환경변수/외부 시크릿으로 관리 권장(레포에 노출 금지).

## 엑셀 파싱 규칙
- 첫 시트 기준, 5번째 줄(인덱스 4)부터 데이터로 처리.
- 컬럼 매핑:
  - 3: 학수번호
  - 4: 교과목명
  - 5: 이수구분
  - 6: 교직영역
  - 7: 선택영역
  - 8: 학점
  - 9: 평가방식
  - 10: 등급
  - 11: 평점
  - 12: 개설학과코드

## 과목 추천 스코어링
- 키워드 매칭 + 규칙 기반 점수 합산.
- 상위 학년/프로젝트형/실습 비중 등 가중치 반영.
- `/api/weight-hints` 응답의 섹터/키워드/규칙을 기반으로 계산.
- 기본 상위 N개는 20개(`WeightHintService.DEFAULT_N`).

## 주요 엔드포인트
- `POST /api/parse-excel` (multipart)
- `POST /api/generate-roadmap` (JSON)
- `POST /api/weight-hints` (JSON)
- `POST /api/subjects/score` (JSON)

## 개발/실행
- 실행: `./gradlew bootRun`
- 테스트: `./gradlew test`
- dev 프로필: `./gradlew bootRun --args='--spring.profiles.active=dev'`
