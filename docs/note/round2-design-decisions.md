# Round 2 설계 결정 기록

이커머스 도메인(브랜드, 상품, 좋아요, 주문) 설계 과정에서 논의하고 결정한 사항들을 정리한다.
설계 문서(요구사항, 시퀀스, 클래스 다이어그램, ERD) 작성 후 8차에 걸친 리뷰를 통해 정제한 과정의 기록이다.
"어떤 agent에 맡겨도 동일한 구현이 나오는 문서"를 목표로, 발견된 모호함을 하나씩 제거해나갔다.

---

## 1. 레이어드 아키텍처 결정

### 1.1 Service 리턴 타입: Info or Entity

**문제:** 초기 시퀀스 다이어그램에서 Service가 `ProductInfo`, `BrandInfo` 같은 application 레이어 객체를 리턴하도록 작성했다. 그런데 클래스 다이어그램에서는 Service가
domain 레이어에 속하고, Info는 application 레이어에 속한다.

**고민:**

- Service가 Info를 리턴하면 domain → application 역방향 의존이 생긴다
- Info를 domain 레이어에 두면 레이어 경계가 무너진다
- 그렇다고 Service에서 Entity를 리턴하면 Facade가 변환 책임을 져야 한다

**결정:** Service는 Entity를 리턴하고, Facade에서 Info로 변환한다.

```
Service    → Entity (domain 레이어 내부에서 완결)
Facade     → Info   (application 레이어에서 변환)
Controller → Dto    (interfaces 레이어에서 변환)
```

**근거:**

- 레이어드 아키텍처의 의존 방향 원칙 준수: 상위 레이어가 하위 레이어에 의존
- Facade가 변환 책임을 가지는 것이 "오케스트레이션 + 데이터 조합" 역할과 일치
- Info에 `companion object { fun from(entity) }` 팩토리 메서드를 두면 변환 코드가 깔끔

**영향:** 시퀀스 다이어그램 전체(21개)에서 Service 리턴 화살표를 `Info` → `Entity`로 수정. Facade → Controller 화살표만 `Info`를 유지.

### 1.2 Facade 메서드 네이밍: getActive~ vs get~ vs getAll~

**문제:** 시퀀스 다이어그램에서는 `getBrand`, `getProducts` 같은 이름을 사용하는데, 클래스 다이어그램에서는 `getActiveBrand`, `getAllProducts` 같은 이름을
사용했다. 6개 메서드에서 이름이 불일치했다.

**고민:**

- 대고객 API와 어드민 API가 같은 이름을 쓰면 삭제 데이터 포함 여부가 모호해진다
- `ForAdmin` 접미사를 붙이면 장황하다
- 메서드 이름만 보고 동작을 추론할 수 있어야 한다

**결정:** `Active` 유무로 대고객/어드민을 구분한다.

| 접근자      | 패턴           | 의미                    | 예시                                   |
|----------|--------------|-----------------------|--------------------------------------|
| 대고객      | `getActive~` | deletedAt IS NULL만 조회 | `getActiveBrand`, `getActiveProduct` |
| 어드민 (단건) | `get~`       | 삭제 포함 조회              | `getBrand`, `getProduct`             |
| 어드민 (목록) | `getAll~`    | 삭제 포함 전체 조회           | `getAllBrands`, `getAllProducts`     |

**근거:**

- `Active`라는 단어가 있으면 필터링이 있다는 신호
- `Active`가 없으면 필터링 없이 raw 데이터 접근
- `All`은 목록 조회에서 "전체"를 명시적으로 표현
- 이 네이밍만으로 agent가 Repository 쿼리의 WHERE 절을 추론할 수 있음

**영향:** 시퀀스 다이어그램 6곳 수정, 클래스 다이어그램과 100% 일치시킴.

### 1.3 Facade 선택적 사용 (1:1은 직접 호출)

**문제:** 단일 도메인만 다루는 API(예: 브랜드 CRUD)에서도 Facade를 거쳐야 하는가? 1:1 passthrough는 불필요한 레이어 아닌가?

**선택지:**

- A: 1:1인 경우 Controller → Service 직접 호출
- B: 모든 경우 Facade 유지

**결정:** 1:1인 경우 Controller가 Service를 직접 호출한다 (선택지 A).

**근거:**

- 1:1 passthrough Facade는 위임만 할 뿐 가치를 더하지 않는다
- 불필요한 레이어는 코드량과 변경 비용만 증가시킨다
- cross-domain 로직이 필요해지는 시점에 Facade를 도입하면 된다 (YAGNI)
- Controller가 Service를 직접 호출해도 레이어 간 역참조가 발생하지 않는다 (interfaces → domain 방향)

