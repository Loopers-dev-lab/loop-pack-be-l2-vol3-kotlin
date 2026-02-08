# Value Object (VO) — 왜 쓰는가?

## 1. Value Object란?

Value Object는 **값 자체로 의미를 가지는 객체**다. 도메인 주도 설계(DDD)에서 나온 개념으로, 식별자(ID) 없이 **값이 같으면 같은 객체**로 취급한다.

핵심 특성:

- **불변(Immutable)**: 생성 후 값이 변하지 않는다
- **자가 검증(Self-Validating)**: 생성 시점에 유효성을 스스로 검증한다
- **동등성(Equality by Value)**: ID가 아닌 값으로 비교한다

## 2. VO 없이 코딩하면 어떻게 되는가?

### 문제 상황: 모든 필드가 String

```kotlin
class User(
    val loginId: String,
    val password: String,
    val name: String,
    val email: String,
)
```

이렇게 하면 발생하는 문제들:

#### 문제 1: 검증 로직이 여기저기 흩어진다

```kotlin
// UserService.kt
fun register(loginId: String, ...) {
    if (loginId.length < 4 || loginId.length > 16) throw ...
    if (!loginId.matches(Regex("^[a-zA-Z0-9]+$"))) throw ...
    // ...
}

// UserController.kt
fun updateLoginId(loginId: String) {
    if (loginId.length < 4 || loginId.length > 16) throw ...  // 또 같은 검증
    // ...
}

// AdminService.kt
fun createUser(loginId: String, ...) {
    // 여기서는 검증을 깜빡했다 → 버그!
}
```

**결과**: 같은 검증이 3곳에 복붙되고, 한 곳은 빠지고, 나중에 규칙이 바뀌면 전부 찾아서 고쳐야 한다.

#### 문제 2: 타입으로 실수를 잡을 수 없다

```kotlin
fun register(loginId: String, password: String, name: String, email: String)

// 호출할 때 순서를 바꿔도 컴파일 에러가 안 난다!
register(email, password, loginId, name)  // 컴파일 OK, 런타임 버그
```

모든 게 `String`이라 **컴파일러가 잘못된 사용을 잡아줄 수 없다.**

#### 문제 3: 도메인 지식이 코드에 없다

"로그인 ID는 4~16자 영숫자"라는 비즈니스 규칙이 Service나 Controller에 흩어져 있으면, **도메인 규칙을 이해하려면 프로젝트 전체를 뒤져야 한다.**

---

## 3. VO를 쓰면 어떻게 달라지는가?

### 실제 프로젝트 코드 (LoginId VO)

```kotlin
class LoginId(val value: String) {
    companion object {
        private const val MIN_LENGTH = 4
        private const val MAX_LENGTH = 16
    }

    init {
        if (!Regex("^[a-zA-Z0-9]+$").matches(value)) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 영문 및 숫자만 허용됩니다.")
        }
        if (value.length < MIN_LENGTH) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 ${MIN_LENGTH}~${MAX_LENGTH}자여야 합니다.")
        }
        if (value.length > MAX_LENGTH) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 ${MAX_LENGTH}자를 초과할 수 없습니다.")
        }
    }
}
```

### 이점 1: 생성 자체가 검증이다

```kotlin
val loginId = LoginId("ab")  // 즉시 예외 발생 — 잘못된 값이 존재할 수 없다
val loginId = LoginId("validUser01")  // 통과 — 이 객체가 존재하면 유효한 값이 보장된다
```

`LoginId` 객체가 **존재한다는 것 자체가 유효한 값**임을 보장한다. 더 이상 "이 String이 검증된 건지 안 된 건지" 고민할 필요가 없다.

### 이점 2: 검증 로직이 단 한 곳에만 존재한다

```kotlin
// Service, Controller, 어디서든 그냥 VO를 만들면 된다
LoginId(rawInput)  // 검증은 VO 내부에서 알아서 한다

// 규칙 변경? LoginId.kt 하나만 수정하면 끝
```

### 이점 3: 타입 안전성

```kotlin
fun register(loginId: LoginId, password: Password, name: Name, email: Email)

// 이제 순서를 바꾸면 컴파일 에러!
register(email, password, loginId, name)  // 컴파일 에러 발생!
```

### 이점 4: 도메인 로직의 자연스러운 위치

```kotlin
class Name(val value: String) {
    // 검증 로직 (init 블록)
    init { ... }

    // 이름과 관련된 비즈니스 로직이 여기에 있는 게 자연스럽다
    fun masked(): String {
        if (value.length == 1) return "*"
        return value.dropLast(1) + "*"
    }
}
```

`masked()`는 "이름을 마스킹한다"는 비즈니스 로직이다. 이게 Service에 있는 것보다 **Name 객체 안에 있는 게 훨씬 자연스럽다.** 이름과 관련된 건 Name에게 물어보면 된다.

---

## 4. 실제 프로젝트에서의 사용 패턴

### User 엔티티에서의 활용

```kotlin
@Entity
class User(
    loginId: String,
    password: String,
    name: String,
    birthDate: LocalDate,
    email: String,
) : BaseEntity() {

    // DB에는 String으로 저장
    @Column(name = "login_id", nullable = false, unique = true, length = 16)
    var loginId: String = loginId
        protected set

    init {
        // 생성 시점에 VO로 검증
        LoginId(loginId)
        Name(name)
        Email(email)
        Password(password, birthDate)
        this.password = encodePassword(password)
    }

    fun changePassword(newPassword: String) {
        // 변경 시점에도 VO로 검증
        Password(newPassword, birthDate)
        this.password = encodePassword(newPassword)
    }
}
```

**패턴 정리:**

- Entity의 필드는 `String`으로 유지 (JPA 호환성)
- 값의 **생성/변경 시점**에 VO를 생성하여 검증
- VO는 검증 후 버려지는 일회용 게이트키퍼 역할

---

## 5. 프로젝트 내 VO 목록과 각각의 규칙

| VO         | 검증 규칙                                      | 비즈니스 로직                 |
|------------|--------------------------------------------|-------------------------|
| `LoginId`  | 4~16자, 영문+숫자만                              | -                       |
| `Email`    | 이메일 형식 (`xxx@xxx.xxx`)                     | -                       |
| `Name`     | 1~10자, 한글+영문만, 빈값 불가                       | `masked()` — 마지막 글자 마스킹 |
| `Password` | 8~16자, 영문+숫자+특수문자, 동일문자 3연속 불가, 생년월일 포함 불가 | -                       |

---

## 6. 정리 — VO를 쓰는 이유 한 줄 요약

| 관점            | VO 없이 (String)                 | VO 사용            |
|---------------|--------------------------------|------------------|
| **검증 위치**     | Service, Controller 등 여러 곳에 분산 | VO 클래스 단 한 곳     |
| **유효성 보장**    | "이 String 검증됐나?" 매번 의심         | 객체가 존재하면 유효함이 보장 |
| **타입 안전성**    | 모든 게 String → 순서 실수 가능         | 타입이 다르면 컴파일 에러   |
| **도메인 로직 위치** | Service에 흩어짐                   | 관련 VO 안에 응집      |
| **규칙 변경**     | 모든 사용처를 찾아 수정                  | VO 하나만 수정        |
| **코드 가독성**    | 의도 파악 어려움                      | 타입명 자체가 의도 표현    |

> **"원시 타입(String, Int)으로 도메인 개념을 표현하지 마라. 도메인 개념에는 이름을 붙여라."**
>
> 이것이 Value Object의 핵심이다.
