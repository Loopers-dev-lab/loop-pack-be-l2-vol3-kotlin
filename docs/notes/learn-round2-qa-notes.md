# Round 2 학습 노트 — 도메인 모델링 / Entity·VO·Domain Service / 레이어드 + DIP

## 학습 개요
- 학습 일자: 2026-04-12
- 라운드 주제: 도메인 모델링, Entity vs Value Object vs Domain Service, 유스케이스 중심 객체 협력, 레이어드 아키텍처 + DIP, 단위 테스트 기반 구현
- 참조 문서: docs/quests/round-2.md
- 진행 방식: 하이브리드 (개념 질문 → 프로젝트 실제 코드로 검증)

---

## Q1. 도메인 분리의 판단 기준 — 왜 `Like`를 독립시키는가

**[질문]**
발제의 첫 예시를 보면, `Product.likeCount++`라는 단순 카운터 구조에서 `Like`를 **독립된 도메인 개념**으로 분리했습니다.

두 단계로 답해 주세요.

(1) 발제에서는 "누가 어떤 걸 언제 눌렀는지 추적할 필요" 때문이라고 짧게 설명하는데, 이걸 당신의 언어로 다시 풀어보면 **진짜 이유**는 뭐라고 생각하세요?

(2) 그리고 더 중요한 질문 — 내일 새 기능이 들어왔을 때, 예컨대 **"상품 조회수"**, **"최근 본 상품"**, **"상품 공유 횟수"** 같은 것들. 이 중에서 어떤 건 독립 도메인으로 분리하고, 어떤 건 그냥 `Product.viewCount++` 같은 카운터로 둘 건가요? 당신이 판단의 잣대로 삼을 **기준**은 무엇입니까?

**[답변]**
> (1) 단순히 Product.likeCount++ 하게 되면 정말 상품에 대한 좋아요 수만 확인할 수 있는 수단이 된다. 하지만 상품을 좋아요 했다는 것은 고객이 이 상품을 나중에 다시 보고 싶거나 다른 상품들과 비교해 구매를 할 수 있게 유도 하는 역할을 한다고 생각 된다.
>
> (2) 상품 조회수는 단순 카운터, 최근 본 상품과 상품 공유 횟수는 따로 도메인을 만들어 관리 할 것 같습니다. 최근 본 상품과 상품 공유 횟수는 상품 판매와 밀접한 관련이 있다고 생각된다. 판매량을 높일 수 있는 수단이라 생각된다. 하지만 상품 조회수는 조회 숫자가 중요하고, 고객이 상품을 봤다 라는 걸 모두 관리할 필요는 없다고 생각된다.

**[후속 질문: "판매 관련성"이 정말 도메인 분리의 기준인가?]**
(1)번은 좋은 감각이 보입니다 — "다시 보고 싶거나 비교하게 만드는 역할"이라는 표현, 이거 잠시 기억해두세요. 뒤에서 다시 돌아올 겁니다.

(2)번 분류 기준을 정리해보면 **"판매에 밀접한가"**가 잣대네요. 이 기준을 두 개의 반례로 흔들어보겠습니다.

- **반례 A (공유는 판매에 밀접한데, 분리할 이유가 있나?)**
  운영팀이 이렇게 말합니다: "어떤 상품이 많이 공유되는지 통계는 필요하다. 근데 외부 SNS(카카오톡/트위터)로 나가는 행위라서 **누가 언제 공유했는지**는 추적할 방법도 없고, 알 필요도 없다. 그냥 '공유 버튼 눌림 횟수'만 집계해라."
  → 그럼 공유는 판매에 밀접해도 `Product.shareCount`로 충분하지 않나요? 그런데도 분리해야 할 이유가 있나요?

- **반례 B (조회수는 판매에 밀접하지 않은데, 분리해야 할 수도 있다)**
  운영팀이 이렇게 말합니다: "고객 경험 분석 목적으로, **'이 사용자가 최근에 어떤 상품들을 봤는지'** 이력을 남겨달라. 추천 시스템에 쓸 거다."
  → 그럼 조회수도 `View(userId, productId, viewedAt)`라는 **개별 이벤트**를 Entity로 남겨야 합니다.

두 반례를 보면, **"판매 관련성"**이 아니라 다른 무언가가 진짜 기준이어야 해요.

힌트는 당신의 (1)번 답변에 이미 있습니다: **"고객이 나중에 다시 보고 싶거나"**. 이 문장을 다시 읽어보면, 당신이 좋아요를 도메인으로 만든 결정적 요인은 "좋아요의 총 개수"가 아니라 **무엇**이었나요? 한 단어로 꺼내 보세요.

**[답변]**
> 너의 반례를 보니 조회는 개인화 시스템을 만들기 위해서는 필요한 데이터 같아.
> 다른 반례인 공유는 공유를 한 고객 보다는 공유 받은 고객의 도메인이 중요하다고 생각된다.

**[후속 질문: 좋아요 "취소" 시나리오 — 개별 사건의 식별성]**
좋은 방향입니다. "공유받은 고객의 도메인이 더 중요하다"는 표현은 **유입 추적(attribution)** 영역과 맞닿아 있는데, 이건 이번 질문 범위 밖이니 메모만 해두고 본 질문으로 돌아올게요.

당신이 말한 "개인화 데이터"는 사실 **결과적인 용도**에 가깝습니다. "이 데이터가 어디에 쓰일까?"에 대한 답이지, "왜 이 개념을 도메인으로 격상시켜야 할까?"라는 **모델링 기준**은 아직 아니에요. 조금 더 근본으로 파고듭시다.

좋아요 하나에 집중해볼게요. 다음 요구사항이 추가됐다고 가정합니다:
> "사용자가 자신이 누른 좋아요를 **취소**할 수 있어야 한다."

- (a) 만약 `Product.likeCount++` 구조라면, '취소'는 어떻게 구현하나요? `likeCount--`로 충분합니까? 어떤 구체적 문제가 생기나요?
- (b) "사용자 A가 상품 X에 누른 **바로 그 좋아요**를 취소한다"는 요구는, `likeCount--`로 실현 가능한가요?
- (c) (a)와 (b)에서 막히는 지점에 **이름을 붙여본다면**, 도메인으로 격상시키는 결정적 이유를 무엇이라고 부르겠어요? 한 단어(또는 한 문장)로 꺼내 주세요.

**[답변]**
> a. 좋아요를 취소한 사람이 누구인지 구분을 못하기 때문에 계속 좋아요 취소를 할 수 있게 됩니다.
> b. 실제로는 불가능하다. 이유는 사용자 A가 상품 좋아요를 누른게 어떤 상품인지 구분을 못하기 때문이다.
> c. 이거는 모르겠어.

