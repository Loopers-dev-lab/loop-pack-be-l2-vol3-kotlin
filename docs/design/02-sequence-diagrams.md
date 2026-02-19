# 시퀀스 다이어그램

시스템의 주요 기능에 대한 **핵심 성공 흐름(Happy Path)** 을 기술한다.
상세한 예외 처리 규칙(400, 404 등)과 필드 검증 로직은 요구사항 명세서를 참고한다.

### 다이어그램 공통 규칙

- **참여자(Participant) 레벨 통일**:
    - **Controller**: 요청 수신, 파라미터 매핑, 응답 변환
    - **Facade**: 트랜잭션 단위 설정 및 도메인 간 조율
    - **Service**: 핵심 비즈니스 로직 및 도메인 규칙 수행
    - **Repository**: DB 접근 (JPA)
- **트랜잭션 경계**: Service는 변경 작업에 `@Transactional` 필수 적용. Facade는 여러 Service를 조합하여 원자성이 필요한 경우에만 선택적 적용.
- **인증**: AuthInterceptor → `@AuthUser userId: Long` 파라미터 주입. 어드민은 AdminInterceptor가 `X-Loopers-Ldap` 헤더 검증.
- **soft delete**: `@Where` 미사용. Repository 메서드마다 `deletedAt IS NULL` 조건을 명시적으로 추가.
- **생략된 내용**:
    - 인증 인터셉터 처리 과정 (전제 조건으로 취급)
    - 상세한 DTO 변환 과정
    - 단순 유효성 검증 실패(400 Bad Request) 흐름

---

## 1. 브랜드 & 상품 — 대고객 API

### 1.1 브랜드 정보 조회

**API:** `GET /api/v1/brands/{brandId}` — 인증 불필요

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant C as BrandController
    participant S as CatalogService
    participant R as BrandRepository
    User ->> C: 브랜드 상세 정보 요청
    C ->> S: 유효한 브랜드 조회 요청
    S ->> R: DB 조회 (삭제되지 않은 브랜드)
    R -->> S: 브랜드 엔티티
    S -->> C: Brand 엔티티
    C -->> User: 200 OK
```

### 1.2 상품 목록 조회

**API:** `GET /api/v1/products` — 인증 불필요

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant C as ProductController
    participant S as CatalogService
    participant R as ProductRepository
    User ->> C: 상품 목록 조회 요청 (brandId, 정렬, 페이징)
    C ->> S: 조건에 맞는 판매중인 상품 검색
    S ->> R: DB 조회 (삭제/HIDDEN 제외, 정렬 적용)
    R -->> S: 상품 목록 (Page)
    S -->> C: Page<Product> 반환
    C -->> User: 200 OK
```

#### 참고

- 필터 조건: `deletedAt IS NULL AND status != 'HIDDEN'`, brandId 선택적 필터
- 응답에 페이징 메타데이터 포함: content, totalElements, totalPages, number, size

### 1.3 상품 상세 조회 (Domain Service)

> **Round 3 변경**: ProductFacade → CatalogService (Domain Service)로 이동

상품 정보와 해당 브랜드 정보를 **도메인 서비스**에서 조합하여 응답하는 흐름이다.
Facade를 사용하지 않고 Controller가 CatalogService를 직접 호출한다.

**API:** `GET /api/v1/products/{productId}` — 인증 불필요

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant C as ProductController
    participant S as CatalogService
    participant PR as ProductRepository
    participant BR as BrandRepository
    User ->> C: 상품 상세 정보 요청
    C ->> S: 상세 정보 조회 요청
    S ->> PR: Product 조회 (삭제/HIDDEN 제외)
    PR -->> S: Product 엔티티
    S ->> BR: 상품의 브랜드 정보 조회
    BR -->> S: Brand 엔티티
    S ->> S: 상품 + 브랜드 정보 조합
    S -->> C: ProductDetail 반환
    C -->> User: 200 OK
