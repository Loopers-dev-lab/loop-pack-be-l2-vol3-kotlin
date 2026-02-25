# 아키텍처 감사 후속 리팩토링 Plan

## Context

Round 3 리팩토링 완료 후 5개 레이어별 병렬 감사 수행. 코드 의존 방향 위반 0건이나, 문서-코드 불일치(6건), soft delete 필터링 누락, 중복 검증, 네이밍 불일치 등 총 56건 발견. 개발자와
논의 후 수정 방향 전부 확정.

## 변경 범위 요약

- **문서**: 3개 CLAUDE.md + architecture-compliance.md
- **구조적 리팩토링**: StatusDto, CatalogCommand, UseCase 네이밍, 확장함수 추출
- **Soft delete**: Brand/Product/Order/User Repository 필터링 적용
- **행위 수정**: existsBy, @Transactional 이동, 중복 검증 제거, likeCount Domain Model 경유

---

## CP0: 문서 수정

**커밋**: `docs: 아키텍처 감사 결과 — 문서-코드 불일치 정정`

| 파일                                         | 변경                                                                                                                           |
|--------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `.claude/rules/architecture-compliance.md` | L24: "Facade" → "UseCase". L25: "UserService" → "AuthenticateUserUseCase". L30: "Domain Service에 위치" → "Domain Model 내부에 위치" |
| `interfaces/CLAUDE.md`                     | L23: "UserService를 직접 호출" → "AuthenticateUserUseCase를 호출". L15: "Domain Model 변환" → "Info DTO(Application) 변환"               |
| `domain/CLAUDE.md`                         | L16: "검증 없이 DB 데이터로 복원" → "생성 로직(파생값 계산) 없이 DB 데이터로 복원"                                                                      |

---

## CP1: 구조적 리팩토링 (Tidy First — 기존 테스트 전부 통과 유지)

### CP1-1: StatusDto → String

**커밋**: `refactor: Interfaces DTO에서 StatusDto enum 제거, String 타입 통일`

| 파일                                                | 변경                                                                            |
|---------------------------------------------------|-------------------------------------------------------------------------------|
| `interfaces/api/product/dto/ProductV1Dto.kt`      | `ProductStatusDto` enum 제거, status 필드 String, `from()` 에서 `info.status` 직접 사용 |
| `interfaces/api/product/dto/ProductAdminV1Dto.kt` | 동일                                                                            |
| `interfaces/api/like/dto/LikeV1Dto.kt`            | 동일                                                                            |
| `interfaces/api/order/dto/OrderV1Dto.kt`          | `OrderStatusDto`/`OrderItemStatusDto` 제거, status String                       |
| `interfaces/api/order/dto/OrderAdminV1Dto.kt`     | 동일                                                                            |

### CP1-2: CatalogCommand 원시 타입

**커밋**: `refactor: CatalogCommand를 원시 타입으로 전환, UseCase에서 VO 생성`

| 파일                                                    | 변경                                                                                                                                                                                                                                   |
|-------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `application/catalog/CatalogCommand.kt`               | `CreateBrand(name: BrandName)` → `(name: String)`. `UpdateBrand` 동일. `CreateProduct(price: Money)` → `(price: BigDecimal)`. `UpdateProduct(price: Money?, status: Product.ProductStatus?)` → `(price: BigDecimal?, status: String?)` |
| `application/catalog/brand/CreateBrandUseCase.kt`     | `Brand(name = BrandName(command.name))` — UseCase에서 VO 생성                                                                                                                                                                            |
| `application/catalog/brand/UpdateBrandUseCase.kt`     | `brand.update(BrandName(command.name))`                                                                                                                                                                                              |
| `application/catalog/product/CreateProductUseCase.kt` | `Product(price = Money(command.price))`                                                                                                                                                                                              |
| `application/catalog/product/UpdateProductUseCase.kt` | `command.price?.let { Money(it) }`, `command.status?.let { ProductStatus.valueOf(it) }`                                                                                                                                              |
| 테스트                                                   | `UpdateProductUseCaseTest` 등 Command 생성부 수정                                                                                                                                                                                          |

### CP1-3: UseCase 네이밍

**커밋**: `refactor: GetProduct UseCase 네이밍 교정 — 대고객/어드민 구분 명확화`

실행 순서 (파일명 충돌 방지):

1. `GetProductUseCase.kt` → `GetProductAdminUseCase.kt` (클래스명+파일명, `executeAdmin` → `execute`, BrandRepository 의존 추가, 반환
   `ProductDetailInfo`)