**Facade가 필요한 경우:**

- 2개 이상의 서비스를 조율할 때 (cross-domain orchestration)
- 예: 좋아요 등록(LikeService + ProductService), 브랜드 삭제(BrandService + ProductService), 주문 생성(ProductService + OrderService)

### 1.4 @Transactional 전략

**문제:** Facade와 Service 중 어디에 @Transactional을 붙여야 하는가?

**결정:**

- **Service:** 변경 작업에 `@Transactional` 필수 적용
- **Facade:** 선택적 적용 — 여러 Service를 조합하여 원자성이 필요한 경우에만
- **단일 Service 호출 Facade:** Service의 @Transactional에 위임 (Facade에 @Transactional 안 붙임)

**Facade에 @Transactional이 정당한 케이스:**

- 좋아요 등록/취소 (LikeService + ProductService)
- 브랜드 삭제 cascade (BrandService + ProductService)
- 상품 등록 (BrandService 존재 확인 + ProductService 생성)
- 주문 생성 (ProductService 재고 차감 + OrderService 생성)

**근거:** @Transactional propagation(REQUIRED)에 의해 Facade에서 열린 트랜잭션이 Service 호출까지 전파된다. 단일 Service만 호출하는 경우 불필요한 트랜잭션 범위
확장을 피하기 위해 Service에 위임한다.

### 1.5 soft delete 조회 전략

**문제:** `@Where(clause = "deleted_at IS NULL")`을 사용할 것인가?

**선택지:**

- A: `@Where` 어노테이션 → 모든 쿼리에 자동 적용
- B: Repository 메서드마다 명시적 조건 추가

**결정:** `@Where` 미사용. Repository 메서드마다 `deletedAt IS NULL` 조건을 명시적으로 추가한다 (선택지 B).

**근거:** 어드민 API에서 삭제된 데이터도 조회해야 하므로 `@Where`는 유연성을 제한한다. `@Where`를 사용하면 어드민 조회 시 native query나 `@Filter` 같은 우회가 필요해져 오히려
복잡해진다.

### 1.6 Repository 메서드 통합 vs 분리

**문제:** 대고객과 어드민이 같은 Entity를 다른 조건으로 조회한다. Repository 메서드를 하나로 통합할 수 있는가?

**결정:** 단건 조회는 Repository 통합, 목록 조회는 Repository 분리.

```
// 단건: Repository는 findById 하나, Service에서 분기
getActiveBrand(id)  → findById → deletedAt 체크 → 없으면 NOT_FOUND
getBrand(id)        → findById → deletedAt 체크 안 함

// 목록: Repository 메서드 분리
findActiveProducts(brandId, sort, pageable)  → 대고객 (필터 + 정렬)
findAllProducts(brandId, pageable)           → 어드민 (필터 없음)
```

**근거:**

- 단건 조회는 `findById` 결과에 대해 Service 레벨에서 null 체크 + deletedAt 체크를 분기하는 것이 자연스러움
- 목록 조회는 WHERE 절이 근본적으로 다르고, 페이징이 DB 레벨에서 일어나므로 Service 레벨 필터링은 부적절 (데이터 누락 위험)
- Querydsl 동적 쿼리로 두 메서드를 내부적으로 공유할 수 있지만, 외부 인터페이스는 의도가 명확하게 분리

---

## 2. 엔티티 설계

### 2.1 Like 엔티티 — BaseEntity 미상속

**결정:** Like는 BaseEntity를 상속하지 않는다. `id`, `userId`, `productId`만 보유.

**근거:**

- 이력 관리가 불필요하고, 취소 시 하드 딜리트(물리 삭제)로 관리
- `created_at`, `updated_at`, `deleted_at` 모두 불필요
- 최신순 정렬이 필요하면 id 역순으로 대체 (YAGNI 원칙)
- id 생성: BaseEntity를 상속하지 않으므로 `@GeneratedValue(strategy = GenerationType.IDENTITY)` 직접 선언

### 2.2 OrderItem — Aggregate 내부 Entity (종속 엔티티)

**결정:** OrderItem은 독립 Entity가 아닌 Order Aggregate의 내부 Entity. BaseEntity를 상속한다.

**특성:**

- id를 가지지만 독립 생명주기와 독립 Repository가 없다. 항상 Order를 통해서만 접근
- 단방향 `@OneToMany(cascade = ALL, orphanRemoval = true)` + `@JoinColumn(name = "order_id", nullable = false)`로 구현
- OrderItem에서 Order로의 역참조(`@ManyToOne`) 없음
- `nullable = false`는 Hibernate의 extra UPDATE 문제를 방지 — INSERT 시 order_id를 함께 설정

