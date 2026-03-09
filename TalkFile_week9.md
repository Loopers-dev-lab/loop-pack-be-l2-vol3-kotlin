## 📌 Summary

Kafka Consumer를 통해 수신한 이벤트를 기반으로 Redis ZSET을 이용한 실시간 랭킹 시스템을 구현했습니다.

**주요 구현 내용:**
- **랭킹 집계 (commerce-streamer)**: Kafka 이벤트 수신 → Spring ApplicationEvent 발행 → 랭킹 점수 계산 및 Redis ZSET 적재
- **랭킹 조회 (commerce-api)**: Redis ZSET에서 랭킹 조회 및 상품 정보 Aggregation
- **이벤트별 가중치 적용**: 조회(0.1), 좋아요(0.2), 주문(0.6) 가중치로 점수 계산
- **Graceful Degradation**: Redis 장애 시 스냅샷 → 기본 랭킹(좋아요순) Fallback
- **ZSET 모듈 분리**: Redis ZSET 조작 로직을 별도 모듈로 분리하여 재사용성 향상

**구현된 기능:**
- `GET /api/v1/rankings?date=yyyyMMdd&size=20&page=1`: 랭킹 페이지 조회
- 상품 상세 조회 시 해당 상품의 랭킹 정보 포함

## 💬 Review Points
### 1. 랭킹은 도메인이 아닌 유스케이스로 판단

**배경 및 문제 상황:**
랭킹 시스템을 구현할 때, 랭킹이 도메인인지 유스케이스인지 판단해야 했습니다. 도메인으로 취급하면 별도의 도메인 레이어를 만들어야 하지만, 랭킹은 비즈니스 규칙을 갖는 독립적인 도메인이 아니라 조회용 파생 데이터(Read Model)입니다.

**해결 방안:**
랭킹을 도메인이 아닌 Application 레이어의 유스케이스로 판단하여, 별도의 도메인 레이어를 만들지 않고 Application 레이어에만 구현했습니다. 랭킹은 CQRS 패턴의 Read Side로 취급하여, Write Side(도메인 이벤트) → Kafka → Read Side(랭킹 집계) → Redis ZSET 구조로 설계했습니다.

**관련 코드:**
```java
// apps/commerce-streamer/src/main/java/com/loopers/application/ranking/RankingService.java
/**
 * 랭킹 점수 계산 및 ZSET 적재 서비스.
 * <p>
 * Application 유즈케이스: Ranking은 도메인이 아닌 파생 View로 취급
 * CQRS Read Model: Write Side(도메인) → Kafka → Read Side(Application) → Redis ZSET
 */
@Service
public class RankingService {
    // 랭킹 점수 계산 및 ZSET 적재 로직
}
```

**고민한 점:**
- 랭킹을 도메인으로 취급하면 별도의 도메인 레이어를 만들어야 하지만, 랭킹은 비즈니스 규칙을 갖는 독립적인 도메인이 아니라 조회용 파생 데이터입니다. 따라서 Application 레이어에만 구현하는 것이 적절하다고 판단했습니다.
- 랭킹은 CQRS 패턴의 Read Side로 취급하여, Write Side(도메인 이벤트)와 분리했습니다. 이렇게 하면 도메인 로직과 랭킹 집계 로직이 독립적으로 진화할 수 있습니다.

---

### 2. 외부 이벤트와 내부 이벤트 구분: Spring ApplicationEvent 사용

**배경 및 문제 상황:**
Kafka Consumer에서 이벤트를 수신한 후, 랭킹 집계에 필요한 데이터를 수집하는 것과 랭킹 점수를 계산하는 것은 서로 다른 책임입니다. Kafka로 consume하는 것은 외부 시스템과의 통신(인터페이스 계층)이고, ZSET의 점수 계산은 애플리케이션 내부 로직(애플리케이션 계층)입니다.

