# Round 3 학습 노트 — 도메인 모델링 실전 적용 / 유스케이스 협력 / 테스트 가능 구조

## 학습 개요
- 학습 일자: 2026-04-21
- 라운드 주제: 도메인 모델링(Entity/VO/Domain Service) 실전 적용, 유스케이스 중심 객체 협력, 레이어드 + DIP, 단위 테스트 기반 구현
- 참조 문서: `docs/quests/round-3.md`
- 진행 방식: Round 2 개념 학습을 전제로, 프로젝트 실제 코드를 통해 판단력 검증
- 이전 라운드 연결: Round 2 Q1(개별 사건 식별성), Q5(불변 조건/지식의 출처), Q6(DIP/Composition Root) 개념 재활용

---

## Q1. "상품이 좋아요 수를 직접 관리해야 할까?" — Entity와 카운터의 동기화 책임

**[질문]**
Round 3 발제의 Feature Suggestion 첫 질문. 프로젝트 현황:
- `Product.likes: LikeCount` 필드가 있고, 증가 메서드는 없음
- `Like` Entity가 독립, `(user_id, product_id)` 유니크 제약
- `LikeService.like()`는 `likeRepository.save()`만 호출 — Product.likes는 건드리지 않음

(a) 좋아요 등록 시 `Product.likes`를 누가 증가시켜야 하는가?
 - A: Product에 `increaseLike()` 메서드 추가, Entity가 스스로 증가
 - B: LikeService가 Like 저장 + Product 조회/수정
 - C: LikeFacade가 두 Service 조합
 - D: 이벤트 발행 → 별도 Consumer가 증가

(b) 악마의 변호인: "`SELECT COUNT(*)`로 세면 되는데 카운터를 유지할 필요가 있나?"
(c) 불변 조건 관점: "`Product.likes == Like 행 개수`" invariant는 누가 보장하는가? 깨지면 어떤 시나리오?

**[답변]**
> a: 이벤트 발행을 안하면 선택지 A, 이벤트 발행을 하면 선택지 D 할거 같은데 이게 생각해볼게 like 개수의 정합성을 중요하게 생각하면 트랜잭션에 like 증가를 하나로 묶고, like의 정합성을 조금 손해 봐도 괜찮으면 이벤트 발행으로 갈거 같아.
>
> b: 조회 성능 문제라고 생각해. Like 에 인덱스를 걸더라도 개수가 많아지면 성능에 문제가 발생할 수 있기에 Product.likes 가 좋을 수 있고, 비즈니스 생각도 같이 하면 product 에 좋아요 이니 같이 가는게 맞을 수 있다.
>
> c: LikeService 에서 해야 할거 같아

**[후속 질문: (a)와 (c)의 모순 — 조율자의 정체]**
(a)에서 "선택지 A"는 방법(Product.increaseLike())일 뿐 조율자가 아님. 누군가 이 메서드를 호출해야 함. (c)에서 "LikeService가 한다"고 했는데, 그러면 LikeService가 LikeRepository + ProductRepository 둘 다 주입받음. Round 2 Q4 기준(Domain Service = 외부 정책/규칙 조합, Facade = 흐름 조율)으로 이 로직은 어디 속하는가?

**[답변]**
> d-1: LikeService 일 이라 생각해. 왜냐면 상태 변경이기 때문이야
> d-2: Product Aggregate 에서 Product 내부에서의 상태 변경은 괜찮다. 그래서 LikeService 는 호출만 하는 것이니 괜찮다고 생각한다.
> d-3: SELECT COUNT(*) 를 하면 풀스캔을 하기 때문에 성능에 문제를 일으킨다

**[보강 설명 + 재검토]**

**(d-1) 재검토 — "정책"의 범위**
"LikeCount +1 증가"는 정책이 아니라 **단순 산술**. 정책의 본질은 **판단/분기(if-else)**:
- "VIP는 좋아요 시 2배 증가" = 정책 (판단 포함)
- "좋아요 시 +1" = 산술 (판단 없음)

