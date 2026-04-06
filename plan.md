# Round 9 — Redis ZSET 기반 실시간 랭킹 시스템

## 개요

유저 행동 이벤트(조회, 좋아요, 주문)를 Kafka Consumer에서 소비할 때 Redis ZSET에 가중치 기반 점수를 실시간 갱신하고,
commerce-api에서 Top-N 랭킹 조회 API와 상품 상세 순위를 제공한다.

- **Must-Have**: ZSET 점수 갱신 + 랭킹 Page 조회 API + 상품 상세 순위 포함
- **Nice-to-Have (이번 계획 제외)**: Score Carry-Over, 시간 단위 랭킹, 실시간 Weight 조절

### 핵심 설계 결정

| 결정 | 선택 | 이유 |
|------|------|------|
| 점수 갱신 위치 | commerce-streamer (기존 Consumer) | 이벤트 중복 소비 방지, 기존 흐름 재사용 |
| 주문 점수 산정 | `0.7 × quantity` (건수 기반) | 현재 payload에 price 없음, product_metrics와 일관성 |
| 가중치 관리 | 코드 내 상수 | 변경 빈도 낮음, 초기 복잡도 최소화 |
| 비즈니스 날짜 | KST (Asia/Seoul) 기준 | 서비스 사용자 문맥이 KST 중심 |
| 페이지 번호 | 0-based | 기존 API 관례와 통일 |
| 멱등성 순서 | EventHandled 체크 → ZINCRBY → RDB 저장 | EventHandled 통과 후에만 Redis 갱신, 중복 방지 |
| 동점 정렬 | Redis 기본 (lexicographic) 허용 | member가 productId 문자열이므로 "100" < "20" 가능. 허용 범위 |
| 0점 이하 노출 | score <= 0 상품 제외 | LIKE_REMOVED 차감으로 음수 가능. 의미 없는 항목 노출 방지 |
| totalElements | 필터링 기반 추정 | Redis 결과 < size → 끝, 아니면 +1로 hasNext 힌트 제공 |
| Redis 장애 | Infrastructure에서 try-catch + 빈 결과 | 랭킹 조회 실패가 전체 API 장애로 전파되지 않도록 격리 |

### 교차 저장소 원자성 Trade-off

RDB(`event_handled` + `product_metrics`)와 Redis(`ZINCRBY`)는 하나의 트랜잭션으로 묶이지 않는다.
현재 구현에서는 EventHandled 체크 → ZINCRBY → RDB commit 순서를 사용한다.

- **RDB commit 실패 시**: ZINCRBY는 이미 반영됨 → 재처리 시 EventHandled 미존재 → 중복 ZINCRBY 발생 가능
- **수용 근거**: 랭킹은 정확한 수치보다 상대적 순위가 중요하며, 소폭의 점수 오차는 사용자 경험에 미미한 영향
- **향후 대응**: Redis dedup key 또는 보정 배치로 해결 가능

## 영향 범위

### 신규 파일

**commerce-streamer (4파일 + 2테스트):**
- `domain/ranking/RankingWeight.kt` — 이벤트별 가중치 상수 (VIEW=0.1, LIKE=0.2, ORDER=0.7)
- `domain/ranking/repository/RankingScoreRepository.kt` — 점수 갱신 Repository 인터페이스
- `infrastructure/ranking/RedisRankingScoreRepository.kt` — Redis ZINCRBY + TTL 구현체
- `infrastructure/ranking/RedisRankingConstants.kt` — Redis 키 prefix, TTL 상수
- `test: FakeRankingScoreRepository.kt` — 단위 테스트용 Fake
- `test: RedisRankingScoreRepositoryTest.kt` — Redis 통합 테스트

**commerce-api (9파일 + 3테스트):**
- `domain/ranking/model/RankingEntry.kt` — 랭킹 항목 도메인 모델 (productId, score)
- `domain/ranking/repository/RankingRepository.kt` — 랭킹 조회 Repository 인터페이스
- `infrastructure/ranking/RedisRankingRepository.kt` — Redis ZREVRANGE/ZREVRANK/ZCARD 구현체
- `infrastructure/ranking/RedisRankingConstants.kt` — Redis 키 prefix 상수 (streamer와 동일 값)
- `application/ranking/GetRankingUseCase.kt` — 랭킹 조회 UseCase
- `application/ranking/RankingInfo.kt` — 랭킹 Application DTO
- `interfaces/api/ranking/spec/RankingV1ApiSpec.kt` — API 명세 인터페이스
- `interfaces/api/ranking/RankingV1Controller.kt` — 컨트롤러
- `interfaces/api/ranking/dto/RankingV1Dto.kt` — API 요청/응답 DTO
- `test: FakeRankingRepository.kt` — 단위 테스트용 Fake
- `test: GetRankingUseCaseTest.kt` — UseCase 단위 테스트
- `test: RankingApiE2ETest.kt` — E2E 통합 테스트

