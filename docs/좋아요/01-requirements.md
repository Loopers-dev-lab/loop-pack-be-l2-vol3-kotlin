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