현재 `LikeService.like()`에는 **정책이 하나도 없음** — "Like 저장 → Product.likes +1" 이라는 순차 흐름일 뿐. 따라서 이 로직의 정체는 **Domain Service가 아니라 Facade의 흐름 조율**.

**(d-2) 재설명 — 유스케이스 본질 vs 결과 파생**
두 실패 시나리오 비교:

| 구분 | A (주문 + 재고 차감) | B (좋아요 + 카운터 증가) |
| --- | --- | --- |
| 관계 | 유스케이스 본질의 일부 | 결과 파생 |
| 실패 시 | 전체 롤백 필수 | 느슨 처리 가능 (배치/이벤트로 재계산) |
| 자연스러운 배치 | Domain Service 수준 조율 가능 | Facade 또는 이벤트로 분리 |

B 패턴은 실무에서 비동기 이벤트로 뽑히는 경우가 많음. 프로젝트의 실제 `LikeFacade.kt`도 Outbox + Kafka Consumer로 카운터를 집계 (Round 3 단계에선 오버엔지니어링 — 그냥 Facade 순차 호출이 정답).

**(d-3) 정리 — RDB 정렬의 구조적 제약**
핵심: **"정렬하려면 정렬 키가 실제 컬럼이어야 한다"**.
- `ORDER BY likes` → 인덱스로 O(log N) 정렬 가능
- `ORDER BY COUNT(*)` → 인덱스 사용 불가, JOIN+GROUP BY+sort 필수

"상품을 좋아요 순 정렬/페이지네이션" 요구가 있는 순간, 집계 결과를 **컬럼으로 비정규화**하는 것은 RDB의 구조적 필연.

**[Q1 종합 정리]**
1. **좋아요 카운터 증가의 책임자** = Facade (Round 3 단계 정답). LikeService에 넣으면 Domain Service가 Application 흐름까지 떠안게 됨.
2. **"정책"의 정의** = 판단/분기가 개입하는 규칙. 단순 산술은 정책이 아님.
3. **"유스케이스 본질 vs 결과 파생"** 기준:
   - 본질의 일부 = 실패 시 전체 롤백 (재고 차감)
   - 결과 파생 = 느슨한 일관성 허용 (카운터 증가)
4. **카운터 공존 패턴의 구조적 필연** = RDB는 컬럼 단위로만 인덱스 정렬 가능 → 집계 컬럼을 비정규화하는 것은 성능 최적화가 아닌 필수 설계 [글감]
5. **프로젝트 현실 연결**: LikeFacade가 Outbox+Kafka로 집계하는 이유 = 카운터 증가가 "결과 파생" 성격이라 비동기화 가능. 동기식 Facade 호출이라도 의미 동일.

---

## Q2. Application Layer의 "경량화" — ProductFacade는 어디까지 해야 하나?

**[질문]**
상품 상세 조회 API가 Product + Brand + Like + "내가 좋아요 했는가?" 를 조합한다고 가정.

(a) 조합의 책임자는 Entity/Service/Facade 중 누구인가?
(b) 프로젝트에서 `Product.brand: Brand`(JPA 연관관계)를 쓰지 않고 `brandId: Long`만 보유한 이유는?
(c) "경량 Application Layer"의 진짜 의미는?

**[1차 답변]**
> a: ProductService 가 조합의 책임자이다. 역할이 외부 역할 흐름 관리
> b: 애그리거트 관점에서 Brand와 Product 관계가 맞지 않는다. 성능 문제도 발생할 수 있다.
> c: 모르겠어

**[1차 재검토 — 함정 + 보강 + 설명]**
**(a) 함정 재발**: 이름은 "ProductService"인데 설명은 Facade 정의("외부 역할 흐름 관리"). Q1 (d-1)과 동일한 함정. 프로젝트에도 `ProductService`(domain)와 `ProductFacade`(application) 별도 존재.

**(b) 보강 요청**: Aggregate 경계 판단 기준 = "한 트랜잭션에서 함께 변경되는가". 저장 시점/조회 시점 두 가지 문제로 구체화 요청.

