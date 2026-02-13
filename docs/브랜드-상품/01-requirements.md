# 브랜드 & 상품 요구사항

## 개요

유저가 브랜드 정보와 상품 목록을 탐색할 수 있는 대고객 조회 API입니다.
인증 없이 누구나 브랜드 상세 정보를 확인하고, 상품을 검색/필터링/정렬하여 조회할 수 있습니다.
브랜드와 상품의 등록/수정은 어드민 API(`/api-admin/v1`)에서 별도로 관리됩니다.

## 관련 도메인

| 도메인 | 관계 | 설명 |
|--------|------|------|
| Brand (브랜드) | 핵심 도메인 | 상품이 소속된 브랜드 정보를 관리합니다. |
| Product (상품) | 핵심 도메인 | 유저가 탐색하고 주문할 수 있는 상품 정보를 관리합니다. |
| Member (유저) | 간접 연관 | 상품 조회 자체는 인증이 불필요하나, 이후 좋아요/주문 기능에서 유저와 연결됩니다. |
| Like (좋아요) | 간접 연관 | 상품의 `likesCount` 필드는 좋아요 도메인에 의해 증감됩니다. 이 요구사항에서는 조회만 합니다. |
| Order (주문) | 간접 연관 | 유저가 상품을 탐색한 뒤 주문으로 이어지는 흐름의 전 단계입니다. |

---

## Part 1: API 명세

### 1.1 브랜드 정보 조회

#### Endpoint
- **METHOD**: `GET`
- **URI**: `/api/v1/brands/{brandId}`
- **인증 필요**: 불필요 (비로그인 상태에서 호출 가능)
- **설명**: 유저가 특정 브랜드의 상세 정보를 조회합니다.

#### Path Variable

| 변수 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `brandId` | Long | O | 조회할 브랜드의 ID |

#### Response (성공)

```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "brandId": 1,
    "name": "나이키",
    "description": "Just Do It",
    "logoImageUrl": "https://example.com/nike-logo.png"
  }
}
```

#### Response (실패)

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 존재하지 않는 브랜드 ID | 404 | Not Found | 존재하지 않는 브랜드입니다. |
| 삭제된 브랜드 | 404 | Not Found | 존재하지 않는 브랜드입니다. |

---

### 1.2 상품 목록 조회

#### Endpoint
- **METHOD**: `GET`
- **URI**: `/api/v1/products`
- **인증 필요**: 불필요 (비로그인 상태에서 호출 가능)
- **설명**: 유저가 상품 목록을 필터링, 정렬, 페이징하여 조회합니다.

#### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `brandId` | Long | X | - | 특정 브랜드의 상품만 필터링합니다. |
| `sort` | String | X | `latest` | 정렬 기준 (`latest`, `price_asc`, `likes_desc`) |
| `page` | Int | X | `0` | 페이지 번호 (0부터 시작) |
| `size` | Int | X | `20` | 페이지당 상품 수 |

#### 정렬 기준

| 값 | 설명 | 정렬 대상 |
|----|------|-----------|
| `latest` | 최신 등록순 (필수 구현) | `createdAt DESC` |
| `price_asc` | 가격 낮은 순 | `price ASC` |
| `likes_desc` | 좋아요 많은 순 | `likesCount DESC` |

#### 요청 예시

```http
GET /api/v1/products?brandId=1&sort=latest&page=0&size=20
```

```http
GET /api/v1/products?sort=price_asc&page=1&size=10
```

#### Response (성공)

Spring Data의 `Page` 기반 페이지네이션을 사용합니다.