**해결 방안:**
도메인의 책임을 명확히 하기 위해, Kafka Consumer는 외부 이벤트를 수신하고 Spring ApplicationEvent로 발행하는 역할만 담당하고, 랭킹 계산 로직은 ApplicationEvent를 구독하는 별도의 핸들러에서 처리하도록 분리했습니다. 이렇게 하면 Kafka Consumer는 메시지 수신/파싱만 담당하고, 비즈니스 로직은 애플리케이션 계층에서 처리할 수 있습니다.

**구조:**
```
Kafka (외부 시스템)
    ↓
RankingConsumer (인터페이스 레이어)
    ├─ Kafka 메시지 수신/파싱
    ├─ 멱등성 체크
    └─ Spring ApplicationEvent 발행
        ↓
RankingEventListener (인터페이스 레이어)
    └─ @EventListener 구독 (@Async로 비동기 처리)
        ↓
RankingEventHandler (애플리케이션 레이어)
    └─ RankingService 호출
        ↓
RankingService (애플리케이션 레이어)
    └─ Redis ZSET 적재
```

**관련 코드:**
```java
// apps/commerce-streamer/src/main/java/com/loopers/interfaces/consumer/RankingConsumer.java
@KafkaListener(topics = "like-events", containerFactory = KafkaConfig.BATCH_LISTENER)
public void consumeLikeEvents(List<ConsumerRecord<String, Object>> records, Acknowledgment acknowledgment) {
    // Kafka 메시지 수신/파싱 후 Spring ApplicationEvent 발행
    LikeEvent.LikeAdded event = parseLikeEvent(record.value());
    applicationEventPublisher.publishEvent(event);
    eventHandledService.markAsHandled(eventId, "LikeAdded", "like-events");
}

// apps/commerce-streamer/src/main/java/com/loopers/interfaces/event/ranking/RankingEventListener.java
@Async
@EventListener
public void handleLikeAdded(LikeEvent.LikeAdded event) {
    rankingEventHandler.handleLikeAdded(event);
}

// apps/commerce-streamer/src/main/java/com/loopers/application/ranking/RankingEventHandler.java
public void handleLikeAdded(LikeEvent.LikeAdded event) {
    rankingService.addLikeScore(event.productId(), LocalDate.now(), true);
}
```

**고민한 점:**
- Kafka Consumer에서 직접 RankingService를 호출하는 방식도 가능하지만, 이렇게 하면 Kafka Consumer가 비즈니스 로직을 포함하게 되어 책임이 섞입니다. Spring ApplicationEvent를 사용하면 관심사가 명확히 분리됩니다.
- `@Async`를 사용하여 랭킹 집계 처리를 비동기로 실행하도록 했습니다. 이렇게 하면 Kafka Consumer의 성능에 영향을 주지 않고 랭킹 집계를 처리할 수 있습니다.

---

### 3. 콜드 스타트 문제 해결: Score Carry-Over 방식

**배경 및 문제 상황:**
일별 랭킹을 독립적으로 계산하면, 매일 자정에 랭킹이 0점에서 시작하는 콜드 스타트 문제가 발생합니다. 예를 들어, 어제 인기 있던 상품이 오늘 자정에 갑자기 랭킹에서 사라지면 사용자 경험이 좋지 않습니다.

**해결 방안 및 방식 선택:**

콜드 스타트 문제를 해결하기 위해 두 가지 방식을 고려했습니다. 첫 번째는 `ZUNIONSTORE` 명령어를 사용하여 시간별 랭킹을 별도로 관리하고 이를 일간 랭킹으로 집계하는 방식입니다. 이 방식은 시간별 랭킹 키(`ranking:hourly:yyyyMMddHH`)와 일간 랭킹 키(`ranking:all:yyyyMMdd`)에 이중으로 점수를 적재하고, `ZUNIONSTORE`로 시간별 랭킹을 일간 랭킹으로 집계합니다. 이 방식의 장점은 시간 단위 랭킹 조회가 가능하고, 시간별 가중치를 적용할 수 있으며, 배치 집계가 최적화된다는 점입니다. 하지만 단점으로는 Redis 메모리 사용량이 약 2.4배 증가하고, 시간별 랭킹 적재 로직, 집계 스케줄러, 시간 단위 Carry-Over 스케줄러 등으로 인해 구현 복잡도가 크게 증가하며, 스케줄러를 3개나 관리해야 하고, 두 개의 ZSET에 적재해야 하므로 실시간성도 약간 저하됩니다.