**(c) 설명**:
| ✅ 경량이 허용 | ❌ 경량이 부정 |
| --- | --- |
| 조합/조립, 흐름 조율, 트랜잭션 관리, 외부 시스템 호출 | 비즈니스 규칙 판단, 도메인 상태 변경 로직, 여러 도메인에 걸친 계산/검증 |

**핵심 구호**: Facade는 "무엇을 할지" 판단하지 않고 "순서대로 시킨다".

**[2차 답변]**
> a: 상품 상세 조회, 단순한 조회 이기 때문에 흐름 조율의 역할이 맞는거 같으니 facade에서 해야할거 같아
> b: product와 brand의 저장/수정 시점이 같나? 라고 생각해보면 아니다. 그리고 만약 Product.brand 를 조회 한다면 n+1 문제가 발생한다.
> c: 경량이야. 이유는 단순하게 조회하고, 흐름 조율의 역할만 하고 있어

**[Q2 종합 정리]**
1. **이름에 속지 말 것** — "Service"가 붙었다고 Domain Service가 아니다. 역할(조율/흐름)로 분류해야 함. 프로젝트의 `XxxService`(domain) vs `XxxFacade`(application) 분리 구조가 이를 물리적으로 강제.
2. **Aggregate 경계 판단 기준** — "한 트랜잭션에서 함께 변경되는가?" Product와 Brand는 독립 업데이트 주기 → 별도 Aggregate → `brandId: Long`만 유지.
3. **JPA 연관관계의 두 가지 함정**:
   - 저장 시점: Product 수정이 Brand까지 영향 가능 (의도치 않은 cascade)
   - 조회 시점: lazy loading → 목록 조회에서 N+1
4. **"경량"의 정의** — 조합/조율은 OK, 비즈니스 판단은 NG. Facade 안에 `if-else 도메인 규칙`이 들어오는 순간 경계가 깨진 신호 [글감]
5. **실전 체크**: 상품 상세 4번 조회 조합은 **경량**. 조회 수는 많지만 판단 로직이 없으므로.

---

## Q3. 재고 차감 — 주문 유스케이스의 "본질"을 누가 책임지는가?

**[질문]**
Round 3 Checklist: "재고 음수 방지는 도메인 레벨에서 처리된다". 프로젝트 코드: `Product.deductStock(quantity)`, `StockQuantity` VO의 음수 방지 내장.

(a) 재고 차감의 책임자 — Order/OrderService/OrderFacade 중?
(b) `StockQuantity` VO만으로 동시 주문에서 재고 음수를 막을 수 있는가?
(c) 부분 실패(A,B 성공, C 실패) 시 처리는 자동인가 명시적인가?

**[1차 답변]**
> a: 옵션 C. 음수 방지에 대한 책임은 product에 있는 stock 에게 줄 수 있고, 여러 상품 차감은 productService에서 순서대로 호출하면 될거 같아.
> b-1: 재고 - 주문개수 < 0, 에러 발생
> b-2: 발생할 수 없다. db에서 조회할 때 a,b 사용자는 5 또는 순서대로 2, 1이 조회될 것이다.
> b-3: 충돌이 될것이다. 도메인에서 막아도 facade에서 막아도 발생할 수 있다.
> c-1: 순서대로 차감이 되어야 성공이다.
> c-2: 순서대로 동작한다는 매커니즘이 필요하다
> c-3: 아니다. @Transactional 는 ACID만 보장하는 것이지 순서를 보장하는것은 아니다

**[1차 재검토 — Race Condition 오해 교정]**
**(b-2) 완전히 틀림** — "DB가 순서대로 조회한다"는 전제 오류. 실제 타임라인:
```
t=0  T1: SELECT stock → 5
t=1  T2: SELECT stock → 5   ← 둘 다 5 읽음
t=2  T1: 5-3=2 가드 통과
t=3  T2: 5-4=1 가드 통과
t=4  T1: UPDATE stock=2
t=5  T2: UPDATE stock=1    ← Lost Update
```
결과: 총 7개 차감되었는데 DB 재고는 1 (원래 5개 → 7개 팔린 셈). **음수는 안 됐지만 더 치명적인 Lost Update**.

