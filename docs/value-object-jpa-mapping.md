# Value Object JPA 매핑 전략

## 개요

DDD에서 Value Object를 JPA Entity에 매핑하는 세 가지 방식을 비교 분석한다.

- **String 저장 방식**: Entity 필드는 원시 타입, Value Object는 검증용으로만 사용
- **@Embedded 방식**: Value Object를 JPA @Embeddable로 직접 매핑
- **@Converter(autoApply) 방식**: AttributeConverter로 VO ↔ 원시 타입 자동 변환

## 현재 프로젝트 구조

### Value Object (검증 로직 + 도메인 의미 캡슐화)

```kotlin
// LoginId.kt — 단일 값 VO
class LoginId private constructor(val value: String) {
    companion object {
        fun of(value: String): LoginId {
            validate(value)
            return LoginId(value)
        }
    }
}

// LikeCount.kt — 도메인 메서드를 가진 VO
class LikeCount private constructor(val value: Int) {
    companion object {
        fun of(value: Int): LikeCount {
            if (value < 0) throw CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0 이상이어야 합니다.")
            return LikeCount(value)
        }
    }
    fun increment(): LikeCount = LikeCount(value + 1)
    fun decrement(): LikeCount = if (value > 0) LikeCount(value - 1) else this
}
```

### Entity (VO 필드 타입 + AttributeConverter 자동 변환)

```kotlin
// User.kt
@Entity
class User(
    loginId: LoginId,
    password: String,
    email: Email,
    // ...
) : BaseEntity() {

    var loginId: LoginId = loginId
        protected set

    var email: Email = email
        protected set
}

// Product.kt
@Entity
class Product(
    price: Money,
    likes: LikeCount,
    stockQuantity: StockQuantity,
    // ...
) : BaseEntity() {

    var price: Money = price
        protected set

    var likes: LikeCount = likes
        protected set

    var stockQuantity: StockQuantity = stockQuantity
        protected set
}
```

### AttributeConverter (infrastructure 계층)

```kotlin
// infrastructure/user/LoginIdConverter.kt
@Converter(autoApply = true)
class LoginIdConverter : AttributeConverter<LoginId, String> {
    override fun convertToDatabaseColumn(attribute: LoginId?): String? = attribute?.value
    override fun convertToEntityAttribute(dbData: String?): LoginId? = dbData?.let { LoginId.of(it) }
}
```

## 방식 비교

### 1. String 저장 방식

```kotlin
// Entity 필드는 원시 타입, init에서 VO로 검증만 수행
var loginId: String = loginId
    protected set

init {
    LoginId.of(loginId)  // 검증만 수행, 결과는 버림
}
```

### 2. @Embedded 방식

```kotlin
// Value Object에 JPA 어노테이션 필요
@Embeddable
class LoginId private constructor(
    @Column(name = "login_id")
    val value: String
) {
    companion object {
        fun of(value: String): LoginId { /* 검증 */ }
    }
}

// Entity
@Embedded
var loginId: LoginId = LoginId.of(loginId)
    protected set
```

### 3. @Converter(autoApply = true) 방식 (현재 채택)

```kotlin
// Value Object — JPA 의존 없이 순수 도메인 객체
class LoginId private constructor(val value: String) {
    companion object {
        fun of(value: String): LoginId { /* 검증 */ }
    }
}

// Entity — VO 타입 필드, Converter 참조 없음
var loginId: LoginId = loginId
    protected set

// infrastructure 계층 — autoApply로 자동 변환
@Converter(autoApply = true)
class LoginIdConverter : AttributeConverter<LoginId, String> { /* 변환 */ }
```

## 비교 분석

| 관점 | String 저장 | @Embedded | @Converter(autoApply) |
|------|-----------|-----------|----------------------|
| **타입 안전성** | ❌ 원시 타입으로 혼동 가능 | ✅ VO 타입으로 구분 | ✅ VO 타입으로 구분 |
| **DDD 순수성** | △ 검증만 위임, 결과 버림 | △ VO에 JPA 어노테이션 오염 | ✅ VO가 JPA에 의존하지 않음 |
| **JPA 복잡도** | ✅ 단순 | ❌ AttributeOverride 필요 | ✅ autoApply로 자동 |
| **DB 스키마** | ✅ 변경 없음 | ✅ 동일 | ✅ 동일 |
| **쿼리 작성** | ✅ `where loginId = ?` | △ `where loginId.value = ?` | ✅ `where loginId = ?` (자동 변환) |
| **직렬화** | ✅ 자동 | △ 추가 설정 필요 | ✅ 자동 (Converter가 처리) |
| **레이어 의존성** | ✅ 무관 | △ domain에 JPA 어노테이션 | ✅ Converter가 infra에 위치 |
| **ArchUnit 호환** | ✅ 무관 | ✅ @Embeddable은 domain 가능 | ✅ domain → infra 의존 없음 |