2. `GetProductDetailUseCase.kt` → `GetProductUseCase.kt` (클래스명+파일명)
3. Controller/ApiSpec 참조 업데이트

| 파일                                                                                | 변경                                                                       |
|-----------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| `application/catalog/product/GetProductUseCase.kt` → `GetProductAdminUseCase.kt`  | 클래스명, `execute()` 통일, BrandRepository 추가, `ProductDetailInfo` 반환         |
| `application/catalog/product/GetProductDetailUseCase.kt` → `GetProductUseCase.kt` | 클래스명만 변경                                                                 |
| `interfaces/api/product/ProductAdminV1Controller.kt`                              | `getProductAdminUseCase.execute()`, `AdminProductResponse`에 brandName 추가 |
| `interfaces/api/product/dto/ProductAdminV1Dto.kt`                                 | `AdminProductResponse.from(info: ProductDetailInfo)`, brandName 필드 추가    |
| `interfaces/api/product/ProductV1Controller.kt`                                   | `getProductUseCase: GetProductUseCase`                                   |
| ApiSpec 파일들                                                                       | 참조 업데이트                                                                  |
| 테스트                                                                               | 클래스명/import 변경                                                           |

### CP1-4: findItemsByOrders 추출

**커밋**: `refactor: findItemsByOrders를 공용 함수로 추출`

| 파일                                            | 변경                                                                                                  |
|-----------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `application/order/OrderQuerySupport.kt` (신규) | `fun findItemsByOrders(repo: OrderItemRepository, orders: List<Order>): Map<Long, List<OrderItem>>` |
| `application/order/GetOrdersUseCase.kt`       | private 메서드 제거 → 공용 함수 호출                                                                           |
| `application/order/GetOrdersAdminUseCase.kt`  | 동일                                                                                                  |

---

## CP2: Soft Delete 필터링

### Soft delete 적용 범위 (도메인 모델에 deletedAt 필드가 있는 엔티티만)

| 엔티티          | deletedAt      | delete()/restore() | 적용           |
|--------------|----------------|--------------------|--------------|
| Brand        | O              | O                  | **적용**       |
| Product      | O              | O                  | **적용**       |
| Order        | O              | X (deletedAt 필드만)  | **적용** (방어적) |
| User         | O              | X (deletedAt 필드만)  | **적용** (방어적) |
| UserPoint    | X              | X                  | 미적용          |
| PointHistory | X              | X                  | 미적용          |
| OrderItem    | X              | X                  | 미적용          |
| Like         | BaseEntity 미상속 | hard delete        | 미적용          |

### CP2-1: Repository 인터페이스 확장 (구조적)

**커밋**: `refactor: Restore용 findByIdIncludeDeleted + findByIdForUpdate 추가`

| 파일                                                       | 변경                                                                                        |
|----------------------------------------------------------|-------------------------------------------------------------------------------------------|
| `domain/catalog/brand/repository/BrandRepository.kt`     | `findByIdIncludeDeleted(id: Long): Brand?` 추가                                             |
| `domain/catalog/product/repository/ProductRepository.kt` | `findByIdIncludeDeleted(id: Long): Product?` + `findByIdForUpdate(id: Long): Product?` 추가 |
| Infrastructure 구현체 2개                                    | 구현 (현재 findById와 동일 — 아직 필터링 미적용)                                                         |
| Fake Repository 2개                                       | 구현                                                                                        |

### CP2-2: deletedAt 필터링 적용

**커밋**: `feat: Brand/Product/Order/User Repository에 deletedAt 필터링 전면 적용`

**JpaRepository 메서드 추가 + RepositoryImpl 수정:**

| Repository  | 수정 메서드                                                                                                  | JPA 메서드 변경                                                                                                                         |
|-------------|---------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| **Brand**   | `findById`, `findAll`                                                                                   | `findByIdAndDeletedAtIsNull`, `findAllByDeletedAtIsNull(Pageable)`                                                                 |
| **Product** | `findById`, `findAll`, `findAllByBrandId`, `findAllByIds`, `findAllByIdsForUpdate`, `findByIdForUpdate` | 각각 `...AndDeletedAtIsNull` 메서드 추가                                                                                                  |
| **Order**   | `findById`, `findAllByUserId`, `findAll`                                                                | `findByIdAndDeletedAtIsNull`, `findAllByRefUserIdAndCreatedAtBetweenAndDeletedAtIsNull(...)`, `findAllByDeletedAtIsNull(Pageable)` |
| **User**    | `findById`, `findByLoginId`, `existsByLoginId`                                                          | `findByIdAndDeletedAtIsNull`, `findByLoginIdAndDeletedAtIsNull`, `existsByLoginIdAndDeletedAtIsNull`                               |

