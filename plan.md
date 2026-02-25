# Plan: Repository 메서드 설계 개선 — isDeleted/isActive 관심사 분리

## Context

CP0~CP3(Domain Service 평탄화) 완료 후, next-refactor.md에 정리된 설계 개선 착수.
현재 `isDeleted`(DB 관심사)와 `isActive`(앱 관심사)가 혼재되어 있다:
- `Product.isActive()` = `!isDeleted() && status != HIDDEN` → isDeleted 중복 검증
- `findById()`가 DB 레벨에서 `deletedAt IS NULL` 필터링 → UseCase가 삭제 상태를 인지 불가
- `findByIdIncludeDeleted`가 별도 메서드로 존재 → 불필요한 인터페이스 복잡성

**목표**: 단건 조회는 DB 필터링 없이 UseCase에서 판단. 다건 조회(페이징)만 DB 필터링 유지.

## 결정 사항

- `isAvailableForOrder()`에서 `!isDeleted()` 제거 (UseCase에서 이미 검증)
- `existsByLoginId`의 deletedAt 필터 제거 (삭제된 사용자의 loginId로 재가입 불가)

---

## CP1: Product 도메인 모델 관심사 분리

`isActive()`, `isAvailableForOrder()`에서 `!isDeleted()` 제거.

**수정 파일:**
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/catalog/product/model/Product.kt`
- `apps/commerce-api/src/test/kotlin/com/loopers/domain/catalog/product/model/ProductTest.kt` (또는 관련 테스트)

**작업:**
- [ ] [RED] Product 단위 테스트: deletedAt 설정 + ON_SALE → `isActive() == true`, `isAvailableForOrder() == true`
- [ ] [GREEN] `isActive()` → `status != ProductStatus.HIDDEN`
- [ ] [GREEN] `isAvailableForOrder()` → `status == ProductStatus.ON_SALE`
- [ ] 기존 테스트 중 isActive/isAvailableForOrder가 isDeleted를 포함한다고 가정한 케이스 수정

---

## CP2: Product 단건 조회 리팩토링

### CP2-1: findById deletedAt 필터 제거

**수정 파일:**
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/catalog/product/ProductRepositoryImpl.kt`
- `apps/commerce-api/src/test/kotlin/.../FakeProductRepository.kt` (테스트 Fake)

**작업:**
- [ ] FakeProductRepository.findById에서 deletedAt 필터 제거
- [ ] ProductRepositoryImpl.findById: `findByIdAndDeletedAtIsNull` → `findById` (JPA 기본)
- [ ] ProductRepositoryImpl.findByIdForUpdate: 동일하게 필터 제거

### CP2-2: UseCase에 상태 검증 추가

**수정 파일:**
- `apps/commerce-api/src/main/kotlin/com/loopers/application/catalog/product/GetProductUseCase.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/application/catalog/product/DeleteProductUseCase.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/application/like/AddLikeUseCase.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/application/like/RemoveLikeUseCase.kt`
- 각 UseCase의 테스트 파일 (LikeUseCaseTest 포함)

**작업:**
- [ ] [RED] GetProductUseCase: 삭제된 상품 조회 → NOT_FOUND
- [ ] [GREEN] `product.isDeleted()` 검증 추가
- [ ] [RED] DeleteProductUseCase: 이미 삭제된 상품 삭제 시도 → NOT_FOUND
- [ ] [GREEN] `product.isDeleted()` 검증 추가
- [ ] [RED] AddLikeUseCase: 삭제된 상품 좋아요 → 에러 (throw)
- [ ] [GREEN] 첫 번째 `findById` 결과에 `product.isDeleted()` 검증 추가
  - 분리 유지: findById(검증) → findByIdForUpdate(likeCount 변경). 락 점유 최소화.
  - 두 번째 findByIdForUpdate 결과에도 `isDeleted()` 검증 추가 (Race Condition 방어: findById~findByIdForUpdate 사이 다른 트랜잭션의 soft delete 대응)
- [ ] [RED] RemoveLikeUseCase: 삭제된 상품의 좋아요 취소 시 likeCount 변경하지 않음 (멱등, throw 아님)
- [ ] [GREEN] `findByIdForUpdate` 결과에 `isDeleted() || !isActive()` 가드 추가
  - 현재: `findByIdForUpdate(id)?.let { ... }` — 필터 제거 후 삭제 상품도 반환되어 likeCount 감소
  - 변경: `findByIdForUpdate(id)?.takeIf { !it.isDeleted() && it.isActive() }?.let { ... }`
  - 에러 없이 조용히 return (멱등 설계)
- [ ] LikeUseCaseTest에서 `findByIdIncludeDeleted` → `findById`로 전환 (CP2-3과 연동)

### CP2-3: findByIdIncludeDeleted 제거

