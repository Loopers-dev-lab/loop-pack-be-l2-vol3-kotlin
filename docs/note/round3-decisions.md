# Round 3 설계 결정 기록

Round 3에서 도입된 도메인 모델링, 바운디드 컨텍스트, 단일 서비스 전략에 대한 설계 결정을 정리하고, 판단과 그 근거를 기록한다.

---

## 중요도 분류

블로그 포스팅 전에 어떤 고민을 했는지 정리하는 인덱스. 중요도는 "레이어드 아키텍처의 본질과 얼마나 가까운 고민인가"를 기준으로 분류한다.

|  중요도   | 섹션                                                                                                          | 핵심 질문                                            |
|:------:|-------------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| **최상** | [1. Repository 인터페이스 위치와 도메인 순수성](#1-repository-인터페이스-위치와-도메인-순수성)                                          | DIP는 누구를 보호하는가? 도메인 계약에 기술 타입이 침투해도 되는가?         |
| **상**  | [2. Catalog 바운디드 컨텍스트](#2-catalog-바운디드-컨텍스트)                                                                | 도메인 경계를 어디에 그을 것인가?                              |
| **상**  | [3. CatalogService 단일 서비스 전략](#3-catalogservice-단일-서비스-전략)                                                  | 같은 BC 내 서비스를 왜 쪼개지 않는가?                          |
| **중**  | [4. 기타 설계 결정](#4-기타-설계-결정)                                                                                  | Entity 생성 패턴, Point 서비스 분리, 조회 메서드 분리 등 도메인별 디테일 |
| **중**  | [5. Application 계층 구성 — Facade vs Application Service](#5-application-계층-구성--facade-vs-application-service) | Facade만 둘 것인가? Application Service는 언제 필요한가?     |
| **하**  | [6. 체크리스트와의 차이](#6-체크리스트와의-차이)                                                                              | 과제 지침과 다르게 설계한 이유                                |
| **참고** | [7. Round 2와의 차이 요약](#7-round-2와의-차이-요약)                                                                    | 변경 이력                                            |
| **참고** | [8. 결정의 트레이드오프](#8-결정의-트레이드오프)                                                                              | 얻은 것과 감수하는 것                                     |
| **참고** | [9. 코드 리뷰 피드백 — 보류 항목](#9-코드-리뷰-피드백--보류-항목-멱등성--동시성)                                                        | 멱등성, 동시성                                         |
| **상**  | [10. 계약에 의한 설계 (Design by Contract)](#10-계약에-의한-설계-design-by-contract)                                      | 도메인 메서드와 서비스 메서드의 계약은 어떻게 정의하는가?                 |
| **상**  | [11. 어드민/대고객 서비스 메서드 분리 원칙](#11-어드민대고객-서비스-메서드-분리-원칙)                                                       | 대고객 조회 메서드를 어드민 CUD에서 재사용해도 되는가?                 |
| **최상** | [12. Pragmatic Clean Architecture 도입](#12-pragmatic-clean-architecture-도입)                                         | Domain Model과 JPA Entity를 왜, 어떻게 분리하는가?         |

---

## 1. Repository 인터페이스 위치와 도메인 순수성

### 1.1 Repository 인터페이스는 왜 Domain 계층에 있어야 하는가

**문제:** Repository 인터페이스를 Domain 계층에 두는 것(Blue Book 스타일)과 Application 계층에 두는 것(일부 Clean Architecture 해석) 중 어느 쪽이 적절한가?
스터디 내부에서도 이 주제로 긴 토론이 있었다.

**대립하는 두 관점:**

|                      | Blue Book (Evans)      | Red Book (Vernon) / Clean Architecture   |
|----------------------|------------------------|------------------------------------------|
| Repository 인터페이스 위치  | Domain 계층              | Domain 계층 (여기까진 동일)                      |
| Repository **호출** 책임 | Domain Service에서 호출 가능 | Application Service가 호출해야 함              |
| 근거                   | 영속화 자체가 도메인 비즈니스       | Repository 호출은 오케스트레이션이므로 Application 책임 |

**사고 과정:**

핵심은 **"DIP(의존 역전)는 누구를 보호하려고 역전시키는가"**이다.

```
Presentation → Application → Domain ← Infrastructure
```

- Repository 인터페이스가 **Domain**에 있으면: Domain이 계약의 주인. Domain Service가 자유롭게 사용. Infrastructure가 바뀌어도 Domain은 안 바뀜. **보호
  대상(Domain)이 계약을 소유** → DIP의 본래 목적과 일치.
- Repository 인터페이스가 **Application**에 있으면: Domain Service가 Repository를 쓸 수 없음(Domain → Application 상향 의존 발생). 도메인의 영속화
  계약이 Application으로 흩어짐. **보호할 필요가 적은 계층(Application)이 계약을 소유** → DIP 목적과 불일치.

코치의 피드백도 같은 방향:

> "도메인 데이터를 영속화하는 것 자체가 도메인 비즈니스. 도메인 비즈니스를 Application이 갖는 건 레이어링 책임에 맞지 않고, 도메인의 책임이 파편화된 것."

> "도메인 서비스를 두어 도메인 경계를 이를 이용해 가둔 경우는 직접 호출하지 않도록 강제한다."

**결정:** Repository 인터페이스는 Domain 계층에 둔다. Domain Service가 Repository를 직접 호출하여 영속화를 포함한 비즈니스 로직을 수행한다 (Blue Book 스타일).

**근거:**

- DIP는 보호하고 싶은 계층이 인터페이스를 소유해야 의미가 있다. 보호 대상은 Domain이지 Application이 아니다
- "UserRepository.findByLoginId"는 도메인 언어 — 영속화 계약 자체가 도메인의 일부다
- Domain Service가 Repository를 사용할 수 없으면, 모든 영속화 호출이 Application으로 올라가고 Domain이 빈약해진다
- 현재 프로젝트의 모든 Domain Service(CatalogService, UserService, OrderService, LikeService, UserPointService,
  PointChargingService)가 일관되게 이 패턴을 따른다

**이전 프로젝트에서 Application에 뒀을 때 문제가 없었던 이유:**

그 프로젝트에서는 Domain Service 레이어가 없거나 얇았을 가능성이 높다. Application Service가 비즈니스 로직까지 담당하는 구조라면 Repository 인터페이스가 Application에
있어도 문제없다 — 어차피 영속화 호출을 하는 게 Application이니까. 하지만 **Domain Service가 Repository를 직접 호출하는 구조**에서는, Repository 인터페이스가 반드시
Domain에 있어야 한다.

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

> "Repository는 도메인 언어만을 사용하여 도메인 객체의 영속화 계약을 정의하는 Domain Layer 구성요소다. 도메인 모델을 오염시키는 기술 의존성(Page, Pageable 등)은 포함하지 않는 것이
> 바람직하다."

> "Pageable은 되도록 사용 안 하는 편. orderBy, page, size 등은 Pageable로 만들 이유가 없다."

**"page/size 파라미터 검증 추가" 피드백과 상반되지 않는가?**

상반되지 않는다. 두 피드백은 **다른 레이어**에 대한 이야기다:

| 피드백                           | 대상 레이어                    | 내용                      |
|-------------------------------|---------------------------|-------------------------|
| "page/size에 @Min/@Max 추가"     | Controller (Presentation) | 요청 경계에서 입력값 검증          |
| "Pageable을 Repository에 쓰지 마라" | Domain Repository         | 도메인 계약에 Spring 타입 노출 금지 |

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

| 레이어                       | 페이지네이션 타입                                                                          | 역할                     |
|---------------------------|------------------------------------------------------------------------------------|------------------------|
| Controller (Presentation) | `page: Int, size: Int` 파라미터 수신 + 검증 → `PageResult`를 `Page<T>`(Spring)로 변환하여 API 응답 | 기술 타입은 presentation에서만 |
| Domain Service            | `page: Int, size: Int`로 Repository 호출 → `PageResult<T>` 반환                         | Spring 의존 없음           |
| Domain Repository (인터페이스) | `page: Int, size: Int` → `PageResult<T>`                                           | Spring 의존 없음           |
| Infrastructure (구현체)      | `PageRequest.of(page, size)` → JPA 호출 → `PageResult`로 변환                           | Spring Data는 여기서만      |

**향후 확장 — UsecaseQuery 패턴:**

복잡한 조회(여러 테이블 조인, DTO 프로젝션)가 필요해지면 Application 계층에 별도 Query 인터페이스를 둘 수 있다. 현재는 단순 CRUD 조회만 있으므로 Domain Repository만으로
충분하다.

---

## 2. Catalog 바운디드 컨텍스트

### 2.1 Product + Brand를 Catalog로 통합하는 이유

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

### 2.2 Like를 Catalog에 포함하지 않는 이유

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

### 2.3 Brand의 성격 정의 — 탐색/전시용

**Gemini 피드백:** "Brand가 단순한 '이름표'인지, '계약 주체'인지를 구분하라. 탐색/전시용이면 Catalog 내부에 포함하고, 계약/정산용이면 별도 컨텍스트로 존재해야 한다."

**결정:** 현재 Brand는 탐색/전시용이다. Catalog 내부에 포함한다.

**근거:**

- 현재 Brand는 name만 가진 단순한 분류 정보다
- 정산 계좌, 입점 계약 등의 비즈니스 속성이 없다
- 향후 Partner/MD 컨텍스트가 생기면 그때 분리 검토 (잠재 리스크에 기록)

---

## 3. CatalogService 단일 서비스 전략

### 3.1 서비스를 쪼개지 않는 이유

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
- 복잡도는 서비스 분리가 아닌 도메인 컴포넌트로 흡수한다: Entity(Product, Brand)와 VO(Stock, Point)
- 재고 차감은 `Product.decreaseStock()`, 가격 검증은 `Price` VO에서 자가 검증, 이름 검증은 `BrandName` VO에서 자가 검증
- CatalogService는 도메인 컴포넌트를 조율하는 얇은 레이어로 유지한다

**CatalogService 메서드 목록:**

| 메서드                                      | 용도                                     |
|------------------------------------------|----------------------------------------|
| `createBrand(command)`                   | 브랜드 등록                                 |
| `updateBrand(brandId, command)`          | 브랜드 수정                                 |
| `deleteBrand(brandId)`                   | 브랜드 삭제 + 소속 상품 cascade soft delete     |
| `restoreBrand(brandId)`                  | 브랜드 복구                                 |
| `getActiveBrand(brandId)`                | 대고객 브랜드 조회                             |
| `getBrand(brandId)`                      | 어드민 브랜드 조회                             |
| `getBrands(page, size)`                  | 브랜드 목록 조회                              |
| `createProduct(command)`                 | 상품 등록 (브랜드 유효성 검증 포함)                  |
| `updateProduct(productId, command)`      | 상품 수정                                  |
| `deleteProduct(productId)`               | 상품 삭제                                  |
| `restoreProduct(productId)`              | 상품 복구                                  |
| `getProducts(brandId, sort, page, size)` | 대고객 상품 목록 조회                           |
| `getProductDetail(productId)`            | 상품 상세 조합 (Product + Brand + likeCount) |
| `getProduct(productId)`                  | 내부용 단건 조회                              |
| `getActiveProduct(productId)`            | 대고객 활성 상품 단건 조회                        |
| `getActiveProductsByIds(productIds)`     | 활성 상품 ID 리스트 조회 (좋아요 목록)               |
| `getProductsForOrder(productIds)`        | 주문 대상 상품 검증                            |
| `decreaseStocks(items)`                  | 주문 시 재고 일괄 차감                          |
| `increaseLikeCount(productId)`           | 좋아요 등록 시 likeCount 증가                  |
| `decreaseLikeCount(productId)`           | 좋아요 취소 시 likeCount 감소 (삭제된 상품은 무시)     |
| `getAdminProducts(page, size)`           | 어드민 상품 목록 조회                           |
| `getAdminProduct(productId)`             | 어드민 상품 단건 조회                           |

**비대화 리스크 대응:**

- 현재 22개 메서드는 관리 가능한 수준
- 복잡도가 증가하면 Catalog 바운디드 컨텍스트 자체를 분리 (예: Brand → Partner 컨텍스트)
- 서비스 내부를 쪼개는 것이 아니라, 경계를 재조정하는 것이 올바른 대응

### 3.2 @Transactional 전략 변경

**Round 2:** Facade에 @Transactional을 걸어 cross-domain 원자성을 보장했다.

**Round 3 변경:** CatalogService가 직접 @Transactional을 관리한다.

- 브랜드 삭제 cascade: CatalogService 내부에서 Brand soft delete + Product 일괄 soft delete를 원자적으로 처리
- 상품 등록: CatalogService 내부에서 Brand 유효성 확인 + Product 생성을 원자적으로 처리
- 기존 cross-domain Facade(@Transactional)는 OrderFacade, LikeFacade에서만 유지

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

이때 PointChargingService가 Facade의 조율 대상이 된다. 만약 UserPointService에 충전 로직을 합쳐놓으면, Facade가 UserPointService의 일부 메서드만 호출하는
어색한 구조가 된다. 충전이라는 유스케이스 단위로 서비스가 분리되어 있어야 Facade가 깔끔하게 조율할 수 있다.

**Catalog와의 차이:**

| 기준              | Catalog            | Point                           |
|-----------------|--------------------|---------------------------------|
| 서비스 간 관계        | 같은 BC 내부, 항상 함께 호출 | 충전은 향후 결제 Facade의 조율 대상         |
| 외부 오케스트레이터 필요성  | 없음 (BC 내부)         | 있음 (PaymentFacade 예정)           |
| 단일 서비스로 합칠 때 문제 | 없음                 | Facade가 서비스 일부 메서드만 호출하는 어색한 구조 |

**결정:** UserPointService와 PointChargingService를 분리한다.

**근거:**

- PointChargingService는 향후 결제 Facade의 조율 대상이 될 서비스다. 독립된 단위로 존재해야 Facade 조합이 깔끔하다
- UserPointService는 잔액 조회, 포인트 사용 등 일반적인 CRUD를 담당한다
- Catalog의 단일 서비스 결정과 모순되지 않는다: Catalog는 BC 내부에서 오케스트레이터가 필요 없었고, Point 충전은 BC 경계를 넘는 조율이 예정되어 있다

### 4.3 Order 생성 패턴 — private constructor + create()

**문제:** Order는 다른 Domain Model(Product, UserPoint)과 달리 `public constructor + init { validate() }` 패턴이 아닌, `private constructor` +
`companion object { create() }` 패턴을 사용한다. 이 차이를 명확히 기록하지 않으면 "다른 Domain Model과 패턴을 통일해야 한다"거나 "Facade에서 OrderItem 조립과 totalPrice
계산을 해야 한다"는 잘못된 판단이 반복될 수 있다.

**선택지:**

| 선택지 | 구성                                                         | 장점                            | 단점                                                       |
|-----|------------------------------------------------------------|-------------------------------|----------------------------------------------------------|
| A   | `public constructor` + `init { validate() }` (Product/UserPoint 패턴 통일) | 코드베이스 일관성                     | Order는 생성 시 복합 조립(OrderItem + totalPrice 계산)이 필요해 단순 생성 패턴에 맞지 않음 |
| B   | `private constructor` + `create()` (현행 유지)                              | 생성 경로를 하나로 강제, 검증 없는 객체 생성 차단 | 다른 Domain Model과 패턴이 다름                                          |

**결정:** 선택지 B. 현행 유지.

**근거:**

- **Product, UserPoint와의 차이**: 이들은 독립적으로 생성 가능하며, `init { validate() }`로 생성 시점에 불변식을 검증한다. 상태 변경은 `update()`, `charge()`, `use()` 등 도메인 메서드가 내부에서 불변식을 유지한다
- **Order의 특성**: `Order.create()`가 `OrderItem` 생성과 `totalPrice` 계산을 내부에서 직접 수행한다. 생성이라는 행위 자체가 복합 조립이므로 팩토리 메서드가 유일한 생성 경로여야 한다. Order와 OrderItem은 JPA 연관관계 매핑(@OneToMany/@ManyToOne) 없이 `refOrderId` FK로 연결되며, 별도 `OrderItemRepository`로 관리한다. 생성 후에도 `cancelItem()` 등 명시적 도메인 메서드를 통해 상태가 변할 수 있으며, 이 메서드들이 내부에서 불변식(totalPrice 재계산 등)을 직접 유지한다
- **private constructor**: `create()`를 우회하여 검증 없이 객체를 만드는 걸 컴파일 타임에 차단한다
- 이 패턴 선택 기준은 CLAUDE.md의 "Entity 생성 패턴 선택 기준"에도 반영하였다
- **OrderItem도 동일한 패턴을 따른다**: `private constructor` + `create()`. `Order.create()` 내부에서만 생성되며, 상태 변경(취소 등)은 반드시 Order를 통해서만 접근한다. `init` 블록에서 `quantity >= 1` 직접 검증으로 생성 시점 검증만 수행

**추가 결정 — OrderProductInfo 도입 (cross-domain 타입 의존 제거):**

`Order.create()`가 `List<Product>`를 받으면 Order 도메인이 Catalog 도메인 타입에 컴파일 타임 의존하게 된다. Entity 필드에서는 `refProductId: Long`으로 ID
참조(Loose Coupling)를 지키고 있으면서, 팩토리 메서드에서 `Product` 타입을 직접 참조하는 것은 일관성이 없다.

- `OrderProductInfo(id, name, price)` 데이터 클래스를 `domain/order/` 패키지에 도입
- `Order.create()`와 `OrderItem.create()`가 `Product` 대신 `OrderProductInfo`를 수신
- `Product` → `OrderProductInfo` 변환(cross-domain 매핑)은 `OrderFacade`에서 수행 (Facade의 역할)
- 이로써 Order 도메인은 Catalog 도메인 타입에 전혀 의존하지 않게 됨

### 4.4 CatalogService 조회 메서드 분리 — 호출 시점에 따른 검증 수준

**문제:** `getActiveProductsByIds`와 `getProductsForOrder`는 둘 다 "ID 리스트로 상품을 조회"하지만 검증 수준이 다르다. 하나로 통합할 수 있지 않은가?

**현재 구현:**

| 메서드                      | 호출처                    | 전략                                                     |
|--------------------------|------------------------|--------------------------------------------------------|
| `getActiveProductsByIds` | LikeFacade (좋아요 목록 조회) | **관대한 필터링** — 삭제/HIDDEN을 조용히 걸러냄. 에러 없이 존재하는 활성 상품만 반환 |
| `getProductsForOrder`    | OrderFacade (주문 생성)    | **엄격한 검증** — 누락/삭제/비판매 상품이 하나라도 있으면 즉시 예외              |

**결정:** 메서드를 분리 유지한다.

**근거:**

- **호출 시점(context)의 차이**: 좋아요 목록은 "예전에 좋아요한 상품이 지금 없어도" 나머지만 보여주면 되지만, 주문은 "결제 직전에 상품 하나라도 문제가 있으면" 즉시 실패시켜야 한다
- **통합 시 문제**: `strict: Boolean` 같은 플래그 파라미터가 생기거나 내부 분기가 복잡해져서 각 호출 맥락의 의도가 흐려진다
- 메서드명이 호출 맥락의 의도를 명확하게 드러낸다

### 4.5 도메인 경계 정의

Round 3에서 확정한 바운디드 컨텍스트와 소속 엔티티:

| 바운디드 컨텍스트   | 소속 Domain Model/VO                                                                   | 서비스                                    | 비고                 |
|-------------|-----------------------------------------------------------------------------|----------------------------------------|--------------------|
| **User**    | User                                                                        | UserService                            | 기존 유지              |
| **Catalog** | Product, Brand, Stock(VO), ProductStatus                                    | CatalogService                         | Product + Brand 통합 |
| **Like**    | Like                                                                        | LikeService                            | 별도 유지 (사용자 관심 표현)  |
| **Order**   | Order, OrderItem, OrderProductInfo, OrderStatus                             | OrderService                           | 기존 유지              |
| **Point**   | UserPoint, PointHistory, Point(VO), PointHistoryType (PointHistory 내부 enum) | UserPointService, PointChargingService | 신규 도메인             |

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

## 5. Application 계층 구성 — Facade vs Application Service

### 5.1 AuthService 제거 — Blue Book 스타일 일관성

**문제:** `AuthService`는 `application/auth/` 패키지에 있었지만, Red Book 스타일(Application Service가 도메인 서비스를 오케스트레이션)에 해당했다. 현재
프로젝트는 Blue Book 스타일(Domain Service가 Repository를 직접 호출)을 따르고 있어 일관성이 깨져 있었다.

**기존 구조:**

```
application/auth/AuthService.kt  →  UserService.getUserInfo()  →  User.verifyPassword()
```

`AuthService`가 하는 일은 `UserService` 호출 + 비밀번호 검증뿐이었다. Application 계층에 별도 서비스를 두어 단순 위임하는 것은 Red Book 패턴이며, Blue Book에서는
불필요한 간접 레이어다.

**결정:** AuthService를 제거하고, AuthInterceptor가 UserService를 직접 호출한다.

**변경 후 구조:**

```
AuthInterceptor  →  UserService.getUserInfo()  →  User.verifyPassword()
```

```
application/
├── user/UserFacade.kt      ← Facade (UserService + UserPointService 조합)
├── like/LikeFacade.kt      ← Facade (LikeService + CatalogService 조합)
└── order/OrderFacade.kt    ← Facade (OrderService + CatalogService + UserPointService 조합)
```

**근거:**

- Blue Book 스타일에서 인증 로직은 Application Service가 아닌 인프라(Interceptor) + 도메인(User.verifyPassword)으로 분리하는 것이 자연스럽다
- AuthInterceptor는 인증이라는 인프라 관심사를 담당하고, 비밀번호 검증은 User 엔티티의 도메인 메서드로 이미 캡슐화되어 있다
- Application 계층에 Facade만 남으므로 계층의 역할이 명확해진다: **cross-domain 오케스트레이션 전용**
- 향후 JWT/세션 관리가 추가되면 Spring Security 필터 체인으로 전환하면 되며, AuthService 레이어를 거칠 필요는 여전히 없다

**Application Service가 필요한 경우의 기준 (향후 참고):**

| 구분                    | Application Service                                        | Facade               |
|-----------------------|------------------------------------------------------------|----------------------|
| 호출하는 Domain Service 수 | 1개 (단일 도메인)                                                | 2개 이상 (cross-domain) |
| 주 관심사                 | 유스케이스 수행                                                   | 도메인 간 오케스트레이션        |
| 도입 시점                 | 로직이 도메인이 아닌 애플리케이션 관심사이면서, Interceptor/Filter로 처리할 수 없는 경우 | 여러 도메인을 조합해야 할 때     |

---

## 6. 체크리스트와의 차이

### 6.1 "Application Layer에서 처리했다" vs Domain Service

**문제:** 과제 체크리스트에 "상품 상세 조회 시 Product + Brand 정보 조합은 Application Layer에서 처리했다"라고 되어 있다. 그런데 우리 설계에서는 CatalogService(
Domain Service)에서 처리한다.

**결정:** 체크리스트 원문은 유지하되, 설계가 다른 이유를 문서로 기록한다.

**근거:**

- Catalog 바운디드 컨텍스트를 도입하면 Product + Brand가 같은 경계 내이므로, Application Layer(Facade)를 거칠 필요가 없다
- Domain Service에서 처리하는 것이 "Application Layer를 경량 수준으로 구성"이라는 목표에 부합한다
- 체크리스트는 일반적인 지침이고, 바운디드 컨텍스트 설계에 따라 달라질 수 있다
- PR 제출 시 이 결정의 근거를 리뷰 포인트로 기록한다

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
| Domain Model | JPA Entity 겸용                                       | 순수 POJO (JPA Entity 완전 분리)                     |
| 검증 전략    | `guard()` (`@PrePersist`/`@PreUpdate`)                  | `init { validate() }` + VO 자가 검증               |
| VO 전략     | `@Converter` 필요로 제한적 사용                              | `@JvmInline value class` 적극 활용                 |
| Repository  | `XxxJpaRepository` 별도 파일                              | `XxxRepositoryImpl.kt` 내 `internal interface`  |

---

## 8. 결정의 트레이드오프

### 얻은 것

- **Application Layer 경량화**: Facade 5개 → 3개. 상품/브랜드 관련 Facade 완전 제거
- **진입점 단순화**: Catalog 관련 로직은 무조건 CatalogService로 진입
- **멘토 원칙 준수**: "최상위 서비스는 하나"
- **문서 일관성**: 바운디드 컨텍스트 기준으로 모든 설계 문서가 정렬됨

### 감수하는 것

- **CatalogService 비대화 리스크**: 메서드 22개. Entity/VO로 복잡도를 분산하지만 향후 증가 가능
- **체크리스트와의 차이**: "Application Layer에서 처리"가 아닌 "Domain Service에서 처리". PR에서 설명 필요
- **Point 서비스 2개**: 멘토 원칙에 따르면 PointService 하나로 통합할 수 있으나, 현재는 관심사 분리를 우선함. 구현 시 재검토

---

## 9. 코드 리뷰 피드백 — 동시성 / 멱등성

Round 3 코드 리뷰에서 식별된 운영 안정성 이슈. 해결된 항목과 보류 항목을 함께 기록한다.

### 9.1 usePoints 멱등성 미보장

**현상:** `UserPointService.usePoints()`가 `refOrderId`를 `PointHistory`에 저장하지만, 동일 `refOrderId`로 중복 호출 시 포인트가 이중 차감된다.

**영향:** 네트워크 재시도, 이벤트 중복 발행 시 잔액 불일치 장애.

**수정 방향:**

- `PointHistory`에 `refOrderId` 유니크 제약 추가, 또는
- `usePoints()` 진입 시 `pointHistoryRepository.existsByRefOrderId(refOrderId)` 체크

**보류 사유:** 멱등성 전략(DB 유니크 vs 서비스 레벨 체크 vs 분산 락)은 결제 도메인 도입 시 함께 설계하는 것이 적절하다. `round3-decisions.md` 4.2절의 "추후 결제 로직 추가"
확장 포인트와 연관된다.

### 9.2 chargePoints 멱등성 미보장

**현상:** `PointChargingService.charge(userId, amount)`에 `idempotencyKey`가 없어 동일 충전 요청의 중복 처리를 구분할 수 없다.

**영향:** 클라이언트 재시도 시 포인트 중복 충전.

**수정 방향:**

- 요청에 `idempotencyKey` 필드 추가 + `PointHistory`에 `(refUserPointId, idempotencyKey)` 복합 유니크 인덱스
- 중복 요청 시 HTTP 409 또는 기존 결과 반환(멱등 응답)

**보류 사유:** 7.1과 동일. 충전/사용 양쪽의 멱등성 전략을 통일하여 설계해야 하며, PaymentFacade 도입 시 `idempotencyKey` 전파 경로도 함께 결정해야 한다.

### 9.3 포인트 동시성 제어 — 비관적 락 적용 (해결)

**현상:** `PointChargingService.charge()`와 `UserPointService.usePoints()`가 `findByUserId()`로 조회 후 잔액을 변경하는 구조여서, 동시 요청 시
lost update가 발생할 수 있었다.

**분석:**

- 포인트는 `user : userPoint = 1:1` 관계로, 락 범위가 단일 row에 한정된다
- 트래픽이 낮고(한 사용자의 동시 충전/사용은 드물다), 데이터 정합성이 최우선이다
- 비관적 락이 적합한 시나리오: 낮은 컨텐션 + 높은 정합성 요구

**결정:** JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)` 비관적 락 적용.

**구현:**

```kotlin
// Domain Repository — 수정 목적의 배타적 조회를 선언
interface UserPointRepository {
    fun findByUserIdForUpdate(userId: Long): UserPoint?
}

// JPA Repository — @Lock으로 FOR UPDATE 자동 생성 (JPQL 작성 불필요)
@Lock(LockModeType.PESSIMISTIC_WRITE)
fun findFirstByRefUserId(refUserId: Long): UserPoint?
```

- `charge()`, `usePoints()`에서 `findByUserIdForUpdate()` 사용
- `getBalance()`는 읽기 전용이므로 기존 `findByUserId()` 유지 (락 없음)

**DB 이식성:** JPA `@Lock`은 DB 방언(dialect)에 따라 적절한 SQL로 변환된다 (MySQL/PostgreSQL/H2 모두 `SELECT ... FOR UPDATE` 지원). DB를 교체해도
코드 변경 없음. 만약 row-level lock을 지원하지 않는 DB로 전환해야 하면 `@Version`(낙관적 락)으로 대체 가능.

### 9.4 재고 동시성 제어 — Redis 전략 (보류, 설계 방향 확정)

**현상:** `CatalogService.decreaseStocks()`에 `@Transactional`은 있으나, 동시 주문 시 재고 손실(lost update)이 발생할 수 있다.

**분석:**

- 재고는 포인트와 달리 **높은 트래픽 + 높은 실시간성 + 높은 정합성**이 모두 요구된다
- DB 비관적 락은 트래픽이 몰리면 병목이 된다 (다수의 상품에 동시 주문)
- Redis는 싱글스레드 기반이므로 동시성 이슈 자체가 발생하지 않는다

**결정:** Redis를 재고 관리의 1차 저장소로 사용한다.

**설계 방향:**

1. **Redis에 상품별 재고를 캐싱** — 상품 등록/수정 시 Redis에 재고 동기화
2. **Lua script로 atomic decrement** — `DECRBY` + 음수 체크를 하나의 Lua script로 실행하여 원자성 보장
3. **DB 동기화** — Redis에서 차감 성공 후 DB에 비동기 반영 (이벤트 또는 배치)
4. **장애 대응** — Redis 장애 시 DB fallback (비관적 락 또는 `UPDATE ... SET stock = stock - :qty WHERE stock >= :qty`)

**포인트와의 차이:**

| 기준     | 포인트               | 재고                       |
|--------|-------------------|--------------------------|
| 트래픽    | 낮음 (1인 1요청)       | 높음 (다수 사용자 동시 주문)        |
| 컨텐션    | 낮음 (user별 독립 row) | 높음 (인기 상품에 집중)           |
| 적합한 전략 | DB 비관적 락          | Redis 싱글스레드 + Lua script |

**보류 사유:** Redis 기반 재고 관리는 인프라 레이어 변경(modules/redis 활용), 동기화 전략, 장애 복구 로직 등 변경 범위가 크다. 현재 라운드에서는 설계 방향만 확정하고, 구현은 향후
라운드에서 진행한다.

### 9.5 주문 흐름 개선 — 재고 선점 + 지연 롤백 (2-Phase 주문)

**현상:** 현재 `OrderFacade.createOrder()`는 단일 `@Transactional` 안에서 재고 차감 → 주문 생성 → 포인트 차감을 모두 수행한다. 선착순 재고 경쟁에서 승리한 사용자가
포인트 부족이나 일시적 네트워크 오류로 주문에 실패하면, 선점한 재고까지 함께 롤백되어 구매 기회를 잃는다.

**문제:** 단일 트랜잭션에서는 "재고만 선점하고, 결제는 여유를 두고 처리"하는 구조가 불가능하다. 트랜잭션은 커밋 또는 롤백 둘 중 하나이므로.

**개선 방향 — 주문 상태 기반 2-Phase 처리:**

```
[API 1] POST /orders        → 재고 확인 + 차감(선점) + 주문 생성(PENDING)
[API 2] POST /orders/{id}/pay → 포인트 차감 + 주문 확정(CONFIRMED)
[Batch] Scheduler            → PENDING 상태 N분 초과 → CANCELLED + 재고 복구
```

**Order 상태 변경:**

- `PENDING` — 재고 선점됨, 결제 대기 (1~3분 유예)
- `CONFIRMED` — 결제 완료
- `CANCELLED` — 시간 초과 또는 결제 실패, 재고 복구

**필요한 변경 범위:**

- Order 엔티티에 상태 머신 추가 (PENDING → CONFIRMED / CANCELLED)
- OrderFacade 분리 (createOrder / confirmOrder)
- API 엔드포인트 추가 (`POST /orders/{id}/pay`)
- commerce-batch에 만료 주문 스케줄러 추가
- 기존 E2E 테스트 전면 수정

**보류 사유:** 단일 API 내에서 해결할 수 없는 구조적 변경이며, 변경 범위가 크다. 현재 라운드에서는 단일 트랜잭션의 원자성으로 기능 정확성을 보장하고, 향후 라운드에서 사용자 경험 개선 관점으로 도입을
검토한다.

---

## 10. 계약에 의한 설계 (Design by Contract)

### 10.1 DbC를 점검한 이유

Round 3 구현이 완료된 뒤, 도메인 객체의 **불변식(invariant)**, **사전 조건(precondition)**, **사후 조건(postcondition)**이 올바른 레이어에 선언되어 있는지
점검했다.

계약에 의한 설계(DbC) 관점에서 핵심 원칙은 다음과 같다:

- **불변식은 Domain Model/VO 내부에 선언한다** — `init` 블록, VO 자가 검증이 항상 보장
- **사전 조건은 가능한 한 Domain Model에 둔다** — Service에만 있으면, 다른 호출 경로에서 우회될 수 있다
- **도메인 판단 로직은 Domain Model이 소유한다** — Service가 Domain Model의 내부 상태를 직접 꺼내 판단하면 책임이 유출된다

### 10.2 발견된 문제와 조치

점검 결과, 5개의 개선 포인트가 식별되었다. 심각도 중 이상 3건은 즉시 수정하고, 하 2건은 기록만 남겼다.

| # | 도메인        | 문제                                                                                 | 심각도 | 조치                         |
|---|------------|------------------------------------------------------------------------------------|:---:|----------------------------|
| 1 | Catalog    | Product에 `isActive()` 메서드 부재 — "삭제되지 않고 HIDDEN이 아닌" 판단이 CatalogService에 분산         |  중  | **수정**                     |
| 2 | Point      | PointHistory에 도메인 검증 없음 — amount에 대한 검증 없이 직접 생성 가능                                |  중  | **수정**                     |
| 3 | Point      | `UserPoint.charge()`의 `amount > 0` 검증이 Entity에 없음 — PointChargingService에서만 보호     |  중  | **수정**                     |
| 4 | Catalog    | `getProductsForOrder()`의 ON_SALE 상태 확인이 Service에 있음                                |  하  | **수정** (#1과 함께)            |
| 5 | User       | Password, Email VO가 data class가 아님 — 다른 VO들과 일관성 깨짐                                |  하  | 보류 (검증 목적 VO이므로 실질적 문제 없음) |
| 6 | Catalog    | Brand에 `isDeleted()` 메서드 부재 — CatalogService에서 `brand.deletedAt != null`로 직접 상태 접근 |  중  | **수정**                     |
| 7 | Interfaces | AuthInterceptor 인증 헤더 누락 시 `ErrorType.BAD_REQUEST` 사용 — 문서 정의(401)와 불일치            |  중  | **수정**                     |
| 8 | Point      | `PointChargingService.charge()`와 `UserPoint.charge()`의 `amount <= 0` 에러 메시지 불일치    |  하  | **수정** (메시지 통일)            |
| 9 | Point      | `PointCommand.Charge` 데이터 클래스가 정의만 있고 미사용 (dead code)                              |  하  | **삭제**                     |

### 10.3 수정 내용

#### Product — 도메인 판단 메서드 도입 (#1, #4)

**변경 전:**

```kotlin
// CatalogService — Entity 내부 상태를 직접 꺼내 판단
fun getActiveProduct(productId: Long): Product {
    val product = getProduct(productId)
    if (product.deletedAt != null || product.status == Product.ProductStatus.HIDDEN) {
        throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
    }
    return product
}

fun getActiveProductsByIds(productIds: List<Long>): List<Product> {
    return productRepository.findAllByIds(productIds)
        .filter { it.deletedAt == null && it.status != Product.ProductStatus.HIDDEN }
}

fun getProductsForOrder(productIds: List<Long>): List<Product> {
    // ...
    products.forEach { product ->
        if (product.deletedAt != null || product.status != Product.ProductStatus.ON_SALE) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 가능한 상태가 아닌 상품이 포함되어 있습니다.")
        }
    }
}
```

"삭제되지 않았고 HIDDEN이 아닌"이라는 **동일한 도메인 판단**이 3곳에 분산되어 있었다. 이 판단의 주인은 Product 자신이어야 한다.

**변경 후:**

```kotlin
// Product — 자신의 활성 상태를 스스로 판단
fun isActive(): Boolean = !isDeleted() && status != ProductStatus.HIDDEN

fun isAvailableForOrder(): Boolean = !isDeleted() && status == ProductStatus.ON_SALE

// CatalogService — Entity에 판단을 위임
fun getActiveProduct(productId: Long): Product {
    val product = getProduct(productId)
    if (!product.isActive()) {
        throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
    }
    return product
}

fun getActiveProductsByIds(productIds: List<Long>): List<Product> {
    return productRepository.findAllByIds(productIds).filter { it.isActive() }
}

fun getProductsForOrder(productIds: List<Long>): List<Product> {
    // ...
    products.forEach { product ->
        if (!product.isAvailableForOrder()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 가능한 상태가 아닌 상품이 포함되어 있습니다.")
        }
    }
}
```

**isActive()와 isAvailableForOrder()를 분리한 이유:**

두 메서드는 "활성"의 기준이 다르다. `isActive()`는 대고객에게 노출 가능한 상품(SOLD_OUT 포함)이고, `isAvailableForOrder()`는 실제 구매 가능한 상품(ON_SALE만)이다.
4.4절의 "조회 메서드 분리"와 같은 맥락 — 호출 시점(context)에 따라 검증 수준이 다르다.

#### PointHistory — 불변식 추가 (#2)

**변경 전:** 생성자에 검증이 없어, `amount = 0`이나 음수 값으로 PointHistory를 생성할 수 있었다.

**변경 후:**

```kotlin
class PointHistory(
    refUserPointId: Long,
    type: PointHistoryType,
    amount: Long,
    refOrderId: Long? = null,
) {
    init {
        if (amount <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "포인트 이력 금액은 0보다 커야 합니다.")
        }
    }
    // ...
}
```

`init` 블록에서 `amount <= 0`일 때 `CoreException`을 throw한다 (amount > 0 직접 검증). 이 불변식은 어떤 경로로 PointHistory를 생성하든 보장된다.

#### UserPoint.charge() — 사전 조건 보강 (#3)

**변경 전:** `charge(amount)`에서 `amount > 0` 검증이 없었다. PointChargingService가 VO를 통해 간접적으로 보호하고 있었지만, Entity 자체의 계약이 불완전했다.

**변경 후:**

```kotlin
fun charge(amount: Long) {
    if (amount <= 0) {
        throw CoreException(ErrorType.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.")
    }
    val newBalance = Point(balance).plus(Point(amount)).value
    if (newBalance > MAX_BALANCE) {
        throw CoreException(ErrorType.BAD_REQUEST, "충전 후 잔액이 최대 한도(${MAX_BALANCE}포인트)를 초과합니다.")
    }
    this.balance = newBalance
}
```

`use(amount)`에는 이미 `amount <= 0` 검증이 있었으므로, `charge(amount)`에도 동일한 수준의 사전 조건을 부여하여 대칭을 맞췄다.

### 10.4 도메인 객체 계약 정리

수정 후 확정된 각 Entity의 계약을 정리한다.

**Product:**

| 구분                         | 계약                                                                 |
|----------------------------|--------------------------------------------------------------------|
| 불변식                        | `price >= 0` (Price VO), `stock >= 0` (Stock VO), `likeCount >= 0` |
| 불변식                        | HIDDEN이 아닌 경우: `stock == 0 ↔ SOLD_OUT`, `stock > 0 ↔ ON_SALE`      |
| `decreaseStock(qty)` 사전    | `qty > 0`, `stock >= qty` (Stock VO가 보장)                           |
| `decreaseStock(qty)` 사후    | `stock' = stock - qty`, stock'가 0이면 SOLD_OUT 전환 (HIDDEN이면 유지)      |
| `isActive()` 사후            | `deletedAt == null && status != HIDDEN`                            |
| `isAvailableForOrder()` 사후 | `deletedAt == null && status == ON_SALE`                           |

**UserPoint:**

| 구분                  | 계약                                                       |
|---------------------|----------------------------------------------------------|
| 불변식                 | `balance >= 0` (Point VO, init 블록에서 검증)                  |
| `charge(amount)` 사전 | `amount > 0`                                             |
| `charge(amount)` 사후 | `balance' = balance + amount`, `balance' <= MAX_BALANCE` |
| `use(amount)` 사전    | `amount > 0`, `balance >= amount`                        |
| `use(amount)` 사후    | `balance' = balance - amount`                            |

**PointHistory:**

| 구분  | 계약                             |
|-----|--------------------------------|
| 불변식 | `amount > 0` (init 블록에서 직접 검증) |

### 10.5 검증 레이어 분담

DbC 수정을 통해 확정된, 각 레이어의 검증 책임:

| 레이어            | 검증 대상             | 메커니즘                                       | 예시                             |
|----------------|-------------------|--------------------------------------------|--------------------------------|
| Controller     | 입력 형식, 범위         | Bean Validation (`@Min`, `@Max`, `@Valid`) | `page >= 0`, `size in 1..100`  |
| Domain Model   | 불변식, 사전/사후 조건     | `init` 블록, VO 자가 검증, 비즈니스 메서드 내 검증         | `stock >= 0`, `amount > 0`     |
| VO             | 값의 도메인 규칙         | `init` 블록에서 자가 검증                          | `Price(value)`: value < 0 → 예외 |
| Domain Service | 엔티티 존재 여부, 도메인 정책 | Repository 조회 + Entity 메서드 위임              | `product.isActive()` 결과에 따른 예외 |

**원칙: 검증은 가능한 한 안쪽(Entity/VO)에 둔다.** Service 레이어의 검증은 "여러 엔티티를 조합한 판단"이나 "존재 여부 확인"에 한정하고, 단일 엔티티의 상태 판단은 Entity가 소유한다.

**보충 원칙: Service의 early-return 검증은 허용한다.** Entity 검증 이전에 DB 통신(예: `findByUserIdForUpdate`)이 있는 경우, Service에서 동일한 사전 조건을
먼저 검증하여 불필요한 DB 호출을 방지한다. 이때 에러 메시지는 Entity와 통일한다.

#### Brand — 도메인 판단 메서드 도입 (#6)

Product에 `isDeleted()`, `isActive()`, `isAvailableForOrder()`가 있는 것과 동일하게, Brand에도 `isDeleted()` 메서드를 추가하였다.

```kotlin
// Brand — 자신의 삭제 상태를 스스로 판단
fun isDeleted(): Boolean = deletedAt != null

// CatalogService — Entity에 판단을 위임
fun getActiveBrand(brandId: Long): Brand {
    val brand = brandRepository.findById(brandId)
        ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
    if (brand.isDeleted()) {  // 변경: brand.deletedAt != null → brand.isDeleted()
        throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
    }
    return brand
}
```

#### AuthInterceptor — 인증 에러 코드 통일 (#7)

인증 헤더 누락 시 `ErrorType.BAD_REQUEST`(400)를 사용하고 있었으나, 요구사항 분석 문서 7.1절에서 인증 관련 오류는 모두 401로 정의하고 있어 `ErrorType.UNAUTHORIZED`
로 통일하였다.

#### PointChargingService — 에러 메시지 통일 (#8)

`PointChargingService.charge()`의 `amount <= 0` 검증 메시지를 `UserPoint.charge()`와 동일한 "충전 금액은 0보다 커야 합니다."로 통일하였다. Service의
early-return 검증은 DB 통신(`findByUserIdForUpdate`) 전에 빠르게 실패시키는 역할이므로 유지한다.

#### PointCommand.Charge — dead code 삭제 (#9)

`PointCommand.Charge` 데이터 클래스가 정의만 있고 어디서도 사용되지 않아 삭제하였다. Controller가 `@RequestParam`으로 충전 금액을 받으므로 Command 패턴이 불필요하다.
향후 `@RequestBody` 전환 시(idempotencyKey 등 필드 추가) 재생성한다.

---

## 11. 어드민/대고객 서비스 메서드 분리 원칙

### 11.1 문제 발견

Round 3 코드 리뷰에서 `updateBrand()`가 대고객용 `getActiveBrand()`를 재사용하여, 어드민이 삭제된 브랜드를 수정할 수 없는 버그가 발견되었다.

**근본 원인:** 어드민/대고객 권한 경계가 요구사항에 정의되지 않았고, 구현 시 편의상 대고객 메서드를 어드민 경로에서 재사용한 것.

### 11.2 확정된 원칙

대고객용 조회 메서드(`getActive*`)는 어드민 CUD 경로에서 재사용하지 않는다.

| 컨텍스트    | 조회 메서드                                   | 반환 범위                   |
|---------|------------------------------------------|-------------------------|
| 대고객 조회  | `getActiveBrand()`, `getActiveProduct()` | 활성 데이터만 (삭제/HIDDEN 필터링) |
| 어드민 조회  | `getBrand()`, `getProduct()`             | 모든 상태 포함                |
| 어드민 CUD | `getBrand()`, `getProduct()`             | 작업 목적에 맞는 조회 메서드 사용     |

### 11.3 수정 내용

| 메서드               | 변경 전                              | 변경 후                      |
|-------------------|-----------------------------------|---------------------------|
| `updateBrand()`   | `getActiveBrand(brandId)`         | `getBrand(brandId)`       |
| `updateProduct()` | `getProduct()` + `isDeleted()` 체크 | `getProduct()` (삭제 체크 제거) |

### 11.4 Soft Delete 복구 정책

soft delete된 엔티티는 어드민이 복구할 수 있다. `BaseEntity.restore()`를 통해 `deletedAt`을 null로 설정한다.

| 대상      | 복구 API                                     | 비고                           |
|---------|--------------------------------------------|------------------------------|
| Brand   | `POST /api-admin/v1/brands/{id}/restore`   | cascade 복구 없음 (소속 상품은 개별 복구) |
| Product | `POST /api-admin/v1/products/{id}/restore` |                              |

**설계 결정:**

- 브랜드 삭제 시 소속 상품이 cascade soft delete되지만, 복구 시에는 cascade 복구하지 않는다
- 이유: 삭제 시점에 이미 개별 삭제된 상품과 cascade 삭제된 상품을 구분할 수 없으므로, 복구는 개별 엔티티 단위로 수행한다
- soft delete의 목적: 감사 로그(audit trail), 참조 무결성 보호, 어드민 복구 가능성

### 11.5 어드민/대고객 권한 경계 정의

각 엔티티의 상태별로 어드민과 대고객이 수행 가능한 작업:

**Brand:**

| 작업         | 대고객     | 어드민 |
|------------|---------|-----|
| 정상 브랜드 조회  | O       | O   |
| 삭제된 브랜드 조회 | X (404) | O   |
| 삭제된 브랜드 수정 | -       | O   |
| 삭제된 브랜드 복구 | -       | O   |

**Product:**

| 작업             | 대고객     | 어드민     |
|----------------|---------|---------|
| 정상 상품 조회       | O       | O       |
| HIDDEN 상품 조회   | X (404) | O       |
| 삭제된 상품 조회      | X (404) | O       |
| 삭제된 상품 수정      | -       | O       |
| HIDDEN 상품 수정   | -       | O       |
| 삭제된 상품 복구      | -       | O       |
| 삭제된 브랜드에 상품 생성 | -       | X (404) |

**Order:**

| 작업       | 대고객                | 어드민    |
|----------|--------------------|--------|
| 본인 주문 조회 | O                  | O (전체) |
| 타인 주문 조회 | X (404, 존재 여부 미노출) | O      |

**Like:**

| 작업                    | 대고객     | 어드민 |
|-----------------------|---------|-----|
| 삭제/HIDDEN 상품 좋아요      | X       | -   |
| 좋아요 목록에서 삭제/HIDDEN 상품 | 조용한 필터링 | -   |

**Point:**

| 작업       | 대고객 | 어드민 |
|----------|-----|-----|
| 충전/조회/사용 | O   | -   |

---

## 12. Pragmatic Clean Architecture 도입

### 12.1 도입 배경

**문제:** Round 2까지 Domain Model이 JPA Entity를 겸하고 있었다. `@Entity`, `@Column` 등 JPA 애노테이션이 도메인 객체에 직접 붙어 있었고, `guard()` 메서드가 `@PrePersist`/`@PreUpdate` 콜백으로 불변식을 보장하는 구조였다.

이 방식의 한계:

- Domain Model이 JPA 기술에 종속되어 순수성이 훼손된다
- VO를 자유롭게 사용하려면 `@Converter`가 필요해 부담이 커진다
- 테스트 시 JPA 컨텍스트 없이 도메인 로직을 검증하기 어렵다
- 영속성 관심사(lazy loading, dirty checking 등)가 도메인 로직에 침투한다

### 12.2 핵심 변경사항

**A. Domain Model ↔ JPA Entity 완전 분리**

| 구분 | Domain Model (`domain/`) | JPA Entity (`infrastructure/`) |
|------|--------------------------|-------------------------------|
| 역할 | 비즈니스 규칙, 상태 변경, 상태 판단 | DB 테이블 매핑 전용 |
| 애노테이션 | 없음 (순수 POJO) | `@Entity`, `@Column` 등 JPA 전용 |
| 검증 | `init { validate() }`, VO 자가 검증 | 없음 (Domain Model에 위임) |
| 생성 패턴 | `public constructor` 또는 `private constructor + create()` | `fromDomain()` / `toDomain()` 매핑 |

**B. Value Object 적극 활용**

Domain Model이 순수 POJO이므로 `@Converter` 부담 없이 모든 도메인 값을 VO로 표현할 수 있다:

- 단일 값: `@JvmInline value class` (Price, Email, BrandName 등)
- 도메인 메서드 있음: 일반 `class` (Stock, Point)
- 복합 필드: `data class` (Address)

**C. Repository 구조 단순화**

`XxxRepositoryImpl.kt` 파일 하나에 `internal interface XxxJpaRepository`와 구현체를 함께 선언한다. JpaRepository는 구현 세부사항이므로 `internal`로 외부 노출을 차단한다.

**D. JPA 연관관계 전면 미사용**

`@OneToMany`, `@ManyToOne`, `@ManyToMany`를 사용하지 않는다. 모든 연관 데이터는 Repository를 통해 명시적으로 조회한다. N+1 문제, Lazy/Eager 로딩 전략의 복잡도를 원천 차단한다.

### 12.3 결정 근거

- **Domain Model 순수성**: 비즈니스 로직이 기술 프레임워크에 독립적이므로, JPA를 교체해도 도메인 코드가 변하지 않는다
- **테스트 용이성**: Domain Model을 JPA 컨텍스트 없이 순수 단위 테스트로 검증할 수 있다 (Fake/Stub Repository 주입)
- **VO 자유도**: `@Converter` 없이 한 줄짜리 검증도 VO로 표현할 수 있어 타입 안전성이 높아진다
- **명시적 데이터 흐름**: 연관관계 매핑 없이 Repository를 통한 명시적 조회로 N+1 문제를 원천 차단한다

### 12.4 기존 guard() 패턴의 대체

| 변경 전 (Round 2) | 변경 후 (Round 3) |
|-------------------|-------------------|
| JPA Entity에 `guard()` (`@PrePersist`/`@PreUpdate`) | Domain Model의 `init { validate() }` + VO 자가 검증 |
| `Product.guard()` — 가격, 재고 검증 | `Price` VO, `Stock` VO가 생성 시점에 자가 검증 |
| `Brand.guard()` — 이름 검증 | `BrandName` VO가 생성 시점에 자가 검증 |
| `UserPoint.guard()` — 잔액 검증 | `Point` VO + `init` 블록에서 검증 |

### 12.5 트레이드오프

**얻은 것:**

- Domain Model이 순수 POJO → 단위 테스트가 빠르고 간결해짐
- VO 적극 활용 → 타입 안전성과 도메인 표현력 향상
- JPA 관심사가 infrastructure에 격리 → 도메인 로직 변경 시 영속성 코드 무관

**감수하는 것:**

- `fromDomain()`/`toDomain()` 매핑 보일러플레이트 증가
- Domain Model과 JPA Entity의 필드 동기화 필요 (스키마 변경 시 양쪽 수정)
- 초기 학습 비용: 두 객체의 역할 구분에 대한 팀 이해 필요

---

## 13. findItemsByOrders 중복과 CQRS 검토

### 13.1 현상

`GetOrdersUseCase`와 `GetOrdersAdminUseCase`에 동일한 `findItemsByOrders()` private 메서드가 중복되어 있다.

```kotlin
private fun findItemsByOrders(orders: List<Order>): Map<Long, List<OrderItem>> {
    if (orders.isEmpty()) return emptyMap()
    return orderItemRepository.findAllByOrderIds(orders.map { it.id })
        .groupBy { it.refOrderId }
}
```

### 13.2 중복 제거 선택지

| 선택지 | 방법 | 장점 | 단점 |
|-----|----|-----|-----|
| A | 공통 유틸 함수 추출 | 단순, 즉시 적용 가능 | UseCase 간 결합 증가, 유틸 패키지 오염 |
| B | OrderQueryService (Domain Service) | 도메인 언어로 표현 | 단순 조회를 위한 서비스는 빈 껍데기 |
| C | CQRS 패턴 도입 | 조회 전용 모델로 근본 해결 | 변경 범위가 큼 |

### 13.3 결정: 보류 (다음 라운드 재검토)

현재 중복은 2곳이며, 로직이 단순하다 (3줄). 잘못된 추상화보다 약간의 중복이 낫다는 원칙에 따라 현행 유지한다.

CQRS 도입 시 조회 전용 Read Model(예: `OrderSummaryView`)을 정의하면 `findItemsByOrders` 중복이 자연 해소되지만, 현재 프로젝트 규모에서는 오버엔지니어링이다. 주문 조회 요구사항이 복잡해지거나(필터, 정렬, 집계), 쓰기/읽기 성능 요구가 분리될 때 재검토한다.

---

## 14. 아키텍처 감사 후속 리팩토링 (CP0~CP3)

Round 3 구현 완료 후 아키텍처 감사를 통해 식별된 개선 항목을 4개 체크포인트(CP)로 분류하여 순차 적용하였다.

### 14.1 CP0 — 문서 정정

**내용:** infrastructure/CLAUDE.md 예시 코드의 `internal interface` 누락 정정.

`XxxRepositoryImpl.kt` 내에 함께 선언되는 JpaRepository 인터페이스는 `internal`로 외부 노출을 차단해야 하지만, 문서 예시에 `internal` 키워드가 누락되어 있었다.

**수정:** `interface ProductJpaRepository` → `internal interface ProductJpaRepository`

### 14.2 CP1 — 구조적 리팩토링

이하 항목은 행위 변경 없이 구조만 정리한 커밋으로 적용하였다.

**StatusDto enum 제거 → String 통일**

Interfaces 레이어 DTO에 선언된 `StatusDto` enum이 Domain enum과 1:1로 중복되어 있었다. `ProductStatus`, `OrderStatus` 등 모든 상태값을 String으로 통일하고 `StatusDto` enum을 삭제하였다.

- 변경 전: `status: StatusDto` (enum)
- 변경 후: `status: String` (Domain enum `.name` 변환)

**CatalogCommand 원시 타입 전환**

`CatalogCommand`의 필드가 VO 타입(예: `BrandName`, `Price`)을 사용하고 있었다. Command는 Application 계층 객체이며, VO 생성(검증)은 UseCase 내부에서 수행하는 것이 적절하다.

- 변경 전: `data class CatalogCommand.CreateProduct(val name: BrandName, val price: Price, ...)`
- 변경 후: `data class CatalogCommand.CreateProduct(val name: String, val price: BigDecimal, ...)`
- VO 생성 위치를 Command → UseCase 내부로 이동

**findItemsByOrders → OrderItemRepository 확장 함수 추출**

`GetOrdersUseCase`와 `GetOrdersAdminUseCase`에 중복된 `findItemsByOrders()` private 메서드를 `OrderItemRepository`의 확장 함수로 추출하였다.

```kotlin
// OrderItemRepository 확장 함수
fun OrderItemRepository.findItemsByOrders(orders: List<Order>): Map<Long, List<OrderItem>> {
    if (orders.isEmpty()) return emptyMap()
    return findAllByOrderIds(orders.map { it.id }).groupBy { it.refOrderId }
}
```

**GetProduct UseCase 네이밍 교정**

대고객/어드민 구분이 명확하지 않던 UseCase 이름을 정정하였다. (섹션 14.5 UseCase 네이밍 규칙 참조)

### 14.3 CP2 — @Transactional UseCase 이동

**문제:** `@Transactional`이 Domain Service(`CatalogService`, `UserPointService` 등)에 선언되어 있었다. 트랜잭션 경계는 Application 계층(UseCase)에서 관리해야 한다.

**변경:** 각 Domain Service의 `@Transactional`을 제거하고, 호출하는 UseCase의 `execute()` 메서드에 이동하였다.

- `CatalogService` 메서드 → `@Transactional` 제거
- `UserPointService` 메서드 → `@Transactional` 제거
- 각 UseCase `execute()` → `@Transactional` 추가

**근거:** Domain Service는 순수 도메인 로직 조율에 집중한다. 트랜잭션 경계를 UseCase에 두면 "어느 작업이 하나의 트랜잭션 단위인가"를 Application 계층에서 일관되게 파악할 수 있다.

### 14.4 CP3 — 행위적 수정

**AddLikeUseCase existsBy 사전검증 도입**

`AddLikeUseCase`에서 중복 좋아요 여부를 `findBy...`로 조회 후 null 체크하는 방식 대신, `existsBy...` 메서드로 존재 여부만 확인하도록 변경하였다. 불필요한 엔티티 로드를 제거하고 의도를 명확히 표현한다.

**Domain Service/Aggregate Root 중복 검증 제거**

Domain Service와 Aggregate Root(Entity)에 동일한 검증 로직이 이중으로 존재하던 항목을 정리하였다. Entity의 `init` 블록/도메인 메서드가 불변식을 보장하므로, Service의 중복 검증을 제거하였다. (섹션 10의 DbC 원칙 적용)

**likeCount Domain Model 경유 전환**

좋아요 등록/취소 시 `likeCount` 증감이 Repository를 통해 직접 DB 값을 조작하던 방식을, Domain Model(`Product`)의 `increaseLikeCount()` / `decreaseLikeCount()` 메서드를 경유하도록 전환하였다.

- 변경 전: Repository에서 `likeCount` 직접 증감
- 변경 후: `product.increaseLikeCount()` → `productRepository.save(product)` 패턴

이로써 likeCount 관련 불변식(`likeCount >= 0` 등)이 Domain Model 내부에서 일관되게 보장된다.

### 14.5 UseCase 네이밍 규칙 (확정)

CP1 리팩토링에서 확정된 대고객/어드민 UseCase 네이밍 규칙:

| 대상   | 패턴                | 예시                                             |
|------|-------------------|------------------------------------------------|
| 대고객  | `XxxUseCase`      | `GetProductUseCase`, `GetOrderUseCase`         |
| 어드민  | `XxxAdminUseCase` | `GetProductAdminUseCase`, `GetOrderAdminUseCase` |

### 14.6 Repository deletedAt 전면 필터링 (확정)

Brand, Product, Order, User 4개 Repository의 `findById`에 `deletedAt` 필터링을 전면 적용하였다. 삭제된 엔티티는 일반 조회에서 자동 제외된다.

- `findById` — `deletedAt IS NULL` 필터링 포함 (기본)
- `findByIdIncludeDeleted` — 삭제된 엔티티 포함 조회 (Restore UseCase 전용)
- `findByIdForUpdate` — 비관적 락 + `deletedAt` 필터링 (동시성 제어 필요 시)