**[개념 정리 — 개별 사건의 식별성]**
(c)를 모르겠다고 했지만, 사실 (a)와 (b)에서 이미 답을 꺼냈습니다. 두 답을 합쳐보면:
- (a) → **누가** 눌렀는지 구분이 안 된다 → 행위 주체의 식별 불가
- (b) → **어떤 건지** 구분이 안 된다 → 개별 사건의 식별 불가

이걸 한 줄로 묶으면 → **"좋아요는 '누가-무엇을-언제' 가 결합된 개별 사건(event)이며, 이 사건 하나하나가 식별 가능해야 조작(취소/중복 방지/이력 조회)이 가능하다."**

모델링 언어로는 **개별 사건의 식별성(event identity)** 또는 **행위의 고유성**이라 부릅니다.

- **카운터(상태)**: "지금 몇 개인가?" 만 중요. 과거 사건 하나하나는 지워져도 됨.
- **도메인(사건/Entity)**: "이 사건 자체가 조작/조회 대상이다." 각 사건이 고유 ID를 가짐.

도메인으로 격상시킬지 판단하는 진짜 기준은 **"판매 관련성"이 아니라, 이 행위 하나하나가 식별/조작/조회 대상이 되어야 하는가"** 입니다. [글감]

**[재검증 질문: 원래 3가지 분류를 다시 본다면?]**
이제 이 기준으로 처음 세 기능을 다시 분류해보세요.

| 기능 | 당신의 원래 분류 | 새로운 분류는? |
| --- | --- | --- |
| 상품 조회수 | 단순 카운터 | ? |
| 최근 본 상품 | 독립 도메인 | ? |
| 상품 공유 횟수 | 독립 도메인 | ? |

- 왜 그렇게 분류했는지, 각 기능마다 **"이 사건 하나하나를 식별/조작할 필요가 있는가?"** 라는 질문에 답하면서 설명해 주세요.
- 그리고 **악마의 변호인**: "그럼 결국 '모든 사용자 행동'은 다 개별 사건이니까 전부 Entity로 분리해야 하는 것 아닌가요? 그 극단까지 가면 Product.viewCount, Product.likeCount 같은 카운터는 세상에 존재할 이유가 없지 않나요?" — 이 반론에 어떻게 대응하시겠어요?

**[답변]**
> 상품 조회수 : 독립 도메인
> 최근 본 상품 : 독립 도메인
> 상품 공유 횟수 : 단순 카운터
>
> 악마의 변호인에서 Product.viewCount, Product.likeCount 는 필요해. 화면마다 개별 entity가 필요하지 않은데 매번 모든 entity를 카운터하는거는 db 부하만 줄뿐이니 count 는 필요해

**[Q1 종합 정리]**
- **재분류 평가**: 공유 → 단순 카운터 전환은 정확합니다(외부 SNS 행위라 개별 추적 불가/불필요). 조회수 → 독립 도메인은 "개인화/추천" 맥락을 전제한 판단으로 타당. 맥락이 없으면 카운터로도 충분하다는 점만 기억.
- **악마의 변호인 대응은 이번 라운드의 숨은 보물**: "Entity와 카운터는 배타적이지 않다. 개별 사건은 Entity로 기록하고, 집계/노출은 카운터로 별도 보관한다" — 이게 실무에서 광범위하게 쓰이는 **CQRS(Command-Query 분리)** 의 맹아. [글감]
- **프로젝트 실제 구현 확인**:
  - `Like.kt` → `BaseEntity` 상속한 Entity, `(user_id, product_id)` 유니크 제약 — "한 사용자가 같은 상품에 좋아요 중복 불가" 비즈니스 규칙을 스키마에서 강제
  - `Product.likes: LikeCount` → 집계 카운터를 상품 속 VO로 보관. `idx_products_brand_id_likes` 인덱스 걸려있음 → 인기순 정렬 쿼리를 위해 **Entity 집계에서 매번 count() 하지 않고 비정규화된 필드 유지**. 사용자가 말한 "DB 부하 회피" 판단과 정확히 일치.
- **핵심 판단 기준 요약**:
  - 도메인 분리 기준 ≠ "비즈니스 가치" 또는 "판매 관련성"
  - 도메인 분리 기준 = **"이 행위 하나하나가 식별/조작/조회의 대상이 되는가"**
  - 둘을 섞을 수도 있다: 사건 기록용 Entity + 집계용 카운터/VO 공존

---

## Q2. VO는 왜 필요한가 — "그냥 Long 쓰면 안 되나?"

**[질문]**
프로젝트 코드를 보면 `Product.price`가 `Long`이 아니라 `Money` 타입이고, `Product.likes`도 `Int`가 아니라 `LikeCount` 타입입니다. 실제 구현이 이렇게 돼 있어요:

```kotlin
// Money.kt
class Money private constructor(val value: Long) {
    operator fun plus(other: Money): Money = Money(value + other.value)
    operator fun times(quantity: Int): Money = Money(value * quantity)
    companion object {
        fun of(value: Long): Money {
            if (value < 0) throw CoreException(ErrorType.BAD_REQUEST, "금액은 0 이상이어야 합니다.")
            return Money(value)
        }
    }
}

// LikeCount.kt
class LikeCount private constructor(val value: Int) {
    fun increment(): LikeCount = LikeCount(value + 1)
    fun decrement(): LikeCount = if (value > 0) LikeCount(value - 1) else this
    companion object {
        fun of(value: Int): LikeCount {
            if (value < 0) throw CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0 이상이어야 합니다.")
            return LikeCount(value)
        }
    }
}
```

누군가 이렇게 주장한다고 해봅시다:

> "그냥 `Long price`, `Int likes` 쓰면 안 돼? 객체로 감싸면 GC 부담, 메모리 오버헤드, 코드 복잡도만 늘어나는 거 아닌가? YAGNI에 위배되는 것 같은데."

이 주장을 **구체적인 버그 시나리오 2가지 이상**으로 반박해 주세요. "가독성이 좋아진다" 같은 추상적 답 말고, **실제로 발생할 수 있는 버그**를 예시로 들어주세요.

(힌트: 생각해볼 각도 — ① 단위/의미가 다른 값끼리 섞일 위험, ② 어디서든 생성 가능한 원시 타입 vs 검증이 강제되는 VO, ③ 음수/허용 범위 같은 도메인 규칙을 원시 타입에 담을 수 있는가)

