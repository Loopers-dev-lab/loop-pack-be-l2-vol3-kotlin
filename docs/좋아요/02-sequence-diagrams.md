# 좋아요 시퀀스 다이어그램

## 개요

이 문서는 좋아요 기능의 3개 API 엔드포인트에 대한 시퀀스 다이어그램을 정의합니다.

| METHOD | URI | 설명 |
|--------|-----|------|
| POST | `/api/v1/products/{productId}/likes` | 상품 좋아요 등록 |
| DELETE | `/api/v1/products/{productId}/likes` | 상품 좋아요 취소 |
| GET | `/api/v1/users/{userId}/likes` | 내가 좋아요한 상품 목록 조회 |

---

## 1. 상품 좋아요 등록 - 성공 흐름

### 1-1. 신규 좋아요 등록

유저가 아직 좋아요하지 않은 상품에 좋아요를 등록하는 경우입니다.

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Filter as HeaderAuthFilter
    participant Controller as LikeV1Controller
    participant Facade as LikeFacade
    participant MemberService as MemberService
    participant ProductService as ProductService
    participant LikeService as LikeService
    participant LikeRepo as LikeRepository
    participant DB as MySQL

    Client->>Filter: POST /api/v1/products/{productId}/likes
    Note over Client,Filter: X-Loopers-LoginId, X-Loopers-LoginPw 헤더 포함

    Filter->>MemberService: findByLoginId(loginId)
    MemberService-->>Filter: MemberModel
    Filter->>Filter: BCryptPasswordEncoder.matches(loginPw, member.password)
    Filter->>Controller: 인증된 요청 전달 (userId를 요청 속성에 설정)

    Controller->>Facade: likeProduct(userId, productId)
    Facade->>ProductService: findById(productId)
    ProductService-->>Facade: ProductModel (deletedAt == null 확인)

    Facade->>LikeService: like(userId, productId)
    LikeService->>LikeRepo: findByUserIdAndProductId(userId, productId)
    LikeRepo->>DB: SELECT * FROM `like` WHERE user_id = ? AND product_id = ?
    DB-->>LikeRepo: null (기록 없음)
    LikeRepo-->>LikeService: null

    LikeService->>LikeService: LikeModel(userId, productId) 생성
    LikeService->>LikeRepo: save(likeModel)
    LikeRepo->>DB: INSERT INTO `like` (user_id, product_id, ...) VALUES (?, ?, ...)
    DB-->>LikeRepo: 저장 완료
    LikeRepo-->>LikeService: LikeModel

    LikeService-->>Facade: Unit
    Facade-->>Controller: Unit
    Controller-->>Client: ApiResponse(SUCCESS, null) - 200 OK
```

### 1-2. 삭제된 좋아요 복원 (재등록)

유저가 이전에 좋아요를 취소한 상품에 다시 좋아요를 등록하는 경우입니다.

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Filter as HeaderAuthFilter
    participant Controller as LikeV1Controller
    participant Facade as LikeFacade
    participant ProductService as ProductService
    participant LikeService as LikeService
    participant LikeRepo as LikeRepository
    participant DB as MySQL

    Client->>Filter: POST /api/v1/products/{productId}/likes
    Filter->>Controller: 인증된 요청 전달 (인증 과정 생략)

    Controller->>Facade: likeProduct(userId, productId)
    Facade->>ProductService: findById(productId)
    ProductService-->>Facade: ProductModel (존재 확인 완료)

    Facade->>LikeService: like(userId, productId)
    LikeService->>LikeRepo: findByUserIdAndProductId(userId, productId)
    LikeRepo->>DB: SELECT * FROM `like` WHERE user_id = ? AND product_id = ?
    DB-->>LikeRepo: LikeModel (deletedAt != null)
    LikeRepo-->>LikeService: LikeModel (삭제된 상태)

    LikeService->>LikeService: likeModel.restore() - deletedAt을 null로 설정
    LikeService->>LikeRepo: save(likeModel)
    LikeRepo->>DB: UPDATE `like` SET deleted_at = NULL, updated_at = ? WHERE id = ?
    DB-->>LikeRepo: 업데이트 완료
    LikeRepo-->>LikeService: LikeModel

    LikeService-->>Facade: Unit
    Facade-->>Controller: Unit
    Controller-->>Client: ApiResponse(SUCCESS, null) - 200 OK
```

