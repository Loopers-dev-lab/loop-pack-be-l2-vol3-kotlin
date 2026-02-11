# Round 2 설계 결정 기록

이커머스 도메인(브랜드, 상품, 좋아요, 주문) 설계 과정에서 논의하고 결정한 사항들을 정리한다.
설계 문서(요구사항, 시퀀스, 클래스 다이어그램, ERD) 작성 후 8차에 걸친 리뷰를 통해 정제한 과정의 기록이다.
"어떤 agent에 맡겨도 동일한 구현이 나오는 문서"를 목표로, 발견된 모호함을 하나씩 제거해나갔다.

---

## 1. 레이어드 아키텍처 결정

### 1.1 Service 리턴 타입: Info or Entity

**문제:** 초기 시퀀스 다이어그램에서 Service가 `ProductInfo`, `BrandInfo` 같은 application 레이어 객체를 리턴하도록 작성했다. 그런데 클래스 다이어그램에서는 Service가 domain 레이어에 속하고, Info는 application 레이어에 속한다.

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

### 1.3 Facade 유지 (1:1 passthrough 포함)

**문제:** 단일 도메인만 다루는 API(예: 브랜드 CRUD)에서도 Facade를 거쳐야 하는가? 1:1 passthrough는 불필요한 레이어 아닌가?

**선택지:**

- A: 1:1인 경우 Controller → Service 직접 호출
- B: 모든 경우 Facade 유지

**결정:** 모든 경우 Facade를 유지한다 (선택지 B).

**근거:**

- Controller는 항상 Facade만 의존 → 의존성이 단순하고 일관
- cross-domain 로직 추가 시 Controller 변경 없이 Facade만 확장
- Info/Dto 변환이 항상 application 레이어에서 일어남
- **단, 1:1 Facade 메서드에 @Transactional은 붙이지 않음** — Service에 위임

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

**결정:** OrderItem은 독립 Entity가 아닌 Order Aggregate의 내부 Entity.

**특성:**

- id를 가지지만 독립 생명주기와 독립 Repository가 없다. 항상 Order를 통해서만 접근
- 단방향 `@OneToMany(cascade = ALL, orphanRemoval = true)` + `@JoinColumn(name = "order_id", nullable = false)`로 구현
- OrderItem에서 Order로의 역참조(`@ManyToOne`) 없음
- `nullable = false`는 Hibernate의 extra UPDATE 문제를 방지 — INSERT 시 order_id를 함께 설정

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

### 3.2 Order.create() 정적 팩토리

**결정:** `Order.create(userId, items)` 정적 팩토리에서 Order + OrderItem을 한 번에 생성.

**책임:**

- OrderItem 생성
- totalPrice 계산 (items의 `productPrice * quantity` 합산)
- 중복 상품 병합 (3.3 참조)

### 3.3 중복 상품 병합 + 재고 차감 순서

**문제:** 같은 productId가 여러 항목에 포함될 수 있다. 재고 차감 시 데드락을 어떻게 방지하는가?

**결정:**

1. **중복 병합:** 같은 productId가 여러 항목에 포함되면 수량을 합산하여 하나의 OrderItem으로 병합. OrderService.createOrder 내부에서 도메인 로직으로 처리.
2. **재고 차감 순서:** 병합 후 productId 오름차순으로 정렬하여 재고를 차감. 모든 트랜잭션이 동일한 순서로 락을 획득하므로 데드락을 방지.

### 3.4 주문 목록 조회 기간 필터

**파라미터:** `startedAt` / `endedAt` (LocalDateTime)

**범위:** `startedAt >=` (inclusive), `endedAt <` (exclusive) — **half-open 범위**

**기준 컬럼:** `created_at`

**Repository 메서드명:** `findOrdersByUserIdInPeriod`

- Spring Data JPA의 `Between`은 양쪽 inclusive를 암시하므로, half-open 범위에는 부적절
- 커스텀 네이밍으로 의도를 명확히 함

**TimeZone:** CommerceApiApplication의 `DefaultTimeZone`으로 ZonedDateTime 변환.

### 3.5 OrderService 메서드 정리

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

---

## 5. 인증/인가

### 5.1 @AuthUser + AuthUserArgumentResolver

**결정:** `@AuthUser` 커스텀 어노테이션 + `AuthUserArgumentResolver` (HandlerMethodArgumentResolver 구현체)

**흐름:**

1. `AuthInterceptor`가 헤더에서 userId 추출 → `request.setAttribute("userId", user.id)`
2. `AuthUserArgumentResolver`가 `@AuthUser userId: Long` 파라미터에 주입
3. 좋아요, 주문 등 인증 필요 API에서 Controller 메서드 파라미터로 userId 주입

**에러 처리:**

- 인증 헤더 누락: 기존 1주차 구현에서 400 (BAD_REQUEST)으로 응답. 설계 문서의 401과 차이가 있으나, 기존 동작을 변경하지 않기로 함.

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

### 6.2 ProductFacade 메서드 수 검증

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

### 7.2 likes UNIQUE 인덱스

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

**현재:** 단일 요청 기준으로 처리 (향후 과제). productId 오름차순 정렬로 데드락은 방지.

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

## 10. 리뷰 수렴 과정

| 차수   | 발견 이슈 | 주요 내용                                                |
|------|-------|------------------------------------------------------|
| 1~4차 | 다수    | 요구사항 정제, 시퀀스 예외 흐름 추가, VO/Entity 경계 정의               |
| 5차   | 4건    | Service 리턴 타입 Info→Entity, 시퀀스 Note 정리               |
| 6차   | 3건    | 메서드 네이밍 불일치 6곳, Like 목록 Product 조회 경로, return 화살표 누락 |
| 7차   | 2건    | Repository 메서드 누락, Section 6 라벨 불완전                  |
| 8차   | 0건    | 전수 검사 통과 — PR 제출 가능                                  |

후반 4번의 반복으로 이슈가 4 → 3 → 2 → 0으로 수렴했다.

---

## 11. 배운 점

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