### 수정 파일

**commerce-streamer (1파일 + 1테스트):**
- `application/metrics/UpdateProductMetricsUseCase.kt` — RankingScoreRepository 의존성 + ZINCRBY 호출 추가
- `test: UpdateProductMetricsUseCaseTest.kt` — 랭킹 점수 갱신 검증 테스트 추가

**commerce-api (3파일 + 기존 테스트 수정):**
- `application/catalog/CatalogInfo.kt` — `rank: Int?` 필드 추가
- `application/catalog/product/GetProductUseCase.kt` — RankingRepository 의존성 + ZREVRANK 조회 추가
- `interfaces/api/product/dto/ProductV1Dto.kt` — `ProductDetailResponse`에 `rank: Int?` 필드 추가

### 관련 기존 파일 (참조만, 수정 안 함)

- `ProductRepository.findAllByIds()` — 랭킹 상품 정보 조회 시 재사용
- `Product.isActive()`, `Product.isDeleted()` — 비활성/삭제 상품 필터링
- `RedisWaitingQueueRepository` — Redis 코딩 패턴 참고 (StringRedisTemplate, Lua 스크립트)
- `RedisQueueConstants` — 상수 관리 패턴 참고
- `ProductCacheRepositoryImpl` — Redis 에러 처리 패턴 참고 (try-catch + graceful fallback)
- `PageResult<T>` — 도메인 페이지네이션 타입

## 구현 계획

### A. [streamer] 랭킹 Domain — 가중치 + Repository 인터페이스

> 신규 2파일. commerce-streamer에 ranking 도메인 패키지를 생성한다.

- [x] [P-A] A-1: `RankingWeight` 가중치 상수 object 정의
  - `VIEW = 0.1`, `LIKE = 0.2`, `ORDER = 0.7`
  - `LIKE_REMOVED`는 score를 `-1`로 전달하여 `LIKE × (-1) = -0.2` 처리
  - 파일: `domain/ranking/RankingWeight.kt`

- [x] [P-A] A-2: `RankingScoreRepository` 인터페이스 정의
  - `fun incrementScore(productId: Long, score: Double)` — 단일 메서드
  - 날짜(키)와 TTL은 Infrastructure 구현체의 관심사
  - 파일: `domain/ranking/repository/RankingScoreRepository.kt`

### B. [streamer] 랭킹 Infrastructure — Redis ZINCRBY 구현

> 신규 2파일 (구현체 + 상수). Redis 통합 테스트 1파일.

- [x] [P-A] B-1: `RedisRankingConstants` 상수 정의
  - `RANKING_KEY_PREFIX = "ranking:all:"`, `RANKING_TTL_SECONDS = 172_800L` (2일)
  - 파일: `infrastructure/ranking/RedisRankingConstants.kt`

- [x] [P-A] B-2: [RED] ZINCRBY로 점수 누적 시 정확한 값이 반영된다 → [GREEN] `RedisRankingScoreRepository` 구현
  - `StringRedisTemplate.opsForZSet().incrementScore(key, productId.toString(), score)`
  - 키 패턴: `ranking:all:{yyyyMMdd}` (KST 기준 `LocalDate.now(ZoneId.of("Asia/Seoul"))`)
  - 파일: `infrastructure/ranking/RedisRankingScoreRepository.kt`

- [x] [P-A] B-3: [RED] 키 최초 생성 시 TTL 2일(172,800초)이 설정된다 → [GREEN] EXPIRE 호출 추가
  - `ZINCRBY` 후 `getExpire()` 확인 → TTL 미설정(-1)이면 `expire(key, 172800, SECONDS)` 호출
  - 테스트: `RedisRankingScoreRepositoryTest.kt` (TestContainers Redis)

### C. [streamer] 랭킹 Application — UseCase 수정

> Fake 1파일 신규, 기존 UseCase + 테스트 수정.

- [x] [P-A] C-1: `FakeRankingScoreRepository` 작성
  - `MutableMap<String, MutableMap<Long, Double>>` 기반 인메모리 구현
  - `incrementScore` 호출 시 날짜별 productId → score 누적
  - 파일: `test/.../ranking/FakeRankingScoreRepository.kt`