### 1-3. 이미 활성화된 좋아요 (멱등 처리)

유저가 이미 좋아요한 상품에 다시 좋아요를 요청하는 경우입니다.

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Filter as HeaderAuthFilter
    participant Controller as LikeV1Controller
    participant Facade as LikeFacade
    participant ProductService as ProductService
    participant LikeService as LikeService
    participant LikeRepo as LikeRepository
    participant DB as MySQL

    Client->>Filter: POST /api/v1/products/{productId}/likes
    Filter->>Controller: 인증된 요청 전달 (인증 과정 생략)

    Controller->>Facade: likeProduct(userId, productId)
    Facade->>ProductService: findById(productId)
    ProductService-->>Facade: ProductModel (존재 확인 완료)

    Facade->>LikeService: like(userId, productId)
    LikeService->>LikeRepo: findByUserIdAndProductId(userId, productId)
    LikeRepo->>DB: SELECT * FROM `like` WHERE user_id = ? AND product_id = ?
    DB-->>LikeRepo: LikeModel (deletedAt == null)
    LikeRepo-->>LikeService: LikeModel (활성 상태)

    Note over LikeService: 이미 활성 상태이므로 아무 작업 없이 반환

    LikeService-->>Facade: Unit
    Facade-->>Controller: Unit
    Controller-->>Client: ApiResponse(SUCCESS, null) - 200 OK
```

### 흐름 설명

| 단계 | 책임 객체 | 수행 내용 |
|------|----------|----------|
| 1 | HeaderAuthFilter | `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더에서 로그인 정보를 추출한다 |
| 2 | HeaderAuthFilter | MemberService를 통해 유저 존재 확인 및 비밀번호 검증을 수행한다 |
| 3 | LikeFacade | ProductService를 통해 상품이 존재하고 삭제되지 않았는지 검증한다 |
| 4 | LikeService | 기존 좋아요 기록을 조회하여 상태에 따라 분기 처리한다 |
| 5 | LikeService | 기록 없음: 새 LikeModel을 생성하여 저장한다 |
| 5 | LikeService | 삭제된 기록: restore()를 호출하여 deletedAt을 null로 복원한다 |
| 5 | LikeService | 활성 기록: 추가 작업 없이 반환한다 (멱등 처리) |

---

## 2. 상품 좋아요 등록 - 에러 흐름

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Filter as HeaderAuthFilter
    participant Controller as LikeV1Controller
    participant Facade as LikeFacade
    participant MemberService as MemberService
    participant ProductService as ProductService

    Client->>Filter: POST /api/v1/products/{productId}/likes

    alt 인증 헤더 누락
        Filter-->>Client: ApiResponse(FAIL, UNAUTHORIZED, "인증이 필요합니다.") - 401
    else 유저가 존재하지 않음
        Filter->>MemberService: findByLoginId(loginId)
        MemberService-->>Filter: null
        Filter-->>Client: ApiResponse(FAIL, UNAUTHORIZED, "인증이 필요합니다.") - 401
    else 비밀번호 불일치
        Filter->>MemberService: findByLoginId(loginId)
        MemberService-->>Filter: MemberModel
        Filter->>Filter: BCryptPasswordEncoder.matches(loginPw, member.password) = false
        Filter-->>Client: ApiResponse(FAIL, UNAUTHORIZED, "인증이 필요합니다.") - 401
    else 인증 성공
        Filter->>Controller: 인증된 요청 전달
        Controller->>Facade: likeProduct(userId, productId)

        alt 상품이 존재하지 않음
            Facade->>ProductService: findById(productId)
            ProductService-->>Facade: CoreException(NOT_FOUND, "존재하지 않는 상품입니다.")
            Facade-->>Controller: CoreException(NOT_FOUND)
            Controller-->>Client: ApiResponse(FAIL, "Not Found", "존재하지 않는 상품입니다.") - 404
        else 상품이 삭제된 상태
            Facade->>ProductService: findById(productId)
            ProductService-->>Facade: CoreException(NOT_FOUND, "존재하지 않는 상품입니다.")
            Facade-->>Controller: CoreException(NOT_FOUND)
            Controller-->>Client: ApiResponse(FAIL, "Not Found", "존재하지 않는 상품입니다.") - 404
        end
    end