**BaseEntity 상속 유지 결정:** 리뷰에서 "OrderItem이 Composition인데 BaseEntity(deletedAt) 상속이 필요한가?"라는 의문이 제기됐다. 추후 주문 일부 취소(OrderItem 개별 soft delete) 시나리오를 대비하여 유지하기로 결정. `orphanRemoval=true`와 soft delete가 공존할 때 의도치 않은 물리 삭제가 발생하지 않도록 구현 시 주의 필요.

**JPA 주의점:** Hibernate 6.x (Spring Boot 3.4)에서 `@JoinColumn(nullable = false)` 없이 단방향 `@OneToMany`를 사용하면, INSERT 시
order_id를 NULL로 넣고 이후 별도 UPDATE로 FK를 설정하는 비효율이 발생한다.

### 2.3 enum inner class 배치

**결정:** ProductStatus는 Product의 inner enum, OrderStatus는 Order의 inner enum.

**근거:** 각 도메인 내부에서만 사용되므로 별도 파일 분리 불필요.

---

## 3. 주문 도메인

### 3.1 Order.totalPrice 반정규화

**문제:** 주문 목록 조회 시 총액을 어떻게 제공할 것인가?

**선택지:**

- A: 조회 시마다 OrderItem을 로딩하여 합산 (정규화)
- B: 주문 생성 시 totalPrice를 계산하여 Order에 저장 (반정규화)

**결정:** 선택지 B. 주문 생성 시 totalPrice를 계산하여 Order 엔티티에 저장한다.

**근거:**

- 주문 목록 조회 시 OrderItem을 로딩하지 않고도 총액 제공 (N+1 방지)
- totalPrice는 주문 생성 이후 변경되지 않으므로 정합성 문제 없음

### 3.2 할루시네이션 — 중복 상품 병합

**문제:** 초기 설계 문서에 "같은 productId가 여러 주문 항목에 포함될 수 있다"는 전제가 있었다. 이를 해결하기 위해 중복 상품 병합(Order.create에서 수량 합산), 재고 차감 순서(
productId 오름차순 정렬로 데드락 방지) 등을 설계했다.

**발견:** 요구사항을 다시 확인한 결과, 실제 규칙은 "주문 항목의 상품은 고유해야 한다 (같은 productId 중복 불가)"였다. 중복 상품이 들어오는 시나리오 자체가 존재하지 않는 가상의 요구사항이었고,
초기 문서 작성 시 AI가 만들어낸 것이었다.

**결정:** 중복 병합, 재고 차감 정렬 관련 설계는 무효 처리한다. 주문 항목에 같은 productId가 포함되면 400 응답으로 거부한다.

### 3.3 주문 목록 조회 기간 필터

**파라미터:** `startedAt` / `endedAt` (LocalDateTime)

**범위:** `startedAt >=` (inclusive), `endedAt <` (exclusive) — **half-open 범위**

**기준 컬럼:** `created_at`

**Repository 메서드명:** `findOrdersByUserIdInPeriod`

- Spring Data JPA의 `Between`은 양쪽 inclusive를 암시하므로, half-open 범위에는 부적절
- 커스텀 네이밍으로 의도를 명확히 함

**TimeZone:** CommerceApiApplication의 `DefaultTimeZone`으로 ZonedDateTime 변환.

### 3.4 OrderService 메서드 정리

| 메서드                                                       | 용도        | 비고                                 |
|-----------------------------------------------------------|-----------|------------------------------------|
| `getOrder(orderId)`                                       | 범용 단건 조회  | 고객: Facade에서 userId 확인, 어드민: 확인 없음 |
| `getOrdersByUserId(userId, startedAt, endedAt, pageable)` | 고객 주문 목록  | SQL WHERE userId 필요                |
| `getAllOrders(pageable)`                                  | 어드민 주문 목록 | 필터 없음                              |

- `getOrderByUserIdAndId` 제거 → `getOrder` + Facade userId 확인으로 대체
- 권한 확인은 Service가 아닌 Facade의 책임

---

## 4. 좋아요 도메인

### 4.1 삭제된 상품에 대한 좋아요 처리

**문제:** soft delete된 상품에 좋아요 등록/취소/조회 시 어떻게 처리하는가?

**결정:**

