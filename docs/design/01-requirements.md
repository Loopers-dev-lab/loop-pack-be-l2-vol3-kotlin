# Round2 전체 요구사항 통합 문서

> 이 문서는 Round2에서 구현할 6개 도메인(브랜드-상품, 브랜드-상품-Admin, 유저, 좋아요, 주문, 주문-Admin)의 요구사항을 하나로 통합한 문서입니다.

## 목차
1. [브랜드 & 상품](#브랜드--상품-요구사항)
2. [브랜드 & 상품 Admin](#브랜드--상품-admin-요구사항)
3. [유저](#유저-요구사항)
4. [좋아요](#좋아요-요구사항)
5. [주문](#주문-요구사항)
6. [주문 Admin](#주문-admin-요구사항)

---

<!-- ========== 다음 도메인 ========== -->

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


---

<\!-- ========== 다음 도메인 ========== -->

# 브랜드 & 상품 Admin 요구사항

## 개요
어드민이 브랜드와 상품을 등록, 조회, 수정, 삭제할 수 있는 관리 API를 제공합니다.
모든 어드민 API는 `X-Loopers-Ldap: loopers.admin` 헤더를 통해 어드민 권한을 검증하며,
`/api-admin/v1` prefix를 사용하여 대고객 API와 명확히 구분합니다.

## 관련 도메인

| 도메인 | 관계 | 설명 |
|--------|------|------|
| 브랜드(Brand) | 핵심 도메인 | 어드민이 직접 등록/관리하는 대상 |
| 상품(Product) | 핵심 도메인 | 브랜드에 소속된 상품, 어드민이 직접 등록/관리 |
| 유저(User) | 간접 참조 | 대고객 API에서 상품을 조회하는 주체 |
| 좋아요(Like) | 간접 참조 | 유저가 상품에 좋아요를 누르면 좋아요 수가 증가 (상품 삭제 시 영향) |
| 주문(Order) | 간접 참조 | 유저가 상품을 주문할 때 상품 정보를 참조 (상품 삭제 시 스냅샷 필요) |

**브랜드-상품 관계**: 하나의 브랜드에 여러 상품이 소속됩니다 (1:N). 브랜드가 삭제되면 소속 상품도 함께 삭제됩니다.

---

## Part 1: API 명세

### 1.1 인증 방식

모든 어드민 API는 `X-Loopers-Ldap` 헤더를 통해 어드민 권한을 검증합니다.

| 헤더 이름 | 값 | 설명 |
|-----------|-----|------|
| `X-Loopers-Ldap` | `loopers.admin` | LDAP 기반 어드민 식별 값 |

헤더가 없거나 값이 `loopers.admin`이 아닌 경우 `401 UNAUTHORIZED`를 반환합니다.

### 1.2 엔드포인트 목록

#### 브랜드 관리

| METHOD | URI | 설명 |
|--------|-----|------|
| POST | `/api-admin/v1/brands` | 브랜드 등록 |
| GET | `/api-admin/v1/brands?page=0&size=20` | 브랜드 목록 조회 |
| GET | `/api-admin/v1/brands/{brandId}` | 브랜드 상세 조회 |
| PUT | `/api-admin/v1/brands/{brandId}` | 브랜드 정보 수정 |
| DELETE | `/api-admin/v1/brands/{brandId}` | 브랜드 삭제 |

#### 상품 관리

| METHOD | URI | 설명 |
|--------|-----|------|
| POST | `/api-admin/v1/products` | 상품 등록 |
| GET | `/api-admin/v1/products?page=0&size=20&brandId={brandId}` | 상품 목록 조회 |
| GET | `/api-admin/v1/products/{productId}` | 상품 상세 조회 |
| PUT | `/api-admin/v1/products/{productId}` | 상품 정보 수정 |
| DELETE | `/api-admin/v1/products/{productId}` | 상품 삭제 |

---

### 1.3 브랜드 API 상세

#### 1.3.1 브랜드 등록

어드민이 새로운 브랜드를 등록합니다. 등록 시 브랜드 상태는 `ACTIVE`로 설정됩니다.

**Endpoint**
```
POST /api-admin/v1/brands
X-Loopers-Ldap: loopers.admin
```

**Request Body**
```json
{
  "name": "나이키",
  "description": "글로벌 스포츠 브랜드",
  "logoUrl": "https://example.com/nike-logo.png"
}
```

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| name | String | O | 1~100자, 중복 불가 | 브랜드명 |
| description | String | X | 최대 500자 | 브랜드 설명 |
| logoUrl | String | X | 최대 500자, URL 형식 | 브랜드 로고 URL |

**Response (성공 - 201 Created)**
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
    "description": "글로벌 스포츠 브랜드",
    "logoUrl": "https://example.com/nike-logo.png",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00+09:00",
    "updatedAt": "2024-01-01T00:00:00+09:00"
  }
}
```

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| LDAP 헤더 없음/불일치 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 브랜드명 누락 | 400 | Bad Request | 브랜드명은 필수입니다. |
| 브랜드명 100자 초과 | 400 | Bad Request | 브랜드명은 100자 이하여야 합니다. |
| 브랜드명 중복 | 409 | Conflict | 이미 존재하는 브랜드명입니다. |
| 브랜드 설명 500자 초과 | 400 | Bad Request | 브랜드 설명은 500자 이하여야 합니다. |
| 로고 URL 형식 오류 | 400 | Bad Request | 유효하지 않은 URL 형식입니다. |

#### 1.3.2 브랜드 목록 조회

어드민이 등록된 브랜드 목록을 페이징하여 조회합니다. 각 브랜드의 소속 상품 수를 함께 표시합니다.

**Endpoint**
```
GET /api-admin/v1/brands?page=0&size=20
X-Loopers-Ldap: loopers.admin
```

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| page | Int | X | 0 | 페이지 번호 (0부터 시작) |
| size | Int | X | 20 | 페이지당 항목 수 |

**Response (성공 - 200 OK)**
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
        "brandId": 1,
        "name": "나이키",
        "description": "글로벌 스포츠 브랜드",
        "logoUrl": "https://example.com/nike-logo.png",
        "status": "ACTIVE",
        "productCount": 15,
        "createdAt": "2024-01-01T00:00:00+09:00",
        "updatedAt": "2024-01-01T00:00:00+09:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3
  }
}
```

#### 1.3.3 브랜드 상세 조회

어드민이 특정 브랜드의 상세 정보를 조회합니다. 해당 브랜드의 소속 상품 수를 함께 표시합니다.

**Endpoint**
```
GET /api-admin/v1/brands/{brandId}
X-Loopers-Ldap: loopers.admin
```

**Response (성공 - 200 OK)**
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
    "description": "글로벌 스포츠 브랜드",
    "logoUrl": "https://example.com/nike-logo.png",
    "status": "ACTIVE",
    "productCount": 15,
    "createdAt": "2024-01-01T00:00:00+09:00",
    "updatedAt": "2024-01-01T00:00:00+09:00"
  }
}
```

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 존재하지 않는 브랜드 | 404 | Not Found | 존재하지 않는 브랜드입니다. |

#### 1.3.4 브랜드 정보 수정

어드민이 기존 브랜드의 정보를 수정합니다. 브랜드 상태를 `ACTIVE`/`INACTIVE`로 변경할 수 있습니다.

**Endpoint**
```
PUT /api-admin/v1/brands/{brandId}
X-Loopers-Ldap: loopers.admin
```

**Request Body**
```json
{
  "name": "나이키 코리아",
  "description": "글로벌 스포츠 브랜드 (한국 공식)",
  "logoUrl": "https://example.com/nike-korea-logo.png",
  "status": "ACTIVE"
}
```

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| name | String | O | 1~100자, 중복 불가 (본인 제외) | 브랜드명 |
| description | String | X | 최대 500자 | 브랜드 설명 |
| logoUrl | String | X | 최대 500자, URL 형식 | 브랜드 로고 URL |
| status | String | O | ACTIVE 또는 INACTIVE | 브랜드 상태 |

**Response (성공 - 200 OK)**
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "brandId": 1,
    "name": "나이키 코리아",
    "description": "글로벌 스포츠 브랜드 (한국 공식)",
    "logoUrl": "https://example.com/nike-korea-logo.png",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00+09:00",
    "updatedAt": "2024-01-15T10:30:00+09:00"
  }
}
```

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 존재하지 않는 브랜드 | 404 | Not Found | 존재하지 않는 브랜드입니다. |
| 브랜드명 중복 (다른 브랜드) | 409 | Conflict | 이미 존재하는 브랜드명입니다. |
| 유효하지 않은 상태 값 | 400 | Bad Request | 유효하지 않은 브랜드 상태입니다. |

#### 1.3.5 브랜드 삭제

어드민이 브랜드를 삭제합니다. 브랜드 삭제 시 해당 브랜드에 소속된 모든 상품도 함께 소프트 삭제됩니다.

**Endpoint**
```
DELETE /api-admin/v1/brands/{brandId}
X-Loopers-Ldap: loopers.admin
```

**Response (성공 - 200 OK)**
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": null
}
```

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 존재하지 않는 브랜드 | 404 | Not Found | 존재하지 않는 브랜드입니다. |

---

### 1.4 상품 API 상세

#### 1.4.1 상품 등록

어드민이 특정 브랜드에 소속된 새로운 상품을 등록합니다. 지정한 브랜드가 실제로 존재하고 활성 상태여야 합니다.

**Endpoint**
```
POST /api-admin/v1/products
X-Loopers-Ldap: loopers.admin
```

**Request Body**
```json
{
  "name": "에어맥스 90",
  "description": "클래식 러닝화",
  "price": 179000,
  "brandId": 1,
  "saleStatus": "SELLING",
  "stockQuantity": 100,
  "displayStatus": "VISIBLE"
}
```

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| name | String | O | 1~200자 | 상품명 |
| description | String | X | 최대 1000자 | 상품 설명 |
| price | Long | O | 0 이상, 최대 100,000,000 | 상품 가격 (원 단위) |
| brandId | Long | O | 존재하는 활성 브랜드 | 소속 브랜드 ID |
| saleStatus | String | O | SELLING 또는 STOP_SELLING | 판매 상태 |
| stockQuantity | Int | O | 0 이상, 최대 999,999 | 재고 수량 |
| displayStatus | String | O | VISIBLE 또는 HIDDEN | 노출 상태 |

**Response (성공 - 201 Created)**
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
    "description": "클래식 러닝화",
    "price": 179000,
    "brand": {
      "brandId": 1,
      "name": "나이키"
    },
    "saleStatus": "SELLING",
    "stockQuantity": 100,
    "displayStatus": "VISIBLE",
    "createdAt": "2024-01-01T00:00:00+09:00",
    "updatedAt": "2024-01-01T00:00:00+09:00"
  }
}
```

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 상품명 누락 | 400 | Bad Request | 상품명은 필수입니다. |
| 상품명 200자 초과 | 400 | Bad Request | 상품명은 200자 이하여야 합니다. |
| 가격 음수 | 400 | Bad Request | 가격은 0 이상이어야 합니다. |
| 가격 상한 초과 | 400 | Bad Request | 가격은 100,000,000원 이하여야 합니다. |
| 존재하지 않는 브랜드 | 404 | Not Found | 존재하지 않는 브랜드입니다. |
| 비활성 브랜드 | 400 | Bad Request | 비활성 브랜드에는 상품을 등록할 수 없습니다. |
| 재고 수량 음수 | 400 | Bad Request | 재고 수량은 0 이상이어야 합니다. |
| 유효하지 않은 판매 상태 | 400 | Bad Request | 유효하지 않은 판매 상태입니다. |
| 유효하지 않은 노출 상태 | 400 | Bad Request | 유효하지 않은 노출 상태입니다. |

#### 1.4.2 상품 목록 조회

어드민이 등록된 상품 목록을 페이징하여 조회합니다. `brandId` 파라미터로 특정 브랜드의 상품만 필터링할 수 있습니다.

**Endpoint**
```
GET /api-admin/v1/products?page=0&size=20&brandId=1
X-Loopers-Ldap: loopers.admin
```

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| page | Int | X | 0 | 페이지 번호 (0부터 시작) |
| size | Int | X | 20 | 페이지당 항목 수 |
| brandId | Long | X | - | 특정 브랜드의 상품만 필터링 |

**Response (성공 - 200 OK)**
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
        "description": "클래식 러닝화",
        "price": 179000,
        "brand": {
          "brandId": 1,
          "name": "나이키"
        },
        "saleStatus": "SELLING",
        "stockQuantity": 100,
        "displayStatus": "VISIBLE",
        "createdAt": "2024-01-01T00:00:00+09:00",
        "updatedAt": "2024-01-01T00:00:00+09:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

#### 1.4.3 상품 상세 조회

어드민이 특정 상품의 상세 정보를 조회합니다.

**Endpoint**
```
GET /api-admin/v1/products/{productId}
X-Loopers-Ldap: loopers.admin
```

**Response (성공 - 200 OK)**
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
    "description": "클래식 러닝화",
    "price": 179000,
    "brand": {
      "brandId": 1,
      "name": "나이키"
    },
    "saleStatus": "SELLING",
    "stockQuantity": 100,
    "displayStatus": "VISIBLE",
    "createdAt": "2024-01-01T00:00:00+09:00",
    "updatedAt": "2024-01-01T00:00:00+09:00"
  }
}
```

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 존재하지 않는 상품 | 404 | Not Found | 존재하지 않는 상품입니다. |

#### 1.4.4 상품 정보 수정

어드민이 기존 상품의 정보를 수정합니다. 상품의 소속 브랜드는 변경할 수 없습니다.

**Endpoint**
```
PUT /api-admin/v1/products/{productId}
X-Loopers-Ldap: loopers.admin
```

**Request Body**
```json
{
  "name": "에어맥스 90 리뉴얼",
  "description": "클래식 러닝화 (2024 리뉴얼 에디션)",
  "price": 189000,
  "saleStatus": "SELLING",
  "stockQuantity": 50,
  "displayStatus": "VISIBLE"
}
```

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| name | String | O | 1~200자 | 상품명 |
| description | String | X | 최대 1000자 | 상품 설명 |
| price | Long | O | 0 이상, 최대 100,000,000 | 상품 가격 (원 단위) |
| saleStatus | String | O | SELLING 또는 STOP_SELLING | 판매 상태 |
| stockQuantity | Int | O | 0 이상, 최대 999,999 | 재고 수량 |
| displayStatus | String | O | VISIBLE 또는 HIDDEN | 노출 상태 |

**주의**: `brandId`는 요청 본문에 포함하지 않습니다. 상품의 브랜드는 변경할 수 없습니다.

**Response (성공 - 200 OK)**
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "productId": 1,
    "name": "에어맥스 90 리뉴얼",
    "description": "클래식 러닝화 (2024 리뉴얼 에디션)",
    "price": 189000,
    "brand": {
      "brandId": 1,
      "name": "나이키"
    },
    "saleStatus": "SELLING",
    "stockQuantity": 50,
    "displayStatus": "VISIBLE",
    "createdAt": "2024-01-01T00:00:00+09:00",
    "updatedAt": "2024-01-15T14:00:00+09:00"
  }
}
```

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 존재하지 않는 상품 | 404 | Not Found | 존재하지 않는 상품입니다. |

#### 1.4.5 상품 삭제

어드민이 상품을 삭제합니다. 소프트 삭제 방식으로 처리됩니다.

**Endpoint**
```
DELETE /api-admin/v1/products/{productId}
X-Loopers-Ldap: loopers.admin
```

