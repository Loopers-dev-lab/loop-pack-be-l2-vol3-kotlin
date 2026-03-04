# 테스트가 프로덕션 코드를 주도한 경우: BrandRepository.save

## 문제 인식

SCRUM-10(브랜드 상세 조회 API)을 TDD로 구현하는 과정에서, 통합 테스트의 데이터 셋업을 위해 `BrandRepository.save`를 추가했다. 그런데 고객 API(`GET /api/v1/brands/{brandId}`)는 조회만 하므로 프로덕션 코드에서 `save`를 호출하는 곳이 없다.

```kotlin
// 도메인 Repository 인터페이스
interface BrandRepository {
    fun find(id: Long): Brand?   // ← 프로덕션에서 사용
    fun save(brand: Brand): Brand // ← 프로덕션에서 사용하지 않음, 테스트 셋업용
}
```

이는 **테스트가 프로덕션 코드의 구현을 주도한 케이스**다.

## 선택지 비교

### 방법 1: BrandJpaRepository를 테스트에서 직접 주입

```kotlin
@SpringBootTest
class BrandServiceIntegrationTest @Autowired constructor(
    private val brandService: BrandService,
    private val brandJpaRepository: BrandJpaRepository,  // infrastructure 레이어 직접 의존
    private val databaseCleanUp: DatabaseCleanUp,
) {
    fun `DB에 저장된 브랜드를 조회하면, 브랜드 정보를 반환한다`() {
        val saved = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        // ...
    }
}
```

| 관점 | 평가 |
|------|------|
| 프로덕션 코드 영향 | ✅ 없음. 도메인 인터페이스에 불필요한 메서드를 추가하지 않음 |
| 테스트 순수성 | ❌ 통합 테스트가 infrastructure 레이어(`BrandJpaRepository`)에 직접 의존 |
| 일관성 | △ E2E 테스트에서는 이미 `BrandJpaRepository`를 직접 주입하고 있음 |

### 방법 2: BrandRepository.save를 도메인 인터페이스에 추가 (현재 방식)

```kotlin
@SpringBootTest
class BrandServiceIntegrationTest @Autowired constructor(
    private val brandService: BrandService,
    private val brandRepository: BrandRepository,  // 도메인 인터페이스에 의존
    private val databaseCleanUp: DatabaseCleanUp,
) {
    fun `DB에 저장된 브랜드를 조회하면, 브랜드 정보를 반환한다`() {
        val saved = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        // ...
    }
}
```

| 관점 | 평가 |
|------|------|
| 프로덕션 코드 영향 | ❌ 현재 사용처 없는 메서드가 도메인 인터페이스에 존재 |
| 테스트 순수성 | ✅ 도메인 인터페이스만 의존 |
| 일관성 | ✅ UserRepository에도 `save`가 정의되어 있어 패턴 일관성 유지 |

## 결론: 현재 방식(방법 2) 유지

### 근거

1. **어드민 API에서 반드시 필요** — `POST /api-admin/v1/brands`(브랜드 등록)에서 `BrandRepository.save`는 필수적으로 사용된다. 현재 사용처가 없지만, 예정된 기능에서 곧 사용될 코드다
2. **Repository 인터페이스의 자연스러운 구성** — `find`만 있고 `save`가 없는 Repository는 오히려 불완전하다. CRUD에서 Read만 있는 인터페이스는 의도적으로 제한하는 경우(ReadOnlyRepository 등)가 아니라면 부자연스럽다
3. **기존 패턴과 일관** — `UserRepository`도 `find`, `save` 등을 모두 정의하고 있다

### 주의할 점

테스트를 위해 프로덕션 코드를 추가하는 것이 항상 정당화되지는 않는다. 이번 경우는 "곧 필요한 코드의 조기 도입"이라는 맥락이 있기에 유지하지만, 사용 예정이 없는 코드를 테스트만을 위해 추가하는 것은 지양해야 한다.
