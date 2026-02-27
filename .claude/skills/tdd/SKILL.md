---
name: tdd
description: TDD 방식으로 개발할 때 아래 워크플로우와 테스트 규칙을 따르세요.
---

TDD 방식으로 개발할 때 아래 워크플로우와 테스트 규칙을 따르세요.

## TDD (Red > Green > Refactor)

### 1. Red Phase: 실패하는 테스트 먼저 작성
- 요구사항을 만족하는 기능 테스트 케이스 작성
- 컴파일 에러가 아닌 실제 실패하는 테스트 목표

### 2. Green Phase: 테스트를 통과하는 코드 작성
- Red Phase의 테스트가 모두 통과할 수 있는 최소한의 코드 작성
- 오버엔지니어링 금지

### 3. Refactor Phase: 불필요한 코드 제거 및 품질 개선
- 불필요한 private 함수 지양, 객체지향적 코드 작성
- unused import 제거
- 성능 최적화
- 모든 테스트 케이스가 통과해야 함

## 테스트 구조 (3A 원칙)

모든 테스트는 **Arrange - Act - Assert** 로 작성하고 주석으로 구분한다.

```kotlin
@Test
fun createsOrder_whenValidRequest() {
    // arrange
    val user = User(id = 1L, balance = Money(BigDecimal(10000)))
    val product = Product(id = 1L, name = "상품", stock = 10)

    // act
    val result = orderService.createOrder(user, listOf(product to 2))

    // assert
    assertAll(
        { assertThat(result.userId).isEqualTo(1L) },
        { assertThat(result.items).hasSize(1) },
    )
}
```

## 테스트 종류별 구분

| 종류 | 파일명 패턴 | 특징 |
|------|-----------|------|
| 도메인 모델 단위 | `{도메인}Test` | 어노테이션 없음, 순수 인스턴스화 |
| 서비스 단위 (Mock) | `{서비스}Test` | `@ExtendWith(MockitoExtension::class)` |
| 서비스 통합 | `{서비스}IntegrationTest` | `@SpringBootTest` + Testcontainers |
| API E2E | `{컨트롤러}E2ETest` | `@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate` |

## 네이밍 컨벤션

### 메서드명: 영어 camelCase `결과_when조건`

```kotlin
fun createsOrder_whenValidRequest()
fun throwsException_whenStockIsInsufficient()
fun returnsProductInfo_whenValidIdProvided()
```

### @DisplayName: 한글 설명

```kotlin
@DisplayName("정상적인 요청이면, 주문이 생성된다.")
@Test
fun createsOrder_whenValidRequest() { ... }
```

### @Nested: 기능별 그룹핑

```kotlin
@DisplayName("주문을 생성할 때,")
@Nested
inner class CreateOrder {

    @DisplayName("정상적인 요청이면, 주문이 생성된다.")
    @Test
    fun createsOrder_whenValidRequest() { ... }

    @DisplayName("재고가 부족하면, 예외가 발생한다.")
    @Test
    fun throwsException_whenStockIsInsufficient() { ... }
}
```

## 단위 테스트 패턴

### 도메인 모델 단위 테스트 (어노테이션 없음)

```kotlin
class MemberTest {
    @DisplayName("회원을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("정상적인 정보가 주어지면, 회원이 생성된다.")
        @Test
        fun createsMember_whenValidInfoProvided() {
            // arrange
            val loginId = "testuser1"

            // act
            val member = Member(loginId = loginId, ...)

            // assert
            assertThat(member.loginId).isEqualTo(loginId)
        }
    }
}
```

### 서비스 단위 테스트 (Mockito)

```kotlin
@ExtendWith(MockitoExtension::class)
class MemberServiceTest {
    @Mock
    private lateinit var memberRepository: MemberRepository

    @InjectMocks
    private lateinit var memberService: MemberService

    @Test
    fun findsMember_whenValidId() {
        // arrange
        whenever(memberRepository.findById(1L)).thenReturn(member)

        // act
        val result = memberService.getMember(1L)

        // assert
        assertThat(result.id).isEqualTo(1L)
    }
}
```

## 예외 테스트 패턴

`assertThrows`로 캡처 후 `errorType` 검증:

```kotlin
@Test
fun throwsException_whenStockIsInsufficient() {
    // arrange
    val product = Product(stock = 0)

    // act
    val result = assertThrows<CoreException> {
        product.decreaseStock(1)
    }

    // assert
    assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
}
```

E2E 테스트에서는 HTTP 상태 코드로 검증:

```kotlin
assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
```

## 다중 검증

여러 필드 검증 시 `assertAll`로 묶어서 하나가 실패해도 모두 검증:

```kotlin
assertAll(
    { assertThat(result.id).isNotNull() },
    { assertThat(result.name).isEqualTo("상품") },
    { assertThat(result.stock).isEqualTo(8) },
)
```

## 예외/경계 케이스 필수 포함

모든 핵심 도메인 로직에 대해 다음 케이스를 반드시 포함한다:
- 정상 흐름 (happy path)
- 예외 흐름 (재고 부족, 포인트 부족, 중복 등)
- 경계값 (재고 0, 금액 0, 최대/최소값)

## 테스트 격리

### 통합/E2E 테스트 DB 초기화

```kotlin
@AfterEach
fun tearDown() {
    databaseCleanUp.truncateAllTables()
}
```

### 테스트 상수는 companion object에 선언

```kotlin
companion object {
    private const val TEST_LOGIN_ID = "testuser1"
    private const val TEST_PASSWORD = "Password1!"
}
```

## Fake/Stub 활용 방침

- 서비스 단위 테스트: **Mockito** (`@Mock`, `whenever`)
- 도메인 단위 테스트: Mock 없이 **순수 인스턴스화**
- 통합/E2E 테스트: Mock 없이 **실제 빈** + Testcontainers
- 도메인 로직의 독립적 검증이 필요하면 **Fake/InMemory Repository** 도입 가능