두 번째 방식은 일간 랭킹 키에 직접 점수를 적재하고, 일간 랭킹 Carry-Over만 구현하는 방식입니다. 이 방식은 구현이 단순하고, Redis 메모리 사용량을 최소화하며, 스케줄러를 1개만 관리하면 되고, 단일 ZSET에만 적재하므로 실시간성도 우수하며, 코드 복잡도가 낮아 유지보수가 용이합니다. 다만 시간별 가중치 적용은 불가능하지만, 현재 요구사항에는 이러한 기능이 필요하지 않습니다.

실무 관점에서 ZUNIONSTORE를 사용하지 않는 두 번째 방식을 최종적으로 선택했습니다. 현재 시간별 가중치 적용이 필요하지 않으므로, ZUNIONSTORE를 사용할 때 발생하는 비용(Redis 메모리 사용량 약 2.4배 증가, 스케줄러 관리 복잡도 증가) 대비 얻는 이점이 현재 상황에서는 적다고 판단했습니다. 또한 일간 랭킹에 직접 적재하는 방식이 더 단순하고 이해하기 쉬우며, 운영하기도 쉬워 실무에서 더 적합합니다. 만약 향후 시간별 랭킹 기능이 실제로 필요하다면, 그때 추가하는 것이 더 효율적입니다.

**구현 코드:**
```java
// apps/commerce-streamer/src/main/java/com/loopers/application/ranking/RankingService.java
/**
 * 점수는 일간 랭킹 키에 직접 적재됩니다.
 */
public void addViewScore(Long productId, LocalDate date) {
    String key = keyGenerator.generateDailyKey(date); // ranking:all:yyyyMMdd
    incrementScore(key, productId, VIEW_WEIGHT);
}

/**
 * Score Carry-Over: 오늘의 랭킹을 가중치를 적용하여 내일 랭킹에 반영합니다.
 */
public Long carryOverScore(LocalDate today, LocalDate tomorrow, double carryOverWeight) {
    String todayKey = keyGenerator.generateDailyKey(today);
    String tomorrowKey = keyGenerator.generateDailyKey(tomorrow);
    return zSetTemplate.unionStoreWithWeight(tomorrowKey, todayKey, carryOverWeight);
}
```

**스케줄러 구현:**
```java
// apps/commerce-streamer/src/main/java/com/loopers/infrastructure/scheduler/RankingCarryOverScheduler.java
/**
 * 랭킹 Score Carry-Over 스케줄러.
 * 매일 자정에 전날 랭킹을 오늘 랭킹에 일부 반영하여 콜드 스타트 문제를 완화합니다.
 */
@Scheduled(cron = "0 0 0 * * ?") // 매일 자정 (00:00:00)
public void carryOverScore() {
    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);
    rankingService.carryOverScore(yesterday, today, DEFAULT_CARRY_OVER_WEIGHT);
}
```

**고민한 점:**
- 일별 랭킹을 독립적으로 계산하면 매일 자정에 랭킹이 0점에서 시작하는 콜드 스타트 문제가 발생합니다. 이를 해결하기 위해 Score Carry-Over 방식을 선택했습니다.
- Score Carry-Over 가중치(예: 0.1 = 10%)는 실제 운영 환경에서 조정이 필요할 수 있습니다. 너무 높으면 오래된 랭킹이 계속 반영되어 신선도가 떨어지고, 너무 낮으면 콜드 스타트 문제가 완전히 해결되지 않을 수 있습니다.
- 매일 자정에 자동으로 전날 랭킹을 오늘 랭킹에 반영하는 스케줄러를 구현하여, 콜드 스타트 문제를 완화했습니다.

---

### 4. 예외 처리: Graceful Degradation 전략

