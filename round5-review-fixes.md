# Round 5 CodeRabbit 리뷰 반영 계획

PR #89 CodeRabbit 리뷰 분석 결과. Round 6 시작 전에 반영한다.

## 수정 항목

### P1 — 반드시 수정

#### 1. `excludeTags("benchmark")` 추가
- **파일**: `build.gradle.kts`
- **문제**: `@Tag("benchmark")` 테스트가 CI에서 제외되지 않아 `measureTimeMillis` 기반 assertion이 플래키
- **수정**: `tasks.test { useJUnitPlatform { excludeTags("benchmark") } }`

#### 2. tearDown 인덱스 복구 + `@Execution(SAME_THREAD)`
- **파일**: `ProductIndexComparisonTest.kt`
- **문제**: DROP INDEX 후 assertion 실패하면 tearDown에서 인덱스 복구 안됨 → 후속 테스트 연쇄 실패
- **수정**: tearDown에서 CREATE INDEX IF NOT EXISTS 3개 추가, 클래스에 `@Execution(SAME_THREAD)` 추가

#### 3. LikeConcurrencyTest Redis 캐시 정리
- **파일**: `LikeConcurrencyTest.kt`
- **문제**: `@AfterEach`에서 DB만 truncate하고 Redis 캐시 안 지움 → 플래키
- **수정**: `@AfterEach`에서 `redisCleanUp.truncateAll()` 추가

#### 4. TTL 지터 ±60s 양방향
- **파일**: `ProductCacheRepositoryImpl.kt`, `JitteredRedisCacheWriter.kt`
- **문제**: 지터가 `0..+60s`로 +방향만. 설계 의도는 `±60s`
- **수정**:
  - `ProductCacheRepositoryImpl.detailTtl()`: `nextLong(0, 61)` → `nextLong(-60, 61)`
  - `JitteredRedisCacheWriter.withJitter()`: `nextLong(0, max+1)` → `nextLong(-max, max+1)`
  - 최소 TTL 하한 보장 필요 (base - jitter > 0 확인)

### P2 — 개선

#### 5. `saveProductDetailIfAbsent` (캐시 미스 race condition)
- **파일**: `ProductCacheRepository.kt`, `ProductCacheRepositoryImpl.kt`, `GetProductUseCase.kt`, `FakeProductCacheRepository.kt`
- **문제**: 캐시 미스 경로에서 `saveProductDetail()`이 overwrite → 늦게 끝난 읽기가 최신 캐시를 구버전으로 덮어쓸 수 있음
- **수정**:
  - Domain 인터페이스에 `saveProductDetailIfAbsent(product: Product)` 추가
  - Infrastructure에서 Redis `SET NX` (putIfAbsent) 사용
  - `GetProductUseCase.kt:38-39`에서 `saveProductDetailIfAbsent` 호출
  - 쓰기 경로(EventListener)는 기존 overwrite 유지
  - Fake에도 putIfAbsent 시뮬레이션 추가

#### 6. DeleteProductUseCaseTest 멱등 경로 이벤트 검증
- **파일**: `DeleteProductUseCaseTest.kt`
- **문제**: 이미 삭제/미존재 상품에서 이벤트 미발행 검증 없음
- **수정**: `deleteProduct_alreadyDeleted_isIdempotent`, `deleteProduct_nonExistent_isIdempotent`에 `publishedEvents.isEmpty()` 검증 추가

#### 7. FakeProductCacheRepository evict 기록
- **파일**: `FakeProductCacheRepository.kt`
- **문제**: `evictProductList()`가 no-op → 거짓 양성
- **수정**: `val evictedListBrandIds = mutableListOf<BrandId?>()` 추가, 호출 시 기록

## 의도적으로 수정하지 않는 항목

| # | 항목 | 이유 |
|---|------|------|
| #1 | 캐시 키 page/size 상한 | Interfaces에서 `@PositiveOrZero`/`@Positive @Max(100)` 이미 검증. Application CLAUDE.md: "페이지네이션 파라미터 재검증은 하지 않는다" |
| #3 | `evictProductList(BrandId?)` 메서드 분리 | 실질적으로 null 전달 경로 없음. Domain 인터페이스 변경 blast radius 대비 이점 부족 |
| #6 | GetProductsUseCacheTest Redis skip 환경 분기 | 로컬 Redis 없는 환경 방어 코드. CI에서 TestContainers 정상 동작하면 문제 없음 |
| #7 | 좋아요 `evictList = false` → `true` | 의도적 트레이드오프. `AddLikeUseCase.kt:34-35` 주석 참고. TTL 5분 eventual consistency |
| #12 | Redis 이미지 `redis:latest` 고정 | 의도한 것 |
| Nitpick#1 | `@DynamicPropertySource` 전환 | `@Configuration` 클래스라 구조 호환 안됨 |
| Nitpick#2,3 | 벤치마크 배치 적재, MySQL 버전 assumption | CI 제외 후 불필요 |

## 참고

- `feature/round5`에 이미 "PR 리뷰 반영" 커밋들이 있음 (`1198cee`, `02a58ff`, `8d62afe`, `c65af05`)
- 일부 수정사항이 이미 반영되어 있을 수 있으니, 작업 시작 전 해당 커밋들과 대조 필요
- 변경 파일 ~10개 → 2세션 분할 또는 서브에이전트 병렬 위임 권장