- **등록:** 404 (삭제된 상품에 좋아요 등록 불가)
- **취소:** 200 (Like 레코드 있으면 삭제, 없으면 무시. 멱등). likeCount는 갱신하지 않음 (상품이 삭제 상태)
- **목록 조회:** 삭제된 상품은 미노출 (ProductService가 활성 상품만 반환)
- **잔존 데이터:** 삭제된 상품의 Like 레코드는 배치로 정리 (현재 scope 외)

### 4.2 좋아요 목록 조회: N+1 방지 전략

**문제:** Like 도메인이 Product를 직접 참조하지 않는 구조(ID 참조)에서 Product 정보를 어떻게 가져올 것인가?

**선택지:**

| 선택지 | 방식                               | 장점               | 단점                                    |
|-----|----------------------------------|------------------|---------------------------------------|
| A   | LikeRepository에서 Product JOIN 쿼리 | 쿼리 1회            | Like 도메인이 Product 테이블 구조를 알아야 함       |
| B   | Like 조회 후 Facade에서 Product IN 조회 | 도메인 분리 유지, 쿼리 2회 | 코드가 약간 더 많음                           |
| C   | @EntityGraph 사용                  | JPA가 관리          | Like와 Product 간 JPA 관계(@ManyToOne) 필요 |

**결정:** 선택지 B. 2-query 방식. Facade에서 조합.

```
1. LikeRepository.findAllByUserId(userId) → List<Like>
2. ProductService.getActiveProductsByIds(productIds) → List<Product>
3. Facade에서 Like + Product 조합 → List<LikeInfo>
```

**근거:**

- ID 참조 아키텍처를 유지하면서 N+1 방지 (IN 쿼리 1회)
- Like 도메인이 Product의 `deletedAt`을 알 필요 없음 — Product 필터링은 ProductService 책임

### 4.3 Like 동시 요청 멱등 처리

**문제:** check-then-act 패턴에서 동시 요청 시 UNIQUE 제약 위반이 발생할 수 있다.

**선택지:**

- A: UPSERT (INSERT IGNORE / ON DUPLICATE KEY)
- B: `DataIntegrityViolationException` catch + 멱등 처리

**결정:** 선택지 B. 예외를 잡아서 멱등 응답을 반환한다.

**근거:**

- JPA 표준 방식으로 처리 가능 (DB 벤더 종속 SQL 없이)
- 동시 요청은 예외적 상황이므로 낙관적 접근이 적절
- UPSERT는 JPA 영속성 컨텍스트와 동기화가 까다로움

### 4.4 좋아요 목록 조회 URL: B안 선택

**문제:** `GET /api/v1/users/{userId}/likes`에서 인증 필수인데 `{userId}` path variable이 필요한가? 어차피 본인만 조회 가능한데 타인 ID를 URL에 넣을 수 있는
구조가 맞는가?

**선택지:**

| 선택지 | URL                            | 타인 조회 방지                                   | 비고                      |
|-----|--------------------------------|--------------------------------------------|-------------------------|
| A   | `/api/v1/users/{userId}/likes` | Controller에서 `userId != authUser` 체크 (403) | RESTful하지만 중복 정보        |
| B   | `/api/v1/users/likes`          | 구조적으로 불가능 (URL에 타인 ID 넣을 곳 없음)             | 기존 `/users/user` 패턴과 일관 |

**결정:** 선택지 B. `GET /api/v1/users/likes` + `@AuthUser userId: Long`

**근거:**

- 기존 프로젝트 패턴 (`/users/user`, `/users/user/password`)과 일관
- 권한 체크를 코드가 아닌 URL 구조로 해결 — 실수로 뚫릴 가능성 제거
- 어드민이 타인 조회 필요하면 `/api-admin/v1/users/{userId}/likes`로 분리 가능 (기존 어드민 경로 분리 패턴)

---

## 5. 인증/인가

### 5.1 @AuthUser + AuthUserArgumentResolver

**결정:** `@AuthUser` 커스텀 어노테이션 + `AuthUserArgumentResolver` (HandlerMethodArgumentResolver 구현체)

**흐름:**

1. `AuthInterceptor`가 헤더에서 userId 추출 → `request.setAttribute("userId", user.id)`
2. `AuthUserArgumentResolver`가 `@AuthUser userId: Long` 파라미터에 주입
3. 좋아요, 주문 등 인증 필요 API에서 Controller 메서드 파라미터로 userId 주입

**에러 처리:**

- 인증 헤더 누락/인증 실패: 401 (UNAUTHORIZED)로 통일. 좋아요, 주문 예외 흐름 테이블에서도 동일하게 반영.
- 이유: 헤더 누락도 "인증되지 않은 요청"이므로 400보다 401이 의미적으로 정확. 섹션 7(인증/인가)의 "인증 실패 시 401 응답" 원칙과 통일.