**Response (성공 - 200 OK)**
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": null
}
```

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 존재하지 않는 상품 | 404 | Not Found | 존재하지 않는 상품입니다. |

---

## Part 2: 비즈니스 규칙

### 2.1 필수 규칙

| # | 규칙 | 설명 |
|---|------|------|
| 1 | 어드민 인증 필수 | 모든 어드민 API는 `X-Loopers-Ldap: loopers.admin` 헤더가 필요합니다 |
| 2 | 브랜드 삭제 시 연쇄 소프트 삭제 | 브랜드를 삭제하면 해당 브랜드에 소속된 모든 상품도 함께 소프트 삭제됩니다 |
| 3 | 상품 등록 시 브랜드 존재 및 활성 검증 | 상품 등록 시 지정한 브랜드가 존재하고 `ACTIVE` 상태여야 합니다 |
| 4 | 상품 브랜드 변경 불가 | 상품 수정 시 소속 브랜드를 다른 브랜드로 변경할 수 없습니다 |
| 5 | 브랜드명 중복 불가 | 동일한 이름의 브랜드를 중복 등록할 수 없습니다 (대소문자 구분) |
| 6 | 소프트 삭제 적용 | 브랜드와 상품 삭제 시 `deletedAt` 필드를 설정하여 논리적으로 삭제합니다 |
| 7 | 삭제된 항목 조회 제외 | 목록/상세 조회 시 소프트 삭제된 항목은 조회 대상에서 제외됩니다 |

### 2.2 입력 검증 규칙

#### 브랜드

| # | 필드 | 규칙 | 검증 시점 |
|---|------|------|-----------|
| 1 | name | 필수, 1~100자, 공백만으로 구성 불가 | 등록/수정 |
| 2 | name | 중복 불가 (삭제되지 않은 브랜드 기준, 수정 시 자기 자신 제외) | 등록/수정 |
| 3 | description | 선택, 최대 500자 | 등록/수정 |
| 4 | logoUrl | 선택, 최대 500자, 입력 시 URL 형식 검증 | 등록/수정 |
| 5 | status | `ACTIVE` 또는 `INACTIVE`만 허용 | 수정 |

#### 상품

| # | 필드 | 규칙 | 검증 시점 |
|---|------|------|-----------|
| 1 | name | 필수, 1~200자, 공백만으로 구성 불가 | 등록/수정 |
| 2 | price | 필수, 0 이상 100,000,000 이하 (원 단위) | 등록/수정 |
| 3 | brandId | 필수, 존재하는 활성 브랜드 ID | 등록 |
| 4 | description | 선택, 최대 1000자 | 등록/수정 |
| 5 | saleStatus | 필수, `SELLING` 또는 `STOP_SELLING` | 등록/수정 |
| 6 | stockQuantity | 필수, 0 이상 999,999 이하 | 등록/수정 |
| 7 | displayStatus | 필수, `VISIBLE` 또는 `HIDDEN` | 등록/수정 |

### 2.3 삭제 및 데이터 보전 규칙

| # | 규칙 | 설명 |
|---|------|------|
| 1 | 소프트 삭제 | `BaseEntity.delete()` 메서드를 사용하여 `deletedAt`을 현재 시각으로 설정 |
| 2 | 브랜드 연쇄 삭제 | 브랜드 소프트 삭제 시, 해당 브랜드에 소속된 모든 상품도 동시에 소프트 삭제 |
| 3 | 스냅샷 고려 | 주문 도메인 구현 시 상품 정보 스냅샷을 주문 시점에 저장하여, 이후 상품이 삭제/수정되더라도 주문 이력에서 원본 정보를 보존 |

### 2.4 대고객 API와의 연동 규칙

| # | 규칙 | 설명 |
|---|------|------|
| 1 | 브랜드 비활성 시 상품 비노출 | `INACTIVE` 상태의 브랜드에 소속된 상품은 대고객 API에서 조회되지 않아야 합니다 |
| 2 | 상품 숨김 시 비노출 | `HIDDEN` 상태의 상품은 대고객 API에서 조회되지 않아야 합니다 |
| 3 | 판매 중지 상품 | `STOP_SELLING` 상태의 상품은 대고객 API에서 조회는 가능하지만 주문할 수 없습니다 |

---

## Part 3: 도메인 모델

### 3.1 Enum 정의

#### BrandStatus
```kotlin
enum class BrandStatus {
    ACTIVE,     // 활성: 정상 운영 중인 브랜드
    INACTIVE    // 비활성: 운영 중단된 브랜드 (소속 상품 대고객 비노출)
}
```

#### SaleStatus
```kotlin
enum class SaleStatus {
    SELLING,       // 판매 중: 주문 가능
    STOP_SELLING   // 판매 중지: 조회 가능하지만 주문 불가
}
```

#### DisplayStatus
```kotlin
enum class DisplayStatus {
    VISIBLE,   // 노출: 대고객 API에서 조회 가능
    HIDDEN     // 숨김: 대고객 API에서 조회 불가, 어드민에서만 관리
}
```

### 3.2 Brand 모델

| 필드 | 타입 | DB 컬럼 | 제약조건 | 설명 |
|------|------|---------|----------|------|
| id | Long | id | PK, auto-increment | 브랜드 ID (BaseEntity) |
| name | String | name | NOT NULL, UNIQUE, max 100 | 브랜드명 |
| description | String? | description | max 500 | 브랜드 설명 |
| logoUrl | String? | logo_url | max 500 | 브랜드 로고 URL |
| status | BrandStatus | status | NOT NULL, default ACTIVE | 브랜드 상태 |
| createdAt | ZonedDateTime | created_at | NOT NULL | 등록일시 (BaseEntity) |
| updatedAt | ZonedDateTime | updated_at | NOT NULL | 수정일시 (BaseEntity) |
| deletedAt | ZonedDateTime? | deleted_at | nullable | 삭제일시 (BaseEntity, 소프트 삭제) |

### 3.3 Product 모델

| 필드 | 타입 | DB 컬럼 | 제약조건 | 설명 |
|------|------|---------|----------|------|
| id | Long | id | PK, auto-increment | 상품 ID (BaseEntity) |
| name | String | name | NOT NULL, max 200 | 상품명 |
| description | String? | description | max 1000 | 상품 설명 |
| price | Long | price | NOT NULL, >= 0 | 상품 가격 (원 단위) |
| brandId | Long | brand_id | NOT NULL, FK | 소속 브랜드 ID |
| saleStatus | SaleStatus | sale_status | NOT NULL | 판매 상태 |
| stockQuantity | Int | stock_quantity | NOT NULL, >= 0 | 재고 수량 |
| displayStatus | DisplayStatus | display_status | NOT NULL | 노출 상태 |
| createdAt | ZonedDateTime | created_at | NOT NULL | 등록일시 (BaseEntity) |
| updatedAt | ZonedDateTime | updated_at | NOT NULL | 수정일시 (BaseEntity) |
| deletedAt | ZonedDateTime? | deleted_at | nullable | 삭제일시 (BaseEntity, 소프트 삭제) |

---

## Part 4: 구현 컴포넌트

### 4.1 레이어별 구조

```
support/
  └── filter/
      └── AdminLdapAuthenticationFilter.kt   # 어드민 LDAP 인증 필터

domain/brand/
  ├── BrandModel.kt          # 브랜드 도메인 모델 (엔티티)
  ├── BrandStatus.kt         # 브랜드 상태 enum
  ├── BrandService.kt        # 브랜드 도메인 서비스
  └── BrandRepository.kt     # 브랜드 레포지토리 인터페이스

domain/product/
  ├── ProductModel.kt        # 상품 도메인 모델 (엔티티)
  ├── SaleStatus.kt          # 판매 상태 enum
  ├── DisplayStatus.kt       # 노출 상태 enum
  ├── ProductService.kt      # 상품 도메인 서비스
  └── ProductRepository.kt   # 상품 레포지토리 인터페이스

infrastructure/brand/
  ├── BrandJpaRepository.kt      # Spring Data JPA 레포지토리
  └── BrandRepositoryImpl.kt     # 도메인 레포지토리 구현체

infrastructure/product/
  ├── ProductJpaRepository.kt    # Spring Data JPA 레포지토리
  └── ProductRepositoryImpl.kt   # 도메인 레포지토리 구현체

application/brand/
  ├── BrandFacade.kt         # 브랜드 애플리케이션 퍼사드
  └── BrandInfo.kt           # 브랜드 정보 전달 객체

application/product/
  ├── ProductFacade.kt       # 상품 애플리케이션 퍼사드
  └── ProductInfo.kt         # 상품 정보 전달 객체

interfaces/api/admin/brand/
  ├── AdminBrandV1Controller.kt  # 브랜드 어드민 컨트롤러
  ├── AdminBrandV1ApiSpec.kt     # OpenAPI 스펙 인터페이스
  └── AdminBrandV1Dto.kt        # Request/Response DTO

interfaces/api/admin/product/
  ├── AdminProductV1Controller.kt  # 상품 어드민 컨트롤러
  ├── AdminProductV1ApiSpec.kt     # OpenAPI 스펙 인터페이스
  └── AdminProductV1Dto.kt        # Request/Response DTO
```

### 4.2 처리 흐름

#### 브랜드 등록 흐름
```
AdminBrandV1Controller (POST /api-admin/v1/brands)
  → AdminLdapAuthenticationFilter에서 LDAP 헤더 검증
  → AdminBrandV1Dto.CreateBrandRequest 입력 검증 (init 블록)
  → BrandFacade.createBrand(name, description, logoUrl)
    → BrandService.create(name, description, logoUrl)
      → BrandRepository.existsByName(name) : 중복 검증
      → BrandModel(name, description, logoUrl) : 모델 생성 (init 블록에서 필드 검증)
      → BrandRepository.save(brand) : 저장
    → BrandInfo.from(brand) : Info 변환
  → AdminBrandV1Dto.BrandResponse.from(info) : Response 변환
  → ApiResponse.success(response) : 응답 반환
```

#### 브랜드 삭제 흐름 (연쇄 삭제 포함)
```
AdminBrandV1Controller (DELETE /api-admin/v1/brands/{brandId})
  → AdminLdapAuthenticationFilter에서 LDAP 헤더 검증
  → BrandFacade.deleteBrand(brandId)
    → BrandService.findById(brandId) : 브랜드 조회
    → ProductService.softDeleteAllByBrandId(brandId) : 소속 상품 일괄 소프트 삭제
    → BrandService.delete(brandId) : 브랜드 소프트 삭제
  → ApiResponse.success() : 응답 반환
```

#### 상품 등록 흐름
```
AdminProductV1Controller (POST /api-admin/v1/products)
  → AdminLdapAuthenticationFilter에서 LDAP 헤더 검증
  → AdminProductV1Dto.CreateProductRequest 입력 검증 (init 블록)
  → ProductFacade.createProduct(name, description, price, brandId, saleStatus, stockQuantity, displayStatus)
    → BrandService.findById(brandId) : 브랜드 존재 및 활성 상태 검증
    → ProductService.create(...) : 상품 생성
      → ProductModel(...) : 모델 생성 (init 블록에서 필드 검증)
      → ProductRepository.save(product) : 저장
    → ProductInfo.from(product, brand) : Info 변환
  → AdminProductV1Dto.ProductResponse.from(info) : Response 변환
  → ApiResponse.success(response) : 응답 반환
