# PR #15 리뷰 코멘트 반영 계획

CodeRabbit + Gemini 리뷰 코멘트 31개를 검증한 결과, 수정 가치가 있는 항목을 우선순위순으로 정리한다.

## Batch 1 — 보안/인프라 (파일 3개)

### 1-1. RedisConfig ObjectMapper 타입 제한 강화

- 파일: `modules/redis/src/main/kotlin/com/loopers/config/redis/RedisConfig.kt`
- 원인: `DefaultTyping.EVERYTHING` + `allowIfSubType("java.")` → RCE 위험
- [ ] [RED] `EVERYTHING` + `java.*` 허용 상태에서 위험 타입 역직렬화 시도 시 차단되는지 테스트
- [ ] [GREEN] `DefaultTyping.NON_FINAL`로 변경, `allowIfSubType`을 `com.loopers.`, `java.math.BigDecimal`,
  `java.time.ZonedDateTime`, `java.util.ArrayList`, `java.util.List` 등 구체 타입만 허용
- [ ] 기존 캐시 테스트 전체 통과 확인

### 1-2. scanAndDelete 커넥션 수정 + findProductDetail 역직렬화 분리

- 파일: `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/catalog/product/ProductCacheRepositoryImpl.kt`

#### scanAndDelete 커넥션 수정

- 원인: `execute` 콜백 내에서 `redisTemplateMaster.delete(batch)` 호출 → 매 delete마다 새 커넥션 획득
- [ ] [GREEN] `connection.del()` 직접 사용으로 변경, SCAN과 DEL을 같은 커넥션에서 처리

#### findProductDetail 역직렬화 분리

- 원인: Redis I/O 실패와 역직렬화 실패를 하나의 try-catch로 처리, 손상 키가 TTL(1h)까지 잔존
- [ ] [RED] 손상된 JSON을 캐시에 저장 → 조회 시 해당 키가 삭제되는지 테스트
- [ ] [GREEN] Redis 조회 try-catch와 역직렬화 try-catch 분리, 역직렬화 실패 시 해당 키 best-effort 삭제

### 1-3. 쿠폰 날짜 포맷 통일

- 파일: `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/coupon/dto/CouponAdminV1Dto.kt`
- 원인: `CouponV1Dto`는 `ISO_OFFSET_DATE_TIME`, `CouponAdminV1Dto`는 `ISO_ZONED_DATE_TIME` → 포맷 불일치
- [ ] [GREEN] `CouponAdminV1Dto`의 `ISO_ZONED_DATE_TIME` → `ISO_OFFSET_DATE_TIME`으로 변경 (3곳: expiredAt, usedAt, createdAt)

## Batch 2 — 캐시 로직 (파일 2개)

### 2-1. 브랜드 검증 전 캐시 저장 순서 수정

- 파일: `apps/commerce-api/src/main/kotlin/com/loopers/application/catalog/product/GetProductUseCase.kt`
- 원인: 브랜드 검증 전에 `saveProductDetail` 호출 → 브랜드 삭제/미존재 시 불필요한 캐시 쓰기
- [ ] [RED] 브랜드가 삭제된 상품 조회 시 캐시에 저장되지 않는지 테스트
- [ ] [GREEN] `saveProductDetail` 호출을 브랜드 검증 이후로 이동

### 2-2. 멱등 복구 시 조건부 이벤트 발행

- 파일: `apps/commerce-api/src/main/kotlin/com/loopers/application/catalog/product/RestoreProductUseCase.kt`
- 원인: 이미 활성 상태인 상품에도 `DetailUpdated(evictList=true)` 발행 → 불필요한 목록 캐시 무효화
- [ ] [RED] 이미 활성 상태인 상품 복구 시 이벤트가 발행되지 않는지 테스트
- [ ] [GREEN] `product.isDeleted()` 선행 확인 후 삭제 상태에서 복구된 경우에만 이벤트 발행

## Batch 3 — 테스트 강화 (파일 4개)

### 3-1. UpdateProductUseCaseTest 이벤트 검증 강화