```

#### 참고

- CatalogService는 Catalog 바운디드 컨텍스트의 단일 Domain Service이다 (`domain/catalog/` 패키지에 위치)
- Product와 Brand가 같은 Catalog 경계에 있으므로 ProductRepository와 BrandRepository를 직접 주입받아 사용한다
- Facade를 거치지 않으므로 Controller가 직접 호출한다

---

## 2. 브랜드 & 상품 — 어드민 API

### 2.1 브랜드 목록 조회

**API:** `GET /api-admin/v1/brands` — LDAP 인증

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 어드민
    participant C as BrandAdminController
    participant S as CatalogService
    participant R as BrandRepository
    Admin ->> C: 브랜드 목록 조회 요청 (페이징)
    C ->> S: 전체 브랜드 조회 요청
    S ->> R: DB 조회 (삭제된 브랜드 포함)
    R -->> S: 브랜드 목록 (Page)
    S -->> C: Page<Brand> 반환
    C -->> Admin: 200 OK
```

#### 참고

- 어드민 목록 조회는 삭제된 브랜드 포함 (필터 없음)

### 2.2 브랜드 상세 조회

**API:** `GET /api-admin/v1/brands/{brandId}` — LDAP 인증

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 어드민
    participant C as BrandAdminController
    participant S as CatalogService
    participant R as BrandRepository
    Admin ->> C: 브랜드 상세 조회 요청
    C ->> S: ID로 브랜드 검색
    S ->> R: DB 조회
    R -->> S: 브랜드 엔티티
    S -->> C: Brand 엔티티
    C -->> Admin: 200 OK
```

#### 참고

- 어드민은 삭제된 브랜드도 조회 가능 (삭제 상태 확인 목적)

### 2.3 브랜드 등록

**API:** `POST /api-admin/v1/brands` — LDAP 인증

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 어드민
    participant C as BrandAdminController
    participant S as CatalogService
    participant R as BrandRepository
    Admin ->> C: 브랜드 등록 요청
    C ->> S: 브랜드 생성 요청
    Note over S, R: @Transactional
    S ->> S: Brand 엔티티 생성 (이름 검증 포함)
    S ->> R: 저장
    R -->> S: 생성된 브랜드 (ID 채번)
    S -->> C: brandId 반환
    C -->> Admin: 200 OK
```

### 2.4 브랜드 수정

**API:** `PUT /api-admin/v1/brands/{brandId}` — LDAP 인증

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 어드민
    participant C as BrandAdminController
    participant S as CatalogService
    participant R as BrandRepository
    Admin ->> C: 브랜드 수정 요청
    C ->> S: 브랜드 정보 수정 요청
    Note over S, R: @Transactional
    S ->> R: ID로 브랜드 조회
    S ->> S: 정보 업데이트 (Dirty Checking)
    S -->> C: brandId 반환
    C -->> Admin: 200 OK
```

### 2.5 브랜드 삭제 (Cascade Soft Delete)

브랜드 삭제 시 소속 상품까지 일괄 soft delete하는 Catalog 경계 내 Domain Service 흐름이다.

**API:** `DELETE /api-admin/v1/brands/{brandId}` — LDAP 인증

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 어드민
    participant C as BrandAdminController
    participant S as CatalogService
    participant BR as BrandRepository
    participant PR as ProductRepository
    Admin ->> C: 브랜드 삭제 요청
    C ->> S: 삭제 프로세스 시작

    rect rgb(245, 245, 245)
        Note right of S: @Transactional
        S ->> BR: 브랜드 조회
        S ->> S: Soft Delete 처리
        S ->> BR: 변경 저장
        S ->> PR: 해당 브랜드의 상품 전체 조회
        S ->> S: 상품별 Soft Delete 마킹
        S ->> PR: 변경 사항 일괄 저장
    end

    S -->> C: 처리 완료
    C -->> Admin: 200 OK
```

#### 참고

- BaseEntity.delete()는 이미 삭제 상태면 무시 (멱등)
- 기존 주문의 OrderItem 스냅샷은 Product 삭제와 무관하게 보존
- CatalogService는 같은 Catalog 경계 내의 ProductRepository를 직접 주입받아 cascade 삭제를 처리한다

### 2.6 상품 목록 조회