---

## 6. 상품 도메인

### 6.1 getActiveProduct vs getProduct: 리턴 타입이 다른 이유

**문제:** 대고객 상품 상세 조회는 `ProductDetailInfo`를, 어드민 상품 상세 조회는 `ProductInfo`를 리턴한다. 왜 같은 상품 조회인데 다른 타입인가?

**결정:** 대고객: `ProductDetailInfo` (Product + Brand), 어드민: `ProductInfo` (Product only)

```
ProductDetailInfo {
    ProductInfo product
    BrandInfo brand       ← 대고객만 필요
}
```

**근거:**

- 대고객 상품 상세 → Facade에서 ProductService + BrandService 호출 (cross-domain)
- 어드민 상품 상세 → Facade에서 ProductService만 호출 (single-domain)
- 응답 객체를 분리하면 불필요한 DB 조회를 방지

**영향:** ProductFacade → BrandService 의존이 "상품 등록 시 브랜드 존재 확인"뿐 아니라 "대고객 상품 상세 조회 시 브랜드 정보 조합"도 포함.

### 6.2 HIDDEN status 우선순위 (수정 시 자동 전이 규칙)

**문제:** 어드민이 상품 수정 시 `status=HIDDEN, stock=123`을 보내면, `adjustStatusByStock()`이 stock > 0이므로 `ON_SALE`로 덮어쓰는가?

**선택지:**

- A: 명시된 status가 무조건 우선 (모순 상태 허용, 어드민 책임)
- B: HIDDEN만 우선, 나머지는 자동 전이 규칙 적용
- C: 모순 상태(stock=0 + ON_SALE) 시 400 에러

**결정:** 선택지 B. HIDDEN만 우선, 나머지는 자동 전이.

**예시:**

| 요청 | 결과 status | 이유 |
|------|-----------|------|
| stock=123, status=HIDDEN | HIDDEN | 어드민 의도 우선 |
| stock=0, status=ON_SALE | SOLD_OUT | 자동 전이 규칙 적용 |
| stock=123, status=SOLD_OUT | ON_SALE | 자동 전이 규칙 적용 |

**근거:**

- HIDDEN은 "어드민이 의도적으로 노출을 차단한 상태"이므로 자동 전이로 풀리면 안 됨
- ON_SALE/SOLD_OUT은 재고 기반 자동 전이가 정확한 상태를 보장
- 선택지 A는 stock=0인데 ON_SALE인 모순 상태를 허용하여 사용자 혼란 유발
- 선택지 C는 어드민 UX가 나빠짐 (stock과 status를 항상 맞춰서 보내야 함)

**구현 힌트:** `update()` 메서드에서 status가 HIDDEN이면 `adjustStatusByStock()`을 건너뛰고, 그 외에는 stock 기반으로 status를 재설정.

**영향:** 요구사항(섹션 4.1), 시퀀스(2.9 Note), 클래스 다이어그램(adjustStatusByStock 설명) 3곳 동시 반영.

### 6.3 ProductFacade 메서드 수 검증

**문제:** 클래스 다이어그램의 ProductFacade에 8개 메서드가 있었는데, API 엔드포인트는 7개뿐이었다. `deleteProductsByBrandId`가 불필요한 메서드였다.

**결정:** ProductFacade에서 `deleteProductsByBrandId` 제거. ProductService에만 유지.

```
BrandFacade.deleteBrand()
  → BrandService.deleteBrand()                  // 브랜드 soft delete
  → ProductService.deleteProductsByBrandId()     // 상품 cascade soft delete
```

**근거:**

- Facade는 Controller에서 호출하는 API 진입점. 다른 Facade에서 호출하는 것은 아키텍처 위반
- Cross-domain 호출은 Facade → 다른 도메인의 Service로 직접 접근이 올바른 패턴
- 불필요한 메서드는 agent의 혼란을 유발

**영향:** ProductFacade 메서드 8개 → 7개 = API 엔드포인트 7개와 정확히 일치.

---

## 7. 인덱스 전략

### 7.1 products 복합 인덱스

**결정:** `(deleted_at, status, like_count DESC)` 복합 인덱스 사용.

**근거:** 대고객 상품 목록 쿼리(`WHERE deleted_at IS NULL AND status != 'HIDDEN' ORDER BY like_count DESC`)에서 MySQL은 하나의 인덱스만
사용하므로, WHERE + ORDER BY를 함께 커버하는 복합 인덱스가 효율적. 기존 `(status, deleted_at)` + `(like_count)` 별도 인덱스를 통합했다.