사용자 답변 모순 — (b-2): "발생 불가", (b-3): "발생 가능" 서로 충돌. (b-3) 직감이 정답.

**(c-2) 정정** — `@Transactional`의 본질은 **순서**가 아니라 **원자성(atomicity)**. "전부 성공 or 전부 롤백".

**(c-3) 보강** — `@Transactional`은 같은 DB 트랜잭션 안에서만 유효. **트랜잭션 경계를 넘을 때**(예: 결제 API 호출 + DB 저장) Saga/보상 트랜잭션 필요.

**[2차 답변]**
> b-2': 락 필요, 비관적락과 낙관적락 필요
> b-2'': 락의 유무에 따라 다르기 때문이다. 지금 말에 오해를 줄 수 있다. 도메인에서 락을 설정해야 한다 처럼. 하지만 facade에 락을 설정 할 수 있다.

**[Q3 종합 정리]**
1. **재고 차감 책임 분산**:
   - Entity(`Product.deductStock()`) = 자기 상태 변경
   - Domain Service/`StockQuantity` = 단일 트랜잭션 내 invariant 가드
   - Facade = 여러 상품 차감의 흐름 조율 + 트랜잭션 경계
2. **"도메인 레벨 음수 방지"의 한계** — VO의 가드는 **단일 트랜잭션 안**에서만 유효. 동시 주문의 Race Condition은 **인프라 레벨 동시성 제어** 필요 [글감]
3. **Lost Update 개념** — 재고 음수가 안 됐다고 안심하면 안 됨. "총 차감량 vs 실제 감소량" 불일치가 더 치명적.
4. **동시성 제어 2가지**:
   - **비관적 락(Pessimistic)**: `SELECT FOR UPDATE`, 충돌 가정하고 먼저 잠금. 동시성 낮을 때 유리
   - **낙관적 락(Optimistic)**: `@Version`, 충돌 없을 거라 가정. 마지막에 버전 비교 후 실패 시 재시도. 동시성 높지만 충돌 적을 때 유리
   - 프로젝트 `Order.kt`에 `@Version`으로 낙관적 락 사용 중
5. **`@Transactional` = 원자성** — 순서가 아니라 "전부 또는 전무"
6. **Saga의 필요 상황** — 트랜잭션 경계를 넘는 외부 시스템 호출(결제, 알림 등). 프로젝트의 `PaymentGateway.kt`가 근거.
7. **발제 Checklist의 한계 지적** — "도메인 레벨에서 재고 음수 방지"라는 표현은 Race Condition을 고려하면 **오해를 유발**. 도메인 가드와 동시성 제어는 별개의 관심사 [글감]

---

## Q4. 테스트 가능한 구조 — "가장 먼저 고려해야 할 것은?"

**[질문]**
(a) Entity 메서드 / Facade / Race Condition 각각 단위/통합/테스트로 못잡음 분류
(b) Mock vs Fake 의 본질적 차이와 선택 기준
(c) DIP가 테스트 가능성에 미치는 영향 + "가장 먼저 고려해야 할 것" 한 줄 정답

**[1차 답변]**
> a: ① = 단위, ② = 통합, ③ = 통합으로 가능
> b: Mock 은 상태 변경에 초점, fake는 정확한 계산에 초점
> c: 인터페이스를 의존하면 facade에 필요한 메소드만 쓰고, jpa 직접 의존하면 볼륨이 거대해진다

**[1차 재검토]**
**(a-③) 반쪽 정답** — Race Condition은 통합 테스트로 시도는 가능하지만 타이밍 의존/재현성 부족으로 **구조적으로 "완전히" 잡을 수 없는 버그 카테고리**. 근본 해결은 DB 락/트랜잭션 설계, 테스트는 보조 수단. [글감]

**(b) 방향이 거꾸로**
| | 검증 초점 | 한 줄 |
| --- | --- | --- |
| **Mock** | **행위(behavior)** | "호출됐는가? 몇 번?" — interaction 검증 |
| **Fake** | **상태(state)** | 실제 InMemory 구현 — "저장 후 꺼내면 같은가?" — state 검증 |