- 파일: `apps/commerce-api/src/test/kotlin/com/loopers/application/catalog/product/UpdateProductUseCaseTest.kt`
- 원인: `hasSize(1)`만 검증, 이벤트 타입/evictList 미검증
- [ ] [GREEN] `isInstanceOf(ProductCacheEvent.DetailUpdated::class.java)` + `evictList=true` + `product.id` 검증 추가

### 3-2. ProductCacheRepositoryImplTest 직렬화 round-trip 강화

- 파일: `apps/commerce-api/src/test/kotlin/com/loopers/infrastructure/catalog/product/ProductCacheRepositoryImplTest.kt`
- 원인: `id`, `name`만 검증, `refBrandId`/`price`/`stock`/`status`/`likeCount` 미검증
- [ ] [GREEN] 전체 핵심 필드 assertion 추가

### 3-3. DeleteProductUseCaseTest brandId 검증 추가

- 파일: `apps/commerce-api/src/test/kotlin/com/loopers/application/catalog/product/DeleteProductUseCaseTest.kt`
- 원인: `evictEvent.productId`만 검증, `brandId` 미검증
- [ ] [GREEN] `assertThat(evictEvent.brandId).isEqualTo(product.refBrandId)` 추가

### 3-4. LikeUseCaseTest evictList 검증 추가

- 파일: `apps/commerce-api/src/test/kotlin/com/loopers/application/like/LikeUseCaseTest.kt`
- 원인: `evictList=false`가 의도적 설계이나 테스트로 고정되지 않음
- [ ] [GREEN] `assertThat(detailEvent.evictList).isFalse()` 추가 (AddLike, RemoveLike 양쪽)

## 반영하지 않는 항목

| #       | 코멘트                       | 이유                                             |
|---------|---------------------------|------------------------------------------------|
| CR-I-8  | showStandardStreams CI 노출 | 개인 프로젝트, 테스트 출력 확인에 유용. 심각도 과장                 |
| CR-I-1  | 리스너 로깅/메트릭/outbox         | 프로젝트 규모에 과도. Impl에 이미 log.warn 존재              |
| CR-N-7  | 페이징 정렬 고정                 | CLAUDE.md 규정 설계 (defaultPageRequest = id DESC) |
| CR-N-9  | cancelItem filter+fold    | OrderItem 수 개 수준, 성능 영향 없음. 가독성도 현재가 나음        |
| CR-N-10 | mutable Product 이벤트 참조    | AFTER_COMMIT 후 Product 수정 코드 없음. 현재 안전         |
| CR-OD-1 | ZonedDateTime.parse 500   | 관리자 API, 입력 오류 빈도 낮음. 별도 이슈로 관리                |
| CR-N-2  | IN 쿼리 대량 ID               | 주문당 쿠폰 1-2개. 대량 ID 호출 경로 없음                    |
| CR-N-3  | 순차 좋아요 실패 처리              | 코드에 이미 구현됨 (무효)                                |
| CR-N-4  | ExecutorService 종료 로직     | 코드에 이미 구현됨 (무효)                                |
| CR-N-5  | 테스트명 불일치                  | DisplayName과 assertion 일치 (무효)                 |
| CR-N-6  | AtomicReference 개선        | 디버깅 편의성만, 기능 영향 없음                             |
| CR-N-8  | applyDiscount 실패 경로       | Order 상태 가드 구현 여부 확인 필요. 별도 이슈                 |
| CR-N-13 | HIDDEN 캐시 히트 테스트          | 삭제 경계값은 커버됨. 추가하면 좋으나 필수 아님                    |
| T-1     | 캐시 사용 증명 테스트              | 현재도 기본 동작 커버. 추가하면 좋으나 필수 아님                   |
| T-2~5   | 성능 테스트 플래키/비용             | 벤치마크 목적 테스트. assertion 제거 정도만 선택적              |
| G-3     | evictList 속성 검증 (Gemini)  | CR-I-10과 중복. Batch 3-1에서 반영                    |