**API:** `GET /api-admin/v1/products` — LDAP 인증

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 어드민
    participant C as ProductAdminController
    participant S as CatalogService
    participant R as ProductRepository
    Admin ->> C: 상품 목록 조회 요청 (brandId 필터, 페이징)
    C ->> S: 조건별 상품 목록 검색
    S ->> R: DB 조회 (삭제된 상품 포함)
    R -->> S: 상품 목록 (Page)
    S -->> C: Page<Product> 반환
    C -->> Admin: 200 OK
```

#### 참고

- 어드민 목록 조회는 삭제된 상품 포함 (필터 없음)

### 2.7 상품 상세 조회

**API:** `GET /api-admin/v1/products/{productId}` — LDAP 인증

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 어드민
    participant C as ProductAdminController
    participant S as CatalogService
    participant R as ProductRepository
    Admin ->> C: 상품 상세 조회 요청
    C ->> S: ID로 상품 검색
    S ->> R: DB 조회
    R -->> S: 상품 엔티티
    S -->> C: Product 엔티티
    C -->> Admin: 200 OK
```

#### 참고

- 어드민은 삭제된 상품도 조회 가능 (삭제 상태 확인 목적)

### 2.8 상품 등록 (Catalog 내 브랜드 검증)

상품 등록 시 같은 Catalog 경계 내의 브랜드 유효성을 검증하는 흐름이다.

**API:** `POST /api-admin/v1/products` — LDAP 인증

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 어드민
    participant C as ProductAdminController
    participant S as CatalogService
    participant BR as BrandRepository
    participant PR as ProductRepository
    Admin ->> C: 상품 등록 요청 (brandId 포함)
    C ->> S: 상품 생성 요청

    rect rgb(245, 245, 245)
        Note right of S: @Transactional
        S ->> BR: 브랜드 유효성 확인 (존재 및 활성 여부)
        BR -->> S: 유효한 브랜드 확인
        S ->> S: Product 엔티티 생성 (가격/재고 검증 포함)
        S ->> PR: 상품 저장
    end

    S -->> C: productId 반환
    C -->> Admin: 200 OK
```

### 2.9 상품 수정

**API:** `PUT /api-admin/v1/products/{productId}` — LDAP 인증

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 어드민
    participant C as ProductAdminController
    participant S as CatalogService
    participant R as ProductRepository
    Admin ->> C: 상품 수정 요청
    C ->> S: 상품 정보 수정 요청
    Note over S, R: @Transactional
    S ->> R: ID로 상품 조회
    S ->> S: 정보 업데이트 (가격/재고/상태)
    Note right of S: 브랜드 변경 불가 규칙 검증<br/>HIDDEN 명시 시 자동 전이 미적용
    S -->> C: productId 반환
    C -->> Admin: 200 OK
```

### 2.10 상품 삭제

**API:** `DELETE /api-admin/v1/products/{productId}` — LDAP 인증

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 어드민
    participant C as ProductAdminController
    participant S as CatalogService
    participant R as ProductRepository
    Admin ->> C: 상품 삭제 요청
    C ->> S: 상품 삭제 요청
    Note over S, R: @Transactional
    S ->> R: ID로 상품 조회
    S ->> S: Soft Delete 처리
    S -->> C: 처리 완료
    C -->> Admin: 200 OK
```

#### 참고

- BaseEntity.delete()는 이미 삭제 상태면 무시 (멱등)
- 삭제된 상품의 like 는 추후 배치에서 제거

---

## 3. 좋아요

### 3.1 좋아요 등록 (멱등성)

**API:** `POST /api/v1/products/{productId}/likes` — 인증 필요

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant C as LikeController
    participant F as LikeFacade
    participant CS as CatalogService
    participant LS as LikeService
    participant R as Repository
    User ->> C: 좋아요 등록 요청
    C ->> F: 좋아요 처리 위임

    rect rgb(245, 245, 245)
        Note right of F: @Transactional (Facade)
        F ->> CS: 활성 상품 조회 (삭제/HIDDEN 제외)
        CS -->> F: Product 엔티티
        F ->> LS: 좋아요 추가 요청
        LS ->> R: 기존 좋아요 여부 확인
        alt 새로운 좋아요
            LS ->> R: 좋아요 엔티티 저장
            LS -->> F: true (실제 변경 발생)
            F ->> CS: 상품의 likeCount 증가 요청
            CS ->> R: 상품 업데이트
        else 이미 좋아요 존재
            LS -->> F: false (멱등, 상태 변화 없음)
        end
    end

    F -->> C: 성공 반환
    C -->> User: 200 OK
```