Fake의 결정적 이점: Mock만 쓰면 "실제 동작을 테스트가 아니라 설정으로 흉내내게" 되는 문제.

**(c) ISP 관찰에 가까움, DIP 핵심 놓침** — 진짜 핵심은 **교체 가능성(substitutability)**. 인터페이스가 있으면 테스트용 Fake 주입 가능, 없으면 항상 실제 DB 필요.

**[2차 답변]**
> 구현체를 변경해도 테스트 코드는 정상동작 하는가를 고려하자

**[2차 평가]**
사용자의 답변이 본질에 닿음. "구현체 교체 가능 = 테스트 정상 동작"은 substitutability의 **테스트 관점 표현**. 내가 제시한 "인터페이스를 두는 것"(구조 관점)과 **동전의 양면**.

**[Q4 종합 정리]**
1. **테스트 계층 분류**:
   - Entity 메서드(자기 상태) → **단위 테스트**
   - Facade/Integration(DB/트랜잭션) → **통합 테스트**
   - Race Condition → **테스트만으론 완전히 못 잡음**. 구조적 보장이 우선
2. **Mock vs Fake**:
   - Mock = 행위(호출 여부/횟수) 검증. 단순한 한두 호출 검증에 유리
   - Fake = 상태 검증. 여러 번 호출 누적 시나리오에 유리
   - 실무 선택 기준: "설정으로 흉내내기 어려운 시나리오"면 Fake, 단순 호출 검증이면 Mock
3. **DIP와 테스트 가능성의 연결** — 교체 가능성(substitutability)이 핵심. 인터페이스 없으면 테스트가 실제 기술(DB, HTTP)에 묶임.
4. **"가장 먼저 고려해야 할 것" 한 줄 정답**:
   - 구조 관점: **외부 기술 의존(DB/HTTP/파일)과 도메인 사이에 인터페이스를 둔다** [글감]
   - 테스트 관점: **구현체를 바꿔도 테스트가 정상 동작하는 구조를 만든다**
5. **프로젝트 적용 사례**:
   - `domain/OrderRepository` ↔ `infrastructure/OrderRepositoryImpl`
   - `domain/payment/PaymentGateway` ↔ 외부 결제 어댑터
   - `domain/like/LikeRepository` ↔ `infrastructure/like/LikeRepositoryImpl`

---

## Q5. 백지 설계 테스트 — 구현 준비도 판정

### 시나리오
이커머스: 여러 상품 한 번에 주문, 브랜드-상품-좋아요, 재고 차감, 좋아요 순 정렬, 동시성 처리, 테스트 가능 구조.

### 백지 답변

**① 도메인 객체 분류**
> Entity => Product, Brand, Like, Order, OrderItem
> VO => LikeCount, Money, StockQuantity, Quantity
> 식별자 유무로 판단

**② 레이어 배치 + DIP**
> ProductRepository 는 도메인 레이어
> ProductService 도메인, ProductFacade 애플리케이션
> ProductService 비즈니스 처리, 다양한 entity 호출
> ProductFacade 흐름/트랜잭션
> ProductFacade → ProductService → ProductRepository ← ProductRepositoryImpl

**③ 유스케이스 흐름**
> 문제를 이해 못하겠어

**④ 불변 조건 + 동시성**
> order에 있는 vo는 모두 불변을 해야한다
> 락을 사용하여 재고 음수를 막고
> facade에서 조합을 하면 좋다

**⑤ 테스트 전략**
> Product.deductStock() 단위테스트
> OrderFacade mock/fake 둘다 (목적이 다르기 때문)
> 동시주문은 e2e 테스트

### 평가 — 커버된 포인트