```

### 에러 시나리오

| 조건 | 발생 시점 | 책임 객체 | 에러 타입 | HTTP 상태 |
|------|----------|----------|----------|----------|
| `X-Loopers-LoginId` 또는 `X-Loopers-LoginPw` 헤더가 누락된 경우 | HeaderAuthFilter | HeaderAuthFilter | UNAUTHORIZED | 401 |
| 헤더의 loginId에 해당하는 유저가 존재하지 않는 경우 | HeaderAuthFilter | MemberService | UNAUTHORIZED | 401 |
| 헤더의 비밀번호가 저장된 비밀번호와 일치하지 않는 경우 | HeaderAuthFilter | HeaderAuthFilter | UNAUTHORIZED | 401 |
| productId에 해당하는 상품이 존재하지 않는 경우 | LikeFacade | ProductService | NOT_FOUND | 404 |
| productId에 해당하는 상품이 삭제된 상태인 경우 | LikeFacade | ProductService | NOT_FOUND | 404 |

---

## 3. 상품 좋아요 취소 - 성공 흐름

### 3-1. 활성 좋아요 취소

유저가 좋아요한 상품의 좋아요를 취소하는 경우입니다.

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Filter as HeaderAuthFilter
    participant Controller as LikeV1Controller
    participant Facade as LikeFacade
    participant LikeService as LikeService
    participant LikeRepo as LikeRepository
    participant DB as MySQL

    Client->>Filter: DELETE /api/v1/products/{productId}/likes
    Note over Client,Filter: X-Loopers-LoginId, X-Loopers-LoginPw 헤더 포함

    Filter->>Controller: 인증된 요청 전달 (인증 과정 생략)

    Controller->>Facade: unlikeProduct(userId, productId)
    Facade->>LikeService: unlike(userId, productId)
    LikeService->>LikeRepo: findByUserIdAndProductId(userId, productId)
    LikeRepo->>DB: SELECT * FROM `like` WHERE user_id = ? AND product_id = ?
    DB-->>LikeRepo: LikeModel (deletedAt == null)
    LikeRepo-->>LikeService: LikeModel (활성 상태)

    LikeService->>LikeService: likeModel.delete() - deletedAt을 현재 시각으로 설정
    LikeService->>LikeRepo: save(likeModel)
    LikeRepo->>DB: UPDATE `like` SET deleted_at = ?, updated_at = ? WHERE id = ?
    DB-->>LikeRepo: 업데이트 완료
    LikeRepo-->>LikeService: LikeModel

    LikeService-->>Facade: Unit
    Facade-->>Controller: Unit
    Controller-->>Client: ApiResponse(SUCCESS, null) - 200 OK
```

### 3-2. 좋아요 기록이 없는 상품 취소 (멱등 처리)

유저가 좋아요하지 않은 상품에 취소를 요청하는 경우입니다.

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Filter as HeaderAuthFilter
    participant Controller as LikeV1Controller
    participant Facade as LikeFacade
    participant LikeService as LikeService
    participant LikeRepo as LikeRepository
    participant DB as MySQL

    Client->>Filter: DELETE /api/v1/products/{productId}/likes
    Filter->>Controller: 인증된 요청 전달 (인증 과정 생략)

    Controller->>Facade: unlikeProduct(userId, productId)
    Facade->>LikeService: unlike(userId, productId)
    LikeService->>LikeRepo: findByUserIdAndProductId(userId, productId)
    LikeRepo->>DB: SELECT * FROM `like` WHERE user_id = ? AND product_id = ?
    DB-->>LikeRepo: null (기록 없음)
    LikeRepo-->>LikeService: null

    Note over LikeService: 기록이 없으므로 아무 작업 없이 반환

    LikeService-->>Facade: Unit
    Facade-->>Controller: Unit
    Controller-->>Client: ApiResponse(SUCCESS, null) - 200 OK
```

### 흐름 설명

| 단계 | 책임 객체 | 수행 내용 |
|------|----------|----------|
| 1 | HeaderAuthFilter | `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더로 유저를 인증한다 |
| 2 | LikeFacade | 좋아요 취소 요청을 LikeService에 위임한다 (상품 존재 검증 불필요) |
| 3 | LikeService | 기존 좋아요 기록을 조회하여 상태에 따라 분기 처리한다 |
| 4 | LikeService | 활성 기록: delete()를 호출하여 deletedAt을 현재 시각으로 설정한다 |
| 4 | LikeService | 기록 없음 또는 이미 삭제: 추가 작업 없이 반환한다 (멱등 처리) |

