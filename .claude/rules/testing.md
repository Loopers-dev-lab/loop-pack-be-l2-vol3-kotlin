---
paths:
  - "apps/commerce-api/src/test/**/*.kt"
---

# 테스트 규칙

## TDD 진행 방식

**기본 사이클**: Red → Green → Refactor

**흐름** (OUT TO IN + Inside-Out 병행):
1. **Domain Unit Test** - 규칙 먼저 고정 (Password, Email, BirthDate 등)
2. **Controller Test** - 요청/응답 계약 정의
3. **UseCase Test** - 중재/조합 로직
4. **E2E Test** - HTTP 플로우

**새 기능 구현 시 제안 순서**:
1. Domain Unit Test 목록
2. Controller Test 시나리오
3. UseCase Test 시나리오
4. E2E Test 시나리오 (Given/When/Then)

## 테스트 피라미드 역할

| 레벨 | 대상 | 특징 |
|------|------|------|
| Unit | 도메인 규칙, 값 객체, 순수 로직 | 외부 의존 없음, 빠름 |
| Integration | UseCase + Repository, 트랜잭션 | DB 연동, Testcontainers |
| E2E | HTTP 레벨 시나리오 | 전체 흐름 검증 |

테스트 작성 시 각 케이스가 어느 레벨에 있어야 적절한지 함께 설명.

## Mock 사용 기준

**원칙**: 격리 목적이 명확할 때만 사용

| 테스트 유형 | Mock 사용 |
|------------|----------|
| Domain Unit | ❌ 사용하지 않음 |
| UseCase Integration | ⭕ 외부 의존성 격리 시 |
| E2E | ❌ 사용하지 않음 |

- UseCase 테스트에서 Repository 등 외부 의존성 격리할 때 주로 사용
- Domain 테스트는 순수 로직이므로 Mock 불필요
- E2E는 실제 흐름 검증이므로 Mock 사용 X

## 단언문 규칙

**원칙**: 테스트당 **검증 대상(행위) 1개**

- 하나의 테스트는 하나의 행위(behavior)만 검증한다
- 그 행위의 결과를 완전히 검증하기 위해 `assertAll()`로 여러 assert를 쓰는 것은 정상
- 관련 없는 부수적 검증(message, cause 등)은 넣지 않음
- 각 테스트가 무엇을 검증하는지 한 문장으로 설명 가능해야 함

```kotlin
// ✅ 좋음 - 하나의 행위, 결과를 완전히 검증
@Test
fun `정상 주문이면 주문이 생성된다`() {
    val result = createOrderUseCase.execute(command)

    assertAll(
        { assertThat(result.totalAmount).isEqualTo(Money(30000)) },
        { assertThat(result.status).isEqualTo(OrderStatus.ORDERED) },
        { assertThat(result.itemCount).isEqualTo(2) },
    )
}

// ✅ 좋음 - 단일 assert로 충분한 경우
@Test
fun `로그인ID에 특수문자가 포함되면 INVALID_LOGIN_ID_FORMAT 에러가 발생한다`() {
    val exception = assertThrows<CoreException> {
        User.create(loginId = "test!", ...)
    }
    assertThat(exception.errorCode).isEqualTo(UserErrorCode.INVALID_LOGIN_ID_FORMAT)
}

// ❌ 나쁨 - 관련 없는 검증 포함
@Test
fun failWhenLoginIdContainsSpecialCharacter() {
    val exception = assertThrows<CoreException> { ... }
    assertThat(exception.errorCode).isEqualTo(...)
    assertThat(exception.message).isNotBlank()  // 불필요
    assertThat(exception.cause).isNull()        // 불필요
}

// ❌ 나쁨 - 여러 행위를 하나의 테스트에
@Test
fun `주문 생성과 취소`() {
    val order = createOrder(...)     // 행위 1
    cancelOrder(order.id)            // 행위 2 — 별도 테스트로 분리해야 함
}
```

## 파일명 패턴

| 유형 | 패턴 | 예시 |
|------|------|------|
| 단위 | `{Class}Test.kt` | `UserTest.kt` |
| 통합 | `{UseCase}Test.kt` | `RegisterBrandUseCaseTest.kt` |
| E2E | `{Api}E2ETest.kt` | `UserV1ApiE2ETest.kt` |

## 테스트 구조

```kotlin
class UserTest {

    @DisplayName("유저 생성")
    @Nested
    inner class Create {

        @DisplayName("정상 입력이면 성공한다")
        @Test
        fun success() {
            // arrange
            // act
            // assert (행위 1개에 대해 완전 검증)
        }
    }
}
```

## 테스트 격리

- `@AfterEach`에서 `databaseCleanUp.truncateAllTables()`
- Testcontainers 사용

## 테스트 더블 사용 기준

| 종류 | 사용 시점 |
|------|----------|
| Dummy | 필요 없는 인자 채우기만 |
| Stub | 고정된 응답 필요 (상태 기반 검증) |
| Mock | 호출 여부/횟수 검증 (행위 기반 검증) |
| Fake | 인메모리 구현, 실제와 비슷하지만 가벼운 대역 |

도메인 로직은 가능하면 Fake/Stub로 빠르고 안정적으로 검증.