**[답변]**
> 원시타입을 매번 작성할 때마다 검증을 해야할 것이다. 만약 검증을 하지 않으면 휴먼 에러가 발생할 것이다.
> 항상 변수명으로 해당 원시 타입이 뭔지 알게 된다. 만약 int a = 10 이 price 인지 likes 인지 알 수 없다.

**[후속 질문: 악마의 변호인 + 시나리오 구체화]**
두 축을 모두 짚었습니다 — **검증의 중앙화**(①)와 **의미의 타입화**(②). 그런데 누군가 이렇게 반박한다면?

> "휴먼 에러는 **테스트**로 잡고, 변수명 모호성은 **IDE 자동완성**으로 해결하면 되잖아. 굳이 VO까지 갈 이유 있어?"

이 반론을 넘으려면 **VO만이 보장하는 안전성** — 테스트나 IDE로는 잡기 어려운 부분을 들어야 합니다.

다음 시그니처를 봐주세요:

```kotlin
// 원시 타입 버전
fun transfer(from: User, to: User, amount: Long, fee: Long) { ... }

// 실제 호출
transfer(userA, userB, 1000L, 100L)   // 정상: 1000원 송금, 수수료 100원
transfer(userA, userB, 100L, 1000L)   // 버그: amount와 fee를 뒤바꿈 — 그래도 컴파일 성공!
```

세 가지 작은 질문입니다:

- (a) 위 버그를 **유닛 테스트로 확실히 잡아낼 수 있을까요**? 잡기 어렵다면 왜 어려운가요? (힌트: 테스트가 어떤 값을 쓰느냐에 따라 달라지는 부분)
- (b) `amount: Money, fee: Fee` 처럼 **서로 다른 VO**로 바꾸면, 이 버그는 *어느 단계*에서 발견되나요?
- (c) (b)에서 버그가 발견되는 그 단계를 한 단어로 이름 붙인다면 — 무슨 **안전성**이라고 부를까요? 이 안전성은 테스트/IDE로는 대체 불가능합니다.

**[답변]**
> a. 테스트 코드에서 잡기 어려울 수 있다. 만약 1000L, 1000L 을 사용하면 0원이 된다. 둘의 값을 바꿔도 0L 이다.
> b. 컴파일 에러에서 발견되기 때문에 실행조차 불가능하다.
> c. 코드 안전성

**[Q2 정리 — VO의 4가지 효용과 Primitive Obsession]**
(a)의 답 — "1000L, 1000L 쓰면 바꿔도 0L" — 은 이 문제의 본질을 한 문장으로 묘사합니다. **유닛 테스트는 테스트 데이터에 의존적이라서 순서 버그를 구조적으로 놓칠 수 있습니다.** 반면 서로 다른 VO(`Money` vs `Fee`)로 타입을 가르면 잘못된 순서가 **컴파일조차 되지 않습니다**.

(c)의 "코드 안전성"은 방향이 맞지만, 정식 명칭은 **타입 안전성(Type Safety)**, 더 정확히는 **컴파일 타임 타입 안전성(compile-time type safety)** 입니다.

VO가 주는 4가지 효용 정리:

| # | 효용 | 프로젝트의 예 |
| --- | --- | --- |
| 1 | **검증의 중앙화** | `Money.of()`에서 음수 차단 → 모든 호출자가 검증 거침 |
| 2 | **타입 안전성** | `price: Money`와 `likes: LikeCount`가 타입으로 구분됨 |
| 3 | **불변성** | `LikeCount.increment()`는 수정이 아닌 **새 인스턴스 반환** (`value > 0` 가드 포함) |
| 4 | **도메인 연산 캡슐화** | `Money + Money`, `Money * Quantity` 같은 도메인 언어가 타입에 붙음 |

참고 — Martin Fowler는 "원시 타입을 도메인 개념으로 쓰는 것"을 **Primitive Obsession(원시 타입 강박)** 코드 냄새로 명명했습니다. VO는 이에 대한 정석 해결책입니다. [글감]

---

## Q3. Entity vs VO — "같다(equality)"의 의미가 가르는 본질

**[질문]**
프로젝트 코드를 다시 봅시다.

```kotlin
// Like.kt  — Entity
class Like(userId: Long, productId: Long) : BaseEntity() {   // id 자동 할당
    var userId: Long = userId ; protected set
    var productId: Long = productId ; protected set
}

// LikeCount.kt — VO
class LikeCount private constructor(val value: Int) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is LikeCount && value == other.value)
    override fun hashCode(): Int = value.hashCode()
    fun increment(): LikeCount = LikeCount(value + 1)
    // id 없음
}
```

같은 "좋아요" 도메인에서 하나는 Entity(`Like`)고 하나는 VO(`LikeCount`)입니다. 발제에선 "식별자 있음 / 없음", "상태 변화 / 불변"으로 구분하는데, 저는 **더 본질적인 기준** 이 있다고 봅니다: 바로 **"두 객체가 언제 '같다'고 판단되는가"** 입니다.

세 가지 질문에 답해주세요:

**(a)** `Like` 객체 두 개가 **같다**는 건 무슨 의미여야 할까요?
아래 두 케이스를 생각해보세요:
- 케이스 1: `Like(userId=1, productId=100)` 과 `Like(userId=1, productId=100)` 두 인스턴스 — 이들은 **같은가요, 다른가요?**
- 케이스 2: 사용자 1이 상품 100에 좋아요를 눌렀다 → 취소 → 다시 누름. 이때 첫 번째 좋아요와 세 번째 좋아요는 **같은가요, 다른가요?**

**(b)** `LikeCount(100)` 두 인스턴스는 **같은가요, 다른가요?** 왜 그렇게 생각하는지.

**(c)** (a)와 (b)의 차이를 한 문장으로 정리한다면, Entity와 VO를 가르는 본질은 **"같다를 어떻게 정의하는가"** 라고 말할 수 있습니다. 각각이 "같다"를 판단하는 기준은 무엇인가요? 두 단어 혹은 두 문장으로 꺼내 주세요.

**[답변]**
> ⏸ **세션 1차 중단 (2026-04-12) — 답변 전, 다음 세션에서 이어서 진행**

---

## 📌 오늘 세션 요약 (2026-04-12, 1차)

### 다룬 주제
- **Q1**: 도메인 분리의 판단 기준 — 왜 `Like`를 독립 도메인으로 만드는가 ✅ 완료
- **Q2**: VO는 왜 필요한가 — "그냥 Long 쓰면 안 되나?" ✅ 완료
- **Q3**: Entity vs VO — "같다(equality)"의 의미가 가르는 본질 ⏸ 질문 던진 상태, 답변 대기

### 핵심 개념 — 한 줄 정리

