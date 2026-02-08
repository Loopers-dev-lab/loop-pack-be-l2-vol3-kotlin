E2E 테스트를 작성한다: 실제 API를 호출해 전체 흐름을 검증한다.

대상 API: $ARGUMENTS

## 절차

1. 대상 API 엔드포인트 확인 (없으면 E2E 테스트가 없는 API 탐색)
2. 전체 흐름 파악: Controller → Facade → Service → Repository
3. E2E 테스트 작성:
    - `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
    - `ApiResponse` 래퍼 고려한 응답 검증
4. 시나리오: 정상(Happy Path) + 실패 케이스
5. 테스트 실행:
   ```
   ./gradlew :apps:commerce-api:test --tests "해당테스트클래스"
   ```
6. 결과 보고

## 규칙

- Mock 사용 금지 — 실제 DB, 실제 서비스
- `@AfterEach`에서 `databaseCleanUp.truncateAllTables()`
- `@Nested` + `@DisplayName`(한국어) BDD 스타일
- VO 검증은 E2E에서 중복하지 않음 — HTTP 상태코드와 API 계약만 검증