**수정 파일:**
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/catalog/product/repository/ProductRepository.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/catalog/product/ProductRepositoryImpl.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/application/catalog/product/GetProductAdminUseCase.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/application/catalog/product/UpdateProductUseCase.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/application/catalog/product/RestoreProductUseCase.kt`
- FakeProductRepository, 관련 테스트

**작업:**
- [ ] Admin/Restore UseCase에서 `findByIdIncludeDeleted()` → `findById()` 전환
- [ ] ProductRepository 인터페이스에서 `findByIdIncludeDeleted` 제거
- [ ] ProductRepositoryImpl, FakeProductRepository에서 구현 제거
- [ ] 관련 테스트 업데이트

---

## CP3: Brand 단건 조회 리팩토링

CP2와 동일 패턴.

### CP3-1: findById deletedAt 필터 제거

**수정 파일:**
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/catalog/brand/BrandRepositoryImpl.kt`
- FakeBrandRepository

**작업:**
- [ ] FakeBrandRepository.findById에서 deletedAt 필터 제거
- [ ] BrandRepositoryImpl.findById: `findByIdAndDeletedAtIsNull` → `findById`

### CP3-2: 대고객 UseCase에 상태 검증 추가

**수정 파일:**
- `apps/commerce-api/src/main/kotlin/com/loopers/application/catalog/brand/GetBrandUseCase.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/application/catalog/brand/DeleteBrandUseCase.kt`
- 각 UseCase의 테스트 파일

**작업:**
- [ ] [RED] GetBrandUseCase: 삭제된 브랜드 조회 → NOT_FOUND
- [ ] [GREEN] `brand.isDeleted()` 검증 추가
- [ ] [RED] DeleteBrandUseCase: 이미 삭제된 브랜드 삭제 시도 → NOT_FOUND
- [ ] [GREEN] `brand.isDeleted()` 검증 추가

### CP3-3: findByIdIncludeDeleted 제거

**수정 파일:**
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/catalog/brand/repository/BrandRepository.kt`
- BrandRepositoryImpl, FakeBrandRepository
- GetBrandAdminUseCase, UpdateBrandUseCase, RestoreBrandUseCase + 테스트

**작업:**
- [ ] Admin/Restore UseCase에서 `findByIdIncludeDeleted()` → `findById()` 전환
- [ ] BrandRepository 인터페이스에서 `findByIdIncludeDeleted` 제거
- [ ] 구현체/Fake에서 제거, 관련 테스트 업데이트

---

## CP4: Order/User 단건 조회 리팩토링

### CP4-1: Order

**수정 파일:**
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/order/OrderRepositoryImpl.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/application/order/GetOrderUseCase.kt`
- FakeOrderRepository, 관련 테스트

**작업:**
- [ ] OrderRepositoryImpl.findById에서 deletedAt 필터 제거
- [ ] FakeOrderRepository.findById 필터 제거
- [ ] [RED] GetOrderUseCase: 삭제된 주문 조회 → NOT_FOUND
- [ ] [GREEN] `order.isDeleted()` 검증 추가
- [ ] GetOrderAdminUseCase: 삭제된 주문도 조회 가능 → 검증 불필요

### CP4-2: User

**수정 파일:**
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/user/UserRepositoryImpl.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/application/user/GetUserInfoUseCase.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/application/user/AuthenticateUserUseCase.kt` (또는 인증 관련)
- FakeUserRepository, 관련 테스트

**작업:**
- [ ] UserRepositoryImpl.findById에서 deletedAt 필터 제거
- [ ] UserRepositoryImpl.findByLoginId에서 deletedAt 필터 제거
- [ ] existsByLoginId에서도 deletedAt 필터 제거
- [ ] FakeUserRepository 필터 제거 (findById, findByLoginId, existsByLoginId)
- [ ] [RED] AuthenticateUserUseCase: 삭제된 사용자 로그인 → 에러
- [ ] [GREEN] `user.isDeleted()` 검증 추가
- [ ] [RED] GetUserInfoUseCase: 삭제된 사용자 조회 → 에러
- [ ] [GREEN] `user.isDeleted()` 검증 추가

---

## CP5: Repository 메서드 순서 정리

구조적 변경만 (Tidy First). 동작 변경 없음.

**작업:**
- [ ] ProductRepository / Impl: 공용 → 단건 조회 → 다건 조회 → 내부용
- [ ] BrandRepository / Impl: 공용 → 단건 조회 → 다건 조회
- [ ] OrderRepository / Impl: 공용 → 단건 조회 → 다건 조회
- [ ] UserRepository / Impl: 공용 → 단건 조회 → 존재 확인
- [ ] Fake Repository들도 동일 순서

---

## CP6: 문서 업데이트

- [ ] next-refactor.md TODO 완료 처리 또는 정리
- [ ] Domain CLAUDE.md에 isDeleted/isActive 관심사 분리 원칙 반영
- [ ] Infrastructure CLAUDE.md에 단건/다건 필터링 정책 반영

---

## 커밋 전략 (Tidy First)

| 순서 | 타입 | 내용 |
|------|------|------|
| 1 | refactor | CP1: Product.isActive() 관심사 분리 |
| 2 | feat | CP2: Product 단건 조회 리팩토링 |
| 3 | feat | CP3: Brand 단건 조회 리팩토링 |
| 4 | feat | CP4: Order/User 단건 조회 리팩토링 |
| 5 | refactor | CP5: Repository 메서드 순서 정리 |
| 6 | docs | CP6: 문서 업데이트 |

## 검증

각 CP 완료 후:
```bash
./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test
```