- [x] [P-A] C-2: [RED] PRODUCT_VIEWED 이벤트 처리 시 랭킹 점수 `+0.1`이 반영된다 → [GREEN] `handleCatalogEvent`에 `rankingScoreRepository.incrementScore(productId, 0.1)` 추가
  - `UpdateProductMetricsUseCase` 생성자에 `RankingScoreRepository` 추가
  - `when` 분기 내에서 metrics 갱신과 함께 ranking score 갱신

- [x] [P-A] C-3: [RED] LIKE_ADDED 시 `+0.2`, LIKE_REMOVED 시 `-0.2`가 반영된다 → [GREEN]
  - `LIKE_ADDED`: `incrementScore(productId, RankingWeight.LIKE)`
  - `LIKE_REMOVED`: `incrementScore(productId, RankingWeight.LIKE * -1)`

- [x] [P-A] C-4: [RED] PAYMENT_COMPLETED 이벤트 시 `+0.7 × quantity`가 반영된다 → [GREEN]
  - `handleOrderEvent`에 `incrementScore(productId, RankingWeight.ORDER * quantity)` 추가

- [x] [P-A] C-5: [RED] 이미 처리된 이벤트(EventHandled 존재)는 랭킹 점수도 갱신하지 않는다 → [GREEN]
  - 기존 `existsByEventId` 조기 반환이 ZINCRBY 전에 실행되므로 자동 보장
  - 테스트에서 중복 이벤트 전송 후 Fake의 score가 1회만 누적됨을 검증

--- checkpoint: streamer 랭킹 점수 갱신 lint + test ---

### D. [api] 랭킹 Domain — 모델 + Repository 인터페이스

> 신규 2파일. commerce-api에 ranking 도메인 패키지를 생성한다.

- [ ] [P-B] D-1: `RankingEntry` 도메인 모델 정의
  - `data class RankingEntry(val productId: Long, val score: Double)`
  - Repository가 ZREVRANGE 결과를 이 타입으로 반환
  - 순위(rank)는 UseCase에서 offset + index로 계산 (Repository 관심사 아님)
  - 파일: `domain/ranking/model/RankingEntry.kt`

- [ ] [P-B] D-2: `RankingRepository` 인터페이스 정의
  - `fun getTopN(date: LocalDate, offset: Int, limit: Int): List<RankingEntry>` — offset~offset+limit-1 범위 조회
  - `fun getRank(date: LocalDate, productId: Long): Int?` — 1-based 순위, 없으면 null
  - `fun getTotalCount(date: LocalDate): Long` — 랭킹 진입 전체 상품 수
  - 파일: `domain/ranking/repository/RankingRepository.kt`

### E. [api] 랭킹 Infrastructure — Redis 조회 구현 + Fake

> 신규 3파일 (구현체 + 상수 + Fake). Redis 통합 테스트 포함.

- [ ] [P-B] E-1: `RedisRankingConstants` 상수 정의
  - `RANKING_KEY_PREFIX = "ranking:all:"` (streamer와 동일 값)
  - 파일: `infrastructure/ranking/RedisRankingConstants.kt`

- [ ] [P-B] E-2: [RED] ZREVRANGE로 지정 범위의 상품 ID + score가 내림차순으로 조회된다 → [GREEN] `RedisRankingRepository.getTopN` 구현
  - `reverseRangeWithScores(key, offset, offset + limit - 1)` 사용
  - `TypedTuple` → `RankingEntry` 변환
  - 키가 없으면 빈 리스트 반환
  - **Redis 장애 시 빈 리스트 반환** (try-catch + warn 로그, ProductCacheRepositoryImpl 패턴)
  - 파일: `infrastructure/ranking/RedisRankingRepository.kt`

- [ ] [P-B] E-3: [RED] ZREVRANK로 특정 상품의 1-based 순위가 반환된다 (없으면 null) → [GREEN] `RedisRankingRepository.getRank` 구현
  - `reverseRank(key, productId.toString())` → null이면 null, 아니면 `+1` (0-based → 1-based)
  - **Redis 장애 시 null 반환** (try-catch + warn 로그)

- [ ] [P-B] E-4: [RED] ZCARD로 랭킹 진입 전체 상품 수가 반환된다 → [GREEN] `RedisRankingRepository.getTotalCount` 구현
  - `zCard(key)` → `Long`
  - **Redis 장애 시 0L 반환**

- [ ] [P-B] E-5: `FakeRankingRepository` 작성
  - score 내림차순 정렬. **동점 시 productId 문자열 lexicographic 오름차순** (Redis ZSET 기본 동작 일치)
  - `getTopN`, `getRank`, `getTotalCount` 모두 구현
  - 테스트에서 데이터 주입을 위한 `addEntry(date, productId, score)` 헬퍼 메서드 제공
  - 파일: `test/.../ranking/FakeRankingRepository.kt`

