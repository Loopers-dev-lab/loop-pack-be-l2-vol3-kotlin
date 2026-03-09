# Round 5 — 읽기 성능 최적화: 인덱스, 비정규화, 캐시

## 참조 문서
- 요구사항: `docs/requirements/round5-requirements-analysis.md`
- 레이어 가이드: 각 레이어 CLAUDE.md

## 구현 순서

### Phase 0: 캐시 인프라 기반

- [x] **0-1. Spring Cache 설정** (`modules/redis`)
  - [RED] RedisCacheManager 빈이 등록되는지 테스트
  - [GREEN] `@EnableCaching` + RedisCacheManager 빈 설정 클래스 작성 (modules/redis에 추가)
  - TTL 기본값 설정, 직렬화 방식(JSON) 설정

### Phase 1: ProductCacheRepository (DIP 캐시 레이어)

- [x] **1-1. Domain 인터페이스 정의**
  - 파일: `domain/catalog/product/repository/ProductCacheRepository.kt`
  - 메서드: `findProductDetail(productId)`, `saveProductDetail(product)`, `evictProductDetail(productId)`, `evictProductList(brandId?)`

- [x] **1-2. Fake 구현체**
  - 파일: `test/.../domain/catalog/product/FakeProductCacheRepository.kt`
  - HashMap 기반 인메모리 구현

- [x] **1-3. Redis 구현체**
  - [RED] ProductCacheRepositoryImpl이 RedisTemplate으로 Product를 저장/조회하는 통합 테스트
  - [GREEN] `infrastructure/catalog/product/ProductCacheRepositoryImpl.kt` 구현
  - `@Qualifier("redisTemplateMaster")` 쓰기, 기본 RedisTemplate 읽기
  - 캐시 키: `product:detail:{id}`
  - 직렬화: Jackson JSON

### Phase 2: 상품 상세 캐시 (RedisTemplate, Write-Through)

- [x] **2-1. GetProductUseCase 캐시 조회**
  - [RED] 캐시에 상품이 있으면 DB 조회 없이 반환하는 테스트 (Fake 사용)
  - [RED] 캐시 미스 시 DB 조회 후 캐시에 저장하는 테스트
  - [GREEN] GetProductUseCase에 ProductCacheRepository 주입, 캐시 조회/저장 로직 추가

- [x] **2-2. Write-Through 무효화 — 상품 수정**
  - [RED] 상품 수정 후 캐시에 최신 데이터가 반영되는 테스트
  - [GREEN] UpdateProductUseCase에서 save 후 `saveProductDetail()` 호출

- [x] **2-3. Write-Through 무효화 — 좋아요 등록/취소**
  - [RED] 좋아요 등록 후 캐시의 likeCount가 갱신되는 테스트
  - [RED] 좋아요 취소 후 캐시의 likeCount가 감소하는 테스트
  - [GREEN] AddLikeUseCase, RemoveLikeUseCase에서 save 후 `saveProductDetail()` 호출

- [x] **2-4. 상품 삭제 시 캐시 삭제**
  - [RED] 상품 soft delete 후 캐시에서 제거되는 테스트
  - [GREEN] DeleteProductUseCase(또는 해당 UseCase)에서 `evictProductDetail()` 호출

### Phase 3: 상품 목록 캐시 (@Cacheable, TTL + 수동 무효화)

- [x] **3-1. GetProductsUseCase @Cacheable 적용**
  - [RED] 동일 조건 2회 조회 시 2번째는 캐시에서 반환하는 통합 테스트
  - [GREEN] GetProductsUseCase 또는 Repository 메서드에 `@Cacheable` 적용
  - 캐시 키: `product:list:{brandId}:{sort}:{page}:{size}`
  - TTL: 5~10분

- [x] **3-2. 수동 무효화**
  - [RED] 상품 수정/등록/삭제 시 관련 목록 캐시가 무효화되는 테스트
  - [GREEN] 변경 이벤트 발생 UseCase에서 `evictProductList()` 호출

### Phase 4: Redis 장애 Fallback

- [x] **4-1. Redis 연결 실패 시 DB fallback**
  - [RED] Redis 예외 발생 시 DB 직접 조회로 정상 응답하는 테스트
  - [GREEN] ProductCacheRepositoryImpl에서 try-catch + 로그 기록, 또는 CacheErrorHandler 구현

### Phase 5: 10만 데이터 시딩 + EXPLAIN 분석

- [x] **5-1. 데이터 시딩 테스트 코드**
  - 파일: `test/.../seeding/ProductSeedingTest.kt`
  - 10만+ 상품 데이터 프로그래밍 방식 생성
  - 분포: 브랜드 50~100개 (편차 있음), 가격 1K~1M, 좋아요 0~10K (멱법칙), 상태 ON_SALE 90%

- [x] **5-2. EXPLAIN 분석**
  - brandId 필터, price ASC 정렬, likeCount DESC 정렬, 깊은 OFFSET 각각에 대해 EXPLAIN 실행
  - key 사용 여부, Using filesort 없음, rows 수 확인
  - 결과를 보고서로 정리

### Phase 6: 동시성 테스트 (likeCount 정합성)

- [x] **6-1. N명 동시 좋아요 테스트**
  - [RED] 10명 동시 좋아요 → likeCount == 10 확인
  - [GREEN] 기존 비관적 락 구조로 이미 통과해야 함 (검증 목적)

- [x] **6-2. 좋아요 취소 동시성**
  - [RED] 동시 취소 시 likeCount 음수 방지 확인

### Phase 7: 설계 문서 업데이트

- [x] **7-1. 요구사항 명세서** (`docs/design/01-requirements.md`)
  - v4 → v5 업데이트, 캐시 관련 요구사항 추가

- [x] **7-2. 시퀀스 다이어그램** (`docs/design/02-sequence-diagrams.md`)
  - 상품 상세/목록 조회에 캐시 흐름 추가
  - 좋아요 등록/취소의 캐시 갱신 흐름 추가

- [x] **7-3. 클래스 다이어그램** (`docs/design/03-class-diagram.md`)
  - ProductCacheRepository 인터페이스 + 구현체 추가

- [x] **7-4. ERD** (`docs/design/04-erd.md`)
  - 변경 없음 (확인만)

- [x] **7-5. 플로우차트** (`docs/design/05-flowcharts.md`)
  - 캐시 조회/무효화 플로우 추가 (필요 시)

## 의존관계

```
Phase 0 → Phase 1 → Phase 2, 3 (병렬 가능) → Phase 4
Phase 5, 6은 독립 실행 가능
Phase 7은 모든 구현 완료 후
```

## 주의사항

- Domain 계층에 Spring/Redis import 금지 — 캐시 인터페이스만 정의
- @Cacheable은 Application 또는 Infrastructure 레이어에 배치 (아키텍처 규칙 확인 필요)
- 캐시에 저장하는 데이터는 Domain Model 직접 저장이 아닌 직렬화 가능한 형태 (DTO/JSON)
- RedisTemplate 쓰기는 `@Qualifier("redisTemplateMaster")`, 읽기는 기본 템플릿