1. ✅ **Entity/VO 구분 정확** — Product/Brand/Like/Order/OrderItem → Entity, Money/Quantity 류 → VO
2. ✅ **레이어 배치 완벽** — Repository 인터페이스 domain, 구현체 infrastructure, 의존 화살표 방향 정확
3. ✅ **Facade vs Service 구분 유지** — 흐름/트랜잭션 = Facade, 비즈니스 = Service
4. ✅ **락으로 재고 음수 방지** — Q3의 Race Condition 학습 반영
5. ✅ **Facade에서 조합** — Q2의 Aggregate 경계 + 경량 Application 반영
6. ✅ **Mock/Fake 둘 다 사용** — Q4 학습 부분 반영 ("목적이 다르기 때문")

### 평가 — 보완이 필요한 포인트

1. ⚠️ **Entity/VO 구분 근거가 표면적** — "식별자 유무" = 구현 표현. 본질은 **equality 정의**(Identity vs Equality). Round 2 Q3 재확인 필요
2. ⚠️ **"다양한 entity 호출"이 ProductService** 표현 — Q2 "이름 함정" 잔재. 다른 도메인까지 호출하면 Facade 영역
3. ❌ **유스케이스 흐름 답변 누락** — 질문 이해 못함. Controller → Facade → Service → Repository 흐름을 **단계별로** 말할 수 있어야 함
4. ❌ **불변 조건 vs 불변성 혼동** — Round 2 Q5의 핵심 개념.
   - 불변성(immutability): VO가 안 변함
   - 불변 조건(invariant): Entity 상태 간 일관성 규칙 (예: totalAmount == items 합계)
   - Q5 다룬 것은 후자였음. 이 개념이 체화되지 않음.
5. ❌ **Race Condition을 e2e로 잡는다** — Q4 결론과 충돌. **"테스트로 못 잡는 버그 카테고리"** 가 Q4 결론. 구조적 보장(락)이 우선, 테스트는 보조.

### 판정

## ⚖️ 일부 보완 필요 (but 구조 이해는 명확히 상승)

**Round 2 백지 설계 대비 향상된 점:**
- DIP 의존 방향을 화살표로 정확히 표현
- Facade vs Service 역할 분리 체화
- Mock/Fake 구분해서 사용 인지
- 재고 동시성 문제에 락이 필요하다는 점 인식

**반복 재학습 필요한 2가지:**
1. **불변 조건(invariant) 개념** — Round 2 Q5 핵심. Entity 상태 간 일관성 규칙과 immutability의 차이
2. **Race Condition 테스트의 한계** — Q4 결론. "테스트로 모든 걸 잡는다"는 사고에서 "구조로 보장하고 테스트는 보조" 사고로 전환 필요

**구현 시 체크리스트:**
- [ ] 각 Aggregate가 지켜야 할 **invariant**를 리스트로 쓸 수 있는가?
- [ ] Invariant는 **Entity 내부**가 지키는가, 외부(Facade)가 지키는가?
- [ ] 동시성 문제가 있는 지점에 **구조적 보장**(락, @Version)을 걸었는가?
- [ ] Repository 인터페이스는 domain에, 구현체는 infrastructure에 있는가?
- [ ] Facade 안에 `if-else 비즈니스 판단`이 들어와 경량성이 깨지지 않는가?

---

## 📝 블로그 글감 목록 (Round 3)

Round 2 + Round 3 에서 드러난 고민 포인트 정리:

### Round 3 신규 글감

1. **"카운터 공존 패턴은 성능 최적화가 아니라 구조적 필연"** [Q1]
   - RDB는 정렬 키가 컬럼이어야 인덱스 정렬 가능 → 집계 결과를 비정규화하는 건 필수 설계
   - "상품을 좋아요 순으로 정렬"이라는 요구가 카운터 공존의 근본 이유

2. **"정책과 산술의 차이"** [Q1]
   - "+1 증가"는 정책이 아니라 산술. 판단/분기가 들어가야 정책
   - Facade와 Domain Service의 구분에서 실전 판단 기준

3. **"유스케이스의 본질 vs 결과 파생"** [Q1]
   - 재고 차감 = 주문의 본질 (실패 시 전체 롤백)
   - 좋아요 카운터 = 결과 파생 (느슨한 일관성 OK)
   - 이 구분이 동기/비동기 처리 설계의 근거

