# Round 5 — 기술 의사결정 기록

읽기 성능 최적화 과정에서 만난 이슈와 해결 방식을 기록한다.

---

## 1. Redis 캐시 직렬화/역직렬화 실패

### 문제

`@Cacheable`로 상품 목록을 Redis에 캐시한 뒤, 캐시 히트 시 역직렬화가 실패한다.
- 1차: `LinkedHashMap cannot be cast to PageResult` (ClassCastException)
- 2차: `Cannot construct instance of PageResult (no Creators exist)` (InvalidDefinitionException)
- 3차: `Unrecognized field "totalPages"` (UnrecognizedPropertyException)

### 원인

3가지 원인이 중첩되어 있었다:

1. **타입 정보 누락**: `GenericJackson2JsonRedisSerializer(objectMapper)` — 커스텀 ObjectMapper를 전달하면 `activateDefaultTyping`이 자동 적용되지 않는다. JSON에 `@class` 타입 정보가 없어서, 역직렬화 시 `LinkedHashMap`으로 복원됨.
2. **KotlinModule 미등록**: 기본 생성자(`GenericJackson2JsonRedisSerializer()`)의 내부 ObjectMapper에는 `KotlinModule`이 없다. Kotlin `data class`는 no-arg 생성자가 없으므로 Jackson이 인스턴스를 생성할 수 없음.
3. **계산 프로퍼티 충돌**: `PageResult`에 `val totalPages: Int get() = ...` 계산 프로퍼티가 있다. 직렬화 시 JSON에 포함되지만, 역직렬화 시 생성자에 해당 파라미터가 없어서 "Unrecognized field" 에러 발생.

### 해결방안

| 방안 | 설명 | 장점 | 단점 |
|------|------|------|------|
| A. 기본 생성자 사용 | `GenericJackson2JsonRedisSerializer()` | 타입 정보 자동 포함 | KotlinModule 없음, data class 역직렬화 불가 |
| B. 커스텀 ObjectMapper + 수동 설정 | KotlinModule 등록 + activateDefaultTyping + FAIL_ON_UNKNOWN_PROPERTIES=false | 모든 문제 해결 | ObjectMapper 설정이 복잡해짐 |
| C. PageResult에 @JsonIgnore + no-arg 생성자 추가 | Domain 객체에 Jackson 어노테이션 부착 | 단순 | Domain 계층에 인프라 의존성 유입 (아키텍처 위반) |
| D. 캐시 전용 DTO 분리 | Redis 전용 직렬화 객체를 별도로 만들기 | Domain 오염 없음 | 변환 로직 추가, 유지보수 부담 |

### 최종 선택

**B. 커스텀 ObjectMapper + 수동 설정**

```kotlin
val cacheObjectMapper = ObjectMapper().apply {
    registerModule(KotlinModule.Builder().build())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    activateDefaultTyping(
        BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType(Any::class.java)
            .build(),
        ObjectMapper.DefaultTyping.EVERYTHING,
    )
}
```

### 근거

- C는 Domain 계층에 Jackson 의존성을 넣게 되므로 아키텍처 원칙(DIP) 위반
- D는 현재 캐시 대상이 2개(상세/목록)뿐이라 DTO 분리는 과잉 설계
- A는 KotlinModule 문제를 해결 못 함
- B는 RedisConfig 한 곳에서 ObjectMapper를 설정하면 되므로 영향 범위가 최소

---

## 2. TestContainers Redis 연결 실패 (테스트 skipped)

### 문제

`ProductCacheComparisonTest`에서 `assumeTrue(isRedisAvailable())`가 항상 false를 반환하여 테스트가 skipped 처리됨. Redis TestContainers는 정상적으로 시작되지만, 애플리케이션이 `localhost:6379`로 연결을 시도함.

### 원인

`RedisTestContainersConfig`에서 system property 설정이 **class-level `init`** 블록에 있었다. class-level `init`은 Spring이 Bean을 인스턴스화할 때 실행되는데, 이 시점에는 이미 `redis.yml`의 property가 바인딩된 이후다. 따라서 TestContainers의 랜덤 포트가 반영되지 않고, YAML의 기본값(`localhost:6379`)으로 연결 시도.

반면 MySQL TestContainers(`MySqlTestContainersConfig`)는 **`companion object init`**(static initializer)에서 설정하므로, 클래스 로드 시점에 즉시 실행되어 Spring property 바인딩 전에 system property가 설정됨.

### 해결방안

| 방안 | 설명 | 장점 | 단점 |
|------|------|------|------|
| A. companion object init으로 이동 | MySQL과 동일한 패턴 | 일관성, 검증된 방식 | 없음 |
| B. redis.yml에 환경변수 placeholder 사용 | `${REDIS_TEST_HOST:localhost}` | YAML 수준에서 해결 | 자기참조 순환 위험, 복잡 |
| C. ApplicationContextInitializer 사용 | Spring 표준 방식 | 가장 정석 | 코드량 증가, 기존 패턴과 불일치 |

### 최종 선택

**A. companion object init으로 이동**

### 근거

- MySQL과 동일한 패턴으로 일관성 유지
- 가장 변경 범위가 작고, 이미 검증된 방식
- B는 실제로 시도했으나 자기참조 순환(`PlaceholderResolutionException`)이 발생
- ktlintFormat이 init 블록을 class-level로 되돌리는 경우가 있어, format 실행 후 확인 필요