**배경 및 문제 상황:**
Redis 장애나 네트워크 오류로 인해 랭킹 조회가 실패할 수 있습니다. 이 경우 사용자에게 에러를 반환하는 것보다, 대체 데이터를 제공하는 것이 더 나은 사용자 경험을 제공합니다.

**해결 방안:**
멘토링 세션에서 "DB 실시간 재계산은 위험하므로 스냅샷 서빙이 현실적"이라는 조언을 받았습니다. 따라서 Redis 장애 시 인메모리 캐시에 저장된 랭킹 스냅샷을 서빙하도록 구현했습니다. 스냅샷도 없을 경우를 대비해 기본 랭킹(좋아요순)을 최종 Fallback으로 제공하지만, 이는 랭킹을 새로 계산하는 것이 아니라 이미 집계된 좋아요 수를 단순 조회하는 것이므로 DB 부하가 크지 않습니다.

**구현 세부사항:**
1. **Redis 조회 시도**: 먼저 요청한 날짜의 랭킹을 조회합니다.
2. **스냅샷 Fallback**: Redis 장애 시 인메모리 캐시에 저장된 랭킹 스냅샷을 조회합니다.
3. **전날 스냅샷 Fallback**: 당일 스냅샷이 없으면 전날 스냅샷을 조회합니다.
4. **기본 랭킹 Fallback**: 스냅샷도 없으면 기본 랭킹(좋아요순)을 제공합니다. 이는 랭킹을 계산하는 것이 아니라 이미 집계된 좋아요 수를 조회하는 단순 쿼리이므로 DB 부하가 크지 않습니다.

**관련 코드:**
```java
// apps/commerce-api/src/main/java/com/loopers/application/ranking/RankingService.java
@Transactional(readOnly = true)
public RankingsResponse getRankings(LocalDate date, int page, int size) {
    try {
        return getRankingsFromRedis(date, page, size);
    } catch (DataAccessException e) {
        // Redis 장애 시 스냅샷으로 Fallback
        Optional<RankingsResponse> snapshot = rankingSnapshotService.getSnapshot(date);
        if (snapshot.isPresent()) {
            return snapshot.get();
        }
        
        // 전날 스냅샷 시도
        Optional<RankingsResponse> yesterdaySnapshot = rankingSnapshotService.getSnapshot(date.minusDays(1));
        if (yesterdaySnapshot.isPresent()) {
            return yesterdaySnapshot.get();
        }
        
        // 최종 Fallback: 기본 랭킹(좋아요순) - 단순 조회, 계산 아님
        return getDefaultRankings(page, size);
    }
}

private RankingsResponse getDefaultRankings(int page, int size) {
    // 좋아요순으로 상품 조회 (이미 집계된 좋아요 수 단순 조회)
    List<Product> products = productService.findAll(null, "likes_desc", page, size);
    // ... 상품 정보 Aggregation 및 랭킹 항목 생성
}
```

**스냅샷 저장 및 조회 구현:**
```java
// apps/commerce-api/src/main/java/com/loopers/infrastructure/scheduler/RankingSnapshotScheduler.java
@Scheduled(fixedRate = 3600000) // 1시간마다
public void saveRankingSnapshot() {
    LocalDate today = LocalDate.now();
    try {
        // 상위 100개 랭킹을 스냅샷으로 저장
        RankingService.RankingsResponse rankings = rankingService.getRankingsFromRedis(today, 0, 100);
        rankingSnapshotService.saveSnapshot(today, rankings);
    } catch (DataAccessException e) {
        // Redis 장애 시 스냅샷 저장 스킵 (다음 스케줄에서 재시도)
    }
}

// apps/commerce-api/src/main/java/com/loopers/application/ranking/RankingSnapshotService.java
@Service
public class RankingSnapshotService {
    private final Map<String, RankingService.RankingsResponse> snapshotCache = new ConcurrentHashMap<>();
    private static final int MAX_SNAPSHOTS = 7; // 최근 7일치만 보관

    public void saveSnapshot(LocalDate date, RankingService.RankingsResponse rankings) {
        snapshotCache.put(date.format(DATE_FORMATTER), rankings);
        cleanupOldSnapshots(); // 메모리 관리
    }

    public Optional<RankingService.RankingsResponse> getSnapshot(LocalDate date) {
        return Optional.ofNullable(snapshotCache.get(date.format(DATE_FORMATTER)));
    }
}
```

