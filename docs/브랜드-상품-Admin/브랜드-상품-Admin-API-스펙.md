# 브랜드 & 상품 Admin API 스펙

브랜드 & 상품 Admin 요구사항 문서(`docs/브랜드-상품-Admin/01-requirements.md`) 작성 시 참고하는 API 명세입니다.
모든 어드민 API는 `X-Loopers-Ldap: loopers.admin` 헤더가 필수입니다.

## API 엔드포인트 목록

### 브랜드 관리

| METHOD | URI | LDAP 필수 | 설명 |
|--------|-----|-----------|------|
| GET | `/api-admin/v1/brands?page=0&size=20` | O | 등록된 브랜드 목록 조회 |
| GET | `/api-admin/v1/brands/{brandId}` | O | 브랜드 상세 조회 |
| POST | `/api-admin/v1/brands` | O | 브랜드 등록 |
| PUT | `/api-admin/v1/brands/{brandId}` | O | 브랜드 정보 수정 |
| DELETE | `/api-admin/v1/brands/{brandId}` | O | 브랜드 삭제 |

### 상품 관리

| METHOD | URI | LDAP 필수 | 설명 |
|--------|-----|-----------|------|
| GET | `/api-admin/v1/products?page=0&size=20&brandId={brandId}` | O | 등록된 상품 목록 조회 |
| GET | `/api-admin/v1/products/{productId}` | O | 상품 상세 조회 |
| POST | `/api-admin/v1/products` | O | 상품 등록 |
| PUT | `/api-admin/v1/products/{productId}` | O | 상품 정보 수정 |
| DELETE | `/api-admin/v1/products/{productId}` | O | 상품 삭제 |

## 상세 스펙

### 브랜드

#### 1. 브랜드 목록 조회

- **METHOD**: `GET`
- **URI**: `/api-admin/v1/brands`
- **쿼리 파라미터**: `page` (기본값 0), `size` (기본값 20)
- **설명**: 등록된 브랜드 목록을 페이징하여 조회합니다.

#### 2. 브랜드 상세 조회

- **METHOD**: `GET`
- **URI**: `/api-admin/v1/brands/{brandId}`
- **설명**: 특정 브랜드의 상세 정보를 조회합니다.

#### 3. 브랜드 등록

- **METHOD**: `POST`
- **URI**: `/api-admin/v1/brands`
- **설명**: 새로운 브랜드를 등록합니다.

#### 4. 브랜드 정보 수정

- **METHOD**: `PUT`
- **URI**: `/api-admin/v1/brands/{brandId}`
- **설명**: 기존 브랜드의 정보를 수정합니다.

#### 5. 브랜드 삭제

- **METHOD**: `DELETE`
- **URI**: `/api-admin/v1/brands/{brandId}`
- **설명**: 브랜드를 삭제합니다. 브랜드 삭제 시 해당 브랜드에 속한 상품들도 함께 삭제되어야 합니다.

### 상품

#### 6. 상품 목록 조회

- **METHOD**: `GET`
- **URI**: `/api-admin/v1/products`
- **쿼리 파라미터**: `page` (기본값 0), `size` (기본값 20), `brandId` (선택, 브랜드별 필터링)
- **설명**: 등록된 상품 목록을 페이징하여 조회합니다. brandId로 특정 브랜드의 상품만 필터링할 수 있습니다.

#### 7. 상품 상세 조회

- **METHOD**: `GET`
- **URI**: `/api-admin/v1/products/{productId}`
- **설명**: 특정 상품의 상세 정보를 조회합니다.

#### 8. 상품 등록

- **METHOD**: `POST`
- **URI**: `/api-admin/v1/products`
- **설명**: 새로운 상품을 등록합니다. 상품의 브랜드는 이미 등록된 브랜드여야 합니다.

#### 9. 상품 정보 수정

- **METHOD**: `PUT`
- **URI**: `/api-admin/v1/products/{productId}`
- **설명**: 기존 상품의 정보를 수정합니다. 상품의 브랜드는 수정할 수 없습니다.

#### 10. 상품 삭제

- **METHOD**: `DELETE`
- **URI**: `/api-admin/v1/products/{productId}`
- **설명**: 상품을 삭제합니다.

## 비즈니스 규칙

| 규칙 | 설명 |
|------|------|
| 브랜드 삭제 시 연쇄 삭제 | 브랜드를 삭제하면 해당 브랜드에 속한 모든 상품도 함께 삭제됩니다 |
| 상품 등록 시 브랜드 검증 | 상품 등록 시 지정한 브랜드가 실제로 존재하는 브랜드여야 합니다 |
| 상품 브랜드 변경 불가 | 상품 수정 시 브랜드를 다른 브랜드로 변경할 수 없습니다 |

## 고객 vs 어드민 제공 정보 비교

요구사항 문서 작성 시 고객과 어드민에게 제공하는 정보의 범위가 다를 수 있습니다.

### 브랜드 정보

| 필드 | 고객 API | 어드민 API | 비고 |
|------|----------|------------|------|
| 브랜드 ID | O | O | |
| 브랜드명 | O | O | |
| 브랜드 설명 | O | O | |
| 등록일시 | - | O | 어드민 관리 목적 |
| 수정일시 | - | O | 어드민 관리 목적 |
| 상품 수 | - | O | 어드민이 브랜드 현황을 파악하는 데 활용 |

### 상품 정보

| 필드 | 고객 API | 어드민 API | 비고 |
|------|----------|------------|------|
| 상품 ID | O | O | |
| 상품명 | O | O | |
| 가격 | O | O | |
| 상품 설명 | O | O | |
| 브랜드 정보 | O | O | |
| 좋아요 수 | O | - | 고객에게 인기도 지표로 제공 |
| 등록일시 | - | O | 어드민 관리 목적 |
| 수정일시 | - | O | 어드민 관리 목적 |
| 재고/판매 상태 | - | O | 어드민이 상품 운영 상태를 관리 |