### 7.2 brandId 복합 인덱스 리스크

**문제:** 대고객 상품 목록 조회에서 `brandId=5 AND deletedAt IS NULL AND status != 'HIDDEN' ORDER BY created_at DESC` 쿼리를 실행하면, 기존 인덱스로는 최적의 실행 계획을 만들 수 없다.

- `(brand_id)` 단독 인덱스 → brand_id 필터는 되지만 정렬에 filesort 필요
- `(deleted_at, status, created_at)` 복합 인덱스 → 정렬은 되지만 brand_id는 후필터

**결정:** 현재 데이터 규모에서는 추가 인덱스 불필요. ERD 리스크 테이블에 "인덱스 커버리지" 항목으로 기록하고, 데이터 증가 시 `(brand_id, deleted_at, status, created_at)` 복합 인덱스 추가를 검토한다.

**근거:** 초기 데이터량에서 불필요한 인덱스는 쓰기 성능만 떨어뜨린다. 실제 느려지는 시점에 EXPLAIN으로 확인하고 추가하는 게 적절.

### 7.3 likes UNIQUE 인덱스

**결정:** `(user_id, product_id)` UNIQUE — 중복 좋아요 방지 + user_id 단독 조회도 커버.

별도 `user_id` 인덱스는 불필요 (복합 인덱스의 최좌측 컬럼이므로).

---

## 8. 문서 정합성 리뷰에서 발견한 이슈

### 8.1 어드민 상품 목록 조회 Repository 메서드 누락 (7차)

**문제:** 시퀀스 2.6에서 `findAll(brandId, pageable)`을 호출하는데, ProductRepository에는 `findAll(pageable)`만 있었다.

**선택지:**

| 선택지 | 방식                                          | 장점    | 단점         |
|-----|---------------------------------------------|-------|------------|
| A   | `findAllProducts(brandId, pageable)` 추가     | 단순명료  | 메서드 하나 추가  |
| B   | findActiveProducts를 일반화하여 admin 모드 파라미터 추가  | 재사용   | 복잡도 증가     |
| C   | `findAll(pageable)` + Service 레벨 brandId 필터 | 변경 없음 | 페이징 데이터 손실 |

**결정:** 선택지 A. `findAllProducts(brandId, pageable)` 메서드 추가.

**근거:** 선택지 C는 페이징이 무의미해지고, 선택지 B는 한 메서드에 두 가지 의도를 담아 가독성이 떨어짐.

### 8.2 도메인 간 의존 관계 라벨의 완전성 (7차)

**문제:** 클래스 다이어그램 Section 6의 의존 관계 화살표 라벨이 의존 이유를 일부만 기술하고 있었다.

- ProductFacade → BrandService: "상품 등록 시 브랜드 존재 확인"만 기술 → **상품 상세 조회 시 브랜드 정보 조합** 누락
- LikeFacade → ProductService: "좋아요 등록/취소 시 likeCount 증감"만 기술 → **좋아요 목록 조회 시 활성 상품 조합** 누락

**결정:** 모든 의존 이유를 라벨에 명시.

**근거:** 구현하는 agent가 이 다이어그램만 보고 의존 주입(DI) 범위를 결정할 수 있어야 한다. 라벨이 불완전하면 "왜 이 의존이 필요하지?" 의문이 생겨 임의 판단의 여지가 생김.

---

## 9. 향후 검토 사항

### 9.1 재고 동시성

**현재:** 단일 요청 기준으로 처리 (향후 과제).

**후보 방안:**

- Atomic SQL UPDATE: `UPDATE products SET stock = stock - :qty WHERE id = :id AND stock >= :qty` (가장 단순)
- @Version (optimistic lock): 충돌 시 재시도 로직 필요
- SELECT FOR UPDATE: 비관적 락, 대기 시간 발생
- Redis Lua: 최고 성능, Redis 도입 필요

### 9.2 like_count 동시 갱신

동시 좋아요 시 lost update 가능. 현재는 단일 트랜잭션 내 처리. 향후 비관적 락 또는 Redis 캐싱 검토.

### 9.3 orders 기간 조회 성능

user_id + created_at 범위 검색, 데이터 증가 시 느려짐. 복합 인덱스 + 기본 1달 제한으로 대응. 향후 파티셔닝 또는 아카이빙 검토.

---

## 10. v2 리팩토링 (Gemini 리뷰 기반)

### 10.1 리팩토링 배경