**고민한 점:**
- **스냅샷 저장 방식 선택**: 멘토링 세션에서 "DB 실시간 재계산은 위험하므로 스냅샷 서빙이 현실적"이라는 조언을 받았습니다. 스냅샷 저장 방식으로는 인메모리 캐시와 파일 시스템 두 가지를 고려했는데, 인메모리 캐시 방식을 선택했습니다. 랭킹은 비즈니스 결정이 아닌 조회용 파생 데이터이므로, 영속성이 필수는 아니며 스냅샷이 없어도 기본 랭킹(좋아요순)으로 대체 가능합니다. 인메모리 캐시는 구현이 간단하고 성능이 우수하며, 애플리케이션 재시작 시 스냅샷이 사라지더라도 기본 랭킹으로 Fallback할 수 있어 충분하다고 판단했습니다. 향후 영속성이 필요해지면 파일 시스템이나 Redis에 별도 키로 저장하는 방식으로 확장할 수 있습니다.
- **스냅샷 저장 주기 선택**: 스냅샷 저장 주기를 1시간으로 지정했습니다. 랭킹은 상위 10위 내에서는 상대적으로 안정적이므로, 1시간정도로 업데이트해도 사용자가 체감하기 어렵다고 판단했습니다. 또한 Redis 장애 시 1시간 전 스냅샷도 어제 랭킹보다 훨씬 신선하며, 기본 랭킹으로 최종 Fallback할 수 있어 충분해 보입니다.
- **기본 랭킹 Fallback의 정당성**: 스냅샷도 없을 경우를 대비해 기본 랭킹(좋아요순)을 최종 Fallback으로 제공하지만, 이는 랭킹을 새로 계산하는 것이 아니라 이미 집계된 좋아요 수를 단순 조회하는 것입니다. `productService.findAll(null, "likes_desc", page, size)`는 인덱스가 있는 컬럼을 기준으로 정렬된 결과를 반환하는 단순 쿼리이므로, 랭킹을 실시간으로 계산하는 것과는 다르게 DB 부하가 크지 않습니다. 다만 멘토링 조언에 따라 스냅샷을 우선적으로 사용하고, 기본 랭킹은 최후의 수단으로만 사용합니다.

---

### 5. ZSET을 별도 모듈로 분리

**배경 및 문제 상황:**
Redis ZSET 조작 로직이 여러 곳에서 사용될 수 있으므로, 재사용성을 높이기 위해 별도 모듈로 분리하는 것이 좋습니다.

**해결 방안:**
Redis ZSET 조작 로직을 `modules/redis` 모듈의 `RedisZSetTemplate` 클래스로 분리하여, 다른 애플리케이션에서도 재사용할 수 있도록 했습니다.

**구현 세부사항:**
- `RedisZSetTemplate`: ZSET 조작 기능을 제공하는 템플릿 클래스
  - `incrementScore`: 점수 증가
  - `getTopRankings`: 상위 N개 조회
  - `getRank`: 특정 멤버의 순위 조회
  - `getSize`: ZSET 크기 조회
  - `unionStore`: 여러 ZSET 합치기
  - `unionStoreWithWeight`: 가중치를 적용하여 ZSET 합치기
  - `setTtlIfNotExists`: TTL 설정