---

## 4. 상품 좋아요 취소 - 에러 흐름

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Filter as HeaderAuthFilter
    participant MemberService as MemberService

    Client->>Filter: DELETE /api/v1/products/{productId}/likes

    alt 인증 헤더 누락
        Filter-->>Client: ApiResponse(FAIL, UNAUTHORIZED, "인증이 필요합니다.") - 401
    else 유저가 존재하지 않음
        Filter->>MemberService: findByLoginId(loginId)
        MemberService-->>Filter: null
        Filter-->>Client: ApiResponse(FAIL, UNAUTHORIZED, "인증이 필요합니다.") - 401
    else 비밀번호 불일치
        Filter->>MemberService: findByLoginId(loginId)
        MemberService-->>Filter: MemberModel
        Filter->>Filter: BCryptPasswordEncoder.matches(loginPw, member.password) = false
        Filter-->>Client: ApiResponse(FAIL, UNAUTHORIZED, "인증이 필요합니다.") - 401
    end
```

### 에러 시나리오

| 조건 | 발생 시점 | 책임 객체 | 에러 타입 | HTTP 상태 |
|------|----------|----------|----------|----------|
| `X-Loopers-LoginId` 또는 `X-Loopers-LoginPw` 헤더가 누락된 경우 | HeaderAuthFilter | HeaderAuthFilter | UNAUTHORIZED | 401 |
| 헤더의 loginId에 해당하는 유저가 존재하지 않는 경우 | HeaderAuthFilter | MemberService | UNAUTHORIZED | 401 |
| 헤더의 비밀번호가 저장된 비밀번호와 일치하지 않는 경우 | HeaderAuthFilter | HeaderAuthFilter | UNAUTHORIZED | 401 |

> 좋아요 취소는 멱등성을 보장하므로, 인증 이후 단계에서는 에러가 발생하지 않습니다.
> 좋아요 기록이 없거나 이미 삭제된 경우에도 200 OK를 반환합니다.

---

## 5. 내가 좋아요한 상품 목록 조회 - 성공 흐름

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Filter as HeaderAuthFilter
    participant Controller as LikeV1Controller
    participant Facade as LikeFacade
    participant LikeService as LikeService
    participant LikeRepo as LikeRepository
    participant DB as MySQL

    Client->>Filter: GET /api/v1/users/{userId}/likes?page=0&size=20
    Note over Client,Filter: X-Loopers-LoginId, X-Loopers-LoginPw 헤더 포함

    Filter->>Controller: 인증된 요청 전달 (userId를 요청 속성에 설정)

    Controller->>Controller: 경로의 userId와 인증된 유저의 id 일치 여부 검증
    Controller->>Facade: getMyLikes(userId, page, size)

    Facade->>LikeService: findByUserId(userId, Pageable(page, size))
    LikeService->>LikeRepo: findByUserIdAndDeletedAtIsNull(userId, pageable)
    LikeRepo->>DB: SELECT l.*, p.*, b.* FROM `like` l<br/>JOIN product p ON l.product_id = p.id<br/>JOIN brand b ON p.brand_id = b.id<br/>WHERE l.user_id = ? AND l.deleted_at IS NULL<br/>ORDER BY l.created_at DESC<br/>LIMIT ? OFFSET ?
    DB-->>LikeRepo: Page<LikeModel> (상품 및 브랜드 정보 포함)
    LikeRepo-->>LikeService: Page<LikeModel>

    LikeService-->>Facade: Page<LikeModel>
    Facade->>Facade: LikeInfo 목록으로 변환 (product.deletedAt 여부로 isDeleted 설정)
    Facade-->>Controller: LikeInfo 페이지 정보
    Controller->>Controller: LikeV1Dto.LikeListResponse로 변환
    Controller-->>Client: ApiResponse(SUCCESS, LikeListResponse) - 200 OK
```

### 흐름 설명