1. **도메인 분리의 진짜 기준**
   - ❌ "비즈니스 가치" / "판매 관련성"
   - ✅ **"이 행위 하나하나가 식별/조작/조회 대상이 되는가" (개별 사건의 식별성, event identity)**

2. **Entity + 카운터 공존 패턴 = CQRS의 맹아**
   - 사건 기록 → Entity (예: `Like`)
   - 집계/노출 → 카운터/VO (예: `Product.likes: LikeCount`)
   - 프로젝트에서 `idx_products_brand_id_likes` 인덱스로 인기순 쿼리를 위한 비정규화를 실제 구현

3. **VO의 4가지 효용**
   | # | 효용 | 프로젝트의 예 |
   | --- | --- | --- |
   | 1 | 검증의 중앙화 | `Money.of()` / `LikeCount.of()` 음수 차단 |
   | 2 | **타입 안전성 (Type Safety)** | `price: Money` vs `likes: LikeCount` |
   | 3 | 불변성 | `LikeCount.increment()` → 새 인스턴스 |
   | 4 | 도메인 연산 캡슐화 | `Money + Money`, `Money * Quantity` |
   - 원시 타입으로 도메인을 다루는 것 = **Primitive Obsession** 코드 냄새 (Fowler 명명)

### 프로젝트 코드 확인한 부분
- `apps/commerce-api/.../domain/like/Like.kt` → Entity (BaseEntity 상속, `(user_id, product_id)` 유니크 제약)
- `apps/commerce-api/.../domain/product/Product.kt` → `price: Money`, `likes: LikeCount`, `stockQuantity: StockQuantity` 등 모두 VO로 설계
- `apps/commerce-api/.../domain/common/LikeCount.kt` → `private constructor + of()` 팩토리 패턴, 불변 연산(`increment/decrement`)
- `apps/commerce-api/.../domain/common/Money.kt` → `plus`, `times` 연산자 오버로딩, 검증 중앙화

### 📝 블로그 글감 후보
1. **도메인 분리의 판단 기준** — "비즈니스 가치"가 아니라 "개별 사건의 식별성". 상품 조회수/좋아요/공유 분류 사례.
2. **Entity와 카운터의 공존** — CQRS의 맹아. 사건은 Entity로, 집계는 비정규화된 VO 필드로. `Like` + `Product.likes: LikeCount` 패턴과 인기순 정렬 인덱스 설계.
3. **VO는 왜 필요한가 — Primitive Obsession 탈출기** — `transfer(amount, fee)` 순서 버그 사례. 테스트·IDE로는 잡기 어려운 **컴파일 타임 타입 안전성**을 VO가 보장하는 이유.

### 🔜 다음 세션에서 이어갈 지점

**바로 재개할 질문**: Q3 (a), (b), (c) — "같다(equality)"의 의미 차이로 Entity vs VO 본질 구분

**이후 Round 2에서 남은 주제**:
- **Q4 (예정)** — Domain Service란 무엇인가
  - 발제의 "QNA - Service 란 무엇일까요?" / "QNA - 행위자(doer) / Manager는 도메인인가?"
  - 3계층의 Service(도메인/Application/Infra) 구분
  - 프로젝트의 `UserService`, `OrderService`, `LikeService` 실제 코드로 검증
- **Q5 (예정)** — 유스케이스 중심 객체 협력 설계
  - 발제의 "주문 생성 Usecase" 흐름
  - Application 계층 Facade와 Domain Service의 역할 차이
  - 프로젝트의 `OrderService` 실제 코드와 비교
- **Q6 (예정)** — 레이어드 아키텍처 + DIP
  - 왜 `OrderRepository`는 도메인에, `OrderRepositoryImpl`은 infrastructure에 두는가?
  - ArchUnit으로 강제되는 이유
  - 이 프로젝트의 실제 구조가 주는 이점
- **Q7 (예정)** — 백지 설계 테스트 (구현 준비도 판정)
  - 이번 라운드 학습 내용으로 "이커머스 도메인"을 처음부터 설계

---

## Q3. Entity vs VO — "같다(equality)"의 의미가 가르는 본질 (이어서)

**[답변]**
> a. 의미적으로 봤을 때 같다. 하지만 식별자가 같은지 봐야 정확히 같은지 다른지 알수 있다. 현재 like는 userId, productId 가 같기 때문에 같다. 그리고 첫번째 좋아요와 세번째 좋아요는 생성되고, 삭제되고, 다시 생성되기 때문에 다르다.
> b. LikeCount(100) 두 인스턴스는 같다. equals와 hashCode 구현을 보면 단순하게 value 값만 비교하기 때문에 같다.
> c. entity는 식별자를 비교하고, vo는 값을 비교한다.

**[후속 질문: userId, productId가 같으면 "같은" Entity인가?]**
(b)와 (c)는 정확합니다. 그런데 (a) 케이스 1에서 "userId, productId가 같기 때문에 같다"고 했는데 — 방금 (c)에서 Entity는 식별자로 비교한다고 했잖아요?

`Like`는 `BaseEntity`를 상속하고, 자동 할당되는 `id`가 있습니다.
- 인스턴스 A: `id=42`, userId=1, productId=100
- 인스턴스 B: `id=43`, userId=1, productId=100

이 둘은 같은가요, 다른가요? 그리고 `(user_id, product_id)` 유니크 제약은 "두 Like가 같다"는 뜻인가요?

**[답변]**
> 아.. 두 인스턴스는 다른 like 야

**[후속 질문: 유니크 제약의 역할]**
그러면 유니크 제약은 뭘 위한 건가요? "두 Like가 같다"는 뜻이 아니라면, 어떤 역할을 하는 건가요?

**[답변]**
> 유니크 제약을 중복 등록을 방지하기 위한거야

**[정리]**
- **유니크 제약** = "같은 사용자가 같은 상품에 좋아요를 두 번 누르는 것을 막는다" → **비즈니스 규칙의 강제**
- **id 동등성** = "이 좋아요가 저 좋아요와 같은 건인가?" → **Entity의 정체성(identity) 판단**
- 이 두 가지는 완전히 다른 관심사. 유니크 제약은 "생성 시점의 중복 방지", id 비교는 "이미 존재하는 객체의 동일성 판별"

| 구분 | "같다"의 기준 | 용어 | 프로젝트 예시 |
| --- | --- | --- | --- |
| **Entity** | **식별자(id)가 같으면 같다** | 동일성(Identity) | `Like` — id=42와 id=43은 속성이 같아도 다른 Entity |
| **VO** | **값이 같으면 같다** | 동등성(Equality) | `LikeCount(100)` — 두 인스턴스는 값이 같으면 같은 것 |

