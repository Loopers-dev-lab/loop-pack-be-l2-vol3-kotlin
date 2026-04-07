# 🧭 루프팩 BE L2 - Round 1

> 단순히 기능을 구현하는 게 아니라, **의도를 설계한다.**
>
>
> 그리고 그 의도는 이제, **AI와 함께 구현된다.**
>

<aside>
🎯

**Summary**

</aside>

- 기능 구현보다 먼저 테스트 코드를 작성해본다.
- 테스트 가능한 구조란 무엇인지 체감해본다.
- 유저 등록/조회/비밀번호 변경 등을 테스트 주도로 구현해본다.

<aside>
📌

**Keywords**

</aside>

- 단위 테스트 vs 통합 테스트
- 테스트 더블(Mock, Stub, Fake 등)
- 테스트 가능한 코드 구조
- 테스트 주도 개발 (TDD)

<aside>
🧠

**Learning**

</aside>

# 🌍 **개발 환경의 변화**

## 🤖 최근 소프트웨어 개발 풍조: LLM · Agent-based 개발

<aside>
💡 최근 개발 현장에서는 더 이상 **“코드를 직접 얼마나 잘 치느냐”**가 생산성의 병목이 아닙니다.
LLM 은 이미 기계적으로 해석 가능한, 반복적인 행동들을 사람보다 빠르게 만들어내고 있습니다.
Agent 기반 도구들은 `요구사항` 을 작업 단위로 나누고, 실패하면 원인을 추론해 다시 시도하려고 하며 테스트 결과를 기준으로 코드를 수정하고 있습니다.

**사람이 AI 의 보조를 받아 직접 구현하던 시대**는 빠르게 지나고, **사람이 의도를 정의하고 AI 가 구현하는 시대**로 넘어왔습니다.

</aside>

### ⚠️ 문제는 속도 가 아니라 통제 다

<aside>
💡 AI 는 코드를 빠르게 생성할 수 있고, 다량의 문서를 빠르게 읽어낼 수 있습니다.
하지만 **우리 비즈니스의 특성, 코드의 의도, 변경영향과 책임** 등에 대해서는 지속적으로 컨텍스트를 유지할 수 없습니다.

이에, 명확하게 코드에 대한 의도를 분명히 하고 달리는 **자동차의 바퀴를 갈아끼울 수 있도록** 안내하고 조율할 수 있어야 합니다.

</aside>

### 🧠 개발자의 역할은 어떻게 바뀌고 있는가

- Agent 를 더 효율적인 생산성과 광범위한 역할의 확장을 위해 잘 활용해 비즈니스의 성장을 가속화
- 사내의 인프라 및 필요한 기술에 대한 구현 비용을 축소, 가능성을 확장하고 이를 잘 통제

### 참고