### 3.2 좋아요 취소 (멱등성 & Hard Delete)

좋아요 취소는 **물리적 삭제(Hard Delete)**를 수행하며, 삭제된 상품에 대해서도 멱등하게 처리된다.

**API:** `DELETE /api/v1/products/{productId}/likes` — 인증 필요

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant C as LikeController
    participant F as LikeFacade
    participant CS as CatalogService
    participant LS as LikeService
    participant R as Repository
    User ->> C: 좋아요 취소 요청
    C ->> F: 처리 위임

    rect rgb(245, 245, 245)
        Note right of F: @Transactional (Facade)
        F ->> CS: 대상 상품 조회 (삭제된 상품 포함)
        CS -->> F: Product 엔티티
        F ->> LS: 좋아요 삭제 요청
        LS ->> R: 기존 좋아요 여부 확인
        alt 좋아요 존재
            LS ->> R: 좋아요 엔티티 물리 삭제
            LS -->> F: true (실제 삭제 발생)
        else 좋아요 없음
            LS -->> F: false (멱등, 상태 변화 없음)
        end

        opt 실제 삭제(true) AND 상품이 활성 상태
            F ->> CS: 상품의 likeCount 감소 요청
            CS ->> R: 상품 업데이트
        end
    end

    F -->> C: 성공 반환
    C -->> User: 200 OK
```

#### 참고

- **삭제된 상품 포함 조회:** 좋아요 취소 시에는 삭제된 상품도 포함하여 조회한다. 삭제된 상품의 좋아요도 취소할 수 있어야 하기 때문이다 (요구사항: "삭제된 상품에 대한 좋아요 취소 → 200 OK")
- 삭제된 상품의 likeCount는 갱신하지 않음 (의미 없는 카운트 변경 방지)

### 3.3 내 좋아요 목록 조회 (Data Assembly)

서로 다른 도메인(Like, Product)의 데이터를 조합하는 흐름이다.

**API:** `GET /api/v1/users/likes` — 인증 필요 (`@AuthUser`)

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant C as LikeController
    participant F as LikeFacade
    participant LS as LikeService
    participant CS as CatalogService
    participant R as Repository
    User ->> C: 내 좋아요 목록 조회 요청
    C ->> F: 데이터 조회 위임
    F ->> LS: 사용자의 좋아요 목록 조회
    LS ->> R: Like 테이블 조회
    LS -->> F: List<Like>
    F ->> CS: 활성 상품 일괄 조회 (productIds)
    CS ->> R: Product IN 쿼리 (삭제/HIDDEN 제외)
    CS -->> F: List<Product>
    F ->> F: 좋아요 + 상품 정보 조합
    F -->> C: List<LikeInfo> 반환
    C -->> User: 200 OK
```

#### 참고

- URL에 userId가 없으므로 타인 좋아요 조회 불가 (`@AuthUser`로 본인만 주입)
- 쿼리 총 2회: Like 조회 1회 + Product IN 조회 1회 (N+1 방지)
- Facade에서 Like + Product를 조합하여 LikeInfo 생성

---

## 4. 주문 — 대고객 API

### 4.1 주문 생성 (Cross-Domain Transaction)

> **Round 3 변경**: 포인트 차감 단계 추가

주문 생성은 **상품 검증 → 재고 차감 → 포인트 차감 → 주문 저장**이 원자적으로 이루어져야 하는 핵심 트랜잭션이다.