```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "content": [
      {
        "productId": 1,
        "name": "에어맥스 90",
        "price": 159000,
        "brandId": 1,
        "brandName": "나이키",
        "description": "클래식 러닝화",
        "thumbnailImageUrl": "https://example.com/airmax90.png",
        "likesCount": 42
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

#### Response (실패)

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 지원하지 않는 정렬 기준 | 400 | Bad Request | 지원하지 않는 정렬 기준입니다: {sort} |
| page가 음수 | 400 | Bad Request | 페이지 번호는 0 이상이어야 합니다. |
| size가 1 미만 또는 100 초과 | 400 | Bad Request | 페이지 크기는 1~100 사이여야 합니다. |

---

### 1.3 상품 정보 조회

#### Endpoint
- **METHOD**: `GET`
- **URI**: `/api/v1/products/{productId}`
- **인증 필요**: 불필요 (비로그인 상태에서 호출 가능)
- **설명**: 유저가 특정 상품의 상세 정보를 조회합니다.

#### Path Variable

| 변수 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `productId` | Long | O | 조회할 상품의 ID |

#### Response (성공)

```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "productId": 1,
    "name": "에어맥스 90",
    "price": 159000,
    "brandId": 1,
    "brandName": "나이키",
    "description": "클래식 러닝화",
    "thumbnailImageUrl": "https://example.com/airmax90.png",
    "likesCount": 42
  }
}
```

#### Response (실패)

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 존재하지 않는 상품 ID | 404 | Not Found | 존재하지 않는 상품입니다. |
| 삭제된 상품 | 404 | Not Found | 존재하지 않는 상품입니다. |

---

## Part 2: 비즈니스 규칙

### 2.1 브랜드 도메인 모델 (BrandModel)

| 필드 | 타입 | 제약 조건 | 설명 |
|------|------|-----------|------|
| `id` | Long | PK, Auto Increment | 브랜드 고유 식별자 (BaseEntity 상속) |
| `name` | String | NOT NULL, 최대 100자 | 브랜드명 |
| `description` | String | NULL 허용, 최대 500자 | 브랜드 설명 |
| `logoImageUrl` | String | NULL 허용, 최대 500자 | 브랜드 로고 이미지 URL |
| `createdAt` | ZonedDateTime | NOT NULL | 생성 시점 (BaseEntity 상속) |
| `updatedAt` | ZonedDateTime | NOT NULL | 수정 시점 (BaseEntity 상속) |
| `deletedAt` | ZonedDateTime | NULL 허용 | 삭제 시점 (BaseEntity 상속) |

#### 브랜드 검증 규칙
- 브랜드명(`name`)은 비어있을 수 없습니다.
- 브랜드명(`name`)은 100자를 초과할 수 없습니다.

### 2.2 상품 도메인 모델 (ProductModel)

| 필드 | 타입 | 제약 조건 | 설명 |
|------|------|-----------|------|
| `id` | Long | PK, Auto Increment | 상품 고유 식별자 (BaseEntity 상속) |
| `brandId` | Long | NOT NULL, FK → Brand | 소속 브랜드 ID |
| `name` | String | NOT NULL, 최대 200자 | 상품명 |
| `price` | Long | NOT NULL, 0 이상 | 상품 가격 (원 단위) |
| `description` | String | NULL 허용, 최대 1000자 | 상품 설명 |
| `thumbnailImageUrl` | String | NULL 허용, 최대 500자 | 상품 대표 이미지 URL |
| `likesCount` | Long | NOT NULL, 기본값 0, 0 이상 | 좋아요 수 |
| `createdAt` | ZonedDateTime | NOT NULL | 생성 시점 (BaseEntity 상속) |
| `updatedAt` | ZonedDateTime | NOT NULL | 수정 시점 (BaseEntity 상속) |
| `deletedAt` | ZonedDateTime | NULL 허용 | 삭제 시점 (BaseEntity 상속) |

#### 상품 검증 규칙
- 상품명(`name`)은 비어있을 수 없습니다.
- 상품명(`name`)은 200자를 초과할 수 없습니다.
- 가격(`price`)은 0 이상이어야 합니다.
- 좋아요 수(`likesCount`)는 0 이상이어야 합니다.
- 브랜드 ID(`brandId`)는 필수입니다.

### 2.3 조회 규칙

- 조회 시 `deletedAt`이 `null`인 항목만 반환합니다 (soft delete 적용).
- 상품 목록 조회 시 `brandId` 필터를 사용하면 해당 브랜드의 상품만 반환합니다.
- 삭제된 브랜드에 소속된 상품은 상품 목록에서 제외합니다.
- 상품 목록의 기본 정렬은 최신 등록순(`latest`)입니다.

### 2.4 Brand-Product 관계

- 하나의 브랜드는 여러 상품을 가질 수 있습니다 (1:N 관계).
- 상품은 반드시 하나의 브랜드에 소속되어야 합니다.
- 상품 조회 시 소속 브랜드의 이름(`brandName`)을 함께 반환합니다.

---

## Part 3: 구현 컴포넌트

### 3.1 레이어별 구조

```
interfaces/api/brand/
  ├── BrandV1ApiSpec.kt          - 브랜드 API 인터페이스 (Swagger 문서화)
  ├── BrandV1Controller.kt       - 브랜드 API 컨트롤러
  └── BrandV1Dto.kt              - 브랜드 요청/응답 DTO

