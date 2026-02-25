# Next Refactor: Repository 메서드 설계 개선

## 배경

CP2-2(soft delete 필터링)를 적용하면서, `isDeleted`(DB 관심사)와 `isActive`/ProductStatus(앱 관심사)가
혼재되어 있는 문제를 발견. 현재 `Product.isActive()`는 `!isDeleted() && status != HIDDEN`으로 두 관심사를 동일시하고 있음.

## 핵심 원칙

- **isDeleted**: DB 레벨 관심사 (soft delete). 페이징이 있는 다건 조회에서만 DB 레벨 필터링 필요.
- **isActive / ProductStatus**: 어플리케이션 레벨 관심사. UseCase에서 판단.
- 단건 조회는 페이징이 없으므로 DB 필터링 없이 UseCase에서 판단 가능.

## Product.isActive() 분리

현재:
```kotlin
fun isActive() = !isDeleted() && status != ProductStatus.HIDDEN
```

개선안:
```kotlin
fun isActive() = status != ProductStatus.HIDDEN  // ProductStatus만 검증
```

- `isDeleted`는 Repository 레벨에서 이미 처리 (다건) 또는 UseCase에서 판단 (단건)
- `findAllByIds` 등에서 DB 레벨로 deletedAt 필터 후 다시 `isActive()`에서 isDeleted 검사하는 중복 제거

## Repository 메서드 카테고리 분류

### ProductRepository

1차 기준: 외부 UseCase의 핵심 메서드인가 vs 내부 보조인가, 2차 기준: 단건/다건

| 카테고리 | 메서드 | 설명 |
|---------|--------|------|
| **공용** | `save`, `saveAll` | 저장 |
| **단건 조회** | `findById`, `findByIdForUpdate` | GetProduct, DeleteProduct 등 UseCase의 핵심 진입점 |
| **다건 조회** | `findAll`, `findAllIncludeDeleted`, `findActiveProducts` | 목록 조회 (어드민/대고객) |
| **내부용** | `findAllByBrandId`, `findAllByIds`, `findAllByIdsForUpdate` | 다른 UseCase 내부에서 보조적으로만 사용 |

- `findByIdIncludeDeleted` → 제거 대상 (findById가 필터링 안 하면 불필요)
- `findById`는 AddLike 등 내부에서도 쓰이지만, GetProduct/DeleteProduct의 핵심 메서드이므로 단건 조회로 분류
- "내부용"은 확실히 내부에서만 보조적으로 사용되는 메서드만 배치

### BrandRepository

| 카테고리 | 메서드 | 설명 |
|---------|--------|------|
| **공용** | `save` | 저장 |
| **단건 조회** | `findById` | UseCase에서 상태 판단 |
| **다건 조회** | `findAll` | 페이징, DB 레벨 필터링 |

- `findByIdIncludeDeleted` → 제거 가능

### OrderRepository

| 카테고리 | 메서드 | 설명 |
|---------|--------|------|
| **공용** | `save` | 저장 |
| **단건 조회** | `findById` | UseCase에서 상태 판단 |
| **다건 조회** | `findAllByUserId`, `findAll` | 페이징, DB 레벨 필터링 |

### UserRepository

| 카테고리 | 메서드 | 설명 |
|---------|--------|------|
| **공용** | `save` | 저장 |
| **단건 조회** | `findById`, `findByLoginId` | UseCase에서 상태 판단 |
| **존재 확인** | `existsByLoginId` | 회원가입 중복 체크 |

## 단건 조회 전환 시 영향 분석

단건 조회에서 DB 레벨 deletedAt 필터를 제거하면, 현재 `findById`로 조회 후 별도 isDeleted() 체크 없이
바로 사용하는 UseCase들이 삭제된 엔티티를 반환받게 됨. 각 UseCase에서 적절한 검증 추가 필요:

- 대고객 UseCase: `isActive()` 또는 `!isDeleted()` 체크 추가
- 어드민 UseCase: 삭제된 엔티티도 조회 가능하므로 별도 체크 불필요
- Restore UseCase: 삭제된 엔티티를 찾아야 하므로 문제 없음

## TODO

- [ ] `Product.isActive()`에서 `isDeleted()` 제거 — ProductStatus만 검증
- [ ] 단건 조회 메서드(findById)에서 deletedAt DB 필터 제거
- [ ] findByIdIncludeDeleted 메서드 제거
- [ ] 대고객 UseCase에 상태 검증 로직 추가
- [ ] Repository 인터페이스/구현체 메서드 순서를 카테고리별로 정리