**API:** `POST /api/v1/orders` — 인증 필요

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant C as OrderController
    participant F as OrderFacade
    participant CS as CatalogService
    participant UPS as UserPointService
    participant OS as OrderService
    participant R as Repository
    User ->> C: 주문 요청 (상품 목록, 수량)
    C ->> F: 주문 프로세스 시작

    rect rgb(245, 245, 245)
        Note right of F: @Transactional (Facade)
    %% 1단계: 상품 유효성 확인
        F ->> CS: 주문 대상 상품 검증 요청
        CS ->> R: 상품 일괄 조회
        Note right of CS: 존재 여부 + 판매 가능 상태 확인
    %% 2단계: 재고 차감
        F ->> CS: 재고 차감 요청
        CS ->> CS: 상품별 재고 차감 처리
        CS ->> R: 재고 변경 저장
        Note right of CS: 재고 부족 시 예외 → 트랜잭션 전체 롤백
    %% 3단계: 주문 생성 (스냅샷 + totalPrice 내부 계산)
        F ->> F: Product → OrderProductInfo 변환
        F ->> OS: 주문 생성 요청 (userId, OrderProductInfo 목록, command)
        OS ->> OS: Order.create() — 스냅샷 + totalPrice 계산
        Note right of OS: 각 상품의 name, price를<br/>OrderItem에 스냅샷으로 복사<br/>totalPrice = Σ(price × quantity)
        OS ->> R: 주문 저장
        OS -->> F: Order 반환
    %% 4단계: 포인트 차감
        F ->> UPS: 포인트 차감 요청 (userId, order.totalPrice, orderId)
        UPS ->> R: UserPoint 조회
        UPS ->> UPS: 잔액 확인 + 차감
        Note right of UPS: 포인트 부족 시 예외 → 트랜잭션 전체 롤백
        UPS ->> R: UserPoint 저장
        UPS ->> R: PointHistory(USE, refOrderId) 저장
    end

    F -->> C: orderId 반환
    C -->> User: 200 OK
```

#### 참고

- 재고 부족 또는 포인트 부족 시 트랜잭션 롤백으로 이전 차감분 모두 원복
- OrderItem은 주문 시점의 상품 정보(이름, 가격)를 스냅샷으로 보존
- Facade는 Product → OrderProductInfo 변환(cross-domain 매핑)을 수행한 뒤 OrderService에 위임한다. Order 도메인은 Catalog 도메인 타입에 의존하지 않는다
- totalPrice는 Order.create() 내부에서 계산하여 Order 엔티티에 저장. Facade는 생성된 Order에서 totalPrice를 추출하여 UserPointService에 전달
- UserPointService는 포인트 차감 + PointHistory(USE, refOrderId) 기록을 함께 수행

### 4.2 주문 목록 조회 (기간 필터링)

**API:** `GET /api/v1/orders` — 인증 필요

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant C as OrderController
    participant OS as OrderService
    participant R as OrderRepository
    User ->> C: 주문 목록 조회 요청 (기간, 페이징)
    C ->> OS: 내 주문 내역 검색 요청
    OS ->> R: DB 조회 (userId + 기간 조건)
    R -->> OS: 주문 목록 (Page)
    OS -->> C: Page<Order> 반환
    C -->> User: 200 OK
```

#### 참고

- startedAt/endedAt 미입력 시 기본값: 최근 1달

### 4.3 주문 상세 조회

**API:** `GET /api/v1/orders/{orderId}` — 인증 필요

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant C as OrderController
    participant OS as OrderService
    participant R as OrderRepository
    User ->> C: 주문 상세 정보 요청
    C ->> OS: 주문 상세 조회 요청
    OS ->> R: DB 조회 (OrderItem 포함)
    R -->> OS: 주문 엔티티
    Note right of OS: 본인 주문인지 소유권 검증
    OS -->> C: Order 엔티티
    C -->> User: 200 OK
```

#### 참고

- 타인 주문 접근 시 403이 아닌 404 반환 (주문 존재 여부 노출 방지)

---

## 5. 주문 — 어드민 API

### 5.1 전체 주문 목록 조회

**API:** `GET /api-admin/v1/orders` — LDAP 인증

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 어드민
    participant C as OrderAdminController
    participant OS as OrderService
    participant R as OrderRepository
    Admin ->> C: 전체 주문 목록 조회 요청
    C ->> OS: 전체 주문 검색 요청
    OS ->> R: DB 조회 (userId 필터 없음)
    R -->> OS: 주문 목록 (Page)
    OS -->> C: Page<Order> 반환
    C -->> Admin: 200 OK
```

### 5.2 주문 상세 조회

