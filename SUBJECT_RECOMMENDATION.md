# 교과목 추천 알고리즘 정리

이 문서는 과목 추천 로직을 개인 참고용으로 정리한 것입니다.  
대상 API: `/api/subjects/score`

## 전체 흐름
1) 진로/키워드로 섹터 힌트 생성  
2) 과목별 점수 계산(키워드 매칭 + 규칙 가중치)  
3) 이미 이수한 과목 제외  
4) 점수 내림차순 정렬  
5) 상위 N개 반환(기본 20개)

## 진로 힌트 생성
위치: `src/main/java/com/hackathon/project/domain/Roadmap/WeightHintService.java`

- 섹터별 키워드는 `SECTOR_KEYWORDS`에 정의됨.
- 입력 텍스트(careerText)에 섹터 키워드가 포함되면 해당 섹터만 사용.
- 매칭이 없으면 모든 섹터 키워드를 사용.
- 매칭은 대소문자 구분 없이 문자열 포함 여부로 판단.

## 점수 계산(정량 규칙)
위치: `src/main/java/com/hackathon/project/domain/Subject/SubjectRecommendationService.java`

아래 조건을 만족할 때마다 점수가 누적됩니다.

- +10: 과목명에 섹터 키워드 포함  
  - `containsAny(subject.getCourseName(), keywords)`
- +8: 선택영역 또는 이수구분에 섹터 키워드 포함  
  - `containsAny(subject.getSelectedArea(), keywords)`  
  - `containsAny(subject.getCourseType(), keywords)`
- +6: 유의사항에 섹터 키워드 또는 프로젝트 키워드 포함  
  - `containsAny(subject.getNotes(), keywords)`  
  - `containsAny(subject.getNotes(), PROJECT_KEYWORDS)`  
  - `PROJECT_KEYWORDS = ["프로젝트","실무","응용"]`
- +7: 개설/주관학과에 섹터 키워드 포함  
  - `containsAny(subject.getOfferingDepartmentMajor(), keywords)`  
  - `containsAny(subject.getHostDepartment(), keywords)`
- +4: 심화 학수번호(첫 숫자 3 또는 4)  
  - `isAdvancedCourseCode(subject.getCourseCode())`
- +4: 학년이 3학년 또는 4학년  
  - `subject.getGradeLevel() == 3 || 4`
- +5: 실습시간 >= 이론시간, 그리고 실습시간 > 0  
  - `practiceHours >= theoryHours`
- +6: 강좌유형에 실습/프로젝트 키워드 포함  
  - `containsAny(subject.getCourseFormat(), PRACTICE_KEYWORDS)`  
  - `PRACTICE_KEYWORDS = ["프로젝트","캡스톤","실험","실습"]`
- +3: 3학점 이상  
  - `subject.getCredits() >= 3.0`
- +2: 영어 강의  
  - `containsAny(subject.getLectureLanguage(), ["영어","English"])`
- +2: 사이버/혼합형 + 키워드 매칭  
  - `isOnline` 조건: `cyberLecture != null` 또는 `courseFormat`에 ["혼합","블렌디드"] 포함  
  - `containsAnyAnyField(subject, keywords)` → 과목명/선택영역/유의사항 중 하나라도 매칭
- +1: 학점교류 가능 + 키워드 매칭  
  - `creditExchangeAvailability != null`  
  - `containsAnyAnyField(subject, keywords)`

## 이수 과목 제외
- 요청에 포함된 완료 과목 코드를 `trim()` 정규화.
- 과목 코드가 일치하면 점수 계산 대상에서 제외.

## 정렬 및 반환
- 점수 내림차순 정렬.
- 동점일 경우 과목명 오름차순(널은 마지막).
- `topN`이 없거나 1 미만이면 기본값 20.

## 관련 파일
- `src/main/java/com/hackathon/project/domain/Roadmap/WeightHintService.java`
- `src/main/java/com/hackathon/project/domain/Subject/SubjectRecommendationService.java`
- `src/main/java/com/hackathon/project/domain/Subject/SubjectController.java`