- **유니크 제약 ≠ 동일성** — 유니크 제약은 비즈니스 규칙(중복 방지)이지, Entity의 identity를 정의하는 것이 아니다 [글감]

---

## Q4. Domain Service — "이 로직은 누구의 책임인가?"

**[질문]**
발제의 `PointChargingService.charge()` 코드를 보면, 사실상 검증 한 줄 + `user.receive(amount)` 위임 한 줄입니다. 검증까지 `User.receive()` 안에 넣으면 `PointChargingService`가 아예 필요 없어지죠. 그런데도 Domain Service로 분리해야 할 이유가 있나요?

**[답변]**
> Domain Service 로 분리할 이유는 없어보이네

**[후속 질문: 그러면 Domain Service가 진짜 필요한 경우는?]**
동의합니다. Entity가 스스로 자기 상태를 지킬 수 있으면 밖으로 꺼낼 이유가 없습니다.

발제의 주문 생성 유스케이스 `OrderService.createOrder()`를 보면 여러 객체가 협력합니다 — Product, User, Order. 이 로직을 `Order.create()` 같은 Entity 메서드 안에 넣으면 안 되나요?

**[답변]**
> Entity는 서로 의존할 수 없다. 그래서 이걸 대체하기 위해 domain service가 나온것이다.
> 만약 Order.create() 로 사용한다면 order entity는 방대해지고, 다른 entity의 상태 변화까지 관여할 것이다.

**[후속 질문: OrderService는 정말 Domain Service인가?]**
`OrderService`는 `OrderRepository`를 주입받아 `save()`를 호출합니다. Repository에 의존해서 저장까지 하는 이 `OrderService`는 정말 Domain Service인가요?

**[답변]**
> Domain Service 야 Repository 는 직접 저장을 하는게 아니라 Repository 를 구현하는 하위 객체에 위임하고 있어

**[후속 질문: createOrder()의 6단계 — 어디까지 Domain, 어디부터 Application?]**
DIP 위반이 아닌 건 맞지만, 계층의 책임 분류가 질문의 핵심입니다.

`createOrder()`가 하는 일:
1. 총 금액 계산
2. 유저 잔액 확인
3. 재고 차감
4. 유저 포인트 차감
5. Order 객체 생성
6. Repository에 저장

이 중 어디까지가 Domain Service의 일이고, 어디부터가 Application Layer(Facade)의 일인가요?

**[답변]**
> Domain Service : 2, 3, 4, 5, 6
> Facade : 1
> 인거 같아

**[후속 질문: 1번과 6번을 다시 생각해보자]**
1번(총 금액 계산) — "상품 가격 × 수량의 합"은 기술적 조율인가요, 비즈니스 규칙인가요?
6번(Repository에 저장) — "언제, 무엇을 저장할 것인가"를 결정하는 건 흐름의 조율일까요, 도메인 규칙일까요?

**[답변]**
> 1번은 Domain Service에 있어야 할거 같아. 비즈니스 규칙이기 때문에
> 그리고 너의 말은 6번은 Application 계층 에 해당 된다는 거 같은데 맞아?

**[정리]**
- **Domain Service**: 1(총 금액 계산), 2(잔액 확인), 3(재고 차감), 4(포인트 차감), 5(Order 생성) — 비즈니스 규칙과 도메인 객체 간 협력
- **Facade(Application)**: 도메인 서비스를 호출하고, 저장 시점을 결정하고, 트랜잭션을 관리 — 유스케이스 흐름의 조율
- "저장한다"는 행위 자체는 비즈니스 규칙이 아니라 기술적 결정. "주문이 유효하면 영속화해라"라는 흐름 조율은 Facade의 몫
- 발제의 `OrderService`는 도메인 로직(1~5)과 저장(6)을 한 곳에 섞어놓은 것으로, 설명의 편의를 위해 단순화한 것

판단 기준:
- "이 로직이 **Entity 하나가 스스로** 할 수 있나?" → **Entity 메서드**
- "**여러 Entity의 협력**이 필요하지만, 비즈니스 규칙인가?" → **Domain Service**
- "**언제 저장하고, 뭘 호출할지** 흐름을 잡는 건가?" → **Facade** [글감]

---

## Q5. 유스케이스 객체 협력 — "누가 총 금액을 계산해야 하는가?"

**[질문]**
Q4에서 "총 금액 계산은 Domain Service"라고 했는데, 한 발 더 들어가봅시다.

```kotlin
val totalPrice = products.sumOf { (product, qty) -> product.price * qty }
```

주문에는 `OrderItem`이라는 개념이 있고, 총 금액이란 결국 "이 주문에 포함된 항목들의 합"입니다. 이 계산이 정말 Domain Service의 일인가요? 다른 곳에 더 자연스럽게 들어갈 수 있는 곳은 없나요?

프로젝트의 실제 `Order.kt`를 보면 `calculateTotalAmount()`가 **Entity 내부 private 메서드**로 있고, `addItem()` 호출 시마다 자동 재계산됩니다.

**[질문 세부]**
- (a) 총 금액 계산을 Entity 메서드로 옮기는 것이 Domain Service보다 나은 이유는?
- (b) 반대로 Domain Service로 꺼내야 하는 "경계 기준"은? Q4의 "여러 Entity 협력 여부"만으로 충분한가?
- (c) `addItem()` 안에서 `calculateTotalAmount()`를 자동 호출하는 설계가 보장하는 "안전장치"는?

**[답변]**
> a. Domain Service는 여러 entity의 묶음으로 계산하는 것이다. entity에서 자체적으로 해결할 수 있다면 자체적으로 하면 좋다.
> b. 나는 충분하다고 생각해
> c. n+1 문제가 발생한다

**[후속 질문: (c) "불변 조건" 끌어내기]**
(c)의 "N+1"은 JPA 성능 이슈 용어로 다른 개념. 가상의 나쁜 설계(`addItem()` 따로, `recalculateTotal()` 따로)를 보여주고, 외부 호출자가 재계산을 깜빡하면 totalAmount=0으로 저장되는 시나리오 제시.

- (c-1) Entity가 항상 지켜야 할 자기 정합성 규칙의 이름은?
- (c-2) 이 규칙을 Entity 바깥에 두면 왜 위험한가?

**[답변]**
> c-1: 불변 조건
> c-2: 휴먼 에러 발생으로 재계산 누락이 발생하여 데이터 정합성에 오류 발생한다

**[정리 — 불변 조건(Invariant)]**
- **Invariant** = 도메인 모델이 언제나 참으로 유지해야 하는 상태 규칙
- Entity 내부에 불변 조건 유지 책임을 가두면 → "이 Entity를 거치지 않고는 일관성을 깰 수 없다"는 강력한 보증
- 이것이 DDD의 **Aggregate Root** 개념의 뿌리 — `Order`가 자기 `OrderItem`들의 변경을 독점 관리 [글감]