| 단계 | 책임 객체 | 수행 내용 |
|------|----------|----------|
| 1 | HeaderAuthFilter | `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더로 유저를 인증한다 |
| 2 | LikeV1Controller | 경로의 `userId`와 인증된 유저의 `id`가 일치하는지 검증한다 |
| 3 | LikeFacade | LikeService에 페이징된 좋아요 목록 조회를 위임한다 |
| 4 | LikeService | 삭제되지 않은 좋아요 기록을 최신순으로 페이징하여 조회한다 |
| 5 | LikeRepository | 상품, 브랜드 정보를 JOIN하여 함께 조회한다 (삭제된 상품도 포함) |
| 6 | LikeFacade | 조회 결과를 LikeInfo로 변환하며, 상품의 deletedAt 여부를 isDeleted로 매핑한다 |
| 7 | LikeV1Controller | LikeInfo를 LikeV1Dto.LikeListResponse로 변환하여 응답한다 |

---

## 6. 내가 좋아요한 상품 목록 조회 - 에러 흐름

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Filter as HeaderAuthFilter
    participant Controller as LikeV1Controller
    participant MemberService as MemberService

    Client->>Filter: GET /api/v1/users/{userId}/likes?page=0&size=20

    alt 인증 헤더 누락
        Filter-->>Client: ApiResponse(FAIL, UNAUTHORIZED, "인증이 필요합니다.") - 401
    else 유저가 존재하지 않음
        Filter->>MemberService: findByLoginId(loginId)
        MemberService-->>Filter: null
        Filter-->>Client: ApiResponse(FAIL, UNAUTHORIZED, "인증이 필요합니다.") - 401
    else 비밀번호 불일치
        Filter->>MemberService: findByLoginId(loginId)
        MemberService-->>Filter: MemberModel
        Filter->>Filter: BCryptPasswordEncoder.matches(loginPw, member.password) = false
        Filter-->>Client: ApiResponse(FAIL, UNAUTHORIZED, "인증이 필요합니다.") - 401
    else 인증 성공
        Filter->>Controller: 인증된 요청 전달

        alt 경로의 userId가 인증된 유저의 id와 다른 경우
            Controller->>Controller: userId != authenticatedUser.id
            Controller-->>Client: ApiResponse(FAIL, UNAUTHORIZED, "본인의 좋아요 목록만 조회할 수 있습니다.") - 401
        end
    end
```

### 에러 시나리오

| 조건 | 발생 시점 | 책임 객체 | 에러 타입 | HTTP 상태 |
|------|----------|----------|----------|----------|
| `X-Loopers-LoginId` 또는 `X-Loopers-LoginPw` 헤더가 누락된 경우 | HeaderAuthFilter | HeaderAuthFilter | UNAUTHORIZED | 401 |
| 헤더의 loginId에 해당하는 유저가 존재하지 않는 경우 | HeaderAuthFilter | MemberService | UNAUTHORIZED | 401 |
| 헤더의 비밀번호가 저장된 비밀번호와 일치하지 않는 경우 | HeaderAuthFilter | HeaderAuthFilter | UNAUTHORIZED | 401 |
| 경로의 `userId`가 인증된 유저의 `id`와 일치하지 않는 경우 | LikeV1Controller | LikeV1Controller | UNAUTHORIZED | 401 |

---

## 품질 체크리스트

- [x] 각 participant의 책임(검증, 변환, 조회, 저장 등)이 메서드명으로 명확히 드러나는가? - HeaderAuthFilter(인증), LikeFacade(오케스트레이션/상품검증), LikeService(좋아요 비즈니스 로직), ProductService(상품 존재 검증)로 분리
- [x] 여러 도메인이 관련된 경우, 각 도메인의 Service가 별도 participant로 분리되어 있는가? - MemberService, ProductService, LikeService를 별도 participant로 분리
- [x] 인증 방식(헤더 기반)이 다이어그램에 정확히 반영되어 있는가? - `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 기반 인증을 HeaderAuthFilter에서 처리
- [x] 성공 흐름과 에러 흐름이 모두 포함되어 있는가? - 3개 API 모두 성공/에러 흐름을 포함 (총 6개 섹션)
- [x] 에러 시나리오 테이블에 발생 시점과 책임 객체가 명시되어 있는가? - 각 에러 시나리오 테이블에 발생 시점, 책임 객체, 에러 타입, HTTP 상태를 명시