interfaces/api/product/
  ├── ProductV1ApiSpec.kt         - 상품 API 인터페이스 (Swagger 문서화)
  ├── ProductV1Controller.kt      - 상품 API 컨트롤러
  └── ProductV1Dto.kt             - 상품 요청/응답 DTO

application/brand/
  ├── BrandFacade.kt              - 브랜드 Facade (서비스 오케스트레이션)
  └── BrandInfo.kt                - 브랜드 Info DTO (Facade ↔ Controller 간 데이터 전달)

application/product/
  ├── ProductFacade.kt            - 상품 Facade (서비스 오케스트레이션)
  └── ProductInfo.kt              - 상품 Info DTO (Facade ↔ Controller 간 데이터 전달)

domain/brand/
  ├── BrandModel.kt               - 브랜드 JPA 엔티티 (검증 로직 포함)
  ├── BrandRepository.kt          - 브랜드 Repository 인터페이스
  └── BrandService.kt             - 브랜드 도메인 서비스

domain/product/
  ├── ProductModel.kt             - 상품 JPA 엔티티 (검증 로직 포함)
  ├── ProductRepository.kt        - 상품 Repository 인터페이스
  └── ProductService.kt           - 상품 도메인 서비스

infrastructure/brand/
  ├── BrandJpaRepository.kt       - 브랜드 Spring Data JPA Repository
  └── BrandRepositoryImpl.kt      - 브랜드 Repository 구현체

infrastructure/product/
  ├── ProductJpaRepository.kt     - 상품 Spring Data JPA Repository
  └── ProductRepositoryImpl.kt    - 상품 Repository 구현체
```

### 3.2 처리 흐름

#### 브랜드 정보 조회
```
Client → BrandV1Controller.getBrand(brandId)
       → BrandFacade.getBrandInfo(brandId)
       → BrandService.findById(brandId)
       → BrandRepository.findByIdAndDeletedAtIsNull(brandId)
       → BrandInfo 변환 → BrandV1Dto.BrandResponse 변환 → ApiResponse 반환
```

#### 상품 목록 조회
```
Client → ProductV1Controller.getProducts(brandId?, sort, page, size)
       → ProductFacade.getProductList(brandId?, sort, page, size)
       → ProductService.findAll(brandId?, sort, pageable)
       → ProductRepository.findAllByCondition(brandId?, sort, pageable)
       → Page<ProductInfo> 변환 → ProductV1Dto.ProductListResponse 변환 → ApiResponse 반환
```

#### 상품 정보 조회
```
Client → ProductV1Controller.getProduct(productId)
       → ProductFacade.getProductInfo(productId)
       → ProductService.findById(productId)
       → ProductRepository.findByIdAndDeletedAtIsNull(productId)
       → ProductInfo 변환 → ProductV1Dto.ProductResponse 변환 → ApiResponse 반환