**[후속 질문: (b) 경계 기준 흔들기 — 쿠폰 할인 시나리오]**
"여러 Entity 협력 = Domain Service" 기준을 쿠폰 할인 예시로 흔든다.

- 옵션 A: `Order.applyCoupon(coupon)` — Order가 쿠폰 정책을 다 알아야 함
- 옵션 B: `CouponDiscountService.calculate()` + `Order.applyCouponDiscount(couponId, discountAmount)` — 결과만 받음

실제 프로젝트는 **옵션 B**를 택함 (`Order.kt:94-98`).

- (b-1) `calculateTotalAmount`은 Entity 안, `calculateDiscount`는 밖 — 두 계산의 본질적 차이는?
- (b-2) 옵션 A를 택하면 "쿠폰 정책 추가" 요구사항에서 어떤 문제?
- (b-3) 경계 기준을 한 문장으로 재정의하면?

**[답변]**
> b-1: 본질은 할인 정책의 계산은 어디에 있냐이다. 할인은 coupon의 본질이고, 계산도 coupon의 본질이다. order은 받은 금액과 총 금액의 계산만하는게 역할이라 생각한다.
> b-2: 쿠폰의 정책이 변경되었는데 수정된 코드에 order entity가 포함된다. 변경되는 범위가 넓다.
> b-3: 각 entity가 오퍼레이션 역할을 하고, 각 entity의 역할에 맞는 연결을 도와준다

**[후속 질문: (b-3) 정제 — "지식의 출처"로 경계 긋기]**
(b-3)이 모호함. "지식의 출처" 언어로 재정의 유도.

| 계산 | 필요한 지식 |
| --- | --- |
| `Order.calculateTotalAmount()` | Order의 `items` — 자기 내부 상태만 |
| 쿠폰 할인 계산 | Order의 `totalAmount` + Coupon의 정책 |

- Entity 메서드는 언제 적합? "자기 ~"로 시작
- Domain Service는 언제 적합? "외부 ~"로 시작

**[답변]**
> entity 메서드는 자기 내부에서만 필요한 계산이고, Domain Service는 외부에서 필요한 데이터들도 이용해 계산을 한다.

**[Q5 종합 정리]**
- **함정 주의**: "외부 데이터"가 너무 넓은 기준. `Product.isCheaperThan(other: Product)` 같은 단순 비교는 여전히 Entity 메서드. 더 정확히는 **"외부 정책/규칙(policy)"** 이 개입해야 Domain Service.
- **최종 판단 기준**: "자기 상태만으로 파생 가능한가" vs "외부 도메인 정책이 조합되어야 하는가"
- **변경의 축(axis of change)**: Order는 "주문 상태" 축, Coupon은 "할인 정책" 축. 이 둘이 섞이면 SRP 위반 [글감]
- **불변 조건 유지 책임의 귀속**: Entity가 자신의 invariant를 스스로 책임져야 하며, 외부 호출자에게 전가하면 누락 시 정합성 깨짐 [글감]

| 구분 | 판단 기준 | 프로젝트 예시 |
| --- | --- | --- |
| Entity 메서드 (자기 상태) | 자기 내부 상태만으로 파생 가능한 계산 | `Order.calculateTotalAmount()` |
| Entity 메서드 (외부 비교) | 같은 타입의 다른 Entity와 값 비교 수준 | `Product.isCheaperThan(other)` 스타일 |
| Domain Service | 외부 정책/규칙을 조합해야 완결되는 계산 | `CouponDiscountService` |
| Facade | 유스케이스 흐름 조율, 저장 시점, 트랜잭션 | `OrderFacade` |

---

## Q6. 레이어드 아키텍처 + DIP — "왜 Repository 인터페이스는 도메인에?"

**[질문]**
이 프로젝트는 `OrderRepository`(interface)는 `domain/order/`에, `OrderRepositoryImpl`은 `infrastructure/order/`에 둔다. 또 `ArchitectureTest.kt`에서 7가지 규칙을 ArchUnit으로 강제한다.

**"인터페이스든 구현체든 둘 다 infrastructure에 두는 게 자연스럽지 않냐?"** 는 반론에 대해:

- (a) 도메인에 두는 결정적 이유는?
- (b) infrastructure로 옮기면 어떤 ArchUnit 규칙이 깨지는가?
- (c) 기계가 검사하도록 강제하는 이유는?

**[답변]**
> a: infrastructure는 저수준 모듈이고 domain은 고수준 모듈로 저수준은 고수준을 의존하는 방향이어야 한다.
> b: OrderService.kt는 infrastructure를 의존하게 된다.
> c: 휴먼에러 발생은 언제든 발생할 수 있다. 이를 방지하기 위해 우리는 항상 안전장치를 마련해야 한다.

**[정리 — DIP와 아키텍처 침식]**
- (a): **SOLID의 D — Dependency Inversion Principle**. "고수준/저수준 둘 다 추상화에 의존"이 정식 정의.
- (b): `ArchitectureTest.kt:14-17`의 "domain은 infrastructure에 의존하지 않는다" 위반 → 빌드 실패.
- (c): "휴먼에러 방지"보다 더 중요한 것은 **아키텍처 침식(architectural erosion)** — 코드베이스가 커질수록 리뷰에서 규칙 위반을 놓치고, 한 번 경계가 무너지면 깨진 유리창 이론처럼 연쇄로 침식된다. 테스트는 첫 위반을 즉시 차단. [글감]

**[후속 질문: DIP의 실전 작동 — Facade는 어떻게 저장하는가?]**
Facade는 infrastructure를 import할 수 없는데, 어떻게 `OrderRepositoryImpl`을 실행시키는가?

- (d-1) 이걸 가능하게 하는 메커니즘은?
- (d-2) 컴파일 타임 의존 vs 런타임 의존은 어떻게 다른가?
- (d-3) Spring이 없다면 "Impl을 Facade에 넘겨주는" 코드는 어느 레이어에 속해야 하나?

**[답변]**
> 1: @Component가 컨테이너에 객체를 만들어 두고, 이 객체를 런타임 시점에 가져온다.
> 2: Facade가 OrderRepositoryImpl을 런타임 시점에 가져온다. Facade는 컴파일 타임에 OrderRepository를 의존한다.
> 3: 이건 모르겠네

**[정리 — Composition Root]**
(d-3) 정답: **Composition Root(조립 루트)** — Mark Seemann이 명명한, 의존성 그래프를 조립하는 단 하나의 지점.

