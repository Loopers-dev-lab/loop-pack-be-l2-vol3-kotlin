# Phase: Test Review

테스트 코드 자체의 신뢰성을 검증하는 페이즈.

> "테스트가 항상 통과한다면 테스트가 아니다." — Kent Beck

## review와의 차이

| 관점 | review | test-review |
|------|--------|-------------|
| 대상 | 프로덕션 코드 | 테스트 코드 |
| 초점 | 단순함, 아키텍처, 버그 | 테스트 신뢰성, 검증 완전성 |
| 검증 | 4레이어 위반, 컨벤션 | assertion 품질, Fake 활용 |

## 입력

`$ARGUMENTS`가 주어지면 해당 테스트 파일/패키지를 검증한다.
주어지지 않으면 `git diff --name-only`로 변경된 테스트 파일을 검증한다.

## 절차

1. **대상 수집**: 테스트 파일 목록 확인
2. **자동 검증**: 체크리스트 기준으로 각 파일 분석
3. **결과 종합**: 심각도별 리포트 생성

## 검증 체크리스트

### CRITICAL — 테스트가 실제로 검증하지 않음

- [ ] **Always-passing test**: assert 없이 예외만 안 나면 통과
- [ ] **Tautology test**: 입력을 그대로 기대값으로 사용 (`result == input`)
- [ ] **Mockito 사용**: 단위 테스트에서 Mockito 사용 (Fake Repository로 대체해야 함)

### WARNING — 검증이 불완전함

- [ ] **Weak assertion**: `assertNotNull`만 검증하고 실제 값 미확인
- [ ] **Partial verification**: 핵심 필드 누락
- [ ] **Missing state change verification**: 상태 변경 후 결과 미검증
- [ ] **@SpringBootTest in unit test**: 단위 테스트에 @SpringBootTest 사용 (도메인 로직은 외부 의존성 없이 검증)

### INFO — 개선 가능

- [ ] **Test name unclear**: @DisplayName이 검증 의도를 드러내지 않음
- [ ] **Missing edge case**: 경계값/에러 케이스 누락
- [ ] **Data setup duplication**: 테스트 데이터 설정 코드 중복
- [ ] **Missing 3A structure**: Arrange → Act → Assert 구조가 명확하지 않음

## commerce-api 테스트 패턴 규칙

### 단위 테스트

```kotlin
class SomeUseCaseTest {

    // Fake Repository 사용 (Mockito 금지)
    private val fakeRepository = FakeSomeRepository()
    private val sut = SomeUseCase(fakeRepository)

    @Nested
    @DisplayName("어떤 기능을")
    inner class SomeFeature {

        @Test
        @DisplayName("정상 조건이면 성공한다")
        fun success() {
            // Arrange
            val command = SomeCommand(...)
            fakeRepository.save(someEntity)

            // Act
            val result = sut.execute(command)

            // Assert
            assertThat(result.field).isEqualTo(expected)
        }
    }
}
```

### 통합 테스트

```kotlin
@SpringBootTest
class SomeIntegrationTest {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }
}
```

### E2E 테스트

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SomeE2ETest {
    // TestRestTemplate 사용
    // TestContainers 자동 구동 (프로파일: test)
}
```

## 출력 형식

```
## [파일명]
### [CRITICAL] 클래스 > @Nested > 메서드명 — 요약
- 문제: 구체적 설명
- 제안: 수정 방법
```

마지막에 전체 요약 (CRITICAL/WARNING/INFO 개수 + 통과 파일 수) 포함.