--- checkpoint: api 랭킹 도메인 + 인프라 lint + test ---

### F. [api] 랭킹 Application — GetRankingUseCase

> 신규 2파일 (UseCase + DTO) + 1 테스트.

- [ ] [P-C] F-1: `RankingInfo` Application DTO 정의
  - `data class RankingInfo(val rank: Int, val productId: Long, val productName: String, val price: BigDecimal, val score: Double)`
  - 파일: `application/ranking/RankingInfo.kt`

- [ ] [P-C] F-2: [RED] Top-N 조회 시 상품 ID로 상품 상세 정보를 Aggregation하여 반환한다 → [GREEN] `GetRankingUseCase.execute` 구현
  - `RankingRepository.getTopN(date, offset, limit)` → productId 목록 추출
  - `ProductRepository.findAllByIds(productIds)` → 상품 정보 조회
  - ZSET 순서 보존: productIds 순서대로 상품을 매핑 (findAllByIds는 순서 미보장)
  - rank 계산: `offset + index + 1` (1-based)
  - 파일: `application/ranking/GetRankingUseCase.kt`

- [ ] [P-C] F-3: [RED] 삭제/비활성 상품과 score <= 0 상품은 결과에서 제외된다 → [GREEN]
  - 상품 조회 후 `filter { !it.isDeleted() && it.isActive() }`
  - RankingEntry에서 `filter { it.score > 0 }` 선행 적용
  - 필터링 후 rank는 연속되지 않을 수 있음 (원본 ZSET 순위 유지)
  - over-fetch는 하지 않음 (초기 구현, 향후 필요 시 추가)

- [ ] [P-C] F-4: [RED] date 파라미터가 null이면 오늘(KST) 날짜로 조회한다 → [GREEN]
  - `date ?: LocalDate.now(ZoneId.of("Asia/Seoul"))`

- [ ] [P-C] F-5: [RED] 페이지네이션이 올바르게 동작한다 → [GREEN]
  - `offset = page * size`
  - totalElements 추정: Redis에서 `size`개 요청 → 필터링 후 결과가 `size`보다 적으면 `offset + filteredSize` (끝), 아니면 `offset + filteredSize + 1` (더 있음 힌트)
  - 반환: `PageResult<RankingInfo>(data, estimatedTotal, page, size)`

--- checkpoint: api 랭킹 UseCase lint + test ---

### G. [api] 랭킹 Interfaces — API 엔드포인트

> 신규 3파일 (ApiSpec + Controller + Dto) + 1 E2E 테스트.

- [ ] [P-C] G-1: `RankingV1Dto` 정의
  - `RankingResponse(rank: Int, productId: Long, productName: String, price: BigDecimal, score: Double)`
  - `companion object { fun from(info: RankingInfo): RankingResponse }`
  - 파일: `interfaces/api/ranking/dto/RankingV1Dto.kt`

- [ ] [P-C] G-2: `RankingV1ApiSpec` 인터페이스 정의
  - `GET /api/v1/rankings` — `ApiResponse<Page<RankingV1Dto.RankingResponse>>` 반환
  - `date: String?` — yyyyMMdd 형식, 미지정 시 null (UseCase에서 오늘 기본값)
  - `page: @PositiveOrZero Int` (기본값 0)
  - `size: @Positive @Max(100) Int` (기본값 20)
  - 유효하지 않은 date 형식 검증 → `400 BAD_REQUEST`
  - 파일: `interfaces/api/ranking/spec/RankingV1ApiSpec.kt`

- [ ] [P-C] G-3: `RankingV1Controller` 구현
  - `@RestController @RequestMapping("/api/v1/rankings") @Validated`
  - `RankingV1ApiSpec` 구현
  - date String → LocalDate 파싱 (`DateTimeFormatter.BASIC_ISO_DATE`)
  - 파싱 실패 시 `CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 날짜 형식입니다. yyyyMMdd 형식을 사용해주세요.")`
  - `GetRankingUseCase.execute()` 호출 → `PageResult` → `.map { RankingResponse.from(it) }` → `.toSpringPage()` → `ApiResponse.success()`
  - 파일: `interfaces/api/ranking/RankingV1Controller.kt`

- [ ] [P-C] G-4: [RED] E2E 테스트 — 랭킹 조회 API가 정상 동작한다 → [GREEN]
  - Redis에 ZINCRBY로 테스트 데이터 직접 세팅 + DB에 상품 데이터 삽입
  - `GET /api/v1/rankings` → 200 OK, content 확인 (rank, productName, score)
  - `GET /api/v1/rankings?date=invalid` → 400 BAD_REQUEST
  - `GET /api/v1/rankings?date=20260404&page=0&size=5` → 페이지네이션 확인
  - 파일: `test/.../ranking/RankingApiE2ETest.kt`