---

## 3. 인덱스 설계: 정렬 패턴별 복합 인덱스

### 문제

10만건 이상의 상품 테이블에서 다양한 정렬(최신순, 가격순, 좋아요순) + 필터(deleted_at IS NULL, status != 'HIDDEN') 조합 시 풀 테이블 스캔(ALL) + filesort 발생.

### 원인

WHERE 절의 필터 조건과 ORDER BY 절의 정렬을 동시에 커버하는 복합 인덱스가 없었다. 기본 PK 인덱스와 `ref_brand_id` 단일 인덱스만 존재.

### 해결방안

| 방안 | 설명 | 장점 | 단점 |
|------|------|------|------|
| A. 정렬 패턴별 복합 인덱스 3개 | `(deleted_at, status, 정렬컬럼)` | 각 정렬 패턴에 최적화 | 인덱스 3개 추가 (쓰기 비용 증가) |
| B. 커버링 인덱스 1개 | 모든 컬럼을 포함하는 단일 인덱스 | 인덱스 1개 | 인덱스 크기 과대, 정렬 커버 불가능 |
| C. 파티셔닝 | status별 파티션 분리 | 파티션 프루닝 | 10만건 수준에서 과잉, 운영 복잡도 증가 |

### 최종 선택

**A. 정렬 패턴별 복합 인덱스 3개**

```sql
CREATE INDEX idx_products_active_like_count ON products (deleted_at, status, like_count DESC);
CREATE INDEX idx_products_active_created_at ON products (deleted_at, status, created_at DESC);
CREATE INDEX idx_products_active_price ON products (deleted_at, status, price ASC);
```

### 근거

- 선두 컬럼(deleted_at, status): WHERE 절의 등치/비교 조건 → 필터링에 활용
- 후미 컬럼(정렬 대상): ORDER BY 절을 인덱스 순서로 커버 → filesort 제거
- 10만건 수준에서 인덱스 3개 추가의 쓰기 비용은 무시할 수 있는 수준
- EXPLAIN 실측: ALL → ref 전환 확인

---

## 4. 좋아요 수 동시성 제어: 비관적 락

### 문제

좋아요 등록/취소 시 `likeCount`를 동시에 증감하면 Lost Update가 발생할 수 있다.

### 원인

`SELECT → 변경 → UPDATE` 패턴에서 여러 트랜잭션이 동시에 같은 row를 읽으면, 마지막 쓰기가 이전 쓰기를 덮어씀.

### 해결방안

| 방안 | 설명 | 장점 | 단점 |
|------|------|------|------|
| A. 비관적 락 (FOR UPDATE) | `findByIdForUpdate()` 후 변경 | 구현 단순, 정합성 보장 | 동시 요청 시 대기 발생 |
| B. 낙관적 락 (@Version) | 충돌 시 재시도 | 대기 없음 | 재시도 로직 필요, 충돌 빈번 시 성능 저하 |
| C. UPDATE SET count = count + 1 | SQL 레벨 원자적 증감 | 가장 빠름 | JPA Entity 상태와 불일치 가능, 도메인 로직 누출 |
| D. Redis 원자적 증감 + 비동기 동기화 | INCR 명령 사용 | 매우 빠름 | 정합성 보장 복잡, Redis 장애 시 불일치 |

### 최종 선택

**A. 비관적 락 (FOR UPDATE)**

### 근거

- 좋아요는 사용자당 1회이므로 동일 상품에 대한 동시 충돌 빈도가 낮다
- C는 성능적으로 최선이지만 JPA Entity 캐시와 불일치 발생 가능
- B는 좋아요 특성상 충돌 시 재시도가 UX에 부정적
- D는 현재 규모에서 과잉 설계
- 동시성 테스트(20 스레드)로 정합성 검증 완료

---

## 5. 캐시 전략: Write-Through vs Cache-Aside

### 문제

상품 상세와 목록 조회에 Redis 캐시를 적용할 때, 캐시 갱신 전략을 결정해야 한다.

### 해결방안

| 방안 | 설명 | 장점 | 단점 |
|------|------|------|------|
| A. Cache-Aside (Lazy Loading) | 읽기 시 캐시 미스면 DB 조회 후 캐시 저장 | 구현 단순 | 첫 요청 느림, 캐시 불일치 가능 |
| B. Write-Through | 쓰기 시 DB + 캐시 동시 갱신 | 캐시 일관성 높음 | 쓰기 시 지연 증가 |
| C. Write-Behind | 캐시에만 쓰고 비동기로 DB 반영 | 쓰기 빠름 | 데이터 유실 위험, 복잡 |

### 최종 선택

- **상품 상세**: B. Write-Through (수정 시 캐시 갱신, 삭제 시 evict)
- **상품 목록**: A + Spring `@Cacheable` (수정/삭제 시 evict)

### 근거

- 상품 상세는 수정 빈도가 낮고 읽기가 압도적이므로, Write-Through로 항상 최신 캐시 유지
- 상품 목록은 정렬/페이징 조합이 다양하여 Write-Through로 모든 캐시를 갱신하기 어려움. `@Cacheable`로 자동 관리하고, 변경 시 해당 브랜드의 목록 캐시만 evict
- C는 현재 규모에서 복잡도 대비 이점이 없음