**findByIdForUpdate (Product) 비관적 락 구현:**

```kotlin
// ProductJpaRepository
@Lock(LockModeType.PESSIMISTIC_WRITE)
fun findForUpdateByIdAndDeletedAtIsNull(id: Long): ProductEntity?
```

- `findForUpdateBy...`는 Spring Data가 `By` 이후만 쿼리 파싱 → `findByIdAndDeletedAtIsNull`과 동일 쿼리 + `@Lock`으로 비관적 락 추가
- OrderBy 불필요 (단건)

**UseCase 마이그레이션:**

| 파일                         | 변경                                                            |
|----------------------------|---------------------------------------------------------------|
| `RestoreBrandUseCase.kt`   | `findById` → `findByIdIncludeDeleted`                         |
| `RestoreProductUseCase.kt` | `findById` → `findByIdIncludeDeleted`                         |
| `RemoveLikeUseCase.kt`     | `findById` → `findByIdIncludeDeleted` (삭제된 상품도 좋아요 취소 가능해야 함) |
| `DeleteBrandUseCase.kt`    | `brand.isDeleted()` 체크 제거 (findById가 이미 필터링)                  |
| `DeleteProductUseCase.kt`  | 동일                                                            |
| `GetBrandUseCase.kt`       | `brand.isDeleted()` 체크 제거                                     |
| `CreateProductUseCase.kt`  | `brand.isDeleted()` 체크 제거                                     |

**Fake Repository 수정:**

| 파일                         | 변경                                                                               |
|----------------------------|----------------------------------------------------------------------------------|
| `FakeBrandRepository.kt`   | `findById`: `?.takeIf { it.deletedAt == null }`, `findAll`: deletedAt 필터         |
| `FakeProductRepository.kt` | 동일 패턴 (findById, findAll, findAllByBrandId, findAllByIds, findAllByIdsForUpdate) |
| `FakeOrderRepository.kt`   | `findById`: deletedAt 필터 추가                                                      |
| `FakeUserRepository.kt`    | `findById`, `findByLoginId`, `existsByLoginId`: deletedAt 필터 추가                  |

---

## CP3: 행위적 수정

### CP3-1: AddLikeUseCase existsBy 패턴

**커밋**: `feat: AddLikeUseCase에 existsBy 사전검증 도입, 인프라 예외 의존 제거`

| 파일                                          | 변경                                                                                                                  |
|---------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| `domain/like/repository/LikeRepository.kt`  | `existsByUserIdAndProductId(userId, productId): Boolean` 추가                                                         |
| `infrastructure/like/LikeRepositoryImpl.kt` | `LikeJpaRepository`에 `existsByRefUserIdAndRefProductId` 추가, 구현                                                      |
| `test/.../FakeLikeRepository.kt`            | `existsByUserIdAndProductId` 구현                                                                                     |
| `application/like/AddLikeUseCase.kt`        | `DataIntegrityViolationException` import 제거, try-catch 제거, `existsByUserIdAndProductId` 사전검증 (`if (exists) return`) |
| `test/.../LikeUseCaseTest.kt`               | `simulateConcurrentInsert` 기반 동시성 테스트 제거 또는 예외 전파 확인으로 변경                                                           |

### CP3-2: @Transactional UseCase 이동

**커밋**: `refactor: @Transactional을 Domain Service에서 UseCase로 이동`

| 파일                                        | 변경                               |
|-------------------------------------------|----------------------------------|
| `application/point/ChargePointUseCase.kt` | `execute()`에 `@Transactional` 추가 |
| `domain/point/PointCharger.kt`            | `@Transactional` 제거, import 제거   |
| `domain/point/PointDeductor.kt`           | `@Transactional` 제거, import 제거   |

테스트 영향 없음 (단위 테스트는 Fake 사용, 트랜잭션 무관)

### CP3-3: 중복 검증 제거

**커밋**: `refactor: Domain Service/Aggregate Root 중복 검증 제거, Domain Model에 일원화`