**API:** `GET /api-admin/v1/orders/{orderId}` — LDAP 인증

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 어드민
    participant C as OrderAdminController
    participant OS as OrderService
    participant R as OrderRepository
    Admin ->> C: 주문 상세 조회 요청
    C ->> OS: 주문 ID로 검색 요청
    OS ->> R: DB 조회
    R -->> OS: 주문 엔티티 (OrderItem 포함)
    OS -->> C: Order 엔티티 (OrderItem 포함)
    C -->> Admin: 200 OK
```

#### 참고

- 어드민은 소유권 검증 없이 모든 주문 조회 가능

---

## 6. 포인트

### 6.1 포인트 충전 (Domain Service)

포인트 충전은 잔액 변경(UserPoint)과 충전 내역(PointHistory) 생성을 **PointChargingService**(Domain Service)가 조율하는 흐름이다.

**API:** `POST /api/v1/users/points/charge` — 인증 필요

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant C as PointController
    participant PCS as PointChargingService
    participant UPR as UserPointRepository
    participant PHR as PointHistoryRepository
    User ->> C: 포인트 충전 요청 (amount)
    C ->> PCS: 충전 처리 위임

    rect rgb(245, 245, 245)
        Note right of PCS: @Transactional
        PCS ->> UPR: UserPoint 조회 (userId)
        UPR -->> PCS: UserPoint 엔티티
        PCS ->> PCS: UserPoint.charge(amount) — 잔액 증가
        PCS ->> UPR: UserPoint 저장
        PCS ->> PHR: PointHistory(CHARGE) 저장
    end

    PCS -->> C: 충전 완료
    C -->> User: 200 OK
```

#### 참고

- PointChargingService는 도메인 레이어의 Domain Service이다
- UserPointRepository와 PointHistoryRepository를 직접 주입받아 사용한다
- Facade를 거치지 않으므로 Controller가 직접 호출한다

### 6.2 포인트 잔액 조회

**API:** `GET /api/v1/users/points` — 인증 필요

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant C as PointController
    participant UPS as UserPointService
    participant R as UserPointRepository
    User ->> C: 포인트 잔액 조회 요청
    C ->> UPS: 잔액 조회 요청
    UPS ->> R: UserPoint 조회 (userId)
    R -->> UPS: UserPoint 엔티티
    UPS -->> C: 잔액 정보 반환
    C -->> User: 200 OK
```

---

## 7. 회원가입

### 7.1 회원가입 (Cross-Domain)

> **Round 3 변경**: UserPoint 초기화를 위해 UserFacade 신규 도입

회원가입 시 User 생성과 함께 UserPoint(초기 잔액 0)를 생성하는 흐름이다.
User(인증/프로필)와 Point(잔액 관리) 경계를 넘으므로 Facade를 사용한다.

**API:** `POST /api/v1/users/sign-up` — 인증 불필요

```mermaid
sequenceDiagram
    autonumber
    actor User as 사용자
    participant C as UserController
    participant F as UserFacade
    participant US as UserService
    participant UPS as UserPointService
    participant R as Repository
    User ->> C: 회원가입 요청 (loginId, password, name 등)
    C ->> F: 회원가입 프로세스 시작

    rect rgb(245, 245, 245)
        Note right of F: @Transactional (Facade)
        F ->> US: 사용자 생성 요청
        US ->> US: loginId 중복 확인
        US ->> R: User 저장
        R -->> US: 생성된 User (ID 채번)
        US -->> F: userId 반환
        F ->> UPS: 초기 포인트 생성 요청 (userId)
        UPS ->> UPS: UserPoint 생성 (balance: 0)
        UPS ->> R: UserPoint 저장
    end

    F -->> C: 회원가입 완료
    C -->> User: 200 OK
```

#### 참고

- UserFacade는 User와 Point 경계를 넘는 조합이므로 Facade를 사용한다
- **@Transactional은 Facade 레벨에서 설정**하여, User 생성과 UserPoint 생성의 원자성을 보장한다 (UserPoint 생성 실패 시 User 생성도 롤백)
- 기존 회원가입(Round 1~2)에서는 UserService만 호출했으나, Round 3에서 UserPoint 초기화가 추가되어 UserFacade를 도입
