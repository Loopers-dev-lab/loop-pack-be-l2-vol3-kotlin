---
name: domain-modeling
description: 도메인 모델링 시 아래 규칙을 따르세요.
---

도메인 모델링 시 아래 규칙을 따르세요.

## 핵심 원칙

- 도메인 객체는 비즈니스 규칙을 캡슐화해야 한다.
- 데이터가 아닌 **행위의 주체와 책임** 중심으로 설계한다.
- 규칙이 여러 서비스에 반복되면 도메인 객체에 속할 가능성이 높다.
- 비즈니스 의미가 커질 수 있는 개념은 별도 도메인으로 분리한다.
- 각 기능의 책임과 결합도에 대해 개발자의 의도를 확인하고 개발을 진행한다.

## Entity / Value Object / Domain Service 구분

### Entity
- 식별 가능한 고유 ID를 가지며, 상태 변화가 중요하다.
- 동일성은 ID로 판단한다. 속성이 같아도 ID가 다르면 다른 객체이다.
- 시간이 지나도 연속성을 가진다.
- 도메인 규칙을 내부 메서드로 스스로 수행한다.
- 예: `User`, `Order`, `Product`

```kotlin
class User(
    val id: Long,
    private var balance: Money
) {
    fun canAfford(amount: Money): Boolean = balance.isGreaterThanOrEqual(amount)
    fun pay(amount: Money) {
        require(canAfford(amount)) { "포인트가 부족합니다." }
        balance -= amount
    }
}
```

### Value Object (VO)
- "누구인지"가 아닌 "그 값이 무엇이냐"만 중요하다.
- 불변(immutable) 특성을 가진다.
- 값 자체가 동일하면 같은 객체로 간주한다.
- 도메인 무결성 보장과 표현력을 높이는 데 사용한다.
- **Context에 따라 VO가 Entity가 될 수 있다.** 해당 도메인에서 식별/추적이 필요하면 Entity, 값 자체만 의미 있으면 VO이다. (예: Address는 이커머스에서는 VO지만, 우체국 시스템에서는 추적 대상이므로 Entity가 될 수 있다)
- 예: `Money`, `Address`, `Quantity`

```kotlin
@JvmInline
value class Money(val amount: BigDecimal) {
    init {
        require(amount >= BigDecimal.ZERO) { "금액은 0 이상이어야 합니다." }
    }
    operator fun plus(other: Money): Money = Money(this.amount + other.amount)
    operator fun minus(other: Money): Money = Money(this.amount - other.amount)
    fun isGreaterThanOrEqual(other: Money): Boolean = this.amount >= other.amount
}
```

### Domain Service
- 도메인 객체들이 직접 수행하기 어려운 도메인 로직을 위임받아 처리한다.
- **상태를 가지지 않는다.** Input과 Output이 명확하다.
- 동일한 도메인 경계 내의 객체 협력 중심으로 설계한다.
- 예: `PointChargingService`, `CouponApplyService`

> **Manager/doer 주의**: `XxxManager`, `XxxProcessor` 같은 클래스는 고유한 상태나 도메인적 의미 없이 연산만 수행하는 "행위자(doer)"일 뿐, 그 자체로 도메인 개념이 아니다. 다만 도메인 모델을 더럽히지 않고 로직을 분리해서 관리할 수 있다는 점에서 유용하다. 도메인 객체와 혼동하지 않도록 주의한다.

```kotlin
class PointChargingService {
    fun charge(user: User, amount: Money) {
        require(amount.amount > BigDecimal.ZERO) { "0원 이상만 충전할 수 있습니다." }
        user.receive(amount)
    }
}
```

## 설계 판단 기준

| 질문 | Entity | VO | Domain Service |
|------|--------|----|---------------|
| 고유 ID가 필요한가? | O | X | X |
| 상태가 변하는가? | O | X (불변) | X (상태 없음) |
| 값 비교만으로 동일성 판단 가능한가? | X | O | - |
| 여러 객체 협력이 필요한가? | - | - | O |

## 잘못된 설계 Anti-pattern

### 비즈니스 개념을 다른 Entity에 끼워 넣기

```kotlin
// ❌ 잘못된 구조 - 좋아요를 Product 안에 넣음
class Product {
    val likedUserIds = mutableSetOf<Long>()
    fun like(userId: Long) { likedUserIds.add(userId) }
}
```

- 응집도가 낮고, 좋아요 자체가 가진 의미(누가, 언제, 어떤 상품에)를 확장하기 어렵다.
- **비즈니스 의미가 커질 수 있는 개념은 별도 도메인으로 분리**해야 한다.

```kotlin
// ✅ 올바른 구조 - Like를 독립된 도메인으로 분리
class Like(
    val userId: Long,
    val productId: Long,
    val likedAt: LocalDateTime = LocalDateTime.now()
)
```

## 유스케이스 중심 객체 협력

- 복잡한 협력은 테스트 가능한 구조로 끊어서 설계한다.
- Application Layer에서 도메인 객체를 조합하여 유스케이스를 완성한다.
- 핵심 비즈니스 로직은 Entity, VO, Domain Service에 위치시킨다.