--- checkpoint: api 랭킹 API lint + test ---

### H. [api] 상품 상세에 순위 포함

> 기존 3파일 수정 + 기존 테스트 수정.

- [ ] [P-D] H-1: `CatalogInfo`에 `rank: Int? = null` 필드 추가
  - `CatalogInfo(product: ProductInfo, brandName: String, rank: Int? = null)`
  - `from(detail)` 팩토리에 기본값 null → 기존 호출 코드 영향 없음
  - 파일: `application/catalog/CatalogInfo.kt`

- [ ] [P-D] H-2: `ProductDetailResponse`에 `rank: Int? = null` 필드 추가
  - `ProductDetailResponse(product, brand, rank)`
  - `from(info: CatalogInfo)` 에서 `rank = info.rank` 전달
  - 파일: `interfaces/api/product/dto/ProductV1Dto.kt`

- [ ] [P-D] H-3: [RED] 상품 상세 조회 시 해당 상품의 오늘(KST) 순위가 1-based로 포함된다 → [GREEN]
  - `GetProductUseCase` 생성자에 `RankingRepository` 추가
  - `execute()` 내에서 `rankingRepository.getRank(LocalDate.now(KST), productId)` 호출
  - `CatalogInfo` 생성 시 rank 값 전달
  - 파일: `application/catalog/product/GetProductUseCase.kt`

- [ ] [P-D] H-4: [RED] 랭킹에 없는 상품은 `rank = null`로 반환된다 → [GREEN]
  - `getRank()`가 null을 반환하면 그대로 `CatalogInfo.rank = null`
  - 응답 JSON에서 `"rank": null` 으로 직렬화

- [ ] [P-D] H-5: [RED] Redis 장애 시에도 상품 상세 조회는 정상 동작한다 (rank만 null) → [GREEN]
  - `getRank()` 호출을 try-catch로 감싸고, 실패 시 null 반환 + 경고 로그
  - 기존 `ProductCacheRepositoryImpl`의 에러 처리 패턴과 동일

--- checkpoint: 상품 상세 순위 lint + test ---

## 병렬 실행 가이드

```
Phase 1 (병렬):
  [P-A] A → B → C  (commerce-streamer 전체)
  [P-B] D → E      (commerce-api 도메인 + 인프라)

Phase 2 (Phase 1 완료 후, 병렬):
  [P-C] F → G      (랭킹 조회 API)
  [P-D] H          (상품 상세 순위)

Phase 3:
  최종 통합 검증 (ktlintCheck + test)
```

## 고려사항

### 기존 패턴과의 일관성
- Repository 인터페이스는 Domain 레이어에 위치 (DIP)
- Redis 구현체는 Infrastructure 레이어에 `@Repository` 등록
- UseCase는 `@Component` + `execute()` 단일 메서드
- ApiSpec 인터페이스에 Bean Validation 어노테이션 집중
- 에러는 `CoreException(ErrorType)` 단일 클래스 사용
- 응답은 `ApiResponse<T>` 래퍼 사용

### ZSET 순서 보존 주의
- `ZREVRANGE` 결과의 순서가 곧 랭킹 순서
- `ProductRepository.findAllByIds()`는 순서를 보장하지 않음
- UseCase에서 ZSET 결과 순서를 기준으로 상품을 재매핑해야 함
- 구현: `productIds` 순서대로 Map에서 꺼내기

### Redis 장애 대응
- 랭킹 조회 실패 시: 빈 목록 반환 (API 자체는 정상 응답)
- 상품 상세 순위 조회 실패 시: rank=null (상품 상세 조회 자체는 정상)
- 기존 `ProductCacheRepositoryImpl` 패턴 참고: try-catch + warn 로그 + graceful fallback

### 비활성 상품 필터링
- ZSET에 남아있는 삭제/비활성 상품은 API 응답에서 제외
- 필터링으로 페이지 크기가 요청보다 작아질 수 있음
- 초기 구현에서는 over-fetch 하지 않음 (요구사항에 "구현 시 판단" 명시)
- 필요 시 향후 over-fetch + 재조회 전략 추가 가능

### 테스트 전략
- **단위 테스트**: Fake Repository 기반, Mockito 사용 금지
- **통합 테스트**: TestContainers Redis로 ZINCRBY/ZREVRANGE 검증
- **E2E 테스트**: `@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate`