4. **"경량 Application Layer의 정의"** [Q2]
   - "조합/조율은 OK, 비즈니스 판단은 NG"
   - Facade에 if-else가 들어오는 순간 경계 파열

5. **"도메인 레벨 재고 음수 방지"의 함정** [Q3]
   - VO 가드는 단일 트랜잭션 내 invariant일 뿐
   - Race Condition(Lost Update)은 도메인 가드로 못 막음
   - 동시성 제어는 인프라 레벨 책임

6. **"Race Condition은 테스트로 못 잡는 버그 카테고리"** [Q4]
   - 타이밍 의존, 스레드 스케줄링 통제 불가
   - 근본 해결은 구조적 보장(락), 테스트는 보조

### Round 2 에서 이어진 글감 (재강조)

7. **도메인 분리 기준** — 개별 사건의 식별성(event identity) [R2 Q1]
8. **Entity + 카운터 공존 = CQRS의 맹아** [R2 Q1]
9. **VO와 Primitive Obsession 탈출** [R2 Q2]
10. **Entity vs VO의 equality 정의 차이** [R2 Q3]
11. **Aggregate Root와 불변 조건** [R2 Q5]
12. **Composition Root와 아키텍처 침식 방지** [R2 Q6]

---

## 🔗 구현 연결 포인트

Round 3 구현 시 바로 적용할 수 있는 설계 결정:

1. **좋아요 카운터 증가는 Facade에서 조율**
   - LikeFacade가 LikeService + ProductService 조합
   - `@Transactional`로 원자성 보장
   - 프로젝트 실제 코드(`LikeFacade.kt`)는 Outbox+Kafka로 비동기화했지만 Round 3 단계에선 동기식 Facade 호출이 정답

2. **재고 차감은 Product.deductStock() + 락**
   - Product Entity가 자기 재고 invariant 지키기 (`StockQuantity` VO 음수 방지)
   - 동시성은 `@Version` 낙관적 락 또는 `PESSIMISTIC_WRITE` 비관적 락
   - Facade에서 `@Transactional`로 여러 상품 차감 원자성 보장

3. **상품 상세 조회는 ProductFacade에서 조합**
   - Product + Brand + LikeCount + liked 여부를 Facade가 여러 Service 호출해 조립
   - Product에 `brandId: Long`만 두고 JPA 연관관계 없음 (Aggregate 경계)

4. **Repository 인터페이스는 domain, 구현은 infrastructure**
   - 테스트용 Fake/InMemory 구현체 주입 가능
   - ArchUnit 테스트로 경계 강제

5. **Entity invariant를 생성자/setter에서 강제**
   - `Order.addItem()` → `calculateTotalAmount()` 자동 호출
   - `StockQuantity.minus()` 음수 체크
   - 외부 호출자에게 invariant 유지 책임 전가 금지

---

## 🔜 다음 단계

Round 3 학습 완료. 다음 방향 제안:

1. **구현 단계로 진입**
   - 위 구현 체크리스트를 `/plan` 스킬로 PRD → plan.md 전환
   - `/go` 스킬로 TDD 사이클 진행
   - 단위 테스트 먼저 작성(`/red` → `/green` → `/refactor`)

2. **재학습이 필요한 개념 2가지**
   - **불변 조건(invariant)** — Round 2 Q5 노트 재독 권장
   - **Race Condition과 테스트의 한계** — Q3, Q4 노트 재독 권장

3. **다음 라운드(Round 4) 발제 문서 준비**
   - `docs/quests/round-4.md` 가 있다면 `/learn-round round-4` 로 이어서 진행

---

## 📌 세션 종료 (2026-04-21)

Round 3 학습 Q1~Q5 완료. 백지 설계 판정: **일부 보완 필요 + 구조 이해 상승**.

Round 2 대비 체화된 개념:
- DIP 의존 방향의 실전 적용
- Facade vs Service 분리 원칙
- Mock/Fake 구분 필요성 인식

재학습 필요:
- 불변 조건(invariant) 개념
- Race Condition 테스트의 구조적 한계