["그만 쓸게요" 카카오도 '결단'…AI 등장에 비명 쏟아진 곳](https://n.news.naver.com/article/015/0005240907?sid=105)

### 🤖 Claude Code

<aside>
💡

**Loopers** 에서는 **Claude Code** 를 기반으로 과정을 진행합니다.

</aside>

- 많은 AI 도구들이 존재하지만, [Claude Code](https://claude.com/product/claude-code) 는 근래 가장 많이 사용되고 있는 Antrophic 이 개발한 터미널 기반의 **에이전트 코딩 도구**
- CLI 환경에서 동작하므로 bash / git / project 등에도 제약없이 접근이 가능
- Claude Code 가 제공하는 기능
    - **개발자가 제공한 프롬프트** → 해석 → 계획 수립 → 코드 작성 → 동작 확인
    - **버그에 관련한 프롬프트** → 코드 베이스 분석 및 문제 식별, 수정
    - **코드베이스 탐색** → 전체 프로젝트 구조를 인식하고, 탐색 가능
    - **MCP 활용** → 필요한 도구들과의 통합을 제공하고, 원하는 동작을 자동화

---

# 💯 테스트

## 🧪 테스트 피라미드

> 테스트는 아래처럼 **범위에 따라 역할과 책임이 나뉘며**,
하단일수록 빠르고 많이, 상단일수록 느리지만 신중하게 구성됩니다.
>

![Untitled](attachment:54f631d6-538a-44fa-8358-026c73efed68:Untitled.png)

### 🧱 1. **단위 테스트 (Unit Test)**

- **대상:** 도메인 모델 (Entity, VO, Domain Service)
- **목적:** 순수 로직의 정합성과 규칙 검증
- **환경:** Spring 없이 순수 JVM에서 실행 (JVM 단위 테스트) / **테스트 대역** 을 활용해 모든 의존성을 대체
- **기술:** JUnit5, Kotest, AssertJ 등

> 💬 예: 포인트 충전 시 최대 한도 초과 여부를 검증하는 테스트
>

### 🔁 2. **통합 테스트 (Integration Test)**

- **대상:** 애플리케이션의 Service, Facade 등 계층 로직
- **목적:** 여러 컴포넌트(Repo, Domain, 외부 API Stub)가 연결된 상태에서 **비즈니스 흐름 전체를 검증**
- **환경:** `@SpringBootTest`, 실제 Bean 구성, Test DB
- **기술:** SpringBootTest + H2 + TestContainers 등

> 💬 예: 실제 포인트가 충전되고, DB에 반영되며, 이벤트가 발행되는 전 과정을 검증
>

### 🌐 3. **E2E 테스트 (End-to-End Test)**

- **대상:** 전체 애플리케이션 (Controller → Service → DB)
- **목적:** 실제 HTTP 요청 단위 시나리오 테스트
- **환경:** `MockMvc` 또는 `TestRestTemplate`을 통해 실제 API 요청 시뮬레이션
- **기술:** SpringBootTest + `@AutoConfigureMockMvc`, `WebTestClient` 등

> 💬 예: 사용자가 회원가입 → 포인트 충전 → 주문 흐름을 HTTP 요청으로 수행했을 때의 결과 확인
>

---

## 🔧 테스트 더블(Test Doubles)

> 테스트 대상이 의존하는 외부 객체의 동작을 **빠르고 안전하게 흉내 내는 대역 객체** 입니다.
느리고 불안정한 실제 구현 대신, 테스트 환경에 맞는 **‘조용한 대역’** 을 세워줍니다.
>

### 🧩 테스트 더블은 역할, `mock()`과 `spy()`는 도구

- `Stub`, `Mock`, `Spy`, `Fake` 는 **테스트 목적 (역할)**
- `mock()`, `spy()`는 **객체 생성 방식 (도구)**

e.g.

```kotlin
val repo = mock<UserRepository>() // 도구: mock()
whenever(repo.findById(1L)).thenReturn(User(...)) // 역할: Stub
verify(repo).findById(1L) // 역할: Mock
```

> ✅ mock 객체에 stub + mock 역할을 동시에 부여할 수 있습니다.
>

### 📚 TestDouble 역할별 정리

| 역할 | 목적 | 사용 방식 | 예시 |
| --- | --- | --- | --- |
| **Dummy** | 자리만 채움 (사용되지 않음) | 생성자 등에서 전달 | `User(null, null)` |
| **Stub** | 고정된 응답 제공 (상태 기반) | `when().thenReturn()` | `repo.find()` → 항상 특정 유저 반환 |
| **Mock** | 호출 여부/횟수 검증 (행위 기반) | `verify(...)` | 함수가 실행되었는지 검증 |
| **Spy** | 진짜 객체 감싸기 + 일부 조작 | `spy()` + `doReturn()` | 진짜 서비스 감싸고 일부만 stub |
| **Fake** | 실제처럼 동작하는 가짜 구현체 | 직접 클래스 구현 | **InMemoryUserRepository** |

### 🔁 TestDouble 실전 예제

### 📦 Stub 예제

```kotlin
val userRepo = mock<UserRepository>()
whenever(userRepo.findById(1L)).thenReturn(User("alen"))
```

- 흐름만 통제하고 싶은 경우
- “이렇게 호출하면, 이렇게 응답해줘”

### 📬 Mock 예제

```kotlin
val speaker = mock<Speaker>()
speaker.say("hello")
verify(speaker, times(1)).say("hello")
```

- 호출 여부가 검증 대상
- “너 이렇게 동작했니?”

### 🕵️ Spy 예제

```kotlin
val friend = Friend()
val spyFriend = spy(friend)
spyFriend.hangout()
verify(spyFriend).hangout()
```

- 진짜 객체처럼 동작하면서 일부만 조작
- "로직은 그대로 쓰고, 특정 동작만 덮어씌우고 / 검증하고 싶다"

### 🧪 Fake 예제

```kotlin
class InMemoryUserRepository : UserRepository {
    private val data = mutableMapOf<Long, User>()

    override fun save(user: User) { data[user.id] = user }
    override fun findById(id: Long): User? = data[user.id]
}
```

- 실제 DB 없이 테스트 가능한 저장소 구현
- "완전히 독립적인 테스트 환경이 필요할 때”

---

## 🧱 테스트 가능한 구조

> **검증하고 싶은 로직을, 외부 의존성과 격리된 상태에서 단독으로 검증할 수 있는 구조**입니다.
>
>
> 테스트 가능한 구조란, 검증하고 싶은 코드만 정확히 꺼내서 **조용하고 단단하게 확인할 수 있는 구조**다.
>

### ❌ 테스트하기 어려운 구조의 특징

| 문제 | 설명 |
| --- | --- |
| **내부에서 의존 객체 직접 생성 (`new`)** | 테스트 대역으로 대체 불가 → 테스트 격리 불가능 |
| **하나의 함수가 너무 많은 책임** | 테스트 대상이 모호해짐 → 실패 원인 추적 어려움 |
| **외부 API 호출, DB 접근 등이 하드코딩** | 실제 환경 없이 테스트 불가능 → 느리고 불안정 |
| **private 로직, static 메서드 남용** | 외부에서 로직 분리 불가 → 단위 테스트 불가 |

### ✅ 테스트 가능한 구조로 변경

| 포인트 | 설명 |
| --- | --- |
| **외부 의존성 분리** | 인터페이스화 + 생성자 주입(DI) |
| **비즈니스 로직 분리** | 도메인 엔티티 or 전용 Service에서 책임 분산 |
| **책임 단일화** | 한 함수는 한 역할만 (e.g. 결제만, 재고만 등) |
| **상태 중심 설계** | “입력 → 상태 변화 → 결과” 구조로 정리 |

### 🔍 사례로 살펴보기

```kotlin
class OrderService {
    fun completeOrder(userId: Long, productId: Long) {
        val user = UserJpaRepository().findById(userId)
        val product = ProductJpaRepository().findById(productId)

        if (product.stock <= 0) throw IllegalStateException()
        product.stock--

        if (user.point < product.price) throw IllegalStateException()
        user.point -= product.price

        OrderRepository().save(Order(user, product))
    }
}
```

- 외부 의존성 직접 생성 → Mock/Fake 불가
- 도메인 로직, 상태변경, 외부 호출이 한 곳에 몰려 있음
- `OrderServiceTest` 하나로 모든 케이스 커버해야 함 → 실패 시 어디서 잘못됐는지 추적 불가

---

```kotlin
class OrderService(
    private val userReader: UserReader,
    private val productReader: ProductReader,
    private val orderRepository: OrderRepository,
) {
    fun completeOrder(command: OrderCommand) {
        val user = userReader.get(command.userId)
        val product = productReader.get(command.productId)

        product.decreaseStock()
        user.pay(product.price)

        orderRepository.save(Order(user, product))
    }
}
```

- 외부는 인터페이스로 주입 → Fake/Mock 가능
- 로직은 `user.pay()`, `product.decreaseStock()` 처럼 도메인으로 위임
- 테스트 단위별로 나눌 수 있음 → `UserTest`, `ProductTest`, `OrderServiceTest`

---

## 🔁 TDD (Test-Driven Development)

> TDD는 테스트의 순서보다
**”설계 단위를 잘게 쪼개고, 그것이 검증 가능하게 구현되었는가”**가 핵심이다.
>

### 🔄 3단계 루프: Red → Green → Refactor

```
< 반복 >
1. 실패하는 테스트 작성 (Red)
2. 통과할 최소한의 코드 작성 (Green)
3. 구조 개선 및 리팩토링 (Refactor)
```

### 🧠 그런데 꼭 테스트를 먼저 써야 할까?

| **전략** | **이름** | **설명** |
| --- | --- | --- |
| 🧪 TFD (Test First Development) | 테스트 먼저 작성 → 코드를 맞춰 구현 | 도메인/로직 중심에 적합 |
| 🏗 TLD (Test Last Development) | 코드를 먼저 작성 → 테스트는 나중에 작성 | API/계층 설계가 먼저 필요한 상황에 적합 |

### 🟢 TDD가 필요한 이유

- **요구사항을 먼저 정리할 수 있다**
- **작게 쪼개고 점진적으로 설계하게 된다**
- **인터페이스 설계가 자연스럽게 나온다**
- **리팩토링이 가능해진다**

<aside>
📚

**References**

</aside>

| 구분 | 링크 |
| --- | --- |
| 🔢 테스트 피라미드 | [Testing Pyramid - Martin Fowler](https://martinfowler.com/bliki/TestPyramid.html) |
| 🧪 JUnit5 | [JUnit5 공식 문서](https://junit.org/junit5/docs/current/user-guide/) |
| ⚙️ Mockito | [Mockito 공식 문서](https://site.mockito.org/) |
| 🧰 Mockito-Kotlin | [GitHub: mockito-kotlin](https://github.com/mockito/mockito-kotlin) |
| 🧵 Spring 테스트 | [Spring Boot Testing Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing) |

# 📝 Round 1 Quests

---

## 🧪 Implementation Quest

> 프로젝트에 자신의 브랜치를 기준으로, 개발 환경을 세팅하고 Agent 관련 설정을 작성하고 필요한 요구사항을 명확하게 개발할 수 있도록 합니다.
지정된 **단위 테스트 / 통합 테스트 / E2E 테스트 케이스**를 필수로 구현하고, 모든 테스트를 통과시키는 것을 목표로 합니다.
>

### 🤖 Claude 관련 설정

- **Claude Code 설치**

[빠른 시작 - Claude Code Docs](https://code.claude.com/docs/ko/quickstart)

- 프로젝트에 [`CLAUDE.md`](http://CLAUDE.md) 파일을 생성하고, 아래 기본 내용을 프로젝트 설정 및 본인의 스타일에 맞게 작성합니다.
    - **기술 스택 및 버전** - **CLAUDE 프롬프트를 통해 작성하기**

        ```markdown
        **Claude Code**
        > 현재의 프로젝트를 분석해 CLAUDE.md 를 만들어줘\
          gradle.properties, build.gradle.kts 등을 참고해 현재 프로젝트의 주요 기술 스택 및 버전, 모듈 구조를 포함해줘.
        ```

    - **개발 규칙** (예시)

        ```markdown
        ## 개발 규칙
        ### 진행 Workflow - 증강 코딩
        - **대원칙** : 방향성 및 주요 의사 결정은 개발자에게 제안만 할 수 있으며, 최종 승인된 사항을 기반으로 작업을 수행.
        - **중간 결과 보고** : AI 가 반복적인 동작을 하거나, 요청하지 않은 기능을 구현, 테스트 삭제를 임의로 진행할 경우 개발자가 개입.
        - **설계 주도권 유지** : AI 가 임의판단을 하지 않고, 방향성에 대한 제안 등을 진행할 수 있으나 개발자의 승인을 받은 후 수행.
        
        ### 개발 Workflow - TDD (Red > Green > Refactor)
        - 모든 테스트는 3A 원칙으로 작성할 것 (Arrange - Act - Assert)
        #### 1. Red Phase : 실패하는 테스트 먼저 작성
        - 요구사항을 만족하는 기능 테스트 케이스 작성
        - 테스트 예시
        #### 2. Green Phase : 테스트를 통과하는 코드 작성
        - Red Phase 의 테스트가 모두 통과할 수 있는 코드 작성
        - 오버엔지니어링 금지
        #### 3. Refactor Phase : 불필요한 코드 제거 및 품질 개선
        - 불필요한 private 함수 지양, 객체지향적 코드 작성
        - unused import 제거
        - 성능 최적화
        - 모든 테스트 케이스가 통과해야 함
        ```

    - **주의사항** (예시)

        ```markdown
        ## 주의사항
        ### 1. Never Do
        - 실제 동작하지 않는 코드, 불필요한 Mock 데이터를 이요한 구현을 하지 말 것
        - null-safety 하지 않게 코드 작성하지 말 것 (Java 의 경우, Optional 을 활용할 것)
        - println 코드 남기지 말 것
        
        ### 2. Recommendation
        - 실제 API 를 호출해 확인하는 E2E 테스트 코드 작성
        - 재사용 가능한 객체 설계
        - 성능 최적화에 대한 대안 및 제안
        - 개발 완료된 API 의 경우, `.http/**.http` 에 분류해 작성
        
        ### 3. Priority
        1. 실제 동작하는 해결책만 고려
        2. null-safety, thread-safety 고려
        3. 테스트 가능한 구조로 설계
        4. 기존 코드 패턴 분석 후 일관성 유지
        ```


### 🗒️ 기능 구현

**회원가입**

- **필요 정보 : { 로그인 ID, 비밀번호, 이름, 생년월일, 이메일 }**
- 이미 가입된 로그인 ID 로는 가입이 불가능함
- 각 정보는 포맷에 맞는 검증 필요 (이름, 이메일, 생년월일)
- 비밀번호는 암호화해 저장하며, 아래와 같은 규칙을 따름

    ```markdown
    1. 8~16자의 영문 대소문자, 숫자, 특수문자만 가능합니다.
    2. 생년월일은 비밀번호 내에 포함될 수 없습니다.
    ```


> 이후, 유저 정보가 필요한 모든 요청은 아래 헤더를 통해 요청
* **`X-Loopers-LoginId`** : 로그인 ID
* **`X-Loopers-LoginPw`** : 비밀번호
>

**내 정보 조회**

- **반환 정보 : { 로그인 ID, 이름, 생년월일, 이메일 }**
- 로그인 ID 는 영문과 숫자만 허용
- 이름은 마지막 글자를 마스킹해 반환

> 마스킹 문자는 `*` 로 통일
>

**비밀번호 수정**

- **필요 정보 : { 기존 비밀번호, 새 비밀번호 }**
- 비밀 번호 RULE 을 따르되, 현재 비밀번호는 사용할 수 없습니다.

> **비밀번호 RULE**
* 영문 대/소문자, 숫자, 특수문자 사용 가능
* 생년월일 사용 불가

### 🎯 Feature Suggestions

- Claude 와 협업하며 바뀌게 된 나의 역할
- 테스트 가능한 구조를 만들기 위해 한 리팩토링
- Mock, Stub, Fake 중 실제 활용 경험과 나만의 구분 기준
- TDD 방식으로 접근하거나 테스트를 작성해보며 어려웠던 점