v1 설계 문서를 Gemini에게 리팩토링 의뢰하여 v2를 생성했다. 핵심 리팩토링 원칙:

1. **메서드 시그니처 제거** → 의도 기반 언어 (예: `getBrand(brandId)` → `브랜드 조회`)
2. **예외 분기(alt) 제거** → Happy Path만 표현
3. **참여자 레벨 통일** → User → Controller → Facade/Service → Repository
4. **복잡도 격리** → Cascade/복잡 로직은 Note로 분리

### 10.2 v1 대비 v2에서 살린 항목

v2 리팩토링 과정에서 v1의 중요 내용이 일부 누락되었다. v1과 비교하여 복원한 항목:

**시퀀스 다이어그램:**

- 공통 규칙 섹션 (트랜잭션, 인증, soft delete 컨벤션)
- 각 시퀀스 하단 "참고" 섹션 (N+1 방지, OrderItem 스냅샷 등 비즈니스 규칙)
- 4.1 주문 생성의 3단계 프로세스 (상품 검증 → 재고 차감 → 주문 생성)

**클래스 다이어그램:**

- ProductStatus / OrderStatus enum 다이어그램
- Product의 update(), increaseStock(), decreaseLikeCount() 메서드
- Like 도메인의 removeLike() (Facade + Service 양쪽)
- Order.totalPrice 필드
- ProductDetailDto / ProductDetailInfo (cross-domain 조합 응답)
- BrandFacade의 getActiveBrand / getBrand 구분
- Facade 의존 관계 라벨 완전화
- VO 정리 테이블 (Section 8)
- 시퀀스 교차 참조 테이블 (Section 10)

### 10.3 Gemini 피드백 5건 반영

v2 문서 4개를 Gemini에게 분석시켜 받은 피드백과 반영 결과:

| # | 피드백                                                     | 반영 파일               | 처리                                                                       |
|---|---------------------------------------------------------|---------------------|--------------------------------------------------------------------------|
| 1 | API 파라미터 네이밍: `startAt`/`endAt` → `startedAt`/`endedAt` | 01-requirements-v2  | 이미 올바르게 사용 중. 원본 `requirements-analysis.md`를 수정하여 통일                     |
| 2 | Enum 타입 매핑: `VARCHAR(20)` + `@Enumerated(STRING)` 명시    | 04-erd-v2           | products.status, orders.status 설명에 `@Enumerated(STRING) → VARCHAR 매핑` 추가 |
| 3 | 인덱스 컬럼 순서: Cardinality 기반 재조정 가능성                       | 04-erd-v2           | 인덱스 전략 하단에 분포도 기반 컬럼 순서 재조정 노트 추가                                        |
| 4 | User 도메인 API: 고객용 API 테이블에 User API 누락                  | 01-requirements-v2  | 회원가입, 내 정보 조회, 비밀번호 변경 3개 API 추가                                         |
| 5 | 삭제된 상품 좋아요 취소 시 likeCount 미갱신 강조                        | 03-class-diagram-v2 | Like 핵심 포인트에 삭제 상품 조건 상세 설명 추가                                           |

### 10.4 v2 2차 리뷰 — Facade 제거 및 문서 확장

v2 문서에 대해 2차 설계 리뷰를 수행하여 추가 개선한 사항:

**Facade 1:1 pass-through 제거 (시퀀스 14개 + 클래스 4개 섹션):**

1.3 Facade 선택적 사용 결정을 실제 v2 문서에 적용. 단일 서비스만 호출하는 시퀀스 14개에서 Facade 참여자를 제거하고, Controller → Service 직접 호출로 변경.

- 시퀀스: 1.1, 1.2, 2.1~2.4, 2.6, 2.7, 2.9, 2.10, 4.2, 4.3, 5.1, 5.2
- 클래스: Brand(섹션2), Product(섹션3), Order(섹션5), Facade Architecture View(섹션6)
- 4.3 주문 상세 조회: 소유권 검증 책임을 Facade에서 Service로 이관 (단일 도메인 관심사)

**플로우차트 문서 신설 (05-flowcharts-v2.md):**

시퀀스 다이어그램과 역할 분리 — "누가 호출하는가" vs "어떤 조건에서 분기하는가".
단순 CRUD는 제외하고 **분기가 복잡한 흐름만** 선정:

| 플로우차트 | 선정 이유 | 시퀀스 대응 |
|-----------|---------|----------|
| 서비스 전체 흐름 | 시스템 조감도 | - |
| 주문 생성 프로세스 | 검증 5단계 분기 + cross-domain | 4.1 |
| 좋아요 토글 (등록/취소) | 멱등성 + 삭제 상품 분기 | 3.1, 3.2 |