```

---

## Part 5: 구현 체크리스트

### Phase 1: 어드민 인증 필터 + Enum 정의

**목표**: 어드민 API의 LDAP 인증 인프라를 구축하고, 브랜드/상품에 사용할 Enum을 정의합니다.

- [ ] `AdminLdapAuthenticationFilter` 구현 (`/api-admin/v1/**` 경로에 적용)
- [ ] LDAP 헤더 누락/불일치 시 `UNAUTHORIZED` 에러 반환
- [ ] `BrandStatus` enum 정의 (ACTIVE, INACTIVE)
- [ ] `SaleStatus` enum 정의 (SELLING, STOP_SELLING)
- [ ] `DisplayStatus` enum 정의 (VISIBLE, HIDDEN)
- [ ] `AdminLdapAuthenticationFilter` 단위 테스트

### Phase 2: Brand 도메인 모델 + Repository

**목표**: 브랜드 도메인 모델, 레포지토리 인터페이스 및 구현체를 작성합니다.

- [ ] `BrandModel` 엔티티 생성 (BaseEntity 상속, init 블록에서 필드 검증)
- [ ] `BrandRepository` 인터페이스 정의 (save, findById, existsByName, findAll 등)
- [ ] `BrandJpaRepository` + `BrandRepositoryImpl` 구현
- [ ] `BrandModelTest` 단위 테스트 (필드 검증, 상태 변경, 소프트 삭제)

### Phase 3: Brand CRUD 서비스 + Facade + Controller

**목표**: 브랜드 CRUD API를 완성합니다.

- [ ] `BrandService` 구현 (create, findById, findAll, update, delete)
- [ ] `BrandInfo` 정보 전달 객체 생성
- [ ] `BrandFacade` 구현 (CRUD 오케스트레이션)
- [ ] `AdminBrandV1Dto` Request/Response DTO 생성
- [ ] `AdminBrandV1ApiSpec` OpenAPI 스펙 인터페이스
- [ ] `AdminBrandV1Controller` 구현
- [ ] `BrandServiceTest` 단위 테스트
- [ ] `BrandFacadeTest` 단위 테스트
- [ ] `AdminBrandV1Controller` 통합 테스트

### Phase 4: Product 도메인 모델 + Repository

**목표**: 상품 도메인 모델, 레포지토리 인터페이스 및 구현체를 작성합니다.

- [ ] `ProductModel` 엔티티 생성 (BaseEntity 상속, brandId FK, init 블록에서 필드 검증)
- [ ] `ProductRepository` 인터페이스 정의 (save, findById, findAllByBrandId, softDeleteAllByBrandId 등)
- [ ] `ProductJpaRepository` + `ProductRepositoryImpl` 구현
- [ ] `ProductModelTest` 단위 테스트 (필드 검증, 상태 변경, 소프트 삭제)

### Phase 5: Product CRUD 서비스 + Facade + Controller

**목표**: 상품 CRUD API를 완성합니다.

- [ ] `ProductService` 구현 (create, findById, findAll, update, delete, softDeleteAllByBrandId)
- [ ] `ProductInfo` 정보 전달 객체 생성
- [ ] `ProductFacade` 구현 (CRUD 오케스트레이션, 브랜드 존재/활성 검증 포함)
- [ ] `AdminProductV1Dto` Request/Response DTO 생성
- [ ] `AdminProductV1ApiSpec` OpenAPI 스펙 인터페이스
- [ ] `AdminProductV1Controller` 구현
- [ ] `ProductServiceTest` 단위 테스트
- [ ] `ProductFacadeTest` 단위 테스트
- [ ] `AdminProductV1Controller` 통합 테스트

### Phase 6: 연쇄 삭제 + E2E 테스트

**목표**: 브랜드 삭제 시 연쇄 삭제 로직을 검증하고, 전체 시나리오 E2E 테스트를 작성합니다.

- [ ] 브랜드 삭제 시 소속 상품 연쇄 소프트 삭제 통합 테스트
- [ ] 삭제된 브랜드/상품이 목록 조회에서 제외되는지 검증
- [ ] 브랜드/상품 CRUD 전체 시나리오 E2E 테스트

### Phase 7: HTTP 테스트 파일

**목표**: IntelliJ HTTP Client 형식의 API 테스트 파일을 작성합니다.

- [ ] `.http/admin/brand/create-brand.http` 생성
- [ ] `.http/admin/brand/get-brands.http` 생성
- [ ] `.http/admin/brand/get-brand.http` 생성
- [ ] `.http/admin/brand/update-brand.http` 생성
- [ ] `.http/admin/brand/delete-brand.http` 생성
- [ ] `.http/admin/product/create-product.http` 생성
- [ ] `.http/admin/product/get-products.http` 생성
- [ ] `.http/admin/product/get-product.http` 생성
- [ ] `.http/admin/product/update-product.http` 생성
- [ ] `.http/admin/product/delete-product.http` 생성

---

## Part 6: 테스트 시나리오

### 6.1 어드민 인증 필터 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 유효한 LDAP 헤더 | `X-Loopers-Ldap: loopers.admin` | 요청 통과 |
| LDAP 헤더 없음 | 헤더 누락 | 401 UNAUTHORIZED |
| 잘못된 LDAP 값 | `X-Loopers-Ldap: invalid` | 401 UNAUTHORIZED |
| 대고객 API 경로 | `/api/v1/products` (LDAP 헤더 없음) | 필터 미적용, 요청 통과 |

### 6.2 브랜드 등록 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 정상 등록 (모든 필드) | name, description, logoUrl | 201 Created + 브랜드 정보 |
| 정상 등록 (필수 필드만) | name만 | 201 Created + description/logoUrl null |
| 브랜드명 누락 | name 없음 | 400 Bad Request |
| 브랜드명 빈 문자열 | name = "" | 400 Bad Request |
| 브랜드명 공백만 | name = "   " | 400 Bad Request |
| 브랜드명 100자 초과 | 101자 name | 400 Bad Request |
| 브랜드명 중복 | 이미 존재하는 name | 409 Conflict |
| 로고 URL 형식 오류 | logoUrl = "not-a-url" | 400 Bad Request |
| 설명 500자 초과 | 501자 description | 400 Bad Request |
| LDAP 인증 실패 | 헤더 없음 | 401 UNAUTHORIZED |

### 6.3 브랜드 목록 조회 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 기본 조회 | page=0, size=20 | 200 OK + 페이징 정보 |
| 빈 목록 | 브랜드 없음 | 200 OK + 빈 content 배열 |
| 페이징 동작 확인 | 21개 브랜드, size=20 | totalPages=2, 첫 페이지 20개 |
| 삭제된 브랜드 제외 | 소프트 삭제된 브랜드 존재 | 삭제된 브랜드 미포함 |
| 상품 수 정확성 | 상품이 등록된 브랜드 | productCount 정확히 반영 |

### 6.4 브랜드 수정 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 정상 수정 | 유효한 필드 값 | 200 OK + 수정된 정보 |
| 존재하지 않는 브랜드 | 없는 brandId | 404 Not Found |
| 다른 브랜드와 이름 중복 | 다른 브랜드의 name | 409 Conflict |
| 자기 자신과 이름 동일 | 기존 name 유지 | 200 OK (중복 아님) |
| 상태 INACTIVE 변경 | status = "INACTIVE" | 200 OK + 상태 변경됨 |

### 6.5 브랜드 삭제 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 정상 삭제 (상품 없음) | 유효한 brandId | 200 OK |
| 정상 삭제 (상품 있음) | 상품이 소속된 brandId | 200 OK + 소속 상품도 소프트 삭제 |
| 존재하지 않는 브랜드 | 없는 brandId | 404 Not Found |
| 삭제 후 목록 조회 | 삭제된 brandId | 목록에서 제외됨 |
| 삭제 후 상세 조회 | 삭제된 brandId | 404 Not Found |

### 6.6 상품 등록 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 정상 등록 | 모든 필수 필드 | 201 Created + 상품 정보 |
| 상품명 누락 | name 없음 | 400 Bad Request |
| 가격 음수 | price = -1 | 400 Bad Request |
| 가격 상한 초과 | price = 100,000,001 | 400 Bad Request |
| 존재하지 않는 브랜드 | 없는 brandId | 404 Not Found |
| 비활성 브랜드 | INACTIVE 브랜드의 brandId | 400 Bad Request |
| 재고 음수 | stockQuantity = -1 | 400 Bad Request |
| 유효하지 않은 판매 상태 | saleStatus = "INVALID" | 400 Bad Request |
| 유효하지 않은 노출 상태 | displayStatus = "INVALID" | 400 Bad Request |

### 6.7 상품 목록 조회 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 전체 조회 | page=0, size=20 | 200 OK + 전체 상품 페이징 |
| 브랜드별 필터링 | brandId=1 | 해당 브랜드 상품만 반환 |
| 삭제된 상품 제외 | 소프트 삭제된 상품 존재 | 삭제된 상품 미포함 |

### 6.8 상품 수정 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 정상 수정 | 유효한 필드 값 | 200 OK + 수정된 정보 |
| 존재하지 않는 상품 | 없는 productId | 404 Not Found |
| 브랜드 불변 확인 | 응답에 기존 브랜드 정보 유지 | brandId 변경되지 않음 |
| 판매 중지로 변경 | saleStatus = "STOP_SELLING" | 200 OK + 상태 변경됨 |
| 숨김으로 변경 | displayStatus = "HIDDEN" | 200 OK + 상태 변경됨 |

### 6.9 상품 삭제 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 정상 삭제 | 유효한 productId | 200 OK |
| 존재하지 않는 상품 | 없는 productId | 404 Not Found |
| 삭제 후 목록 조회 | 삭제된 productId | 목록에서 제외됨 |

### 6.10 연쇄 삭제 E2E 테스트

| 시나리오 | 조건 | 기대 결과 |
|---------|------|----------|
| 브랜드 삭제 시 상품 연쇄 삭제 | 브랜드에 상품 3개 소속 | 브랜드 + 상품 3개 모두 소프트 삭제 |
| 연쇄 삭제 후 상품 목록 조회 | 위 시나리오 이후 | 해당 상품 3개 모두 목록에서 제외 |

---

## Part 7: 보안 고려사항

### 7.1 인증/인가
- 모든 어드민 API에 `AdminLdapAuthenticationFilter`를 적용하여 LDAP 헤더를 검증합니다
- 대고객 API(`/api/v1/**`)와 어드민 API(`/api-admin/v1/**`)의 경로를 분리하여 권한 체계를 명확히 구분합니다
- 필터는 `/api-admin/v1/**` 경로에만 적용되며, 다른 경로의 요청에는 영향을 주지 않습니다

### 7.2 입력 검증
- 모든 입력 값은 서버 사이드에서 검증합니다 (DTO init 블록 + 도메인 모델 init 블록)
- SQL Injection 방지: JPA 파라미터 바인딩을 통해 안전하게 쿼리를 실행합니다
- XSS 방지: 입력 문자열의 HTML 태그를 이스케이프 처리합니다 (필요 시)

### 7.3 데이터 보전
- 소프트 삭제를 사용하여 데이터 복구 가능성을 유지합니다
- 주문/좋아요 등 다른 도메인에서 참조하는 상품 데이터의 정합성을 보전합니다
- 브랜드 삭제 시 연쇄 삭제는 단일 트랜잭션 내에서 처리하여 데이터 일관성을 보장합니다

### 7.4 에러 메시지
- 에러 메시지에 내부 구현 세부사항이나 스택 트레이스를 노출하지 않습니다
- 사용자 친화적인 메시지를 반환합니다

---

## Part 8: 검증 명령어

```bash
# 전체 테스트
./gradlew :apps:commerce-api:test

# ktlint 검사
./gradlew :apps:commerce-api:ktlintCheck

# ktlint 자동 수정
./gradlew :apps:commerce-api:ktlintFormat

# 빌드
./gradlew :apps:commerce-api:build

# 테스트 커버리지 리포트
./gradlew :apps:commerce-api:test jacocoTestReport
```

`.http/admin/brand/*.http` 및 `.http/admin/product/*.http` 파일로 수동 API 테스트가 가능합니다.

---

## 품질 체크리스트

- [x] 상품/브랜드/좋아요/주문 등 관련 도메인과의 관계가 명시되어 있는가?
- [x] 기능 요구사항이 유저 중심("어드민이 ~한다")으로 서술되어 있는가?
- [x] 인증 방식(LDAP 헤더 기반)이 정확히 명시되어 있는가?
- [x] 에러 케이스와 예외 상황이 포함되어 있는가?
- [x] Phase별 구현 체크리스트가 TDD 워크플로우에 맞게 구성되어 있는가?
- [x] 소프트 삭제 및 스냅샷 고려사항이 반영되어 있는가?
- [x] 입력 검증 규칙(중복 브랜드명, 가격 범위, URL 형식 등)이 상세히 정의되어 있는가?
- [x] 대고객 API와의 연동 규칙(비활성 브랜드, 숨김 상품 비노출 등)이 명시되어 있는가?


---

<\!-- ========== 다음 도메인 ========== -->

# 유저 요구사항

## 개요

유저 도메인은 회원가입, 내 정보 조회, 비밀번호 변경 기능을 제공합니다.
Round2에서는 기존 Round1의 `member` 도메인을 내부적으로 유지하면서, API 경로를 `/api/v1/users`로 변경하고, 인증 방식을 JWT 토큰에서 HTTP 헤더 기반(`X-Loopers-LoginId`, `X-Loopers-LoginPw`)으로 전환합니다.

## 관련 도메인

| 도메인 | 관계 | 설명 |
|--------|------|------|
| 브랜드-상품 | 간접 | 유저가 상품을 조회하고 주문하기 위해 회원가입이 선행되어야 합니다 |
| 좋아요 | 의존 | 좋아요 등록/조회 시 유저 식별 정보(`X-Loopers-LoginId`)가 필요합니다 |
| 주문 | 의존 | 주문 생성 시 유저 식별 정보가 필요하며, 주문자 정보로 유저 데이터를 참조합니다 |

---

## Part 1: API 명세

### 1.1 회원가입

#### Endpoint
- **METHOD**: `POST`
- **URI**: `/api/v1/users`
- **인증 필요**: 불필요

#### Request Body
```json
{
  "loginId": "testuser01",
  "password": "password1!",
  "name": "홍길동",
  "birthDate": "1990-01-01",
  "email": "test@example.com"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| loginId | String | O | 로그인 ID (4~20자, 영문 소문자/숫자/언더스코어) |
| password | String | O | 비밀번호 (8~16자, Part 2 검증 규칙 참조) |
| name | String | O | 이름 (2~50자, 한글 또는 영문) |
| birthDate | LocalDate | O | 생년월일 (미래 날짜 불가) |
| email | String | O | 이메일 (이메일 형식 검증) |

#### Response (성공) - `200 OK`
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "id": 1,
    "loginId": "testuser01",
    "name": "홍길동",
    "email": "test@example.com"
  }
}
```

#### Response (실패)

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 이미 존재하는 로그인 ID | 409 | Conflict | 이미 존재하는 리소스입니다. |
| 필수 필드 누락 또는 형식 오류 | 400 | Bad Request | 잘못된 요청입니다. |
| 비밀번호 검증 실패 | 400 | Bad Request | (검증 규칙별 상세 메시지) |

---

### 1.2 내 정보 조회

#### Endpoint
- **METHOD**: `GET`
- **URI**: `/api/v1/users/me`
- **인증 필요**: O (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 필수)

#### Request Headers
| 헤더 이름 | 값 | 설명 |
|-----------|-----|------|
| `X-Loopers-LoginId` | 로그인 ID | 유저를 식별하기 위한 로그인 ID |
| `X-Loopers-LoginPw` | 비밀번호 | 유저의 비밀번호 (평문 전송, 서버에서 BCrypt 매칭으로 검증) |

#### Response (성공) - `200 OK`
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "loginId": "testuser01",
    "name": "홍*동",
    "birthDate": "1990-01-01",
    "email": "te***@example.com"
  }
}
```

마스킹 규칙:
- **이름**: 첫 글자와 마지막 글자만 표시하고 나머지는 `*`로 대체 (예: "홍길동" -> "홍*동")
- **이메일**: 로컬 파트의 처음 2글자만 표시하고 나머지를 `***`로 대체 (예: "test@example.com" -> "te***@example.com")

#### Response (실패)

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 인증 헤더 누락 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 로그인 ID에 해당하는 유저가 없음 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 비밀번호 불일치 | 401 | UNAUTHORIZED | 인증이 필요합니다. |

---

### 1.3 비밀번호 변경

#### Endpoint
- **METHOD**: `PUT`
- **URI**: `/api/v1/users/password`
- **인증 필요**: O (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 필수)

#### Request Headers
| 헤더 이름 | 값 | 설명 |
|-----------|-----|------|
| `X-Loopers-LoginId` | 로그인 ID | 유저를 식별하기 위한 로그인 ID |
| `X-Loopers-LoginPw` | 비밀번호 | 유저의 현재 비밀번호 (평문 전송, 서버에서 BCrypt 매칭으로 검증) |

#### Request Body
```json
{
  "newPassword": "newPassword1!"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| newPassword | String | O | 새 비밀번호 (Part 2 검증 규칙 참조) |

> Round1에서는 `currentPassword`와 `newPassword`를 Request Body에 모두 포함했지만, Round2에서는 현재 비밀번호가 인증 헤더(`X-Loopers-LoginPw`)로 전달되므로 Request Body에는 `newPassword`만 포함합니다.

#### Response (성공) - `200 OK`
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": null
}
```

#### Response (실패)

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 인증 헤더 누락 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 로그인 ID에 해당하는 유저가 없음 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 비밀번호 불일치 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 현재 비밀번호와 동일한 새 비밀번호 | 400 | Bad Request | 현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다. |
| 새 비밀번호 검증 실패 | 400 | Bad Request | (검증 규칙별 상세 메시지) |

---

## Part 2: 비즈니스 규칙

### 2.1 회원가입 규칙

1. **로그인 ID 유효성 검증**: 4~20자, 영문 소문자/숫자/언더스코어만 허용
2. **로그인 ID 중복 검사**: 이미 동일한 로그인 ID가 존재하면 회원가입을 거부합니다
3. **이름 유효성 검증**: 2~50자, 한글 또는 영문만 허용
4. **이메일 형식 검증**: 이메일 형식(`xxx@xxx.xxx`)을 만족해야 합니다
5. **생년월일 검증**: 미래 날짜는 허용하지 않습니다
6. **비밀번호 검증**: Part 2.2의 비밀번호 검증 규칙을 모두 통과해야 합니다
7. **비밀번호 암호화**: 비밀번호는 BCrypt로 해싱하여 저장합니다

### 2.2 비밀번호 검증 규칙

1. **길이 제한**: 8자 이상 16자 이하
2. **허용 문자**: 영문 대소문자, 숫자, 특수문자만 허용
3. **문자 종류 조합**: 영문, 숫자, 특수문자 중 2종류 이상 조합 필수
4. **연속 동일 문자 제한**: 동일 문자 3개 이상 연속 사용 불가 (예: "aaa")
5. **연속 순서 문자 제한**: 순차적 문자 3개 이상 연속 사용 불가 (예: "abc", "321")
6. **생년월일 포함 금지**: YYYYMMDD, YYMMDD, MMDD 형식 모두 검사
7. **로그인 ID 포함 금지**: 비밀번호에 로그인 ID 문자열이 포함되면 거부

### 2.3 헤더 기반 인증 규칙

1. 유저가 보호된 API를 호출할 때 `X-Loopers-LoginId`와 `X-Loopers-LoginPw` 헤더를 함께 전송합니다
2. 서버는 `X-Loopers-LoginId`로 유저를 조회하고, `X-Loopers-LoginPw`를 BCrypt로 인코딩된 비밀번호와 매칭하여 인증합니다
3. 인증 실패 시(헤더 누락, 유저 미존재, 비밀번호 불일치) 모두 동일한 `UNAUTHORIZED` 에러를 반환합니다 (보안을 위해 실패 원인을 구분하지 않음)
4. 인증이 필요한 API 경로: `/api/v1/users/me`, `/api/v1/users/password`

### 2.4 비밀번호 변경 규칙

1. 인증 헤더의 현재 비밀번호와 새 비밀번호가 동일하면 변경을 거부합니다
2. 새 비밀번호는 Part 2.2의 검증 규칙을 모두 통과해야 합니다
3. 새 비밀번호는 BCrypt로 해싱하여 저장합니다

---

## Part 3: 구현 컴포넌트

### 3.1 레이어별 구조

Round1의 `member` 패키지 구조를 유지하면서, API 경로와 인증 방식만 Round2 스펙에 맞게 변경합니다.

```
interfaces/api/member/
  ├── MemberV1ApiSpec.kt      - API 인터페이스 (경로를 /api/v1/users로 변경)
  ├── MemberV1Controller.kt   - 컨트롤러 (헤더 기반 인증으로 변경)
  └── MemberV1Dto.kt          - 요청/응답 DTO

application/member/
  ├── MemberFacade.kt         - 비즈니스 로직 조합 (비밀번호 변경 시그니처 변경)
  └── MemberInfo.kt           - 내부 전달 객체

domain/member/
  ├── MemberModel.kt          - 도메인 엔티티 (변경 없음)
  ├── MemberService.kt        - 도메인 서비스 (변경 없음)
  └── MemberRepository.kt     - 리포지토리 인터페이스 (변경 없음)

infrastructure/member/
  ├── MemberRepositoryImpl.kt - 리포지토리 구현체 (변경 없음)
  ├── MemberJpaRepository.kt  - JPA 리포지토리 (변경 없음)
  └── BCryptPasswordEncoder.kt - 비밀번호 인코더 (변경 없음)

infrastructure/auth/
  └── HeaderAuthenticationFilter.kt - 헤더 기반 인증 필터 (신규, JwtAuthenticationFilter 대체)

support/util/
  ├── PasswordValidator.kt    - 비밀번호 검증 유틸리티 (변경 없음)
  └── MaskingUtils.kt         - 마스킹 유틸리티 (변경 없음)
```

### 3.2 처리 흐름

#### 회원가입
```
유저 → POST /api/v1/users
  → MemberV1Controller.signUp()
    → MemberFacade.signUp()
      → PasswordValidator.validatePassword() (비밀번호 검증)
      → BCryptPasswordEncoder.encode() (비밀번호 해싱)
      → MemberService.signUp() (중복 검사 + 저장)
    → MemberV1Dto.SignUpResponse 반환
```

#### 내 정보 조회
```
유저 → GET /api/v1/users/me [X-Loopers-LoginId, X-Loopers-LoginPw 헤더]
  → HeaderAuthenticationFilter (헤더에서 loginId/password 추출, 유저 조회 및 비밀번호 검증)
    → MemberV1Controller.getMyInfo()
      → MemberFacade.getMyInfo()
        → MemberService.findById()
        → MemberInfo 생성
      → MemberV1Dto.MyInfoResponse (마스킹 적용) 반환
```

#### 비밀번호 변경
```
유저 → PUT /api/v1/users/password [X-Loopers-LoginId, X-Loopers-LoginPw 헤더]
  → HeaderAuthenticationFilter (헤더에서 loginId/password 추출, 유저 조회 및 비밀번호 검증)
    → MemberV1Controller.changePassword()
      → MemberFacade.changePassword()
        → 현재 비밀번호와 새 비밀번호 동일 여부 검사
        → PasswordValidator.validatePassword() (새 비밀번호 검증)
        → BCryptPasswordEncoder.encode() (새 비밀번호 해싱)
        → MemberService.changePassword() (저장)
      → 성공 응답 반환
```

---

## Part 4: 구현 체크리스트

### Phase 1: 헤더 기반 인증 필터 구현

기존 JWT 인증 필터(`JwtAuthenticationFilter`)를 대체하는 헤더 기반 인증 필터(`HeaderAuthenticationFilter`)를 구현합니다.

- [ ] **RED**: `HeaderAuthenticationFilter` 단위 테스트 작성
  - [ ] `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더가 모두 있으면 인증 성공하는지 검증
  - [ ] 헤더가 누락되면 `UNAUTHORIZED` 에러를 반환하는지 검증
  - [ ] 존재하지 않는 로그인 ID로 요청하면 `UNAUTHORIZED` 에러를 반환하는지 검증
  - [ ] 비밀번호가 일치하지 않으면 `UNAUTHORIZED` 에러를 반환하는지 검증
  - [ ] 보호 대상 경로(`/api/v1/users/me`, `/api/v1/users/password`)에만 필터가 적용되는지 검증
  - [ ] 보호 대상이 아닌 경로에는 필터가 적용되지 않는지 검증
- [ ] **GREEN**: `HeaderAuthenticationFilter` 구현
  - [ ] `OncePerRequestFilter`를 상속하여 헤더 기반 인증 필터 작성
  - [ ] `shouldNotFilter()`에서 보호 대상 경로를 판별
  - [ ] `doFilterInternal()`에서 헤더 추출, 유저 조회, 비밀번호 매칭 수행
  - [ ] 인증 성공 시 유저 정보를 요청 속성에 설정
- [ ] **REFACTOR**: 기존 `JwtAuthenticationFilter` 비활성화 또는 제거

### Phase 2: API 경로 변경 및 컨트롤러 수정

기존 컨트롤러의 API 경로를 Round2 스펙에 맞게 변경하고, 인증 방식을 헤더 기반으로 전환합니다.

- [ ] **RED**: 컨트롤러 통합 테스트 작성
  - [ ] `POST /api/v1/users` 회원가입 요청이 정상 동작하는지 검증
  - [ ] `GET /api/v1/users/me` 내 정보 조회가 인증 헤더와 함께 정상 동작하는지 검증
  - [ ] `PUT /api/v1/users/password` 비밀번호 변경이 인증 헤더와 함께 정상 동작하는지 검증
- [ ] **GREEN**: 컨트롤러 수정
  - [ ] `@RequestMapping` 경로를 `/api/v1/members`에서 `/api/v1/users`로 변경
  - [ ] 회원가입: `@PostMapping("/sign-up")`을 `@PostMapping`(루트)으로 변경
  - [ ] 내 정보 조회: `@GetMapping("/me")` 유지, 인증 객체를 요청 속성에서 가져오도록 수정
  - [ ] 비밀번호 변경: `@PatchMapping("/me/password")`을 `@PutMapping("/password")`으로 변경
  - [ ] 비밀번호 변경 Request Body에서 `currentPassword` 필드 제거 (인증 헤더로 대체)
- [ ] **REFACTOR**: ApiSpec 인터페이스도 함께 수정하고 불필요한 JWT 관련 import 정리

### Phase 3: Facade 및 비즈니스 로직 수정

비밀번호 변경 로직에서 현재 비밀번호를 인증 헤더에서 받은 값과 새 비밀번호를 비교하도록 수정합니다.

- [ ] **RED**: `MemberFacade` 단위 테스트 수정
  - [ ] `changePassword()`에서 인증 헤더의 비밀번호(평문)와 새 비밀번호가 동일하면 거부하는지 검증
  - [ ] 새 비밀번호가 검증 규칙을 통과하지 못하면 거부하는지 검증
  - [ ] 정상적인 비밀번호 변경이 성공하는지 검증
- [ ] **GREEN**: `MemberFacade.changePassword()` 시그니처 및 로직 수정
  - [ ] 기존: `changePassword(memberId, currentPassword, newPassword)`
  - [ ] 변경: `changePassword(memberId, currentPlainPassword, newPassword)` - 인증 필터에서 이미 비밀번호 검증이 완료되었으므로 BCrypt 매칭 로직 제거
  - [ ] 현재 비밀번호(평문)와 새 비밀번호 동일 여부 검사 유지
- [ ] **REFACTOR**: 불필요한 코드 제거 및 정리

### Phase 4: E2E 테스트 및 .http 파일 작성

모든 API 엔드포인트에 대한 E2E 테스트와 .http 테스트 파일을 작성합니다.

- [ ] **RED**: E2E 테스트 작성
  - [ ] 회원가입 -> 내 정보 조회 -> 비밀번호 변경 전체 흐름 테스트
  - [ ] 인증 실패 케이스 (헤더 누락, 잘못된 비밀번호) 테스트
  - [ ] 비밀번호 변경 후 변경된 비밀번호로 인증 성공하는지 테스트
- [ ] **GREEN**: 모든 E2E 테스트 통과 확인
- [ ] `.http` 파일 작성
  - [ ] `.http/user/sign-up.http`: 회원가입 API 테스트
  - [ ] `.http/user/me.http`: 내 정보 조회 API 테스트
  - [ ] `.http/user/change-password.http`: 비밀번호 변경 API 테스트

---

## Part 5: 테스트 시나리오

### 5.1 단위 테스트

#### HeaderAuthenticationFilter
| 시나리오 | 입력 | 기대 결과 |
|----------|------|-----------|
| 인증 헤더가 모두 있고 유효한 경우 | loginId: "testuser01", loginPw: "password1!" | 요청 속성에 인증 정보 설정, 필터 체인 진행 |
| `X-Loopers-LoginId` 헤더 누락 | loginPw만 존재 | 401 UNAUTHORIZED 응답 |
| `X-Loopers-LoginPw` 헤더 누락 | loginId만 존재 | 401 UNAUTHORIZED 응답 |
| 존재하지 않는 로그인 ID | loginId: "nonexistent" | 401 UNAUTHORIZED 응답 |
| 비밀번호 불일치 | loginId: "testuser01", loginPw: "wrongpw1!" | 401 UNAUTHORIZED 응답 |
| 보호 대상이 아닌 경로 | POST /api/v1/users | 필터 스킵, 필터 체인 진행 |

#### MemberFacade.changePassword()
| 시나리오 | 입력 | 기대 결과 |
|----------|------|-----------|
| 현재 비밀번호와 새 비밀번호가 동일 | currentPw: "password1!", newPw: "password1!" | BAD_REQUEST 에러 |
| 새 비밀번호가 검증 규칙 위반 (짧은 길이) | newPw: "abc1!" | BAD_REQUEST 에러 |
| 새 비밀번호가 검증 규칙 위반 (연속 동일 문자) | newPw: "aaab1234!" | BAD_REQUEST 에러 |
| 정상적인 비밀번호 변경 | newPw: "newValid1!" | 성공, BCrypt 해싱 후 저장 |

### 5.2 통합 테스트

| 시나리오 | 요청 | 기대 결과 |
|----------|------|-----------|
| 회원가입 성공 | POST /api/v1/users (유효한 데이터) | 200 OK, 가입 정보 반환 |
| 회원가입 중복 로그인 ID | POST /api/v1/users (중복 loginId) | 409 Conflict |
| 내 정보 조회 성공 | GET /api/v1/users/me (유효한 인증 헤더) | 200 OK, 마스킹된 정보 반환 |
| 내 정보 조회 인증 실패 | GET /api/v1/users/me (잘못된 비밀번호) | 401 UNAUTHORIZED |
| 비밀번호 변경 성공 | PUT /api/v1/users/password (유효한 데이터) | 200 OK |
| 비밀번호 변경 동일 비밀번호 | PUT /api/v1/users/password (현재=새 비밀번호) | 400 BAD_REQUEST |

### 5.3 E2E 테스트

| 시나리오 | 흐름 | 기대 결과 |
|----------|------|-----------|
| 전체 유저 플로우 | 회원가입 -> 내 정보 조회 -> 비밀번호 변경 -> 변경된 비밀번호로 내 정보 조회 | 모든 단계 성공 |
| 인증 실패 후 재시도 | 잘못된 비밀번호로 내 정보 조회 실패 -> 올바른 비밀번호로 재시도 성공 | 첫 시도 401, 재시도 200 |
| 비밀번호 변경 후 이전 비밀번호 거부 | 비밀번호 변경 -> 이전 비밀번호로 내 정보 조회 | 401 UNAUTHORIZED |

---

## Part 6: 보안 고려사항

1. **비밀번호 평문 전송**: `X-Loopers-LoginPw` 헤더로 비밀번호가 평문으로 전송됩니다. 프로덕션 환경에서는 반드시 HTTPS를 사용해야 하지만, Round2에서는 인증/인가가 주요 스코프가 아니므로 현행 방식을 유지합니다.
2. **인증 실패 메시지 통일**: 로그인 ID 미존재, 비밀번호 불일치 등 인증 실패의 구체적인 원인을 응답에 노출하지 않고, 동일한 `UNAUTHORIZED` 메시지를 반환하여 정보 유출을 방지합니다.
3. **비밀번호 해싱**: 모든 비밀번호는 BCrypt로 해싱하여 데이터베이스에 저장합니다.
4. **접근 제한**: 유저는 헤더에 포함된 본인의 로그인 정보를 기준으로 자신의 데이터에만 접근할 수 있으며, 타 유저의 정보에는 접근할 수 없습니다.

---

## Part 7: 검증 명령어

```bash
# 전체 빌드
./gradlew build

# 유저 관련 테스트만 실행
./gradlew :apps:commerce-api:test --tests "*Member*"
./gradlew :apps:commerce-api:test --tests "*HeaderAuthentication*"

# 린트 체크
./gradlew ktlintCheck

# 린트 자동 수정
./gradlew ktlintFormat

# 테스트 커버리지 확인
./gradlew test jacocoTestReport
```

---

## 품질 체크리스트

- [x] 상품/브랜드/좋아요/주문 등 관련 도메인과의 관계가 명시되어 있는가?
- [x] 기능 요구사항이 유저 중심("유저가 ~한다")으로 서술되어 있는가?
- [x] 인증 방식(헤더 기반)이 정확히 명시되어 있는가?
- [x] 에러 케이스와 예외 상황이 포함되어 있는가?
- [x] Phase별 구현 체크리스트가 TDD 워크플로우에 맞게 구성되어 있는가?


---

<\!-- ========== 다음 도메인 ========== -->

# 좋아요 요구사항

## 개요

유저가 마음에 드는 상품에 좋아요를 등록하거나 취소하고, 자신이 좋아요한 상품 목록을 조회할 수 있는 기능입니다.
좋아요 데이터는 이후 개인화 추천, 인기 상품 정렬(`likes_desc`) 등에 활용됩니다.

## 관련 도메인

| 도메인 | 관계 | 설명 |
|--------|------|------|
| 유저(Users) | 좋아요의 주체 | 로그인한 유저만 좋아요를 등록/취소/조회할 수 있습니다. |
| 상품(Products) | 좋아요의 대상 | 좋아요는 특정 상품에 대해 등록됩니다. 존재하는 상품에만 좋아요가 가능합니다. |
| 브랜드(Brands) | 간접 연관 | 좋아요 목록 조회 시 상품의 브랜드 정보를 함께 제공합니다. |

---

## Part 1: API 명세

### 1.1 상품 좋아요 등록

#### Endpoint
- **METHOD**: `POST`
- **URI**: `/api/v1/products/{productId}/likes`
- **인증 필요**: O (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 필수)

#### Path Variable
| 변수 | 타입 | 설명 |
|------|------|------|
| `productId` | Long | 좋아요를 등록할 상품의 ID |

#### Request Body
없음

#### Response (성공) - 200 OK
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": null
}
```

#### Response (실패)
| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 인증 헤더가 없거나 로그인 정보가 유효하지 않은 경우 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 존재하지 않는 상품에 좋아요를 요청한 경우 | 404 | Not Found | 존재하지 않는 상품입니다. |
| 삭제된 상품에 좋아요를 요청한 경우 | 404 | Not Found | 존재하지 않는 상품입니다. |

#### 멱등성 처리
- 이미 좋아요한 상품에 다시 좋아요를 요청하면 에러 없이 200 OK를 반환합니다.
- 좋아요 상태는 변경되지 않으며, 중복 데이터가 생성되지 않습니다.

---

### 1.2 상품 좋아요 취소

#### Endpoint
- **METHOD**: `DELETE`
- **URI**: `/api/v1/products/{productId}/likes`
- **인증 필요**: O (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 필수)

#### Path Variable
| 변수 | 타입 | 설명 |
|------|------|------|
| `productId` | Long | 좋아요를 취소할 상품의 ID |

#### Request Body
없음

#### Response (성공) - 200 OK
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": null
}
```

#### Response (실패)
| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 인증 헤더가 없거나 로그인 정보가 유효하지 않은 경우 | 401 | UNAUTHORIZED | 인증이 필요합니다. |

#### 멱등성 처리
- 좋아요하지 않은 상품에 취소를 요청하면 에러 없이 200 OK를 반환합니다.
- 존재하지 않는 상품에 대한 취소 요청도 에러 없이 200 OK를 반환합니다.

---

### 1.3 내가 좋아요한 상품 목록 조회

#### Endpoint
- **METHOD**: `GET`
- **URI**: `/api/v1/users/{userId}/likes`
- **인증 필요**: O (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 필수)

#### Path Variable
| 변수 | 타입 | 설명 |
|------|------|------|
| `userId` | Long | 좋아요 목록을 조회할 유저의 ID |

#### Query Parameters
| 파라미터 | 타입 | 필수 여부 | 기본값 | 설명 |
|----------|------|-----------|--------|------|
| `page` | Int | X | `0` | 페이지 번호 (0부터 시작) |
| `size` | Int | X | `20` | 페이지당 항목 수 |

#### Response (성공) - 200 OK
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
        "likeId": 1,
        "product": {
          "productId": 100,
          "productName": "클래식 티셔츠",
          "price": 29000,
          "brandId": 10,
          "brandName": "루퍼스 브랜드",
          "isDeleted": false
        },
        "createdAt": "2026-02-13T10:30:00+09:00"
      },
      {
        "likeId": 2,
        "product": {
          "productId": 200,
          "productName": "삭제된 상품",
          "price": 15000,
          "brandId": 20,
          "brandName": "다른 브랜드",
          "isDeleted": true
        },
        "createdAt": "2026-02-12T15:00:00+09:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

#### Response (실패)
| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 인증 헤더가 없거나 로그인 정보가 유효하지 않은 경우 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 타인의 좋아요 목록을 조회하려는 경우 | 401 | UNAUTHORIZED | 본인의 좋아요 목록만 조회할 수 있습니다. |

#### 삭제된 상품 처리
- 좋아요한 상품이 삭제된 경우에도 목록에 포함하되, `isDeleted: true`로 표시합니다.
- 유저가 삭제된 상품의 좋아요를 직접 취소할 수 있도록 정보를 제공합니다.

---

## Part 2: 비즈니스 규칙

### 2.1 필수 규칙

1. **인증 필수**: 모든 좋아요 API는 `X-Loopers-LoginId`와 `X-Loopers-LoginPw` 헤더를 통한 유저 인증이 필요합니다.
2. **본인 확인**: 좋아요 목록 조회 시 경로의 `userId`와 인증된 유저의 ID가 일치해야 합니다. 타인의 좋아요 목록은 조회할 수 없습니다.
3. **상품 존재 검증**: 좋아요 등록 시 해당 상품이 존재하고 삭제되지 않은 상태여야 합니다.
4. **좋아요 등록 멱등성**: 이미 좋아요한 상품에 다시 등록 요청을 보내면 에러 없이 성공 응답을 반환합니다.
5. **좋아요 취소 멱등성**: 좋아요 기록이 없는 상품에 취소 요청을 보내면 에러 없이 성공 응답을 반환합니다.
6. **유저-상품 유니크 제약**: 하나의 유저는 하나의 상품에 대해 최대 하나의 좋아요 기록만 가질 수 있습니다.

### 2.2 추가 규칙

1. **정렬 기준**: 좋아요 목록은 좋아요 등록 시점의 최신순(내림차순)으로 정렬됩니다.
2. **소프트 삭제**: 좋아요 취소 시 `deletedAt` 필드를 설정하여 소프트 삭제 처리합니다. 같은 상품에 다시 좋아요를 등록하면 `deletedAt`을 null로 복원합니다.
3. **삭제된 상품 노출**: 좋아요 목록 조회 시 좋아요한 상품이 삭제된 경우에도 목록에 포함하되, `isDeleted` 필드로 삭제 상태를 표시합니다.

---

## Part 3: 구현 컴포넌트

### 3.1 레이어별 구조

```
interfaces/api/like/
  ├── LikeV1ApiSpec.kt          - API 인터페이스 (Swagger 문서화)
  ├── LikeV1Controller.kt       - REST 컨트롤러
  └── LikeV1Dto.kt              - 요청/응답 DTO

application/like/
  ├── LikeFacade.kt             - 좋아요 비즈니스 오케스트레이션
  └── LikeInfo.kt               - 애플리케이션 레이어 정보 객체

domain/like/
  ├── LikeModel.kt              - 좋아요 도메인 엔티티
  ├── LikeService.kt            - 좋아요 도메인 서비스
  └── LikeRepository.kt         - 좋아요 저장소 인터페이스

infrastructure/like/
  ├── LikeJpaRepository.kt      - Spring Data JPA 인터페이스
  └── LikeRepositoryImpl.kt     - 저장소 구현체
```

### 3.2 처리 흐름

#### 좋아요 등록
```
Controller (POST /api/v1/products/{productId}/likes)
  → 인증 헤더에서 유저 식별
  → LikeFacade.likeProduct(userId, productId)
    → MemberService.findByLoginId(loginId) - 유저 존재 확인 및 비밀번호 검증
    → ProductService.findById(productId) - 상품 존재 확인 (삭제 여부 포함)
    → LikeService.like(userId, productId) - 좋아요 등록 (멱등 처리)
      → 기존 좋아요 기록 조회
        → 기록 없음: 새 LikeModel 생성 후 저장
        → 삭제된 기록 있음: restore() 호출하여 복원
        → 활성 기록 있음: 아무 작업 없이 반환
  → 200 OK 응답
```

#### 좋아요 취소
```
Controller (DELETE /api/v1/products/{productId}/likes)
  → 인증 헤더에서 유저 식별
  → LikeFacade.unlikeProduct(userId, productId)
    → MemberService.findByLoginId(loginId) - 유저 존재 확인 및 비밀번호 검증
    → LikeService.unlike(userId, productId) - 좋아요 취소 (멱등 처리)
      → 기존 좋아요 기록 조회
        → 활성 기록 있음: delete() 호출하여 소프트 삭제
        → 기록 없음 또는 이미 삭제됨: 아무 작업 없이 반환
  → 200 OK 응답
```

#### 좋아요 목록 조회
```
Controller (GET /api/v1/users/{userId}/likes?page=0&size=20)
  → 인증 헤더에서 유저 식별
  → 경로의 userId와 인증된 유저의 ID 일치 여부 검증
  → LikeFacade.getMyLikes(userId, page, size)
    → LikeService.findByUserId(userId, pageable) - 페이징된 좋아요 목록 조회
      → 삭제되지 않은 좋아요 기록만 조회 (deletedAt IS NULL)
      → 상품 정보를 JOIN하여 함께 조회 (삭제된 상품 포함)
    → LikeInfo 목록으로 변환 (상품 삭제 여부 isDeleted 포함)
  → 200 OK 응답 (페이징 정보 포함)
```

---

## Part 4: 구현 체크리스트

### Phase 1: 도메인 레이어 (LikeModel, LikeRepository, LikeService)

좋아요 도메인 엔티티와 저장소 인터페이스, 도메인 서비스를 구현합니다.

- [ ] **RED**: `LikeModel` 엔티티 테스트 작성
  - 유저 ID와 상품 ID로 좋아요 엔티티 생성 검증
  - BaseEntity 상속 확인 (id, createdAt, updatedAt, deletedAt)
- [ ] **GREEN**: `LikeModel` 엔티티 구현
  - `userId` (Long), `productId` (Long) 필드
  - `like` 테이블, `user_id`와 `product_id` 유니크 제약 조건
- [ ] **RED**: `LikeRepository` 인터페이스 메서드 정의를 위한 통합 테스트 작성
- [ ] **GREEN**: `LikeRepository` 인터페이스 및 `LikeRepositoryImpl`, `LikeJpaRepository` 구현
  - `findByUserIdAndProductId(userId, productId): LikeModel?`
  - `findByUserIdAndDeletedAtIsNull(userId, pageable): Page<LikeModel>`
  - `save(like: LikeModel): LikeModel`
- [ ] **RED**: `LikeService` 테스트 작성
  - 좋아요 등록 (신규, 복원, 이미 활성) 시나리오
  - 좋아요 취소 (활성 기록, 기록 없음, 이미 삭제) 시나리오
  - 유저별 좋아요 목록 조회 시나리오
- [ ] **GREEN**: `LikeService` 구현
- [ ] **REFACTOR**: 코드 정리 및 전체 테스트 통과 확인

### Phase 2: 인증 필터 (헤더 기반 유저 식별)

Round2 인증 방식(`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더)에 맞는 인증 필터를 구현합니다.
기존 Round1의 JWT 기반 인증과 별도로 Round2 헤더 기반 인증을 처리할 수 있어야 합니다.

- [ ] **RED**: 헤더 기반 인증 필터 테스트 작성
  - 유효한 헤더로 인증 성공 시나리오
  - 헤더 누락 시 401 응답 시나리오
  - 잘못된 로그인 정보로 401 응답 시나리오
- [ ] **GREEN**: 헤더 기반 인증 필터 구현
  - `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더에서 로그인 정보 추출
  - MemberService를 통한 유저 존재 확인 및 비밀번호 검증
  - 인증된 유저 정보를 요청 속성에 설정
- [ ] **REFACTOR**: 기존 JWT 필터와 공존할 수 있는 구조로 정리

### Phase 3: 애플리케이션 레이어 (LikeFacade, LikeInfo)

좋아요 비즈니스 로직을 오케스트레이션하는 Facade와 정보 객체를 구현합니다.

- [ ] **RED**: `LikeFacade` 테스트 작성
  - 좋아요 등록: 상품 존재 검증 후 좋아요 등록
  - 좋아요 등록: 존재하지 않는 상품에 대한 404 에러
  - 좋아요 등록: 삭제된 상품에 대한 404 에러
  - 좋아요 취소: 정상 취소
  - 좋아요 목록 조회: 페이징 및 상품 정보 포함 검증
- [ ] **GREEN**: `LikeFacade`, `LikeInfo` 구현
- [ ] **REFACTOR**: 코드 정리

### Phase 4: 인터페이스 레이어 (Controller, DTO, ApiSpec)

REST API 엔드포인트를 구현합니다.

- [ ] **RED**: `LikeV1Controller` 통합 테스트 작성
  - POST `/api/v1/products/{productId}/likes` 정상 등록
  - POST 중복 좋아요 멱등 처리 확인
  - POST 존재하지 않는 상품 404
  - DELETE `/api/v1/products/{productId}/likes` 정상 취소
  - DELETE 좋아요 기록 없는 상품 멱등 처리 확인
  - GET `/api/v1/users/{userId}/likes` 목록 조회 및 페이징
  - GET 타인의 목록 조회 시 401
  - 인증 헤더 누락 시 401
- [ ] **GREEN**: `LikeV1Controller`, `LikeV1Dto`, `LikeV1ApiSpec` 구현
- [ ] **REFACTOR**: 코드 정리 및 전체 테스트 통과 확인

### Phase 5: E2E 테스트 및 API 문서

실제 API를 호출하여 전체 흐름을 검증하고, `.http` 파일을 작성합니다.

- [ ] E2E 테스트: 회원가입 → 좋아요 등록 → 목록 조회 → 좋아요 취소 → 목록 재조회 흐름 검증
- [ ] `.http/like/like-product.http` 작성 (좋아요 등록)
- [ ] `.http/like/unlike-product.http` 작성 (좋아요 취소)
- [ ] `.http/like/get-my-likes.http` 작성 (좋아요 목록 조회)

---

## Part 5: 테스트 시나리오

### 5.1 단위 테스트

#### LikeModel 테스트
| 테스트 케이스 | 설명 |
|--------------|------|
| 유효한 유저 ID와 상품 ID로 생성 | 정상적으로 LikeModel 엔티티가 생성된다 |

#### LikeService 테스트
| 테스트 케이스 | 설명 |
|--------------|------|
| 좋아요 등록 - 신규 | 좋아요 기록이 없는 상품에 좋아요를 등록하면 새 기록이 생성된다 |
| 좋아요 등록 - 삭제 복원 | 이전에 취소한 좋아요를 다시 등록하면 deletedAt이 null로 복원된다 |
| 좋아요 등록 - 이미 활성 | 이미 좋아요한 상품에 다시 등록하면 기존 상태를 유지한다 |
| 좋아요 취소 - 정상 | 활성 좋아요 기록을 취소하면 deletedAt이 설정된다 |
| 좋아요 취소 - 기록 없음 | 좋아요 기록이 없는 상품을 취소해도 에러가 발생하지 않는다 |
| 좋아요 취소 - 이미 삭제 | 이미 취소된 좋아요를 다시 취소해도 에러가 발생하지 않는다 |
| 좋아요 목록 조회 - 페이징 | 유저의 활성 좋아요 목록을 페이징하여 조회한다 |

#### LikeFacade 테스트
| 테스트 케이스 | 설명 |
|--------------|------|
| 좋아요 등록 - 상품 존재 | 존재하는 상품에 좋아요를 등록한다 |
| 좋아요 등록 - 상품 미존재 | 존재하지 않는 상품에 좋아요 등록 시 NOT_FOUND 에러가 발생한다 |
| 좋아요 등록 - 삭제된 상품 | 삭제된 상품에 좋아요 등록 시 NOT_FOUND 에러가 발생한다 |
| 좋아요 취소 - 정상 | 정상적으로 좋아요를 취소한다 |
| 좋아요 목록 조회 | 유저의 좋아요 목록을 상품 정보와 함께 조회한다 |

### 5.2 통합 테스트

| 테스트 케이스 | 설명 |
|--------------|------|
| 좋아요 등록 API 정상 호출 | POST 요청으로 상품에 좋아요를 등록하고 200 OK를 받는다 |
| 중복 좋아요 멱등 처리 | 같은 상품에 두 번 좋아요를 등록해도 200 OK를 받으며 중복 데이터가 생기지 않는다 |
| 존재하지 않는 상품 좋아요 | 없는 상품에 좋아요 등록 시 404를 받는다 |
| 좋아요 취소 API 정상 호출 | DELETE 요청으로 좋아요를 취소하고 200 OK를 받는다 |
| 좋아요 기록 없는 상품 취소 | 좋아요하지 않은 상품을 취소해도 200 OK를 받는다 |
| 좋아요 목록 조회 API 정상 호출 | GET 요청으로 자신의 좋아요 목록을 페이징하여 조회한다 |
| 타인의 좋아요 목록 조회 차단 | 다른 유저의 좋아요 목록 조회 시 401을 받는다 |
| 삭제된 상품 포함 목록 조회 | 좋아요한 상품이 삭제되어도 isDeleted: true로 목록에 포함된다 |
| 인증 헤더 누락 시 401 | 인증 헤더 없이 API 호출 시 401을 받는다 |
| 좋아요 등록 후 취소 후 재등록 | 좋아요 → 취소 → 재등록 흐름이 정상 동작한다 |

### 5.3 E2E 테스트

| 테스트 케이스 | 설명 |
|--------------|------|
| 좋아요 전체 플로우 | 회원가입 → 좋아요 등록 → 목록 조회(1건) → 좋아요 취소 → 목록 조회(0건) |
| 복수 상품 좋아요 | 여러 상품에 좋아요 등록 후 목록 조회 시 최신순 정렬 확인 |

---

## Part 6: 보안 고려사항

1. **인증 검증**: 모든 좋아요 API는 `X-Loopers-LoginId`와 `X-Loopers-LoginPw` 헤더를 통한 인증이 필수입니다. 헤더가 누락되거나 유효하지 않은 경우 401 응답을 반환합니다.
2. **본인 확인**: 좋아요 목록 조회 시 경로의 `userId`가 인증된 유저의 ID와 일치하는지 검증합니다. 타인의 데이터에 접근할 수 없도록 서버 측에서 검증합니다.
3. **입력 검증**: `productId`와 `userId`는 Long 타입으로 변환이 가능한 양수 값이어야 합니다. 잘못된 형식의 경로 변수가 전달되면 400 응답을 반환합니다.
4. **SQL Injection 방지**: Spring Data JPA의 파라미터 바인딩을 사용하여 SQL Injection을 방지합니다.

---

## Part 7: 검증 명령어

```bash
# 전체 테스트 실행
./gradlew :apps:commerce-api:test

# 좋아요 관련 테스트만 실행
./gradlew :apps:commerce-api:test --tests "*Like*"

# 테스트 커버리지 리포트 생성
./gradlew :apps:commerce-api:test jacocoTestReport

# ktlint 검사
./gradlew ktlintCheck

# ktlint 자동 수정
./gradlew ktlintFormat

# 전체 빌드
./gradlew :apps:commerce-api:build
```

---

## 품질 체크리스트

- [x] 상품/브랜드/좋아요/주문 등 관련 도메인과의 관계가 명시되어 있는가? - 유저(주체), 상품(대상), 브랜드(간접 연관) 관계를 명시함
- [x] 기능 요구사항이 유저 중심("유저가 ~한다")으로 서술되어 있는가? - 개요 및 처리 흐름에서 유저 행동 중심으로 서술함
- [x] 인증 방식(헤더 기반)이 정확히 명시되어 있는가? - `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 기반 인증을 명시함
- [x] 에러 케이스와 예외 상황이 포함되어 있는가? - 각 API별 실패 응답, 멱등성 처리, 본인 확인 등을 포함함
- [x] Phase별 구현 체크리스트가 TDD 워크플로우에 맞게 구성되어 있는가? - 각 Phase에 RED → GREEN → REFACTOR 단계를 포함함


---

<\!-- ========== 다음 도메인 ========== -->

# 주문 요구사항

## 개요

유저가 여러 상품을 한 번에 주문하고, 자신의 주문 내역을 기간별로 조회하거나 특정 주문의 상세 정보를 확인할 수 있는 기능입니다.
주문 생성 시 상품의 재고를 확인하고 차감하며, 주문 당시의 상품 정보를 스냅샷으로 보존합니다.

## 관련 도메인

| 도메인 | 관계 | 설명 |
|--------|------|------|
| 유저(Member) | 주문자 | 로그인한 유저만 주문할 수 있으며, 본인의 주문만 조회 가능합니다. |
| 상품(Product) | 주문 대상 | 주문 항목에 포함되는 상품이며, 재고 확인 및 차감의 대상입니다. |
| 브랜드(Brand) | 상품 소속 | 주문 스냅샷에 브랜드명이 포함되어 주문 당시의 브랜드 정보를 보존합니다. |

---

## Part 1: API 명세

### 1.1 주문 요청 API

#### Endpoint
- **Method**: `POST`
- **URI**: `/api/v1/orders`
- **인증**: 필수 (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더)
- **설명**: 유저가 여러 상품을 한 번에 주문합니다. 주문 항목별로 상품 ID와 수량을 지정합니다.

#### Request Body
```json
{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 3, "quantity": 1 }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 | 제약 조건 |
|------|------|------|------|-----------|
| `items` | Array | O | 주문 항목 목록 | 최소 1개 이상 |
| `items[].productId` | Long | O | 주문할 상품 ID | 존재하는 상품이어야 함 |
| `items[].quantity` | Int | O | 주문 수량 | 1 이상 99 이하 |

#### Response (성공) - HTTP 200
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "orderId": 1,
    "orderStatus": "ORDERED",
    "orderedAt": "2026-02-13T10:30:00",
    "totalAmount": 55000,
    "items": [
      {
        "productName": "베이직 티셔츠",
        "brandName": "루퍼스",
        "price": 25000,
        "quantity": 2,
        "subTotal": 50000
      },
      {
        "productName": "캔버스 에코백",
        "brandName": "루퍼스",
        "price": 5000,
        "quantity": 1,
        "subTotal": 5000
      }
    ]
  }
}
```

#### Response (실패)

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 인증 헤더 누락 또는 잘못된 로그인 정보 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 요청 바디가 비어있거나 items가 빈 배열 | 400 | Bad Request | 주문 항목은 최소 1개 이상이어야 합니다. |
| 수량이 1 미만 또는 99 초과 | 400 | Bad Request | 주문 수량은 1개 이상 99개 이하여야 합니다. |
| 동일 상품 ID가 중복으로 포함됨 | 400 | Bad Request | 동일한 상품을 중복으로 주문할 수 없습니다. 수량을 조정해 주세요. |
| 존재하지 않는 상품 ID | 404 | Not Found | 존재하지 않는 상품입니다. |
| 재고 부족 | 400 | Bad Request | 상품의 재고가 부족합니다. (상품명: {상품명}, 요청 수량: {quantity}개, 현재 재고: {stock}개) |

---

### 1.2 주문 목록 조회 API

#### Endpoint
- **Method**: `GET`
- **URI**: `/api/v1/orders`
- **인증**: 필수 (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더)
- **설명**: 유저가 자신의 주문 목록을 기간별로 조회합니다. 본인의 주문만 조회 가능합니다.

#### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 예시 | 설명 |
|----------|------|------|--------|------|------|
| `startAt` | LocalDate | O | - | `2026-01-01` | 조회 시작일 |
| `endAt` | LocalDate | O | - | `2026-02-13` | 조회 종료일 |

#### 요청 예시
```http
GET /api/v1/orders?startAt=2026-01-01&endAt=2026-02-13
X-Loopers-LoginId: testuser01
X-Loopers-LoginPw: password123!
```

#### Response (성공) - HTTP 200
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "orders": [
      {
        "orderId": 2,
        "orderStatus": "ORDERED",
        "orderedAt": "2026-02-10T14:00:00",
        "totalAmount": 55000,
        "itemCount": 2
      },
      {
        "orderId": 1,
        "orderStatus": "ORDERED",
        "orderedAt": "2026-01-15T09:30:00",
        "totalAmount": 30000,
        "itemCount": 1
      }
    ]
  }
}
```

- 주문 목록은 최신 주문순(orderedAt 내림차순)으로 정렬됩니다.
- 각 주문의 상세 항목은 포함하지 않고, 주문 건 단위의 요약 정보만 반환합니다.

#### Response (실패)

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 인증 헤더 누락 또는 잘못된 로그인 정보 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| startAt 또는 endAt 누락 | 400 | Bad Request | 조회 시작일과 종료일은 필수입니다. |
| startAt이 endAt보다 이후인 경우 | 400 | Bad Request | 조회 시작일은 종료일보다 이전이어야 합니다. |
| 조회 기간이 3개월(90일)을 초과 | 400 | Bad Request | 조회 기간은 최대 3개월까지 가능합니다. |

---

### 1.3 단일 주문 상세 조회 API

#### Endpoint
- **Method**: `GET`
- **URI**: `/api/v1/orders/{orderId}`
- **인증**: 필수 (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더)
- **설명**: 유저가 특정 주문의 상세 내역을 조회합니다. 본인의 주문만 조회 가능합니다.

#### Path Parameters

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `orderId` | Long | 조회할 주문 ID |

#### Response (성공) - HTTP 200
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "orderId": 1,
    "orderStatus": "ORDERED",
    "orderedAt": "2026-02-13T10:30:00",
    "totalAmount": 55000,
    "items": [
      {
        "productName": "베이직 티셔츠",
        "brandName": "루퍼스",
        "price": 25000,
        "quantity": 2,
        "subTotal": 50000
      },
      {
        "productName": "캔버스 에코백",
        "brandName": "루퍼스",
        "price": 5000,
        "quantity": 1,
        "subTotal": 5000
      }
    ]
  }
}
```

#### Response (실패)

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 인증 헤더 누락 또는 잘못된 로그인 정보 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 존재하지 않는 주문 ID | 404 | Not Found | 존재하지 않는 주문입니다. |
| 다른 유저의 주문을 조회하려는 경우 | 404 | Not Found | 존재하지 않는 주문입니다. |

- 다른 유저의 주문을 조회하려는 경우 보안상 404를 반환합니다. (주문 존재 여부를 노출하지 않기 위함)

---

## Part 2: 비즈니스 규칙

### 2.1 필수 규칙

#### 주문 생성 규칙
1. **인증 필수**: 로그인한 유저만 주문할 수 있습니다.
2. **최소 항목 수**: 주문에는 최소 1개 이상의 항목이 포함되어야 합니다.
3. **수량 범위**: 각 주문 항목의 수량은 1개 이상 99개 이하여야 합니다.
4. **상품 중복 불가**: 동일한 상품 ID가 주문 항목에 중복으로 포함될 수 없습니다. 같은 상품을 여러 개 주문하려면 수량(quantity)을 조정해야 합니다.
5. **상품 존재 확인**: 주문 항목의 모든 상품 ID가 실제 존재하는 상품이어야 합니다.
6. **재고 확인 및 차감**: 주문 요청된 각 상품의 재고가 충분한지 확인하고, 충분한 경우 주문 수량만큼 재고를 차감합니다.
7. **원자성 보장**: 여러 상품을 한 번에 주문하므로, 일부 상품의 재고가 부족하면 전체 주문이 실패해야 합니다. (All or Nothing)
8. **상품 정보 스냅샷**: 주문 당시의 상품명, 가격, 브랜드명을 주문 항목에 스냅샷으로 저장합니다. 이후 상품 정보가 변경되더라도 주문 내역에는 주문 시점의 정보가 유지됩니다.
9. **주문 상태**: 주문 생성 시 초기 상태는 `ORDERED`입니다.

#### 주문 조회 규칙
1. **본인 주문만 조회**: 유저는 자신의 주문만 조회할 수 있습니다. 다른 유저의 주문에는 접근할 수 없습니다.
2. **기간 필수**: 주문 목록 조회 시 시작일(startAt)과 종료일(endAt)은 필수입니다.
3. **기간 유효성**: 시작일은 종료일보다 이전이어야 합니다.
4. **최대 조회 기간**: 시작일과 종료일 사이의 기간은 최대 3개월(90일)까지 가능합니다.
5. **정렬**: 주문 목록은 최신 주문순(주문일시 내림차순)으로 정렬됩니다.
6. **보안**: 다른 유저의 주문 ID로 상세 조회를 시도하면 주문 존재 여부를 노출하지 않기 위해 404를 반환합니다.

### 2.2 동시성 제어

- 동일 상품에 여러 유저가 동시에 주문할 때 재고 정합성을 보장하기 위해 **비관적 락(Pessimistic Lock)**을 사용합니다.
- JPA의 `@Lock(LockModeType.PESSIMISTIC_WRITE)`를 사용하여 재고 조회 시 행 잠금을 걸어 동시성을 제어합니다.
- 데드락 방지를 위해 상품 ID 오름차순으로 정렬하여 순서대로 락을 획득합니다.

### 2.3 결제

- 결제 기능은 이후 단계에서 추가로 개발됩니다.
- 초기 구현에서는 주문 생성과 재고 차감까지만 처리합니다.

---

## Part 3: 구현 컴포넌트

### 3.1 레이어별 구조

```
interfaces/api/order/
  ├── OrderV1ApiSpec.kt          # API 인터페이스 (Swagger 문서)
  ├── OrderV1Controller.kt       # REST Controller
  └── OrderV1Dto.kt              # Request/Response DTO

application/order/
  ├── OrderFacade.kt             # 주문 유스케이스 조합 (인증 + 상품 조회 + 주문 생성)
  └── OrderInfo.kt               # Facade 레이어 정보 객체

domain/order/
  ├── OrderModel.kt              # 주문 엔티티 (주문 헤더)
  ├── OrderItemModel.kt          # 주문 항목 엔티티 (주문 상세, 스냅샷 포함)
  ├── OrderStatus.kt             # 주문 상태 열거형 (ORDERED)
  ├── OrderService.kt            # 주문 도메인 서비스
  └── OrderRepository.kt         # 주문 Repository 인터페이스

infrastructure/order/
  ├── OrderRepositoryImpl.kt     # 주문 Repository 구현
  └── OrderJpaRepository.kt      # Spring Data JPA Repository
```

### 3.2 처리 흐름

#### 주문 요청 흐름
```
1. Controller: 요청 수신 및 DTO 변환
2. Facade: 인증 검증 → 유저 조회
3. Facade: 요청 검증 (중복 상품, 수량 범위)
4. Facade: 상품 목록 조회 (ProductService 활용)
5. Domain Service: 비관적 락으로 재고 확인 및 차감
6. Domain Service: 주문 생성 (스냅샷 포함)
7. Controller: 응답 반환
```

#### 주문 목록 조회 흐름
```
1. Controller: 요청 수신 및 쿼리 파라미터 바인딩
2. Facade: 인증 검증 → 유저 조회
3. Facade: 기간 유효성 검증 (startAt < endAt, 최대 3개월)
4. Domain Service: 해당 유저의 주문 목록 조회 (기간 필터, 최신순 정렬)
5. Controller: 응답 반환
```

#### 단일 주문 상세 조회 흐름
```
1. Controller: 요청 수신 및 orderId 추출
2. Facade: 인증 검증 → 유저 조회
3. Domain Service: 주문 조회 (유저 ID + 주문 ID로 조회하여 본인 확인)
4. Controller: 응답 반환 (스냅샷 데이터 포함)
```

---

## Part 4: 구현 체크리스트

### Phase 1: 도메인 모델 및 Repository 구현

주문 도메인의 핵심 엔티티와 Repository를 구현합니다.

- [ ] **RED**: `OrderModel`, `OrderItemModel`, `OrderStatus` 도메인 모델에 대한 단위 테스트 작성
  - 주문 생성 시 상태가 ORDERED로 설정되는지 검증
  - 주문 항목에 스냅샷 정보(상품명, 가격, 브랜드명)가 저장되는지 검증
  - 총 주문 금액(totalAmount) 계산이 올바른지 검증
- [ ] **GREEN**: 테스트를 통과하는 최소 구현
  - `OrderStatus` 열거형 생성
  - `OrderModel` 엔티티 생성 (유저 ID, 주문 상태, 주문 일시, 총 금액)
  - `OrderItemModel` 엔티티 생성 (주문 ID, 상품 ID, 상품명, 브랜드명, 가격, 수량, 소계)
  - `OrderRepository` 인터페이스 생성
  - `OrderJpaRepository`, `OrderRepositoryImpl` 구현
- [ ] **REFACTOR**: 코드 정리 및 품질 개선

### Phase 2: 주문 생성 서비스 구현

주문 생성의 비즈니스 로직(재고 확인, 재고 차감, 스냅샷 저장)을 구현합니다.

- [ ] **RED**: `OrderService.createOrder` 에 대한 단위 테스트 작성
  - 정상 주문 생성 시 재고 차감 및 스냅샷 저장 검증
  - 재고 부족 시 예외 발생 및 상세 에러 메시지 검증
  - 여러 상품 주문 시 일부 재고 부족이면 전체 주문 실패 검증 (원자성)
  - 비관적 락을 통한 동시성 제어 검증
- [ ] **GREEN**: 테스트를 통과하는 최소 구현
  - 상품 조회 및 재고 확인 로직
  - 비관적 락을 활용한 재고 차감 로직
  - 주문 및 주문 항목 생성 로직 (스냅샷 포함)
- [ ] **REFACTOR**: 코드 정리 및 품질 개선

### Phase 3: 주문 조회 서비스 구현

주문 목록 조회 및 단일 주문 상세 조회의 비즈니스 로직을 구현합니다.

- [ ] **RED**: `OrderService`의 조회 메서드에 대한 단위 테스트 작성
  - 기간별 주문 목록 조회 (최신순 정렬) 검증
  - 시작일이 종료일보다 이후인 경우 예외 발생 검증
  - 조회 기간 3개월 초과 시 예외 발생 검증
  - 본인 주문만 조회되는지 검증
  - 존재하지 않는 주문 ID로 조회 시 예외 발생 검증
  - 다른 유저의 주문 조회 시 NOT_FOUND 예외 발생 검증
- [ ] **GREEN**: 테스트를 통과하는 최소 구현
  - 기간 유효성 검증 로직
  - 기간별 주문 목록 조회 쿼리 (유저 ID + 기간 필터)
  - 단일 주문 상세 조회 (유저 ID + 주문 ID)
- [ ] **REFACTOR**: 코드 정리 및 품질 개선

### Phase 4: Facade 및 Controller 구현

API 레이어(Controller, Facade, DTO)를 구현하고 통합 테스트를 작성합니다.

- [ ] **RED**: `OrderFacade` 및 `OrderV1Controller`에 대한 통합 테스트 작성
  - 주문 생성 API 통합 테스트 (정상 / 인증 실패 / 유효성 검증 실패)
  - 주문 목록 조회 API 통합 테스트 (정상 / 기간 유효성 / 인증 실패)
  - 단일 주문 상세 조회 API 통합 테스트 (정상 / 미존재 / 타인 주문)
- [ ] **GREEN**: 테스트를 통과하는 최소 구현
  - `OrderV1Dto` (Request/Response DTO) 생성
  - `OrderV1ApiSpec` (Swagger API 인터페이스) 생성
  - `OrderInfo` (Facade 레이어 정보 객체) 생성
  - `OrderFacade` (인증 검증, 서비스 조합) 생성
  - `OrderV1Controller` 생성
- [ ] **REFACTOR**: 코드 정리 및 품질 개선

### Phase 5: E2E 테스트 및 API 문서 정리

실제 API를 호출하여 전체 흐름을 검증하고 .http 파일을 작성합니다.

- [ ] E2E 테스트 작성 (실제 API 호출을 통한 전체 흐름 검증)
- [ ] `.http/order/create-order.http` 작성 (주문 생성 API 테스트)
- [ ] `.http/order/get-orders.http` 작성 (주문 목록 조회 API 테스트)
- [ ] `.http/order/get-order-detail.http` 작성 (주문 상세 조회 API 테스트)

---

## Part 5: 테스트 시나리오

### 5.1 단위 테스트

#### 도메인 모델 테스트
| 테스트 케이스 | 검증 내용 |
|--------------|-----------|
| 주문 생성 시 초기 상태 | 주문 생성 시 상태가 ORDERED로 설정되는지 검증 |
| 총 금액 계산 | 주문 항목들의 (가격 x 수량) 합계가 올바른지 검증 |
| 스냅샷 저장 | 주문 항목에 상품명, 가격, 브랜드명이 스냅샷으로 저장되는지 검증 |

#### 도메인 서비스 테스트
| 테스트 케이스 | 검증 내용 |
|--------------|-----------|
| 정상 주문 생성 | 재고 확인 후 차감, 주문 및 항목 생성 성공 |
| 재고 부족 | 재고 부족 시 상세 에러 메시지와 함께 BAD_REQUEST 예외 발생 |
| 원자성 보장 | 3개 상품 중 1개 재고 부족 시 전체 주문 실패, 재고 차감 없음 |
| 중복 상품 검증 | 동일 상품 ID가 중복 포함 시 BAD_REQUEST 예외 발생 |
| 수량 범위 검증 | 수량이 0 이하 또는 100 이상일 때 BAD_REQUEST 예외 발생 |
| 기간 유효성 검증 | startAt > endAt 시 BAD_REQUEST 예외 발생 |
| 기간 초과 검증 | 조회 기간 90일 초과 시 BAD_REQUEST 예외 발생 |
| 본인 주문 조회 | 본인의 주문만 조회 결과에 포함되는지 검증 |
| 타인 주문 조회 불가 | 다른 유저의 주문 ID로 조회 시 NOT_FOUND 예외 발생 |

### 5.2 통합 테스트

| 테스트 케이스 | 검증 내용 |
|--------------|-----------|
| 주문 생성 성공 | POST /api/v1/orders 정상 요청 시 200 + 주문 데이터 반환 |
| 주문 생성 - 인증 실패 | 헤더 누락 시 401 반환 |
| 주문 생성 - 빈 항목 | items가 빈 배열일 때 400 반환 |
| 주문 생성 - 수량 초과 | quantity가 100일 때 400 반환 |
| 주문 생성 - 중복 상품 | 동일 productId 중복 시 400 반환 |
| 주문 생성 - 미존재 상품 | 존재하지 않는 productId 포함 시 404 반환 |
| 주문 생성 - 재고 부족 | 재고보다 많은 수량 요청 시 400 + 상세 메시지 반환 |
| 주문 목록 조회 성공 | GET /api/v1/orders?startAt=...&endAt=... 정상 요청 시 200 반환 |
| 주문 목록 조회 - 기간 초과 | 91일 기간 요청 시 400 반환 |
| 주문 목록 조회 - 역순 기간 | startAt > endAt 시 400 반환 |
| 주문 상세 조회 성공 | GET /api/v1/orders/{orderId} 정상 요청 시 200 + 스냅샷 데이터 반환 |
| 주문 상세 조회 - 타인 주문 | 다른 유저의 주문 조회 시 404 반환 |

### 5.3 E2E 테스트

| 시나리오 | 흐름 |
|----------|------|
| 주문 전체 흐름 | 회원가입 -> 상품 등록(어드민) -> 주문 생성 -> 주문 목록 조회 -> 주문 상세 조회 |
| 재고 부족 시나리오 | 상품 등록(재고 5개) -> 주문 생성(수량 3개) -> 주문 생성(수량 3개, 실패 확인) |
| 동시 주문 시나리오 | 상품 등록(재고 1개) -> 두 유저가 동시에 주문 -> 한 건만 성공, 한 건은 재고 부족 확인 |

---

## Part 6: 보안 고려사항

| 항목 | 대응 방안 |
|------|-----------|
| 인증 검증 | 모든 주문 API는 `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더로 유저를 식별하고, 로그인 정보가 올바른지 검증합니다. |
| 본인 데이터 접근 제한 | 주문 조회 시 반드시 유저 ID를 조건에 포함하여 본인의 주문만 조회되도록 합니다. |
| 주문 존재 여부 노출 방지 | 다른 유저의 주문 조회 시 403(Forbidden) 대신 404(Not Found)를 반환하여 주문 존재 여부를 외부에 노출하지 않습니다. |
| 입력값 검증 | 요청 본문의 모든 필드를 서버 측에서 검증합니다. (items 비어있음, 수량 범위, 상품 중복 등) |
| SQL 인젝션 방지 | JPA 파라미터 바인딩을 사용하여 쿼리 파라미터를 안전하게 처리합니다. |
| 비관적 락 타임아웃 | 비관적 락에 적절한 타임아웃을 설정하여 무한 대기를 방지합니다. |

---

## Part 7: 검증 명령어

```bash
# 전체 테스트 실행
./gradlew :apps:commerce-api:test

# 주문 관련 테스트만 실행
./gradlew :apps:commerce-api:test --tests "*Order*"

# 테스트 커버리지 확인
./gradlew :apps:commerce-api:test jacocoTestReport

# ktlint 검사
./gradlew ktlintCheck

# ktlint 자동 수정
./gradlew ktlintFormat

# 빌드
./gradlew :apps:commerce-api:build
```

---

## 품질 체크리스트

- [x] 상품/브랜드/좋아요/주문 등 관련 도메인과의 관계가 명시되어 있는가?
  - 유저(주문자), 상품(주문 대상), 브랜드(스냅샷) 도메인과의 관계를 "관련 도메인" 섹션에 명시함
- [x] 기능 요구사항이 유저 중심("유저가 ~한다")으로 서술되어 있는가?
  - 개요 및 각 API 설명에 "유저가 주문한다", "유저가 조회한다" 형태로 서술함
- [x] 인증 방식(헤더 기반)이 정확히 명시되어 있는가?
  - 모든 API에 `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 필수 명시함
- [x] 에러 케이스와 예외 상황이 포함되어 있는가?
  - 각 API별 실패 응답 표에 모든 에러 케이스를 상세히 정리함
- [x] Phase별 구현 체크리스트가 TDD 워크플로우에 맞게 구성되어 있는가?
  - Phase 1~4는 RED -> GREEN -> REFACTOR 단계로 구성, Phase 5는 E2E 및 문서 정리


---

<\!-- ========== 다음 도메인 ========== -->

# 주문 Admin 요구사항

## 개요
어드민이 전체 사용자의 주문을 조회하고 관리할 수 있는 API를 제공합니다. 대고객 주문 API와 달리, 어드민은 모든 유저의 주문을 페이징/필터링/정렬하여 조회할 수 있으며, 주문 상세에서 주문자 정보와 주문 항목별 소계를 포함한 확장 정보를 확인할 수 있습니다.

## 관련 도메인
| 도메인 | 관계 | 설명 |
|--------|------|------|
| 주문(Order) | 직접 대상 | 조회 대상이 되는 핵심 도메인. 주문 모델, 주문 항목 모델, 주문 상태 등을 사용합니다. |
| 유저(Member) | 주문자 정보 | 주문을 생성한 유저의 정보(loginId, 이름, 이메일)를 상세 조회에서 표시합니다. |
| 상품(Product) | 주문 항목 참조 | 주문 항목에 저장된 상품 스냅샷(상품명, 가격)을 표시합니다. |
| 브랜드(Brand) | 상품 소속 | 주문 항목의 상품 스냅샷에 브랜드명을 포함합니다. |

**선행 조건**: 이 기능은 주문 도메인(Order, OrderItem, OrderStatus)이 먼저 구현되어 있어야 합니다. 대고객 주문 API(`docs/주문/01-requirements.md`)의 구현이 선행되어야 합니다.

---

## Part 1: API 명세

### 1.1 주문 목록 조회

#### Endpoint
```
GET /api-admin/v1/orders
X-Loopers-Ldap: loopers.admin
```

#### 쿼리 파라미터

| 파라미터 | 타입 | 필수 여부 | 기본값 | 예시 | 설명 |
|----------|------|-----------|--------|------|------|
| `page` | Int | X | `0` | `0` | 페이지 번호 (0부터 시작) |
| `size` | Int | X | `20` | `20` | 페이지당 주문 수 (최대 100) |
| `status` | String | X | 없음 | `ORDERED` | 주문 상태별 필터링 |
| `loginId` | String | X | 없음 | `testuser01` | 특정 유저의 주문만 필터링 |
| `sort` | String | X | `orderedAt` | `orderedAt` | 정렬 기준 필드 (`orderedAt`, `totalAmount`) |
| `direction` | String | X | `DESC` | `ASC` | 정렬 방향 (`ASC`, `DESC`) |

#### 요청 예시
```http
GET /api-admin/v1/orders?page=0&size=20&status=ORDERED&loginId=testuser01&sort=orderedAt&direction=DESC
X-Loopers-Ldap: loopers.admin
```

#### Response (성공 - 200 OK)
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
        "orderId": 1,
        "loginId": "testuser01",
        "status": "ORDERED",
        "totalAmount": 35000,
        "itemCount": 2,
        "orderedAt": "2026-02-10T14:30:00+09:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### Response (실패)

##### LDAP 헤더 누락 또는 유효하지 않은 값 (401 Unauthorized)
```json
{
  "meta": {
    "result": "FAIL",
    "errorCode": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  },
  "data": null
}
```

##### 유효하지 않은 status 값 (400 Bad Request)
```json
{
  "meta": {
    "result": "FAIL",
    "errorCode": "Bad Request",
    "message": "유효하지 않은 주문 상태입니다. 사용 가능한 값: [ORDERED, PREPARING, SHIPPING, DELIVERED, CANCELLED]"
  },
  "data": null
}
```

##### 유효하지 않은 sort 값 (400 Bad Request)
```json
{
  "meta": {
    "result": "FAIL",
    "errorCode": "Bad Request",
    "message": "유효하지 않은 정렬 기준입니다. 사용 가능한 값: [orderedAt, totalAmount]"
  },
  "data": null
}
```

---

### 1.2 단일 주문 상세 조회

#### Endpoint
```
GET /api-admin/v1/orders/{orderId}
X-Loopers-Ldap: loopers.admin
```

#### 요청 예시
```http
GET /api-admin/v1/orders/1
X-Loopers-Ldap: loopers.admin
```

#### Response (성공 - 200 OK)
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "orderId": 1,
    "orderer": {
      "loginId": "testuser01",
      "name": "홍길동",
      "email": "hong@example.com"
    },
    "status": "ORDERED",
    "items": [
      {
        "orderItemId": 1,
        "productId": 10,
        "productName": "클래식 티셔츠",
        "brandName": "루퍼스",
        "price": 15000,
        "quantity": 2,
        "subtotal": 30000
      },
      {
        "orderItemId": 2,
        "productId": 25,
        "productName": "데님 팬츠",
        "brandName": "루퍼스",
        "price": 5000,
        "quantity": 1,
        "subtotal": 5000
      }
    ],
    "totalAmount": 35000,
    "orderedAt": "2026-02-10T14:30:00+09:00"
  }
}
```

#### Response (실패)

##### 존재하지 않는 주문 (404 Not Found)
```json
{
  "meta": {
    "result": "FAIL",
    "errorCode": "Not Found",
    "message": "존재하지 않는 주문입니다."
  },
  "data": null
}
```

##### LDAP 헤더 누락 또는 유효하지 않은 값 (401 Unauthorized)
```json
{
  "meta": {
    "result": "FAIL",
    "errorCode": "UNAUTHORIZED",
    "message": "인증이 필요합니다."
  },
  "data": null
}
```

---

## Part 2: 비즈니스 규칙

### 2.1 필수 규칙

| # | 규칙 | 설명 |
|---|------|------|
| 1 | LDAP 인증 필수 | 모든 어드민 API는 `X-Loopers-Ldap: loopers.admin` 헤더가 반드시 포함되어야 합니다. 헤더가 없거나 값이 `loopers.admin`이 아닌 경우 401 Unauthorized를 반환합니다. |
| 2 | 전체 유저 주문 조회 가능 | 대고객 API와 달리, 어드민은 모든 유저의 주문을 조회할 수 있습니다. 본인 주문 제한이 없습니다. |
| 3 | 페이징 필수 적용 | 주문 목록 조회는 항상 페이징 처리됩니다. page 기본값 0, size 기본값 20, size 최대값 100입니다. |
| 4 | 상세 조회 시 주문자 정보 포함 | 어드민이 주문자를 식별할 수 있도록 주문 상세에 주문자의 loginId, 이름, 이메일을 마스킹 없이 원본으로 포함합니다. |
| 5 | 상품 스냅샷 데이터 사용 | 주문 항목의 상품 정보는 주문 시점의 스냅샷 데이터를 사용합니다. 현재 상품 정보가 아닌 주문 당시 저장된 정보를 표시합니다. |

### 2.2 주문 상태 정의

주문 도메인에서 사용하는 상태 값은 다음과 같습니다:

| 상태 | 설명 |
|------|------|
| `ORDERED` | 주문 완료 (초기 상태) |
| `PREPARING` | 배송 준비 중 |
| `SHIPPING` | 배송 중 |
| `DELIVERED` | 배송 완료 |
| `CANCELLED` | 주문 취소 |

### 2.3 필터링 규칙

| 필터 | 규칙 |
|------|------|
| `status` | OrderStatus enum 값 중 하나와 정확히 일치해야 합니다. 유효하지 않은 값이면 400 Bad Request를 반환합니다. |
| `loginId` | 해당 loginId를 가진 유저의 주문만 조회합니다. 존재하지 않는 loginId인 경우 빈 목록을 반환합니다 (에러가 아닙니다). |

### 2.4 정렬 규칙

| 정렬 기준 | 설명 |
|-----------|------|
| `orderedAt` (기본값) | 주문 일시 기준 정렬 |
| `totalAmount` | 주문 총액 기준 정렬 |

- 정렬 방향 기본값은 `DESC` (내림차순)입니다.
- `direction` 파라미터는 `ASC` 또는 `DESC`만 허용합니다. 유효하지 않은 값이면 400 Bad Request를 반환합니다.

### 2.5 금액 계산 규칙

| 항목 | 계산 방식 |
|------|-----------|
| `subtotal` (항목별 소계) | `price * quantity` |
| `totalAmount` (주문 총액) | 모든 항목의 `subtotal` 합계 |

---

## Part 3: 구현 컴포넌트

### 3.1 레이어별 구조

```
interfaces/api/admin/order/
  ├── OrderAdminV1Controller.kt     # 어드민 주문 조회 컨트롤러
  ├── OrderAdminV1ApiSpec.kt        # OpenAPI 스펙 인터페이스
  └── OrderAdminV1Dto.kt            # Request/Response DTO

application/admin/order/
  ├── OrderAdminFacade.kt           # 어드민 주문 조회 퍼사드
  └── OrderAdminInfo.kt             # 내부 전달 DTO

domain/order/
  ├── OrderModel.kt                 # 주문 엔티티 (주문 도메인에서 구현)
  ├── OrderItemModel.kt             # 주문 항목 엔티티 (주문 도메인에서 구현)
  ├── OrderStatus.kt                # 주문 상태 enum (주문 도메인에서 구현)
  ├── OrderService.kt               # 주문 도메인 서비스 (주문 도메인에서 구현, 어드민용 조회 메서드 추가)
  └── OrderRepository.kt            # 주문 리포지토리 인터페이스 (주문 도메인에서 구현, 어드민용 조회 메서드 추가)

infrastructure/order/
  ├── OrderJpaRepository.kt         # JPA 리포지토리 (주문 도메인에서 구현)
  └── OrderRepositoryImpl.kt        # 리포지토리 구현체 (주문 도메인에서 구현, 어드민용 조회 메서드 추가)

infrastructure/admin/
  └── LdapAuthenticationFilter.kt   # LDAP 어드민 인증 필터 (/api-admin/** 경로 보호)
```

### 3.2 처리 흐름

#### 주문 목록 조회
```
Controller (GET /api-admin/v1/orders)
  -> LdapAuthenticationFilter: X-Loopers-Ldap 헤더 검증
  -> OrderAdminFacade.getOrders(page, size, status, loginId, sort, direction)
    -> OrderService.findAllForAdmin(page, size, status, loginId, sort, direction)
      -> OrderRepository.findAllWithFilters(pageable, status, loginId): Page<OrderModel>
    -> 각 주문의 itemCount, totalAmount 계산
    -> OrderAdminInfo.OrderListItem 목록으로 변환
  -> OrderAdminV1Dto.OrderListResponse로 변환하여 응답
```

#### 단일 주문 상세 조회
```
Controller (GET /api-admin/v1/orders/{orderId})
  -> LdapAuthenticationFilter: X-Loopers-Ldap 헤더 검증
  -> OrderAdminFacade.getOrderDetail(orderId)
    -> OrderService.findByIdWithItems(orderId): OrderModel (주문 항목 포함)
      -> 주문이 존재하지 않으면 NOT_FOUND 예외 발생
    -> MemberService.findById(order.memberId): MemberModel (주문자 정보 조회)
    -> OrderAdminInfo.OrderDetail로 변환 (주문자 정보 + 주문 항목 + 금액 계산)
  -> OrderAdminV1Dto.OrderDetailResponse로 변환하여 응답
```

---

## Part 4: 구현 체크리스트

### Phase 1: LDAP 인증 필터 구현
어드민 API 경로(`/api-admin/**`)를 보호하는 LDAP 인증 필터를 구현합니다.

- [ ] `LdapAuthenticationFilter` 구현: `X-Loopers-Ldap` 헤더 값이 `loopers.admin`인지 검증
- [ ] `/api-admin/` 으로 시작하는 경로에만 필터 적용 (`shouldNotFilter` 메서드)
- [ ] 인증 실패 시 401 Unauthorized 응답 반환
- [ ] `LdapAuthenticationFilterTest` 단위 테스트 작성
  - 유효한 LDAP 헤더로 요청 시 필터 통과
  - LDAP 헤더 누락 시 401 반환
  - 잘못된 LDAP 값 시 401 반환
  - `/api-admin/` 이 아닌 경로는 필터 미적용

### Phase 2: 어드민 주문 목록 조회 API 구현
어드민이 전체 유저의 주문을 페이징/필터링/정렬하여 조회할 수 있는 API를 구현합니다.

- [ ] `OrderAdminV1Dto.OrderListResponse` DTO 정의 (orderId, loginId, status, totalAmount, itemCount, orderedAt)
- [ ] `OrderAdminV1Dto.PageResponse` DTO 정의 (content, page, size, totalElements, totalPages)
- [ ] `OrderAdminInfo.OrderListItem` 내부 DTO 정의
- [ ] `OrderRepository`에 어드민용 조회 메서드 추가: `findAllForAdmin(pageable, status, loginId)`
- [ ] `OrderRepositoryImpl`에 구현 추가 (Spring Data JPA Specification 또는 QueryDSL 활용)
- [ ] `OrderService`에 어드민용 조회 메서드 추가: `findAllForAdmin(page, size, status, loginId, sort, direction)`
- [ ] `OrderAdminFacade.getOrders()` 구현
- [ ] `OrderAdminV1ApiSpec` 인터페이스 정의
- [ ] `OrderAdminV1Controller` 구현 (GET /api-admin/v1/orders)
- [ ] 단위 테스트 작성
  - `OrderServiceTest`: 필터링/정렬/페이징 조합 테스트
  - `OrderAdminFacadeTest`: 목록 조회 오케스트레이션 테스트
- [ ] 통합 테스트 작성
  - 필터 없이 전체 목록 조회
  - status 필터 적용
  - loginId 필터 적용
  - status + loginId 복합 필터 적용
  - 정렬 기준 변경 (orderedAt ASC, totalAmount DESC 등)
  - 페이징 동작 확인
  - 유효하지 않은 status 값 시 400 응답
  - LDAP 인증 미포함 시 401 응답

### Phase 3: 어드민 단일 주문 상세 조회 API 구현
어드민이 특정 주문의 상세 내역(주문자 정보, 주문 항목, 금액 소계/총액)을 조회할 수 있는 API를 구현합니다.

- [ ] `OrderAdminV1Dto.OrderDetailResponse` DTO 정의 (orderId, orderer, status, items, totalAmount, orderedAt)
- [ ] `OrderAdminV1Dto.OrdererResponse` DTO 정의 (loginId, name, email)
- [ ] `OrderAdminV1Dto.OrderItemResponse` DTO 정의 (orderItemId, productId, productName, brandName, price, quantity, subtotal)
- [ ] `OrderAdminInfo.OrderDetail` 내부 DTO 정의
- [ ] `OrderRepository`에 주문 항목 포함 조회 메서드 추가 (이미 있으면 재사용): `findByIdWithItems(orderId)`
- [ ] `OrderAdminFacade.getOrderDetail(orderId)` 구현 (주문 조회 + 주문자 조회 + DTO 변환)
- [ ] `OrderAdminV1Controller`에 상세 조회 엔드포인트 추가 (GET /api-admin/v1/orders/{orderId})
- [ ] 단위 테스트 작성
  - `OrderAdminFacadeTest`: 상세 조회 시 주문자 정보 포함 확인
  - `OrderAdminFacadeTest`: 주문 항목별 subtotal 계산 확인
  - `OrderAdminFacadeTest`: totalAmount 계산 확인
  - `OrderAdminFacadeTest`: 존재하지 않는 주문 ID 시 NOT_FOUND 예외
- [ ] 통합 테스트 작성
  - 정상 상세 조회 (주문자 정보 + 주문 항목 + 금액 확인)
  - 존재하지 않는 orderId 시 404 응답
  - LDAP 인증 미포함 시 401 응답

### Phase 4: HTTP 테스트 파일 작성
완성된 API를 IntelliJ HTTP Client로 수동 테스트할 수 있도록 .http 파일을 작성합니다.

- [ ] `.http/admin/order/list-orders.http` 생성 (필터/정렬/페이징 다양한 케이스)
- [ ] `.http/admin/order/get-order-detail.http` 생성

---

## Part 5: 테스트 시나리오

### 5.1 단위 테스트

#### LDAP 인증 필터 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 유효한 LDAP 헤더 | `X-Loopers-Ldap: loopers.admin` | 필터 통과, 다음 필터 체인 호출 |
| LDAP 헤더 누락 | 헤더 없음 | 401 Unauthorized |
| 잘못된 LDAP 값 | `X-Loopers-Ldap: invalid.value` | 401 Unauthorized |
| 어드민 경로가 아닌 요청 | `GET /api/v1/products` | 필터 미적용 (shouldNotFilter = true) |

#### OrderAdminFacade 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 목록 조회 (필터 없음) | page=0, size=20 | 전체 주문 페이징 결과 반환 |
| 상태 필터 적용 | status=ORDERED | 해당 상태의 주문만 반환 |
| 유저 필터 적용 | loginId=testuser01 | 해당 유저의 주문만 반환 |
| 상세 조회 성공 | orderId=1 | 주문 + 주문자 정보 + 항목 + 금액 반환 |
| 상세 조회 시 subtotal 계산 | price=15000, quantity=2 | subtotal=30000 |
| 상세 조회 시 totalAmount 계산 | 항목1: 30000, 항목2: 5000 | totalAmount=35000 |
| 존재하지 않는 주문 | orderId=999 | NOT_FOUND 예외 |

### 5.2 통합 테스트

#### 주문 목록 조회 API

| 시나리오 | 조건 | 기대 결과 |
|---------|------|----------|
| 전체 목록 조회 | 필터 없음, LDAP 헤더 포함 | 200 OK + 페이징된 주문 목록 |
| 상태 필터 | status=ORDERED | 200 OK + ORDERED 상태 주문만 |
| 유저 필터 | loginId=testuser01 | 200 OK + testuser01의 주문만 |
| 복합 필터 | status=ORDERED&loginId=testuser01 | 200 OK + 두 조건 모두 만족하는 주문만 |
| 주문일시 오름차순 정렬 | sort=orderedAt&direction=ASC | 200 OK + 오래된 순으로 정렬 |
| 총액 내림차순 정렬 | sort=totalAmount&direction=DESC | 200 OK + 금액 높은 순으로 정렬 |
| 존재하지 않는 유저 필터 | loginId=nonexistent | 200 OK + 빈 목록 |
| 유효하지 않은 status | status=INVALID | 400 Bad Request |
| 유효하지 않은 sort | sort=invalidField | 400 Bad Request |
| 유효하지 않은 direction | direction=INVALID | 400 Bad Request |
| LDAP 헤더 누락 | 헤더 없음 | 401 Unauthorized |
| 잘못된 LDAP 값 | X-Loopers-Ldap: wrong.value | 401 Unauthorized |
| size 최대값 초과 | size=200 | 400 Bad Request |

#### 단일 주문 상세 조회 API

| 시나리오 | 조건 | 기대 결과 |
|---------|------|----------|
| 정상 상세 조회 | 존재하는 orderId + LDAP 헤더 | 200 OK + 주문자 정보 + 주문 항목 + 금액 |
| 주문자 정보 포함 확인 | 상세 조회 응답 | orderer에 loginId, name, email 포함 (마스킹 없음) |
| 주문 항목 스냅샷 확인 | 상세 조회 응답 | items에 productName, brandName, price (주문 시점 스냅샷) 포함 |
| 항목별 소계 확인 | price=15000, quantity=2 | subtotal=30000 |
| 주문 총액 확인 | 여러 항목 | totalAmount = 모든 subtotal 합계 |
| 존재하지 않는 주문 | orderId=999 | 404 Not Found |
| LDAP 헤더 누락 | 헤더 없음 | 401 Unauthorized |

### 5.3 E2E 테스트

| 시나리오 | 흐름 | 기대 결과 |
|---------|------|----------|
| 주문 목록 → 상세 조회 | 목록에서 orderId 확인 후 상세 조회 | 목록의 orderId와 상세 조회 결과 일치 |
| 유저별 주문 확인 | 두 유저가 각각 주문 후 어드민이 loginId 필터로 조회 | 각 유저의 주문만 정확히 반환 |
| 필터 + 정렬 조합 | status=ORDERED&sort=totalAmount&direction=DESC | 조건에 맞는 결과가 올바르게 정렬되어 반환 |

---

## Part 6: 보안 고려사항

### 6.1 LDAP 인증
- 모든 어드민 API 요청은 `X-Loopers-Ldap: loopers.admin` 헤더를 검증합니다.
- LDAP 헤더 값은 대소문자를 구분합니다 (`loopers.admin` 정확히 일치).
- 인증 실패 시 에러 메시지에 유효한 LDAP 값을 노출하지 않습니다.

### 6.2 데이터 접근 범위
- 어드민 API는 대고객 API와 URL prefix(`/api-admin/v1`)로 명확히 분리합니다.
- 어드민 상세 조회에서 주문자 정보(이름, 이메일)는 마스킹 없이 원본을 표시합니다. 이는 어드민 업무에 필요한 정보이므로 의도된 동작입니다.

### 6.3 입력 검증
- 페이징 파라미터: page >= 0, 1 <= size <= 100 범위를 검증합니다.
- status 파라미터: OrderStatus enum 값과 정확히 일치하는지 검증합니다.
- sort 파라미터: 허용된 필드(`orderedAt`, `totalAmount`)만 접수합니다.
- direction 파라미터: `ASC` 또는 `DESC`만 허용합니다.

---

## Part 7: 검증 명령어

```bash
# 전체 테스트 실행
./gradlew :apps:commerce-api:test

# ktlint 검사
./gradlew :apps:commerce-api:ktlintCheck

# 빌드
./gradlew :apps:commerce-api:build

# 테스트 커버리지 리포트
./gradlew :apps:commerce-api:test jacocoTestReport
```

`.http/admin/order/list-orders.http`와 `.http/admin/order/get-order-detail.http`로 수동 API 테스트가 가능합니다.

---

## 품질 체크리스트
- [x] 상품/브랜드/좋아요/주문 등 관련 도메인과의 관계가 명시되어 있는가?
  - 주문(직접 대상), 유저(주문자 정보), 상품(스냅샷), 브랜드(스냅샷) 관계를 "관련 도메인" 섹션에 명시함
- [x] 기능 요구사항이 유저 중심("유저가 ~한다")으로 서술되어 있는가?
  - "어드민이 전체 사용자의 주문을 조회한다", "어드민이 주문 상세를 확인한다" 형태로 서술함
- [x] 인증 방식(헤더 기반)이 정확히 명시되어 있는가?
  - `X-Loopers-Ldap: loopers.admin` 헤더 기반 인증을 Part 2.1, Part 6.1에 명시함
- [x] 에러 케이스와 예외 상황이 포함되어 있는가?
  - LDAP 인증 실패(401), 유효하지 않은 파라미터(400), 존재하지 않는 주문(404) 케이스를 Part 1에 명시함
- [x] Phase별 구현 체크리스트가 TDD 워크플로우에 맞게 구성되어 있는가?
  - Phase 1~4로 분리하고 각 Phase에 단위 테스트/통합 테스트 항목을 포함함
