# Phase Red: 실패하는 테스트 작성

TDD Red Phase. 요구사항에 대한 실패하는 테스트를 하나 작성한다.

## 절차

1. **테스트 종류 판단**: 요구사항 또는 버그 유형에 따라 선택한다.
   - **단위 테스트**: 도메인 로직, Entity 검증, VO, 순수 비즈니스 규칙. 프레임워크 의존 없이 순수 테스트.
   - **통합 테스트**: Service + Repository 연동, DB 관련 로직. 실제 DB 연결 필요.
   - **E2E 테스트**: API 엔드포인트 → 전체 흐름. 웹 서버 기동 필요.

2. **기존 테스트 패턴 확인**: 동일 클래스나 인접 테스트를 Glob/Read로 확인하여 일관성을 유지한다.

3. **테스트 작성**: 3A 원칙 (Arrange → Act → Assert)을 따른다.

4. **실패 확인**: 테스트를 실행하여 실패하는지 확인한다.
   - 컴파일 에러도 실패로 인정한다 (구현 대상이 아직 없으면 당연히 컴파일 에러).

5. **보고**: Phase 완료 보고 형식으로 결과를 보고한다.
   - 테스트 클래스명, 메서드명
   - 실패 사유 (컴파일 에러 / assertion 실패 / 예외 등)
   - Green Phase에서 구현해야 할 내용 요약

## 규칙

- 한 번에 하나의 테스트만 작성한다.
- 테스트 이름은 동작을 설명하도록 작성한다.
- VO 검증은 도메인 단위 테스트에서만 — 상위 계층에서 동일 VO 규칙을 중복 테스트하지 않는다.
- `--fix` 모드: 버그를 재현하는 테스트를 작성한다. API 레벨(E2E) → 단위 순서로 범위를 좁힌다.

## 완료 보고

```
## Red Phase 완료

- **테스트**: 패키지.클래스명.메서드명
- **결과**: 실패 (컴파일 에러 / assertion 실패 / 예외)
- **다음 단계**: Green Phase — [구현해야 할 내용 1~3줄 요약]
```

---

## 이 프로젝트의 테스트 컨벤션

### 단위 테스트
- `@SpringBootTest` 사용 금지, Mockito 사용 금지
- Fake Repository(인메모리 컬렉션)를 직접 구현하여 사용

### 통합 테스트
- `@SpringBootTest`, `@AfterEach`에서 `databaseCleanUp.truncateAllTables()`

### E2E 테스트
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`

### 테스트 스타일
```kotlin
@Nested
@DisplayName("한국어로 상황 설명")
inner class ContextName {
    @Test
    fun `동작을 한국어로 설명한다`() {
        // Arrange
        // Act
        // Assert
    }
}
```

### 검증 명령
```bash
./gradlew :apps:commerce-api:test --tests "패키지.클래스명.메서드명"
```