**API 엔드포인트 마인드맵 (01-requirements-v2.md에 추가):**

요구사항 섹션 6 API 명세에 마인드맵 추가. 도메인별/액터별 엔드포인트를 계층적으로 시각화하여, 상세 테이블 이전에 전체 구조를 한눈에 파악할 수 있게 함.

### 10.5 requirements-analysis.md 동기화

원본 요구사항 문서(`docs/requirements-analysis.md`)와 design-v2 간 괴리 4건을 수정:

| 항목          | 수정 전                           | 수정 후                        |
|-------------|--------------------------------|-----------------------------|
| 좋아요 목록 URI  | `/api/v1/users/{userId}/likes` | `/api/v1/users/likes`       |
| 주문 조회 파라미터  | `startAt` / `endAt`            | `startedAt` / `endedAt`     |

---

## 11. 리뷰 수렴 과정

### v1 (초기 설계)

| 차수   | 발견 이슈 | 주요 내용                                                |
|------|-------|------------------------------------------------------|
| 1~4차 | 다수    | 요구사항 정제, 시퀀스 예외 흐름 추가, VO/Entity 경계 정의               |
| 5차   | 4건    | Service 리턴 타입 Info→Entity, 시퀀스 Note 정리               |
| 6차   | 3건    | 메서드 네이밍 불일치 6곳, Like 목록 Product 조회 경로, return 화살표 누락 |
| 7차   | 2건    | Repository 메서드 누락, Section 6 라벨 불완전                  |
| 8차   | 0건    | 전수 검사 통과 — PR 제출 가능                                  |

후반 4번의 반복으로 이슈가 4 → 3 → 2 → 0으로 수렴했다.

### v2 (2차 리뷰)

| 차수  | 발견 이슈 | 주요 내용                                                                         |
|-----|-------|-------------------------------------------------------------------------------|
| 1차  | 6건    | status 우선순위 경계 미정의, 시퀀스 2.9 규칙 미반영, adjustStatusByStock 갭, isDeleted 불일치, OrderItem BaseEntity, brandId 인덱스 |
| 2차  | 0건    | 6건 모두 반영 확인. 잔여 2건은 구현 단계 테스트로 커버                                             |

1차 6건 → 2차 0건으로 수렴. 설계 문서 5종(요구사항, 시퀀스, 클래스, ERD, 플로우차트) 간 정합성 확보.

---

## 12. 배운 점

### 설계 문서는 "코드의 선행 지표"다

시퀀스 다이어그램에서 메서드 이름이 클래스 다이어그램과 다른 걸 발견했을 때, 만약 코드를 먼저 작성했다면 리팩토링 비용이 훨씬 컸을 것이다. 문서 단계에서 텍스트 수정으로 해결할 수 있었다.

### 교차 검증이 핵심이다

한 문서만 보면 완벽해 보여도, 다른 문서와 대조하면 불일치가 드러난다. 특히:

- **시퀀스 ↔ 클래스 다이어그램**: 메서드명, 파라미터, 리턴 타입
- **클래스 다이어그램 ↔ ERD**: 필드, 관계, 제약조건
- **요구사항 ↔ 시퀀스**: 예외 흐름, API 엔드포인트 수

### "agent가 구현할 수 있는가?"가 완성도의 기준이다

리뷰할 때마다 "이 문서만 보고 agent가 동일한 코드를 작성할 수 있는가?"를 기준으로 삼았다. Repository 메서드가 누락된 것을 발견한 것도, 의존 관계 라벨이 불완전한 것을 발견한 것도 이 기준
덕분이다.

### 선택지를 남기면 의사결정이 추적 가능해진다

각 결정에서 "왜 이 방법을 선택했는가"뿐만 아니라 "어떤 대안이 있었는가"를 기록했다. 나중에 요구사항이 바뀌어 다른 선택이 필요해질 때, 이 기록이 출발점이 된다.

### 헛점을 분류하면 학습 효율이 올라간다

리뷰 과정에서 발견한 문제를 "설계의 헛점"과 "지식의 헛점"으로 분류했다:

- **설계의 헛점**: agent가 만든 것이라도 내가 검증하지 못하면 버그가 되는 것 (예: Repository 메서드 누락, 의존 라벨 불완전)
- **지식의 헛점**: agent의 코드를 검증하려면 내가 알아야 하는 것 (예: JPA cascade 동작, @Transactional propagation, @Where 사이드이펙트)

이 분류 덕분에 "무엇을 공부해야 하는지"가 명확해졌다.