```kotlin
fun main() {
    val jpa = OrderJpaRepository(...)
    val repo: OrderRepository = OrderRepositoryImpl(jpa)
    val facade = OrderFacade(OrderService(repo))
    startServer(facade)
}
```

- 어느 레이어에도 속하지 않는 "가장 바깥쪽 링". 레이어 의존성 규칙의 **예외 지대**.
- 비즈니스 로직 없고, 순수하게 "누구에게 누구를 꽂을지"만 결정.
- **Spring의 `@SpringBootApplication` + Component Scan은 이 Composition Root의 자동화**. 프레임워크가 대신 해주는 것일 뿐 본질은 같음. [글감]

**[Q6 종합 정리]**

| 개념 | 요약 | 프로젝트 근거 |
| --- | --- | --- |
| DIP | 고수준·저수준 모두 추상화에 의존 | `OrderRepository`(domain) ← `OrderRepositoryImpl`(infra) |
| 컴파일 타임 의존 | Facade는 도메인 인터페이스만 import | `OrderFacade` → `domain.OrderRepository` |
| 런타임 의존 | Spring DI가 `@Component` 구현체를 주입 | `OrderRepositoryImpl`의 `@Component` |
| Composition Root | 의존성을 조립하는 가장 바깥쪽 지점 | Spring의 Component Scan |
| 실행 가능한 아키텍처 계약 | ArchUnit으로 빌드 타임 차단 | `ArchitectureTest.kt` 7가지 규칙 |

**핵심 교훈:**
- 인터페이스를 도메인에 두는 건 "취향"이 아니라 도메인의 독립성을 보장하기 위한 구조적 결정
- 아키텍처 규칙은 기계로 강제해야 유지됨 — 리뷰에만 의존하면 깨진 유리창 이론이 작동 [글감]
- Spring은 마법이 아니라 Composition Root의 자동화일 뿐. DIP는 프레임워크 없이도 성립하는 구조적 원칙 [글감]

---

## Q7. 백지 설계 테스트 — 구현 준비도 판정

**[시나리오]**
사용자가 여러 상품을 한 번에 주문. 상품은 가격/재고, 사용자는 포인트 잔액 보유. 주문 완료 시 재고 차감 + 포인트 차감 + 주문 저장이 모두 이뤄져야 함.

**[백지 설계 답변]**

① **도메인 객체 설계**
> Order의 필드는 totalPrice, totalCount, List<OrderItem> orderItems
> totalPrice, totalCount는 VO로 만든다
> Aggregate Root = Order, Order를 통해 orderItems 접근

② **행위의 배치**
> 총 주문 금액 계산은 Order가 한다
> 재고 차감은 상품 entity에서 한다
> 포인트 차감은 point entity에서 한다
> 유스케이스 흐름 조율은 OrderService에서 한다
> Entity/Domain Service/Facade 기준: Entity는 자기 상태 변경, Domain Service는 다른 entity도 변경, Facade는 트랜잭션과 흐름

③ **레이어 & DIP**
> Repository는 domain 레이어에 있다
> Facade → Repository 방향
> ArchUnit: Facade → domain ← infrastructure

**[평가 — 커버된 포인트]**
1. Order Aggregate Root 식별 + OrderItem 접근 제어
2. VO로 Money, Quantity 뽑은 감각
3. 총 금액 계산 → Order 내부 (Q5 학습 반영)
4. 재고 차감 → Product, 포인트 차감 → User/Point
5. Facade = 트랜잭션 + 흐름 조율
6. Repository 인터페이스 = domain, DIP 방향 맞음

**[평가 — 보완이 필요한 포인트]**
1. **Entity 누락** — Product, User 설계가 빠짐. Order 필드에서도 userId, status 빠짐
2. **Aggregate 불변 조건을 명시 안 함** — Q5의 핵심이었던 invariant(예: "items 변경 시 totalAmount 자동 재계산") 누락
3. **Q5의 "지식의 출처" 기준이 Q4 수준으로 회귀** — "여러 entity 협력"보다 "자기 상태만 vs 외부 정책"이 더 본질적 기준
4. **ArchUnit 규칙을 방향 그림으로만 표현** — 실제 규칙 문장(예: `domain은 infrastructure에 의존하지 않는다`)으로 꺼내야 완성
5. **Repository 구현체 위치를 암시만** — "interface는 domain, 구현체는 infrastructure"를 명시적으로 쌍으로 꺼내야 함

**[판정]**
## ⚖️ 일부 보완 필요 (but 구조적 뼈대는 탄탄함)

큰 그림은 잘 잡았으나, **불변 조건 의식**과 **Q5의 정제된 기준**이 백지에서 자동으로 튀어나오지 않았다. 머리의 이해 ↔ 손의 반사 사이 간극. 구현 시 의식적 체크리스트로 활용하면 보완 가능.

**구현 시 자기 체크리스트:**
- [ ] 각 Aggregate가 지켜야 할 불변 조건을 리스트로 쓸 수 있는가?
- [ ] 그 불변 조건을 외부(Facade)가 아닌 Aggregate 내부가 지키는가?
- [ ] "자기 상태만으로 가능한가, 외부 정책이 필요한가?" 를 Entity/Service 배치 전에 묻는가?
- [ ] `./gradlew test` 실행 시 ArchUnit 규칙 전부 녹색인가?

---

## 📚 Round 2 전체 학습 요약

### 핵심 개념 지도

```
[도메인 모델링의 본질]
    └─ Q1: 도메인 분리 기준 = "개별 사건의 식별성(event identity)"
         └─ 카운터(상태) vs Entity(사건) 공존 = CQRS의 맹아

[도메인 객체의 3종 세트]
    ├─ Q2: VO — Primitive Obsession 탈출, 타입 안전성, 불변
    ├─ Q3: Entity vs VO — "같다"의 정의
    │       ├─ Entity: 동일성(Identity) — id가 같으면 같다
    │       └─ VO: 동등성(Equality) — 값이 같으면 같다
    └─ Q4: Domain Service — Entity에 귀속되지 않는 도메인 로직

[유스케이스 협력]
    └─ Q5: Entity 메서드 vs Domain Service vs Facade
         ├─ Entity: 자기 상태만으로 파생 가능
         ├─ Domain Service: 외부 정책/규칙 조합
         └─ Facade: 흐름 조율, 트랜잭션, 저장 시점
         └─ 불변 조건(Invariant) + Aggregate Root가 이를 내부에서 지킴

[구조적 안정성]
    └─ Q6: 레이어드 + DIP + Composition Root
         ├─ 인터페이스는 domain, 구현체는 infrastructure
         ├─ 컴파일 타임 의존 vs 런타임 의존 구분
         ├─ Spring DI = Composition Root 자동화
         └─ ArchUnit = 실행 가능한 아키텍처 계약
```

