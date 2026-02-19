# Round 3 설계 결정 기록

Round 3에서 도입된 도메인 모델링, 바운디드 컨텍스트, 단일 서비스 전략에 대한 설계 결정을 정리하고, 판단과 그 근거를 기록한다.

---

## 중요도 분류

블로그 포스팅 전에 어떤 고민을 했는지 정리하는 인덱스. 중요도는 "레이어드 아키텍처의 본질과 얼마나 가까운 고민인가"를 기준으로 분류한다.

| 중요도 | 섹션 | 핵심 질문 |
|:---:|------|---------|
| **최상** | [1. Repository 인터페이스 위치와 도메인 순수성](#1-repository-인터페이스-위치와-도메인-순수성) | DIP는 누구를 보호하는가? 도메인 계약에 기술 타입이 침투해도 되는가? |
| **상** | [2. Catalog 바운디드 컨텍스트](#2-catalog-바운디드-컨텍스트) | 도메인 경계를 어디에 그을 것인가? |
| **상** | [3. CatalogService 단일 서비스 전략](#3-catalogservice-단일-서비스-전략) | 같은 BC 내 서비스를 왜 쪼개지 않는가? |
| **중** | [4. 기타 설계 결정](#4-기타-설계-결정) | Entity 생성 패턴, Point 서비스 분리, 조회 메서드 분리 등 도메인별 디테일 |
| **중** | [5. Application 계층 구성 — Facade vs Application Service](#5-application-계층-구성--facade-vs-application-service) | Facade만 둘 것인가? Application Service는 언제 필요한가? |
| **하** | [6. 체크리스트와의 차이](#6-체크리스트와의-차이) | 과제 지침과 다르게 설계한 이유 |
| **참고** | [7. Round 2와의 차이 요약](#7-round-2와의-차이-요약) | 변경 이력 |
| **참고** | [8. 결정의 트레이드오프](#8-결정의-트레이드오프) | 얻은 것과 감수하는 것 |
| **참고** | [9. 코드 리뷰 피드백 — 보류 항목](#9-코드-리뷰-피드백--보류-항목-멱등성--동시성) | 멱등성, 동시성 |

---

## 1. Repository 인터페이스 위치와 도메인 순수성

### 1.1 Repository 인터페이스는 왜 Domain 계층에 있어야 하는가

**문제:** Repository 인터페이스를 Domain 계층에 두는 것(Blue Book 스타일)과 Application 계층에 두는 것(일부 Clean Architecture 해석) 중 어느 쪽이 적절한가? 스터디 내부에서도 이 주제로 긴 토론이 있었다.

**대립하는 두 관점:**

| | Blue Book (Evans) | Red Book (Vernon) / Clean Architecture |
|---|---|---|
| Repository 인터페이스 위치 | Domain 계층 | Domain 계층 (여기까진 동일) |
| Repository **호출** 책임 | Domain Service에서 호출 가능 | Application Service가 호출해야 함 |
| 근거 | 영속화 자체가 도메인 비즈니스 | Repository 호출은 오케스트레이션이므로 Application 책임 |

**사고 과정:**

핵심은 **"DIP(의존 역전)는 누구를 보호하려고 역전시키는가"**이다.

```
Presentation → Application → Domain ← Infrastructure
```

- Repository 인터페이스가 **Domain**에 있으면: Domain이 계약의 주인. Domain Service가 자유롭게 사용. Infrastructure가 바뀌어도 Domain은 안 바뀜. **보호 대상(Domain)이 계약을 소유** → DIP의 본래 목적과 일치.
- Repository 인터페이스가 **Application**에 있으면: Domain Service가 Repository를 쓸 수 없음(Domain → Application 상향 의존 발생). 도메인의 영속화 계약이 Application으로 흩어짐. **보호할 필요가 적은 계층(Application)이 계약을 소유** → DIP 목적과 불일치.

코치의 피드백도 같은 방향:

> "도메인 데이터를 영속화하는 것 자체가 도메인 비즈니스. 도메인 비즈니스를 Application이 갖는 건 레이어링 책임에 맞지 않고, 도메인의 책임이 파편화된 것."

> "도메인 서비스를 두어 도메인 경계를 이를 이용해 가둔 경우는 직접 호출하지 않도록 강제한다."

**결정:** Repository 인터페이스는 Domain 계층에 둔다. Domain Service가 Repository를 직접 호출하여 영속화를 포함한 비즈니스 로직을 수행한다 (Blue Book 스타일).

**근거:**

- DIP는 보호하고 싶은 계층이 인터페이스를 소유해야 의미가 있다. 보호 대상은 Domain이지 Application이 아니다
- "UserRepository.findByLoginId"는 도메인 언어 — 영속화 계약 자체가 도메인의 일부다
- Domain Service가 Repository를 사용할 수 없으면, 모든 영속화 호출이 Application으로 올라가고 Domain이 빈약해진다
- 현재 프로젝트의 모든 Domain Service(CatalogService, UserService, OrderService, LikeService, UserPointService, PointChargingService)가 일관되게 이 패턴을 따른다

**이전 프로젝트에서 Application에 뒀을 때 문제가 없었던 이유:**

그 프로젝트에서는 Domain Service 레이어가 없거나 얇았을 가능성이 높다. Application Service가 비즈니스 로직까지 담당하는 구조라면 Repository 인터페이스가 Application에 있어도 문제없다 — 어차피 영속화 호출을 하는 게 Application이니까. 하지만 **Domain Service가 Repository를 직접 호출하는 구조**에서는, Repository 인터페이스가 반드시 Domain에 있어야 한다.

### 1.2 Domain Repository에서 Spring Data 타입 제거 (Pageable/Page)

**문제:** Repository 인터페이스가 Domain 계층에 있는 건 맞지만, `Pageable`과 `Page<T>`라는 **Spring Data 타입**이 도메인 계약에 침투해 있었다.

```kotlin
// 변경 전 — 도메인 계약에 Spring 의존
interface ProductRepository {
    fun findAll(pageable: Pageable): Page<Product>          // Spring Data 타입
    fun findActiveProducts(..., pageable: Pageable): Page<Product>  // Spring Data 타입
}
```

코치 피드백:

> "Repository는 도메인 언어만을 사용하여 도메인 객체의 영속화 계약을 정의하는 Domain Layer 구성요소다. 도메인 모델을 오염시키는 기술 의존성(Page, Pageable 등)은 포함하지 않는 것이 바람직하다."

> "Pageable은 되도록 사용 안 하는 편. orderBy, page, size 등은 Pageable로 만들 이유가 없다."

**"page/size 파라미터 검증 추가" 피드백과 상반되지 않는가?**

상반되지 않는다. 두 피드백은 **다른 레이어**에 대한 이야기다:

| 피드백 | 대상 레이어 | 내용 |
|---|---|---|
| "page/size에 @Min/@Max 추가" | Controller (Presentation) | 요청 경계에서 입력값 검증 |
| "Pageable을 Repository에 쓰지 마라" | Domain Repository | 도메인 계약에 Spring 타입 노출 금지 |

**결정:** Domain Repository에서 `Pageable`/`Page` 제거. 도메인 고유 타입 `PageResult<T>`를 도입한다.

**변경 내용:**

```kotlin
// 변경 후 — 도메인 계약은 기본 타입만 사용
interface ProductRepository {
    fun findAll(page: Int, size: Int): PageResult<Product>
    fun findActiveProducts(..., page: Int, size: Int): PageResult<Product>
}

// 도메인 고유 페이징 타입
data class PageResult<T>(
    val content: List<T>,
    val totalElements: Long,
    val page: Int,
    val size: Int,
)
```

**레이어별 책임 분리:**

| 레이어 | 페이지네이션 타입 | 역할 |
|---|---|---|
| Controller (Presentation) | `page: Int, size: Int` 파라미터 수신 + 검증 → `PageResult`를 `Page<T>`(Spring)로 변환하여 API 응답 | 기술 타입은 presentation에서만 |
| Domain Service | `page: Int, size: Int`로 Repository 호출 → `PageResult<T>` 반환 | Spring 의존 없음 |
| Domain Repository (인터페이스) | `page: Int, size: Int` → `PageResult<T>` | Spring 의존 없음 |
| Infrastructure (구현체) | `PageRequest.of(page, size)` → JPA 호출 → `PageResult`로 변환 | Spring Data는 여기서만 |

**향후 확장 — UsecaseQuery 패턴:**

복잡한 조회(여러 테이블 조인, DTO 프로젝션)가 필요해지면 Application 계층에 별도 Query 인터페이스를 둘 수 있다. 현재는 단순 CRUD 조회만 있으므로 Domain Repository만으로 충분하다.

---

## 2. Catalog 바운디드 컨텍스트

### 1.1 Product + Brand를 Catalog로 통합하는 이유

**문제:** Round 2에서는 Product와 Brand를 별도 도메인으로 취급했다. 그 결과 상품 상세 조회(Product + Brand), 상품 등록(Brand 유효성 검증), 브랜드 삭제(소속 상품
cascade)에서 매번 Facade를 통한 cross-domain 오케스트레이션이 필요했다.

**사고 과정:**

Round 3 요구사항을 분석하면서, Domain Service와 Facade의 역할 경계를 다시 정리했다.

- **단순 조합** — 두 Repository를 조회해서 조합하는 수준 (예: Product + Brand 상세 조회) → Domain Service로 충분
- **복합 오케스트레이션** — 여러 유스케이스를 조합하고 트랜잭션 원자성이 필요한 경우 (예: 주문 생성 = 재고 차감 + 포인트 검증 + 주문 생성) → Facade가 적절

이 기준으로 보면, 상품 상세 조회는 ProductRepository + BrandRepository를 조합하는 단순 조합이므로 Facade가 아닌 Domain Service에서 처리할 수 있다고 판단했다.
여기서 "ProductDetailService"라는 개념이 나왔다 — Product와 Brand를 조합하는 전용 Domain Service.

ProductDetailService 개념 자체는 나쁘지 않았지만, 의문이 있었다:

- ProductService, BrandService가 이미 있는데 ProductDetailService를 또 만들면 서비스가 3개가 된다
- "Application Layer를 경량 수준으로 구성"이라는 Round 3 목표와 맞는가?
- 근본적으로, Product와 Brand가 별도 도메인이어야 하는가?

**멘토 상담:**

이 고민을 멘토에게 공유했다. 멘토의 조언:

> "Product와 Brand를 Catalog라는 바운디드 컨텍스트의 서브도메인으로 생각하면, 같은 경계 안에 있으므로 Facade를 거칠 필요 없이 Controller → CatalogService로 직접 호출할
> 수 있다."

핵심 인사이트는 **도메인 경계를 재설정**하는 것이었다. Product와 Brand를 별도 도메인으로 두고 Facade로 연결하는 대신, 하나의 Catalog 경계 안에 묶으면 cross-domain 문제 자체가
사라진다.

**결정:** Product와 Brand를 하나의 Catalog 바운디드 컨텍스트로 통합한다.

**근거:**

- 내 초기 분석(단순 조합 → Domain Service)과 멘토의 제안(Catalog 바운디드 컨텍스트)이 같은 방향을 가리킨다
- 상품 탐색 시 브랜드 정보는 필수적으로 함께 제공된다. 대고객 API 3개(상품 목록, 상품 상세, 브랜드 조회) 모두 Product와 Brand를 함께 다룬다
- Catalog 통합으로 ProductFacade, BrandFacade 2개가 완전히 제거된다
- Application Layer가 경량화된다 (Facade 3개 → LikeFacade, OrderFacade, UserFacade만 존재)
- 같은 경계 내에서 Repository를 직접 주입받으므로 Domain Service로 처리 가능

**영향:** 설계 문서 4종(요구사항, 시퀀스, 클래스, 요구사항 분석) 전체에서 ProductFacade/BrandFacade 제거, 서비스 참조를 CatalogService로 통합.

### 1.2 Like를 Catalog에 포함하지 않는 이유

**문제:** Product + Brand를 Catalog로 묶었다면, Product.likeCount를 다루는 Like도 함께 묶어야 하는가?

**선택지:**

| 선택지 | 구성               | 장점                              | 단점                                  |
|-----|------------------|---------------------------------|-------------------------------------|
| A   | Catalog에 Like 포함 | 서비스 하나로 통합, likeCount 갱신이 내부 호출 | Catalog 비대화, 변경 이유가 다른 개념 혼재        |
| B   | Like 별도 도메인 유지   | 관심사 분리, 향후 추천/랭킹 확장 용이          | LikeFacade를 통한 cross-boundary 호출 필요 |

**결정:** 선택지 B. Like는 Catalog와 별도 도메인으로 유지한다.

**근거:**

- Like는 "사용자의 관심 표현"이지 카탈로그 정보가 아니다
- likeCount는 Product에 비정규화된 캐시 값이고, 실제 데이터는 Like 도메인에 있다
- 향후 추천/랭킹 시스템의 기반 데이터로 확장될 가능성이 있다
- Catalog에 포함 시 "상품 정보 변경"과 "사용자 행동 변경"이라는 서로 다른 변경 이유가 혼재되어 응집도가 떨어진다
- LikeFacade를 통한 cross-boundary 호출은 이미 Round 2에서 안정적으로 설계되어 있다

### 1.3 Brand의 성격 정의 — 탐색/전시용

**Gemini 피드백:** "Brand가 단순한 '이름표'인지, '계약 주체'인지를 구분하라. 탐색/전시용이면 Catalog 내부에 포함하고, 계약/정산용이면 별도 컨텍스트로 존재해야 한다."

**결정:** 현재 Brand는 탐색/전시용이다. Catalog 내부에 포함한다.

**근거:**

- 현재 Brand 엔티티는 name만 가진 단순한 분류 정보다
- 정산 계좌, 입점 계약 등의 비즈니스 속성이 없다
- 향후 Partner/MD 컨텍스트가 생기면 그때 분리 검토 (잠재 리스크에 기록)

---

## 3. CatalogService 단일 서비스 전략

### 2.1 서비스를 쪼개지 않는 이유

**문제:** Catalog 바운디드 컨텍스트를 도입하기로 했다. 그렇다면 경계 내부의 서비스를 어떻게 구성할 것인가?

**사고 과정:**

1.1에서 Catalog 바운디드 컨텍스트를 도입한 이유는 Facade를 통한 cross-domain 오케스트레이션을 없애기 위해서였다. 그런데 경계 안에서 ProductService + BrandService를
유지하면 어떻게 되는가?

- 상품 상세 조회: ProductService + BrandService 조합 필요
- 상품 등록: BrandService로 유효성 확인 + ProductService로 생성
- 브랜드 삭제: BrandService로 soft delete + ProductService로 cascade

결국 이 조합을 조율하는 **무언가**(ProductDetailService든, CatalogFacade든)가 다시 필요해진다. Facade를 없애려고 바운디드 컨텍스트를 도입했는데, 서비스를 쪼개면 내부에서 동일한
문제가 반복되는 셈이다.

**멘토 피드백:**

멘토도 같은 맥락에서 서비스 분리에 대해 조언을 줬다:

> "서비스를 여러 개 나누는 걸 허용하지 않습니다. 최상위 서비스는 하나만 만들고 그 안에서 복잡도는 도메인 컴포넌트로 풀어내는 걸 선호합니다. WriterService, ReadService 등을 엄청나게
> 만드는데 그거 자체가 유지 보수를 힘들게 만든다고 생각합니다."

멘토의 예시(WriterService/ReadService)는 기능 단위 분리에 대한 것이고, ProductService/BrandService는 서브도메인 단위 분리이므로 성격이 다르다. 하지만 핵심 문제는 같다:
**같은 바운디드 컨텍스트 안에서 서비스를 쪼개면, 쪼갠 서비스들을 다시 조율해야 하는 구조가 생긴다.**

**선택지:**

| 선택지 | 구성                                                   | 장점             | 단점                                       |
|-----|------------------------------------------------------|----------------|------------------------------------------|
| A   | ProductService + BrandService + ProductDetailService | 역할별 명확한 분리     | 서비스 3개, 조합 시 다시 오케스트레이터 필요 (BC 도입 의미 퇴색) |
| B   | CatalogService 단일 서비스                                | 진입점 명확, 경계와 일치 | 서비스가 커질 수 있음                             |

**결정:** 선택지 B. CatalogService 하나로 통합한다.

**근거:**

- 바운디드 컨텍스트 도입의 목적(Facade 제거)과 일관된 결론이다. 경계 내부에서 서비스를 쪼개면 동일한 오케스트레이션 문제가 반복된다
- 멘토의 "최상위 서비스는 하나" 원칙도 이 판단과 같은 방향을 가리킨다
- 복잡도는 서비스 분리가 아닌 도메인 컴포넌트로 흡수한다: Entity(Product, Brand)와 VO(Stock, Price, BrandName)
- 재고 차감은 `Product.decreaseStock()`, 가격 검증은 `Price` VO, 이름 검증은 `BrandName` VO가 담당
- CatalogService는 도메인 컴포넌트를 조율하는 얇은 레이어로 유지한다

**CatalogService 메서드 목록:**

| 메서드                             | 용도                                     |
|---------------------------------|----------------------------------------|
| `getProductDetail(productId)`   | 상품 상세 조합 (Product + Brand + likeCount) |
| `getProducts(filter)`           | 상품 목록 조회                               |
| `createProduct(command)`        | 상품 등록 (브랜드 유효성 검증 포함)                  |
| `decreaseStocks(items)`         | 주문 시 재고 일괄 차감                          |
| `increaseLikeCount(productId)`  | 좋아요 등록 시 likeCount 증가                  |
| `decreaseLikeCount(productId)`  | 좋아요 취소 시 likeCount 감소                  |
| `getProductsForOrder(ids)`      | 주문 대상 상품 검증                            |
| `getActiveBrand(brandId)`       | 대고객 브랜드 조회                             |
| `getBrand(brandId)`             | 어드민 브랜드 조회                             |
| `createBrand(command)`          | 브랜드 등록                                 |
| `updateBrand(brandId, command)` | 브랜드 수정                                 |
| `deleteBrand(brandId)`          | 브랜드 삭제 + 소속 상품 cascade soft delete     |

**비대화 리스크 대응:**

- 현재 12개 메서드는 관리 가능한 수준
- 복잡도가 증가하면 Catalog 바운디드 컨텍스트 자체를 분리 (예: Brand → Partner 컨텍스트)
- 서비스 내부를 쪼개는 것이 아니라, 경계를 재조정하는 것이 올바른 대응

### 2.2 @Transactional 전략 변경

**Round 2:** Facade에 @Transactional을 걸어 cross-domain 원자성을 보장했다.

**Round 3 변경:** CatalogService가 직접 @Transactional을 관리한다.

- 브랜드 삭제 cascade: CatalogService 내부에서 Brand soft delete + Product 일괄 soft delete를 원자적으로 처리
- 상품 등록: CatalogService 내부에서 Brand 유효성 확인 + Product 생성을 원자적으로 처리
- 기존 cross-domain Facade(@Transactional)는 OrderFacade, LikeFacade에서만 유지

---

## 5. Application 계층 구성 — Facade vs Application Service

### 5.1 AuthService의 위치와 성격

**문제:** `AuthService`는 `application/auth/` 패키지에 있지만, Facade가 아니라 "Service"라는 네이밍을 사용한다. 프로젝트의 application 계층에는 Facade만 있어야 하는가?

**현재 구현:**

```
application/
├── auth/AuthService.kt     ← Facade가 아닌 Application Service
├── user/UserFacade.kt      ← Facade (UserService + UserPointService 조합)
├── like/LikeFacade.kt      ← Facade (LikeService + CatalogService 조합)
└── order/OrderFacade.kt    ← Facade (OrderService + CatalogService + UserPointService 조합)
```

`AuthService`는 `UserService` 하나만 사용하며, cross-domain 오케스트레이션이 아니라 **인증이라는 단일 유스케이스**를 수행한다.

**결정:** 현행 유지. Application 계층에 Facade와 Application Service가 공존할 수 있다.

**근거:**

- **Facade**: 여러 Domain Service를 조합하는 cross-domain 오케스트레이션 (예: OrderFacade)
- **Application Service**: 단일 도메인의 유스케이스를 수행하되, 해당 로직이 도메인 비즈니스가 아닌 애플리케이션 관심사일 때 (예: 인증/세션)
- 인증은 "비밀번호 검증 + 인증 실패 예외 처리"라는 애플리케이션 수준 관심사이며, User 엔티티의 비즈니스 로직(`verifyPassword`)은 도메인에 있다
- 향후 JWT 토큰 발급, 세션 관리 등이 추가되면 AuthService의 책임이 명확히 애플리케이션 레벨임이 드러난다

**구분 기준:**

| 구분 | Application Service | Facade |
|---|---|---|
| 호출하는 Domain Service 수 | 1개 (단일 도메인) | 2개 이상 (cross-domain) |
| 주 관심사 | 유스케이스 수행 | 도메인 간 오케스트레이션 |
| 예시 | AuthService (인증) | OrderFacade (주문 생성) |
| 도입 시점 | 로직이 도메인이 아닌 애플리케이션 관심사일 때 | 여러 도메인을 조합해야 할 때 |

---

## 6. 체크리스트와의 차이

### 3.1 "Application Layer에서 처리했다" vs Domain Service

**문제:** 과제 체크리스트에 "상품 상세 조회 시 Product + Brand 정보 조합은 Application Layer에서 처리했다"라고 되어 있다. 그런데 우리 설계에서는 CatalogService(
Domain Service)에서 처리한다.

**결정:** 체크리스트 원문은 유지하되, 설계가 다른 이유를 문서로 기록한다.

**근거:**

- Catalog 바운디드 컨텍스트를 도입하면 Product + Brand가 같은 경계 내이므로, Application Layer(Facade)를 거칠 필요가 없다
- Domain Service에서 처리하는 것이 "Application Layer를 경량 수준으로 구성"이라는 목표에 부합한다
- 체크리스트는 일반적인 지침이고, 바운디드 컨텍스트 설계에 따라 달라질 수 있다
- PR 제출 시 이 결정의 근거를 리뷰 포인트로 기록한다

---

## 4. 기타 설계 결정

### 4.1 UserFacade 도입

**문제:** 회원가입 시 User 생성과 UserPoint 초기화가 함께 이루어져야 한다.

**결정:** UserFacade를 신설하여 UserService + UserPointService를 조합한다.

**근거:**

- User와 UserPoint는 다른 도메인 경계에 속한다
- 원자성이 필요한 cross-boundary 작업이므로 Facade가 적절하다
- Round 2에서 UserFacade가 없었던 이유: UserPoint가 없었으므로

### 4.2 Point 도메인 — PointChargingService를 별도로 두는 이유

**문제:** CatalogService처럼 Point 도메인도 단일 서비스로 통합해야 하는가?

**사고 과정:**

Catalog에서는 "서비스를 쪼개면 다시 오케스트레이터가 필요해진다"는 이유로 단일 서비스를 선택했다. 그렇다면 Point도 같은 논리로 PointService 하나로 합쳐야 하는가?

여기서 차이가 있다. 과제 요구사항에 "추후 결제 로직이 추가된다"라는 확장 포인트가 명시되어 있다. 결제가 도입되면:

- 포인트 충전 = **결제 확인 + 잔액 변경 + 내역 생성**
- Payment와 Point는 다른 도메인 경계에 속한다
- 이 cross-domain 조율을 위한 Facade(예: PaymentFacade)가 필요해진다

이때 PointChargingService가 Facade의 조율 대상이 된다. 만약 UserPointService에 충전 로직을 합쳐놓으면, Facade가 UserPointService의 일부 메서드만 호출하는 어색한 구조가 된다. 충전이라는 유스케이스 단위로 서비스가 분리되어 있어야 Facade가 깔끔하게 조율할 수 있다.

**Catalog와의 차이:**

| 기준                    | Catalog                          | Point                                    |
|-----------------------|----------------------------------|------------------------------------------|
| 서비스 간 관계              | 같은 BC 내부, 항상 함께 호출              | 충전은 향후 결제 Facade의 조율 대상                  |
| 외부 오케스트레이터 필요성        | 없음 (BC 내부)                      | 있음 (PaymentFacade 예정)                    |
| 단일 서비스로 합칠 때 문제       | 없음                               | Facade가 서비스 일부 메서드만 호출하는 어색한 구조          |

**결정:** UserPointService와 PointChargingService를 분리한다.

**근거:**

- PointChargingService는 향후 결제 Facade의 조율 대상이 될 서비스다. 독립된 단위로 존재해야 Facade 조합이 깔끔하다
- UserPointService는 잔액 조회, 포인트 사용 등 일반적인 CRUD를 담당한다
- Catalog의 단일 서비스 결정과 모순되지 않는다: Catalog는 BC 내부에서 오케스트레이터가 필요 없었고, Point 충전은 BC 경계를 넘는 조율이 예정되어 있다

### 4.3 Order 엔티티 생성 패턴 — private constructor + create()

**문제:** Order는 다른 엔티티(Product, UserPoint)와 달리 `guard()` 메서드를 사용하지 않고, `private constructor` + `companion object { create() }` 패턴을 사용한다. 이 차이를 명확히 기록하지 않으면 "다른 엔티티와 패턴을 통일해야 한다"거나 "Facade에서 OrderItem 조립과 totalPrice 계산을 해야 한다"는 잘못된 판단이 반복될 수 있다.

**선택지:**

| 선택지 | 구성 | 장점 | 단점 |
|-----|------|------|------|
| A | `public constructor` + `guard()` (Product/UserPoint 패턴 통일) | 코드베이스 일관성 | Order는 생성 후 불변이라 재검증할 필드 없음. `guard()`가 의미 없는 코드가 됨 |
| B | `private constructor` + `create()` (현행 유지) | 생성 경로를 하나로 강제, 검증 없는 객체 생성 차단 | 다른 엔티티와 패턴이 다름 |

**결정:** 선택지 B. 현행 유지.

**근거:**

- **Product, UserPoint와의 차이**: 이들은 생성 후에도 `update()`, `charge()`, `use()` 등으로 상태가 변한다. `guard()`가 `@PrePersist`/`@PreUpdate`마다 재검증하는 의미가 있다
- **Order의 특성**: 생성 시 products + command를 조합하여 OrderItem을 만들고 totalPrice를 계산해야 한다. 생성 이후에는 OrderStatus를 제외하면 전부 불변이므로 재검증할 필드가 없다
- **private constructor**: `create()`를 우회하여 검증 없이 객체를 만드는 걸 컴파일 타임에 차단한다
- **Order.create()의 책임**: OrderItem 생성 + totalPrice 계산은 Order 자신의 구성 요소에 대한 책임이다. Facade는 "어떤 서비스를 어떤 순서로 호출할지"만 결정하고, 생성된 Order에서 totalPrice를 추출하여 다음 서비스에 전달한다
- 이 패턴 선택 기준은 CLAUDE.md의 "Entity 생성 패턴 선택 기준"에도 반영하였다

**추가 결정 — OrderProductInfo 도입 (cross-domain 타입 의존 제거):**

`Order.create()`가 `List<Product>`를 받으면 Order 도메인이 Catalog 도메인 타입에 컴파일 타임 의존하게 된다. Entity 필드에서는 `refProductId: Long`으로 ID 참조(Loose Coupling)를 지키고 있으면서, 팩토리 메서드에서 `Product` 타입을 직접 참조하는 것은 일관성이 없다.

- `OrderProductInfo(id, name, price)` 데이터 클래스를 `domain/order/` 패키지에 도입
- `Order.create()`와 `OrderItem.create()`가 `Product` 대신 `OrderProductInfo`를 수신
- `Product` → `OrderProductInfo` 변환(cross-domain 매핑)은 `OrderFacade`에서 수행 (Facade의 역할)
- 이로써 Order 도메인은 Catalog 도메인 타입에 전혀 의존하지 않게 됨

### 4.4 CatalogService 조회 메서드 분리 — 호출 시점에 따른 검증 수준

**문제:** `getActiveProductsByIds`와 `getProductsForOrder`는 둘 다 "ID 리스트로 상품을 조회"하지만 검증 수준이 다르다. 하나로 통합할 수 있지 않은가?

**현재 구현:**

| 메서드 | 호출처 | 전략 |
|--------|--------|------|
| `getActiveProductsByIds` | LikeFacade (좋아요 목록 조회) | **관대한 필터링** — 삭제/HIDDEN을 조용히 걸러냄. 에러 없이 존재하는 활성 상품만 반환 |
| `getProductsForOrder` | OrderFacade (주문 생성) | **엄격한 검증** — 누락/삭제/비판매 상품이 하나라도 있으면 즉시 예외 |

**결정:** 메서드를 분리 유지한다.

**근거:**

- **호출 시점(context)의 차이**: 좋아요 목록은 "예전에 좋아요한 상품이 지금 없어도" 나머지만 보여주면 되지만, 주문은 "결제 직전에 상품 하나라도 문제가 있으면" 즉시 실패시켜야 한다
- **통합 시 문제**: `strict: Boolean` 같은 플래그 파라미터가 생기거나 내부 분기가 복잡해져서 각 호출 맥락의 의도가 흐려진다
- 메서드명이 호출 맥락의 의도를 명확하게 드러낸다

### 4.5 도메인 경계 정의

Round 3에서 확정한 바운디드 컨텍스트와 소속 엔티티:

| 바운디드 컨텍스트   | 소속 엔티티/VO                                                          | 서비스                                    | 비고                 |
|-------------|--------------------------------------------------------------------|----------------------------------------|--------------------|
| **User**    | User                                                               | UserService                            | 기존 유지              |
| **Catalog** | Product, Brand, Stock(VO), Price(VO), BrandName(VO), ProductStatus | CatalogService                         | Product + Brand 통합 |
| **Like**    | Like                                                               | LikeService                            | 별도 유지 (사용자 관심 표현)  |
| **Order**   | Order, OrderItem, Quantity(VO), OrderStatus                        | OrderService                           | 기존 유지              |
| **Point**   | UserPoint, PointHistory, Point(VO), PointHistoryType               | UserPointService, PointChargingService | 신규 도메인             |

### 4.6 패키지 구조

```
domain/
├── catalog/
│   ├── CatalogService.kt      ← 단일 서비스
│   ├── product/
│   │   ├── Product.kt
│   │   └── ProductRepository.kt
│   └── brand/
│       ├── Brand.kt
│       └── BrandRepository.kt
├── like/
├── order/
├── point/
└── user/

application/
├── user/     ← UserFacade (회원가입)
├── like/     ← LikeFacade (좋아요)
└── order/    ← OrderFacade (주문 생성)
```

---

## 7. Round 2와의 차이 요약

| 항목        | Round 2                                              | Round 3                                         |
|-----------|------------------------------------------------------|-------------------------------------------------|
| 도메인 경계    | Product, Brand 별도                                    | Catalog (Product + Brand 통합)                    |
| 서비스 구조    | ProductService + BrandService + ProductDetailService | CatalogService 단일                               |
| Facade    | ProductFacade, BrandFacade, OrderFacade, LikeFacade  | OrderFacade, LikeFacade, UserFacade             |
| 상품 상세 조합  | ProductFacade (Application Layer)                    | CatalogService (Domain Layer)                   |
| 상품 등록     | ProductFacade → BrandService + ProductService        | CatalogService (내부에서 Brand 검증)                  |
| 브랜드 삭제    | BrandFacade → BrandService + ProductService          | CatalogService (내부에서 cascade)                   |
| 테스트 전략    | E2E 위주                                               | 단위 테스트 중심 (Fake/Stub)                           |
| Point 도메인 | 없음                                                   | UserPoint + PointHistory + PointChargingService |

---

## 8. 결정의 트레이드오프

### 얻은 것

- **Application Layer 경량화**: Facade 5개 → 3개. 상품/브랜드 관련 Facade 완전 제거
- **진입점 단순화**: Catalog 관련 로직은 무조건 CatalogService로 진입
- **멘토 원칙 준수**: "최상위 서비스는 하나"
- **문서 일관성**: 바운디드 컨텍스트 기준으로 모든 설계 문서가 정렬됨

### 감수하는 것

- **CatalogService 비대화 리스크**: 메서드 12개. Entity/VO로 복잡도를 분산하지만 향후 증가 가능
- **체크리스트와의 차이**: "Application Layer에서 처리"가 아닌 "Domain Service에서 처리". PR에서 설명 필요
- **Point 서비스 2개**: 멘토 원칙에 따르면 PointService 하나로 통합할 수 있으나, 현재는 관심사 분리를 우선함. 구현 시 재검토

---

## 9. 코드 리뷰 피드백 — 보류 항목 (멱등성 / 동시성)

Round 3 코드 리뷰에서 식별된 운영 안정성 이슈 중, 현재 라운드 범위를 초과하거나 설계 결정이 추가로 필요한 항목을 기록한다.

### 7.1 usePoints 멱등성 미보장

**현상:** `UserPointService.usePoints()`가 `refOrderId`를 `PointHistory`에 저장하지만, 동일 `refOrderId`로 중복 호출 시 포인트가 이중 차감된다.

**영향:** 네트워크 재시도, 이벤트 중복 발행 시 잔액 불일치 장애.

**수정 방향:**
- `PointHistory`에 `refOrderId` 유니크 제약 추가, 또는
- `usePoints()` 진입 시 `pointHistoryRepository.existsByRefOrderId(refOrderId)` 체크

**보류 사유:** 멱등성 전략(DB 유니크 vs 서비스 레벨 체크 vs 분산 락)은 결제 도메인 도입 시 함께 설계하는 것이 적절하다. `round3-decisions.md` 4.2절의 "추후 결제 로직 추가" 확장 포인트와 연관된다.

### 7.2 chargePoints 멱등성 미보장

**현상:** `PointChargingService.charge(userId, amount)`에 `idempotencyKey`가 없어 동일 충전 요청의 중복 처리를 구분할 수 없다.

**영향:** 클라이언트 재시도 시 포인트 중복 충전.

**수정 방향:**
- 요청에 `idempotencyKey` 필드 추가 + `PointHistory`에 `(refUserPointId, idempotencyKey)` 복합 유니크 인덱스
- 중복 요청 시 HTTP 409 또는 기존 결과 반환(멱등 응답)

**보류 사유:** 7.1과 동일. 충전/사용 양쪽의 멱등성 전략을 통일하여 설계해야 하며, PaymentFacade 도입 시 `idempotencyKey` 전파 경로도 함께 결정해야 한다.

### 7.3 decreaseStocks 동시성 제어 부재

**현상:** `CatalogService.decreaseStocks()`에 `@Transactional`은 있으나, `Product` 엔티티에 `@Version`(낙관적 락)이나 비관적 락이 없어 동시 주문 시 재고 손실(lost update)이 발생할 수 있다.

**영향:** 두 주문이 동시에 같은 재고를 조회 → 각각 차감 → 마지막 쓰기만 반영되어 재고가 실제보다 많이 남음.

**수정 방향:**
- `Product` 엔티티에 `@Version` 추가 (낙관적 락, 충돌 시 `OptimisticLockException` → 재시도), 또는
- `ProductRepository`에 `@Lock(PESSIMISTIC_WRITE)` 조회 메서드 추가, 또는
- 재고 차감을 DB 레벨 원자적 연산(`UPDATE ... SET stock = stock - :qty WHERE stock >= :qty`)으로 변경

**보류 사유:** 락 전략 선택(낙관적 vs 비관적 vs DB 원자 연산)은 예상 동시성 수준과 성능 요구사항에 따라 달라진다. 현재 라운드에서는 기능 완성도를 우선하고, 성능/운영 요구사항이 구체화되면 적용한다.