## 권장 사항

### @Converter(autoApply) 방식을 권장하는 경우 (현재 프로젝트 채택)

1. **단일 값 Value Object**
   - LoginId, Email, Money처럼 하나의 값만 감싸는 경우
   - VO가 순수 도메인 객체로 유지되어야 하는 경우

2. **도메인 메서드가 있는 Value Object**
   - LikeCount.increment(), LikeCount.decrement() 등 불변 연산
   - VO를 타입으로 관리해야 도메인 로직이 자연스러운 경우

3. **레이어드 아키텍처 + DIP 준수**
   - ArchUnit 등으로 domain → infrastructure 의존을 금지하는 경우
   - VO에 JPA 어노테이션(@Embeddable)을 넣고 싶지 않은 경우

4. **타입 안전성이 중요한 경우**
   - `signUp(loginId, email)` 매개변수 순서 실수를 컴파일 타임에 방지
   - Entity 필드에서 도메인 의미가 타입으로 명확히 드러나야 하는 경우

### String 저장 방식을 권장하는 경우

1. **암호화/변환이 필요한 필드**
   - Password처럼 저장 시 인코딩이 필요한 경우
   - Value Object와 저장 값이 1:1 매핑되지 않는 경우

2. **VO 도입 비용이 이점을 초과하는 경우**
   - Converter 작성 비용 대비 타입 안전성 이점이 적은 경우

### @Embedded 방식을 권장하는 경우

1. **복합 값 Value Object**
   - Address(city, street, zipCode)처럼 여러 필드를 가지는 경우
   - 하나의 개념이 여러 컬럼에 매핑되는 경우
   - AttributeConverter는 단일 컬럼만 지원하므로 복합 값에는 @Embedded 필요

## 프로젝트 적용 결론

| 필드 | 저장 방식 | 이유 |
|------|----------|------|
| `loginId` | `LoginId` VO + Converter | 타입 안전성, 매개변수 혼동 방지 |
| `email` | `Email` VO + Converter | 타입 안전성, 매개변수 혼동 방지 |
| `password` | `String` | 암호화된 값 저장, VO와 1:1 매핑 불가 |
| `price` | `Money` VO + Converter | 금액 음수 방지, 연산자 오버로딩(+, *) 지원 |
| `likes` | `LikeCount` VO + Converter | increment()/decrement() 도메인 메서드 보유 |
| `stockQuantity` | `StockQuantity` VO + Converter | 재고 음수 방지, Quantity 감산 연산자 지원 |
| `quantity` | `Quantity` VO + Converter | 주문 수량 0 이하 방지, Money 곱셈 연산 지원 |
| `totalAmount` | `Money` VO + Converter | 주문 총액 자동 계산, Money 덧셈 연산 활용 |

현재 구조는 다음 DDD 목적을 달성한다:

- ✅ 도메인 규칙이 Value Object에 캡슐화됨
- ✅ Entity 필드가 VO 타입으로 도메인 의미를 명확히 표현
- ✅ 컴파일 타임에 타입 안전성 보장 (매개변수 혼동 방지)
- ✅ VO가 JPA에 의존하지 않는 순수 도메인 객체
- ✅ ArchUnit 아키텍처 테스트 통과 (domain → infrastructure 의존 없음)
- ✅ Converter가 infrastructure 계층에 위치하여 DIP 준수

## 의사 결정 흐름

```
VO가 여러 컬럼에 매핑되는가?
├─ Yes → @Embedded (예: Address)
└─ No → VO와 저장 값이 1:1 매핑되는가?
    ├─ No → String 저장 (예: Password — 인코딩 필요)
    └─ Yes → @Converter(autoApply = true) (예: LoginId, Money, LikeCount)
```

## 참고

- [Vaughn Vernon - Implementing Domain-Driven Design](https://www.amazon.com/Implementing-Domain-Driven-Design-Vaughn-Vernon/dp/0321834577)
- [JPA @Embeddable vs AttributeConverter](https://thorben-janssen.com/jpa-attribute-converter/)
- [Hibernate AttributeConverter autoApply](https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#basic-jpa-convert)