| 파일                             | 변경                                                                                            |
|--------------------------------|-----------------------------------------------------------------------------------------------|
| `domain/point/PointCharger.kt` | `if (amount.value == 0L)` 체크 제거 (L22-24). `MAX_CHARGE_AMOUNT` 검증은 유지 (고유 정책)                  |
| `domain/order/model/Order.kt`  | `cancelItem()`에서 `if (item.status == CANCELLED)` 체크 제거 (L36-38). `item.cancel()` 내부에 동일 검증 존재 |

검증 책임 정리:

- `Point VO init`: `value >= 0` (일반 제약)
- `UserPoint.charge()/use()`: `value > 0` (비즈니스 규칙) — **유지**
- `PointCharger`: 없음 (Domain Model에 위임) — **제거**
- `OrderItem.cancel()`: `status != CANCELLED` — **유지**
- `Order.cancelItem()`: 없음 (자식에게 위임) — **제거**

### CP3-4: likeCount Domain Model 경유

**커밋**: `feat: likeCount 변경을 Domain Model 경유로 전환, Repository 직접 UPDATE 제거`

| 파일                                                        | 변경                                                                                                                                                                                                  |
|-----------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `domain/catalog/product/repository/ProductRepository.kt`  | `increaseLikeCount(productId)` 제거, `decreaseLikeCount(productId)` 제거                                                                                                                                |
| `application/like/AddLikeUseCase.kt`                      | `productRepository.increaseLikeCount(productId)` → `productRepository.findByIdForUpdate(productId)!!.also { it.increaseLikeCount(); productRepository.save(it) }`                                   |
| `application/like/RemoveLikeUseCase.kt`                   | `productRepository.decreaseLikeCount(productId)` → `productRepository.findByIdForUpdate(productId)?.let { it.decreaseLikeCount(); productRepository.save(it) }` (deletedAt 필터링으로 삭제된 상품은 null → 스킵) |
| `infrastructure/catalog/product/ProductRepositoryImpl.kt` | QueryDSL `increaseLikeCount`/`decreaseLikeCount` 구현 삭제 (L89-103)                                                                                                                                    |
| `test/.../FakeProductRepository.kt`                       | `increaseLikeCount`/`decreaseLikeCount` 삭제                                                                                                                                                          |
| `test/.../LikeUseCaseTest.kt`                             | likeCount 검증 로직 조정                                                                                                                                                                                  |

### CP3-5: 최종 문서

**커밋**: `docs: 아키텍처 감사 후속 리팩토링 반영`

| 파일                              | 변경                                                |
|---------------------------------|---------------------------------------------------|
| `infrastructure/CLAUDE.md`      | soft delete 필터링 패턴 + `findByIdIncludeDeleted` 문서화 |
| `application/CLAUDE.md`         | UseCase 네이밍 규칙 업데이트                               |
| `docs/note/round3-decisions.md` | 감사 결과 및 리팩토링 섹션 추가                                |

---

## 의존성 순서

```
CP0 (문서) ─────────────────────────────────
CP1-1 (StatusDto) ──┐
CP1-2 (Command)  ───┤ 독립, 병렬 가능
CP1-3 (네이밍)   ───┤
CP1-4 (추출)     ───┘
                    │
CP2-1 (인터페이스 확장) ──→ CP2-2 (필터링 적용)
                              │
                    ┌─────────┼─────────┐
              CP3-1 (existsBy) CP3-2 (@Tx) CP3-3 (검증)
                    │
              CP3-4 (likeCount) ← CP3-1 + CP2-2 이후
                    │
              CP3-5 (최종 문서)
```

## 커밋 순서 (총 12건)

1. `docs:` CP0
2. `refactor:` CP1-1 StatusDto
3. `refactor:` CP1-2 CatalogCommand
4. `refactor:` CP1-3 UseCase 네이밍
5. `refactor:` CP1-4 findItemsByOrders
6. `refactor:` CP2-1 Repository 확장
7. `feat:` CP2-2 soft delete 필터링
8. `feat:` CP3-1 existsBy
9. `refactor:` CP3-2 @Transactional
10. `refactor:` CP3-3 중복 검증 제거
11. `feat:` CP3-4 likeCount Domain Model
12. `docs:` CP3-5 최종 문서

## Verification

매 체크포인트마다:

```bash
./gradlew ktlintFormat && ./gradlew ktlintCheck test
```

전체 완료 후: 292+ 테스트 통과 + lint clean 확인