```

---

## Part 4: 구현 체크리스트

### Phase 1: 도메인 모델 및 Repository

브랜드와 상품의 JPA 엔티티, Repository 인터페이스, Repository 구현체를 생성합니다.

- [ ] **RED**: `BrandModel` 엔티티 검증 테스트 작성 (이름 빈값/길이 초과 시 예외 발생)
- [ ] **RED**: `ProductModel` 엔티티 검증 테스트 작성 (이름 빈값, 가격 음수, likesCount 음수 시 예외 발생)
- [ ] **GREEN**: `BrandModel` JPA 엔티티 구현 (BaseEntity 상속, 필드 정의, 검증 로직)
- [ ] **GREEN**: `ProductModel` JPA 엔티티 구현 (BaseEntity 상속, 필드 정의, Brand와 ManyToOne 관계, 검증 로직)
- [ ] **GREEN**: `BrandRepository` 인터페이스 정의 및 `BrandRepositoryImpl` 구현
- [ ] **GREEN**: `ProductRepository` 인터페이스 정의 및 `ProductRepositoryImpl` 구현
- [ ] **GREEN**: `BrandJpaRepository`, `ProductJpaRepository` Spring Data JPA 인터페이스 생성
- [ ] **REFACTOR**: 모델 검증 로직 정리, 불필요한 코드 제거

### Phase 2: 브랜드 정보 조회 API

브랜드 단건 조회 API를 완성합니다 (Service → Facade → Controller).

- [ ] **RED**: `BrandService.findById()` 테스트 작성 (정상 조회, 존재하지 않는 ID, 삭제된 브랜드)
- [ ] **RED**: `BrandFacade.getBrandInfo()` 테스트 작성
- [ ] **RED**: `BrandV1Controller` 통합 테스트 작성 (GET /api/v1/brands/{brandId})
- [ ] **GREEN**: `BrandService` 구현 (findById with deletedAt null 필터)
- [ ] **GREEN**: `BrandInfo` DTO 생성 및 `BrandFacade` 구현
- [ ] **GREEN**: `BrandV1Dto`, `BrandV1ApiSpec`, `BrandV1Controller` 구현
- [ ] **REFACTOR**: 코드 정리, 응답 형식 검증

### Phase 3: 상품 단건 조회 API

상품 상세 조회 API를 완성합니다 (Service → Facade → Controller).

- [ ] **RED**: `ProductService.findById()` 테스트 작성 (정상 조회, 존재하지 않는 ID, 삭제된 상품)
- [ ] **RED**: `ProductFacade.getProductInfo()` 테스트 작성 (brandName 포함 검증)
- [ ] **RED**: `ProductV1Controller` 통합 테스트 작성 (GET /api/v1/products/{productId})
- [ ] **GREEN**: `ProductService` 구현 (findById with deletedAt null 필터)
- [ ] **GREEN**: `ProductInfo` DTO 생성 및 `ProductFacade` 구현 (brandName 포함)
- [ ] **GREEN**: `ProductV1Dto`, `ProductV1ApiSpec`, `ProductV1Controller` 구현
- [ ] **REFACTOR**: 코드 정리, 응답 형식 검증

### Phase 4: 상품 목록 조회 API

상품 목록 조회 API를 완성합니다 (필터링, 정렬, 페이지네이션 포함).

- [ ] **RED**: `ProductService.findAll()` 테스트 작성 (기본 조회, brandId 필터, 각 정렬 기준, 페이징)
- [ ] **RED**: `ProductFacade.getProductList()` 테스트 작성 (Page 변환 검증)
- [ ] **RED**: `ProductV1Controller` 통합 테스트 작성 (GET /api/v1/products, 쿼리 파라미터 조합)
- [ ] **GREEN**: `ProductRepository`에 조건부 조회 메서드 구현 (brandId 필터, 정렬, 페이징)
- [ ] **GREEN**: `ProductService.findAll()` 구현
- [ ] **GREEN**: `ProductFacade.getProductList()` 구현 (Page<ProductInfo> 반환)
- [ ] **GREEN**: `ProductV1Controller`에 상품 목록 엔드포인트 추가
- [ ] **REFACTOR**: 정렬 기준 검증 로직 정리, 불필요한 코드 제거

### Phase 5: .http 테스트 파일 및 마무리

API 테스트 파일을 생성하고, 전체 빌드 및 테스트를 검증합니다.

- [ ] `.http/brand/get-brand.http` 파일 생성
- [ ] `.http/product/get-products.http` 파일 생성
- [ ] `.http/product/get-product.http` 파일 생성
- [ ] 전체 빌드 검증 (`./gradlew :apps:commerce-api:build`)
- [ ] 전체 테스트 통과 확인 (`./gradlew :apps:commerce-api:test`)
- [ ] ktlint 검사 통과 확인 (`./gradlew ktlintCheck`)

---

## Part 5: 테스트 시나리오

### 5.1 단위 테스트

#### BrandModel 테스트
| 시나리오 | 입력 | 기대 결과 |
|----------|------|-----------|
| 유효한 브랜드 생성 | name="나이키", description="Just Do It", logoImageUrl="https://..." | 정상 생성 |
| 이름이 빈 문자열인 경우 | name="" | `CoreException(BAD_REQUEST)` |
| 이름이 100자를 초과하는 경우 | name="a".repeat(101) | `CoreException(BAD_REQUEST)` |

#### ProductModel 테스트
| 시나리오 | 입력 | 기대 결과 |
|----------|------|-----------|
| 유효한 상품 생성 | name="에어맥스", price=159000, brand=validBrand | 정상 생성 |
| 이름이 빈 문자열인 경우 | name="" | `CoreException(BAD_REQUEST)` |
| 이름이 200자를 초과하는 경우 | name="a".repeat(201) | `CoreException(BAD_REQUEST)` |
| 가격이 음수인 경우 | price=-1 | `CoreException(BAD_REQUEST)` |
| 좋아요 수가 음수인 경우 | likesCount=-1 | `CoreException(BAD_REQUEST)` |

#### BrandService 테스트
| 시나리오 | 기대 결과 |
|----------|-----------|
| 존재하는 브랜드 ID로 조회 | BrandModel 반환 |
| 존재하지 않는 브랜드 ID로 조회 | `CoreException(NOT_FOUND)` |

#### ProductService 테스트
| 시나리오 | 기대 결과 |
|----------|-----------|
| 존재하는 상품 ID로 조회 | ProductModel 반환 |
| 존재하지 않는 상품 ID로 조회 | `CoreException(NOT_FOUND)` |
| 전체 상품 목록 조회 (기본 정렬) | Page<ProductModel> 반환 (latest 정렬) |
| brandId 필터 적용 조회 | 해당 브랜드 상품만 포함된 Page 반환 |
| price_asc 정렬 조회 | 가격 낮은 순 정렬된 Page 반환 |
| likes_desc 정렬 조회 | 좋아요 많은 순 정렬된 Page 반환 |

### 5.2 통합 테스트

| 시나리오 | HTTP | 기대 결과 |
|----------|------|-----------|
| 브랜드 정상 조회 | GET /api/v1/brands/1 | 200, 브랜드 정보 반환 |
| 존재하지 않는 브랜드 조회 | GET /api/v1/brands/999 | 404, NOT_FOUND 에러 |
| 상품 목록 기본 조회 | GET /api/v1/products | 200, 페이징된 상품 목록 반환 |
| 상품 목록 브랜드 필터 | GET /api/v1/products?brandId=1 | 200, 해당 브랜드 상품만 반환 |
| 상품 목록 가격순 정렬 | GET /api/v1/products?sort=price_asc | 200, 가격 낮은 순 반환 |
| 상품 목록 좋아요순 정렬 | GET /api/v1/products?sort=likes_desc | 200, 좋아요 많은 순 반환 |
| 상품 목록 잘못된 정렬 기준 | GET /api/v1/products?sort=invalid | 400, BAD_REQUEST 에러 |
| 상품 목록 페이징 | GET /api/v1/products?page=1&size=5 | 200, 2페이지 데이터 반환 |
| 상품 정상 조회 | GET /api/v1/products/1 | 200, 상품 정보 + brandName 반환 |
| 존재하지 않는 상품 조회 | GET /api/v1/products/999 | 404, NOT_FOUND 에러 |

### 5.3 E2E 테스트

| 시나리오 | 설명 |
|----------|------|
| 상품 탐색 플로우 | 브랜드 조회 → 해당 브랜드의 상품 목록 조회 → 상품 상세 조회까지의 전체 흐름을 검증합니다. |
| 상품 필터링 플로우 | 다양한 정렬 기준과 필터 조합으로 상품 목록을 조회하여 올바른 결과가 반환되는지 검증합니다. |
| 페이징 플로우 | 여러 페이지를 순차적으로 조회하여 데이터 일관성과 페이징 메타 정보를 검증합니다. |

---

## Part 6: 보안 고려사항

| 항목 | 대응 방안 |
|------|-----------|
| SQL Injection | Spring Data JPA의 파라미터 바인딩을 사용하여 쿼리 파라미터를 안전하게 처리합니다. |
| 대량 요청 방지 | 페이지 크기(`size`)를 최대 100으로 제한하여 과도한 데이터 요청을 방지합니다. |
| 정렬 기준 검증 | 허용된 정렬 값(`latest`, `price_asc`, `likes_desc`)만 수용하고, 그 외의 값은 BAD_REQUEST로 거부합니다. |
| 삭제 데이터 노출 방지 | `deletedAt`이 null인 데이터만 조회하여 삭제된 데이터가 노출되지 않도록 합니다. |
| Path Variable 타입 검증 | `brandId`, `productId`가 Long 타입이 아닌 경우 Spring의 타입 변환 예외로 자동 처리됩니다. |

---

## Part 7: 검증 명령어

```bash
# 전체 빌드
./gradlew :apps:commerce-api:build

# 테스트 실행
./gradlew :apps:commerce-api:test

# 특정 테스트 클래스 실행
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.brand.*"
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.product.*"

# 테스트 커버리지 리포트
./gradlew :apps:commerce-api:test jacocoTestReport

# Lint 검사
./gradlew ktlintCheck

# Lint 자동 수정
./gradlew ktlintFormat
```

---

## 품질 체크리스트

- [x] 상품/브랜드/좋아요/주문 등 관련 도메인과의 관계가 명시되어 있는가?
- [x] 기능 요구사항이 유저 중심("유저가 ~한다")으로 서술되어 있는가?
- [x] 인증 방식(인증 불필요)이 정확히 명시되어 있는가?
- [x] 에러 케이스와 예외 상황이 포함되어 있는가?
- [x] Phase별 구현 체크리스트가 TDD 워크플로우에 맞게 구성되어 있는가?