### 📝 블로그 글감 후보 (최종)

1. **도메인 분리의 판단 기준** — "비즈니스 가치"가 아니라 "개별 사건의 식별성". 상품 조회수/좋아요/공유 분류 사례.
2. **Entity와 카운터의 공존** — CQRS의 맹아. 사건은 Entity로, 집계는 비정규화된 VO 필드로. `Like` + `Product.likes: LikeCount` + 인기순 정렬 인덱스 설계.
3. **VO는 왜 필요한가 — Primitive Obsession 탈출기** — `transfer(amount, fee)` 순서 버그. 테스트·IDE로는 잡기 어려운 컴파일 타임 타입 안전성.
4. **유니크 제약 ≠ Entity의 동일성** — DB 제약은 비즈니스 규칙(중복 방지), id 비교는 정체성 판단. 같은 속성을 가진 두 Entity도 id가 다르면 다른 객체.
5. **Domain Service vs Facade — 경계를 긋는 기준** — "비즈니스 규칙인가, 흐름 조율인가?" PointChargingService의 불필요한 분리 vs 주문 생성의 정당한 분리.
6. **Q5 심화: "지식의 출처"로 경계 긋기** — 자기 상태만 vs 외부 정책. 쿠폰 할인 예시. 변경의 축(axis of change)과 SRP.
7. **Aggregate Root와 불변 조건** — `Order.addItem()` 내부에서 `calculateTotalAmount()` 자동 호출. "외부가 불변 조건을 깰 수 없도록 Aggregate 내부가 독점 관리".
8. **ArchUnit으로 만드는 실행 가능한 아키텍처 계약** — 아키텍처 침식과 깨진 유리창 이론. 리뷰가 아닌 테스트로 강제.
9. **Composition Root의 정체** — Mark Seemann의 개념. Spring의 마법은 Composition Root의 자동화. DIP는 프레임워크 없이도 성립하는 구조적 원칙.

### 🎯 구현 연결 포인트

이 학습 내용이 실제 구현에 영향을 줄 **구체적 결정들**:

1. **Entity 설계 시** — 먼저 `List<불변조건>`을 적어두고, 그 각각을 어디서 지킬지 표로 그린 다음 메서드를 짠다.
2. **"총액, 할인액, 최종결제액" 같은 파생 값** — 외부에서 `calculate()` 호출하게 두지 말고, Aggregate Root 내부에서 상태 변경 때마다 자동 재계산하도록 봉인.
3. **새 Service를 만들기 전 체크** — "이 로직에 필요한 지식이 자기 Entity 안에 있는가? 있으면 Entity 메서드. 외부 정책이 필요하면 Domain Service. 흐름만 조율이면 Facade."
4. **Repository 인터페이스는 무조건 domain에, `@Component` 구현체는 infrastructure에**. Facade는 interface 타입으로만 주입받는다.
5. **VO 후보 감지** — 새 필드 추가 시 "이 값에 음수가 가능한가? 다른 타입과 헷갈릴 수 있는가?"를 묻고 YES면 VO로 감싼다.
6. **ArchUnit 테스트를 CI 필수 단계로** — 빌드 파이프라인에서 `./gradlew test`가 녹색이어야 PR 머지 가능.

### 🔜 다음 단계 안내

이번 라운드 내용으로 실제 구현을 하거나, 다른 라운드 학습으로 이어가려면:

- **실제 구현 시**: `/plan` 으로 plan.md를 작성 → `/red` → `/green` → `/refactor` TDD 루프 진행. 불변 조건을 먼저 테스트로 잡아두면 설계가 자연스러워짐.
- **다음 라운드 학습 시**: `/learn-round {N}` 으로 다른 라운드 주제 시작. 기존 QnA 파일 경로를 알려주면 이어서 진행.
- **블로그 글감 선택**: 위 9개 글감 중 가장 "직접 반례/시나리오로 설명하고 싶은 것"부터 1~2개 골라 풀어쓰면 좋음.

---

**🎓 Round 2 학습 종료 — 2026-04-19**

---

## 📌 세션 요약 (2차, 2026-04-18)

### 다룬 주제
- **Q3**: Entity vs VO — "같다(equality)"의 의미 ✅ 완료
- **Q4**: Domain Service — "이 로직은 누구의 책임인가?" ✅ 완료
- **Q5**: 유스케이스 객체 협력 — "누가 총 금액을 계산해야 하는가?" ⏸ 질문 던진 상태, 답변 대기

### 핵심 개념 — 한 줄 정리

3. **Entity vs VO의 본질은 "같다"의 정의 차이**
   - Entity: 동일성(Identity) — id가 같으면 같다
   - VO: 동등성(Equality) — 값이 같으면 같다
   - 유니크 제약 ≠ 동일성 — 비즈니스 규칙(중복 방지)과 정체성 판단은 별개

4. **Domain Service vs Application Service(Facade) vs Entity 메서드**
   - Entity 메서드: 자기 상태를 스스로 보호/변경 (`User.pay()`, `Product.decreaseStock()`)
   - Domain Service: Entity 하나에 귀속되지 않는 도메인 로직, 상태 없음
   - Facade: 유스케이스 흐름 조율, 저장 시점 결정, 트랜잭션 관리

### 📝 블로그 글감 후보 (추가)
4. **유니크 제약 ≠ Entity의 동일성** — DB 제약은 비즈니스 규칙(중복 방지), id 비교는 정체성(identity) 판단. 같은 속성을 가진 두 Entity도 id가 다르면 다른 객체.
5. **Domain Service vs Facade — 경계를 긋는 기준** — "비즈니스 규칙인가, 흐름 조율인가?" PointChargingService 같은 불필요한 분리 vs 주문 생성의 정당한 분리.

### 🔜 다음 세션에서 이어갈 지점

**바로 재개할 질문**: Q5 — "총 금액 계산은 OrderItem/Order에 속하지 않나?"

**이후 남은 주제**:
- **Q6 (예정)** — 레이어드 아키텍처 + DIP: Repository Interface가 Domain Layer에 있는 이유
- **Q7 (예정)** — 백지 설계 테스트 (구현 준비도 판정)

### 세션 재개 방법
다음에 이 라운드를 이어가려면, 학습 스킬을 다시 실행할 때 **기존 QnA 파일 경로로 `docs/notes/learn-round2-qa-notes.md`** 를 알려주시면 Q5 답변부터 이어갑니다.