**관련 코드:**
```java
// modules/redis/src/main/java/com/loopers/zset/RedisZSetTemplate.java
/**
 * Redis ZSET 템플릿.
 * <p>
 * Redis Sorted Set (ZSET) 조작 기능을 제공합니다.
 * ZSET은 Redis 전용 데이터 구조이므로 인터페이스 분리 없이 클래스로 직접 제공합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class RedisZSetTemplate {
    private final RedisTemplate<String, String> redisTemplate;
    
    public void incrementScore(String key, String member, double score) {
        redisTemplate.opsForZSet().incrementScore(key, member, score);
    }
    
    public List<ZSetEntry> getTopRankings(String key, long start, long end) {
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, start, end);
        // ... 변환 로직
    }
    
    // ... 기타 메서드
}
```

**고민한 점:**
- ZSET은 Redis 전용 데이터 구조이므로 인터페이스 분리 없이 클래스로 직접 제공했습니다. 이렇게 하면 불필요한 추상화를 제거하고 단순성을 유지할 수 있습니다.
- `RedisZSetTemplate`을 `@Component`로 등록하여 다른 애플리케이션에서도 쉽게 사용할 수 있도록 했습니다.

## ✅ Checklist
### Ranking Consumer

- [x] **랭킹 ZSET의 TTL, 키 전략을 적절하게 구성하였다**
  - 키 형식: `ranking:all:yyyyMMdd` (일간 랭킹)
  - TTL: 2일 (`Duration.ofDays(2)`)
  - `apps/commerce-streamer/src/main/java/com/loopers/application/ranking/RankingKeyGenerator.java`

- [x] **날짜별로 적재할 키를 계산하는 기능을 만들었다**
  - `RankingKeyGenerator.generateDailyKey()`: 일간 랭킹 키 생성 (`ranking:all:yyyyMMdd`)
  - `apps/commerce-streamer/src/main/java/com/loopers/application/ranking/RankingKeyGenerator.java`

- [x] **이벤트가 발생한 후, ZSET에 점수가 적절하게 반영된다**
  - 조회: Weight = 0.1
  - 좋아요: Weight = 0.2
  - 주문: Weight = 0.6, Score = log(1 + orderAmount) * ORDER_WEIGHT
  - `apps/commerce-streamer/src/main/java/com/loopers/application/ranking/RankingService.java`

### Ranking API

- [x] **랭킹 Page 조회 시 정상적으로 랭킹 정보가 반환된다**
  - `GET /api/v1/rankings?date=yyyyMMdd&size=20&page=1`
  - `apps/commerce-api/src/main/java/com/loopers/interfaces/api/ranking/RankingV1Controller.java`

- [x] **랭킹 Page 조회 시 단순히 상품 ID가 아닌 상품정보가 Aggregation 되어 제공된다**
  - 상품 정보 배치 조회 (N+1 쿼리 문제 방지)
  - 브랜드 정보 배치 조회
  - `apps/commerce-api/src/main/java/com/loopers/application/ranking/RankingService.java` (98-170줄)

- [x] **상품 상세 조회 시 해당 상품의 순위가 함께 반환된다 (순위에 없다면 null)**
  - `RankingService.getProductRank()`: 특정 상품의 순위 조회
  - `apps/commerce-api/src/main/java/com/loopers/application/catalog/CatalogFacade.java` (랭킹 정보 포함)

-->

## 📎 References
<!--
  (Optional: 참고 자료가 없는 작업 - 단순 버그 픽스 등 의 경우엔 해당 란을 제거해주세요 !)
  리뷰어가 참고할 수 있는 추가적인 정보나 문서, 링크 등을 작성해주세요.
  예시:
  - 관련 문서 링크
  - 관련 정책 링크
-->

<!-- This is an auto-generated comment: release notes by coderabbit.ai -->

## Summary by CodeRabbit

## 릴리스 노트

* **새로운 기능**
  * 상품에 순위 정보 추가: 조회수, 좋아요, 주문을 기반으로 계산된 상품 순위가 상품 응답에 포함됩니다.
  * 랭킹 API 추가: /api/v1/rankings 엔드포인트를 통해 날짜별 상품 순위를 페이지네이션과 함께 조회할 수 있습니다.

<sub>✏️ Tip: You can customize this high-level summary in your review settings.</sub>

<!-- end of auto-generated comment: release notes by coderabbit.ai -->
