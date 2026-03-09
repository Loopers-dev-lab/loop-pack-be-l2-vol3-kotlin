## 📌 Summary

Spring Batch를 활용하여 `product_metrics` 테이블 기반으로 주간/월간 랭킹 시스템을 구현했습니다. 대량 데이터 집계의 정확성과 안정성을 위해 2-Step 구조로 집계와 랭킹을 분리하고, Materialized View에 TOP 100 랭킹을 저장하여 조회 성능을 최적화했습니다.

**주요 구현 내용:**
- **Spring Batch Job 구현**: `product_metrics` 테이블을 읽어 Chunk-Oriented Processing으로 대량 데이터 집계
- **2-Step 구조**: Step 1에서 점수 집계 → Step 2에서 TOP 100 선정 및 랭킹 부여
- **Materialized View 설계**: 하나의 테이블(`mv_product_rank`)에 `period_type`으로 주간/월간 구분하여 TOP 100 저장
- **Ranking API 확장**: 기존 API에 `period` 파라미터 추가하여 일간(Redis), 주간/월간(Materialized View) 랭킹 제공
- **commerce-batch 모듈 분리**: 실행 주기, 트랜잭션 성격, 장애 대응 방식의 차이를 고려하여 API와 배치를 독립적인 애플리케이션으로 분리 (지속 실행 vs 단발성 실행, 짧은 트랜잭션 vs 긴 트랜잭션, 즉시 응답 vs 재시작 가능)
- **비즈니스 로직 중심 테스트**: 배치 전체 실행 대신 Reader/Processor/Writer의 핵심 로직만 단위 테스트로 검증

**구현된 기능:**
- `GET /api/v1/rankings?date=yyyyMMdd&period=WEEKLY&size=20&page=1`: 주간/월간 랭킹 조회
- Spring Batch Job 파라미터 기반 실행: `periodType=WEEKLY targetDate=20241215`

## 💬 Review Points

### 1. 2-Step 구조로 집계와 랭킹 분리: 전체 데이터 기반 정확한 TOP 100 선정

**배경 및 설계 의도:**
대량 데이터를 Chunk 단위로 처리할 때, 각 Chunk마다 TOP 100을 계산하면 전체 데이터를 기반으로 한 정확한 TOP 100을 선정할 수 없습니다. 예를 들어, 첫 번째 Chunk에서 점수가 높은 상품 100개를 선정했지만, 이후 Chunk에서 더 높은 점수를 가진 상품이 나타날 수 있어 결과가 부정확해집니다.

이 문제를 해결하기 위해 Step을 실패 격리와 재시작 단위로 사용하여 집계 계산과 랭킹 적재를 분리했습니다. 이렇게 분리하면:
- **전체 데이터 기반 정확한 TOP 100 선정**: Step 1에서 모든 데이터를 집계한 후, Step 2에서 전체 집계 데이터를 기반으로 랭킹 계산
- **트랜잭션 경계 명확화**: 각 Step이 독립적인 트랜잭션 경계를 가지므로, 집계 계산과 랭킹 적재의 트랜잭션 성격 차이를 명확히 구분
- **재시작 가능성**: Step 1이 완료되면 Step 2는 독립적으로 재시작 가능하여, 집계 계산은 성공했지만 랭킹 적재만 실패한 경우 Step 2만 재실행 가능
- **의존성 분리**: Step 1의 집계 결과를 임시 테이블에 저장하여 Step 2와의 의존성을 명확히 분리

**구조:**
```
Step 1: scoreAggregationStep
  ├─ Reader: product_metrics 테이블 페이징 조회 (Chunk 단위)
  ├─ Processor: Pass-through
  └─ Writer: product_id별 메트릭 집계 → tmp_product_rank_score 저장
      ↓ (임시 테이블을 통한 데이터 전달)
Step 2: rankingCalculationStep
  ├─ Reader: tmp_product_rank_score 전체 조회 (점수 내림차순)
  ├─ Processor: TOP 100 선정 및 랭킹 번호 부여
  └─ Writer: mv_product_rank 저장 (delete + insert)
```

**관련 코드:**
```java
@Bean
public Job productRankAggregationJob(
    Step scoreAggregationStep,
    Step rankingCalculationStep
) {
    return new JobBuilder("productRankAggregationJob", jobRepository)
        .start(scoreAggregationStep)        // Step 1 먼저 실행
        .next(rankingCalculationStep)        // Step 1 완료 후 Step 2 실행
        .build();
}
```

**고민한 점 및 의사결정:**

1. **Step 분리 vs StepListener 사용**
   - **고민**: StepListener를 사용하여 하나의 Step 내에서 집계와 저장을 분리하는 방안도 고려했습니다.
   - **선택**: Step을 분리하여 트랜잭션 경계를 명확히 하고, 재시작 가능성을 확보하는 방식을 선택했습니다.
   - **이유**: 집계 계산(Step 1)과 랭킹 적재(Step 2)는 트랜잭션 성격과 자원 사용 특성이 다르다고 판단했습니다. 집계 계산은 재시작 가능성을 우선 고려하고, 랭킹 적재는 데이터 정합성과 원자성을 우선 고려합니다.

2. **임시 테이블 도입**
   - **고민**: Step 간 데이터 전달을 위해 임시 테이블(`tmp_product_rank_score`)을 도입했습니다.
   - **선택**: 임시 테이블을 사용하여 Step 1과 Step 2를 완전히 분리했습니다.
   - **이유**: 
     - Step 1과 Step 2가 독립적인 트랜잭션으로 처리되어 실패 격리가 명확함
     - Step 1이 완료되면 Step 2는 독립적으로 재시작 가능
     - 다음 배치 실행 시 자동으로 덮어쓰기되므로 별도 정리 로직 불필요
   - **트레이드오프**: 중간 저장소 관리 오버헤드가 있지만, 재시작 가능성과 실패 격리 측면에서 이점이 더 큼

3. **주간/월간 처리 방식**
   - **고민**: 주간 랭킹과 월간 랭킹을 별도 Step으로 분리하는 방안을 고려했습니다.
   - **선택**: Job 파라미터(`periodType`)로 분기하여 별도 실행하는 방식을 선택했습니다.
   - **이유**: 
     - 주간 랭킹과 월간 랭킹은 서로 독립적인 결과 스냅샷이므로 별도 실행이 자연스러움
     - 실행 주기가 다르므로(주간은 매주, 월간은 매월) 별도 실행이 더 적합
     - 하나의 Job에서 주간과 월간을 모두 처리하면 불필요한 의존성과 복잡도가 증가
   - **트레이드오프**: Step 단위 재시작은 불가능하지만, Job 단위 재시작으로 충분하며 구조가 단순함

4. **Chunk 단위 처리와 전체 데이터 집계**
   - **고민**: Step 1에서 Chunk 단위로 처리하면서도 전체 데이터를 기반으로 집계해야 합니다.
   - **선택**: Chunk 단위로 집계하되, 같은 `product_id`가 여러 Chunk에 걸쳐 있을 경우 임시 테이블(`tmp_product_rank_score`)에 UPSERT 방식으로 누적했습니다.
   - **이유**: 
     - 메모리 효율성을 위해 Chunk 단위로 처리
     - 전체 데이터를 읽기 전에 집계를 완료할 수 없으므로, 임시 테이블에 누적 저장
     - Step 2에서 전체 집계 데이터를 읽어 TOP 100 선정
   - **구현**: 
     - Step 1의 Writer에서 Chunk 내 product_id별로 집계한 후, 기존 데이터를 일괄 조회(`findAllByProductIdIn`)하여 누적합니다.
     - 누적된 데이터를 `productRankScoreRepository.saveAll()`로 저장하며, Repository 구현체에서 `entityManager.merge()`를 사용하여 UPSERT 방식으로 저장합니다.

5. **Materialized View 저장 방식: delete+insert**
   - **고민**: Step 2에서 Materialized View에 저장할 때 upsert, delete+insert, staging 기반 교체 방식을 고려했습니다.
   - **선택**: delete+insert 방식을 선택했습니다.
   - **이유**: 
     - 단순하고 의도가 명확함 (기존 데이터를 완전히 교체)
     - 랭킹을 기간 종료 시점의 스냅샷으로 다루는 설계 원칙과 일치
     - 과제 범위와 운영 복잡도를 고려했을 때 가장 적절
   - **구현**: 
     - Step 2 Writer에서 모든 Chunk를 메모리에 수집한 후, 각 Chunk마다 전체 데이터를 저장합니다.
     - `saveRanks()` 메서드에서 `deleteByPeriod()` 호출 후 `entityManager.persist()`로 저장합니다.
     - 각 Chunk마다 전체를 저장하지만, `saveRanks()`가 delete+insert를 수행하므로 중복 저장 문제가 없습니다.

---

### 2. Materialized View 설계: 하나의 테이블에 period_type으로 구분

**배경 및 문제 상황:**
요구사항에서는 `mv_product_rank_weekly`와 `mv_product_rank_monthly`를 별도 테이블로 설계하라고 했습니다. 하지만 실제 구현에서는 하나의 테이블(`mv_product_rank`)에 `period_type` 컬럼으로 주간/월간을 구분하는 방식으로 구현했습니다.

**해결 방안:**
논리적으로는 별도 테이블처럼 동작하지만, 물리적으로는 하나의 테이블에 `period_type`으로 구분하는 방식을 선택했습니다:
- **테이블 구조**: `mv_product_rank` 테이블에 `period_type` (WEEKLY/MONTHLY) 컬럼으로 구분
- **인덱스 전략**: `(period_type, period_start_date, rank)` 복합 인덱스로 기간별 랭킹 조회 최적화
- **조회 로직**: `period_type`과 `period_start_date`로 필터링하여 조회

이 방식의 장점:
- 테이블 관리가 단순함 (하나의 테이블만 관리)
- 인덱스 전략이 명확함
- 향후 일간 랭킹 추가 시에도 동일한 테이블 구조 활용 가능

**관련 코드:**
```java
// apps/commerce-batch/src/main/java/com/loopers/domain/rank/ProductRank.java
@Entity
@Table(
    name = "mv_product_rank",
    indexes = {
        @Index(name = "idx_period_type_start_date_rank", columnList = "period_type, period_start_date, rank"),
        @Index(name = "idx_period_type_start_date_product_id", columnList = "period_type, period_start_date, product_id")
    }
)
public class ProductRank {
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private PeriodType periodType; // WEEKLY 또는 MONTHLY
    
    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;
    // ...
}
```

**고민한 점:**
- 요구사항에서는 별도 테이블을 요구했지만, 하나의 테이블에 `period_type`으로 구분하는 방식이 더 유연하고 관리하기 쉽다고 판단했습니다. 논리적으로는 별도 테이블처럼 동작하므로 요구사항의 의도는 충족한다고 봅니다.
- 향후 일간 랭킹을 추가할 때도 동일한 테이블 구조를 활용할 수 있어 확장성이 좋습니다.

---

### 3. 배치 모듈 분리: API와 배치를 독립적인 애플리케이션으로 분리

**배경 및 문제 상황:**
API 요청 처리와 배치 집계는 실행 주기, 트랜잭션 성격, 장애 대응 방식이 다릅니다. API는 실시간 요청 처리에 최적화되어 있고, 배치는 대량 데이터 처리에 최적화되어 있습니다. 하나의 모듈에 두 가지를 모두 포함하면 설정, Job/Step 구성, 테스트 전략이 섞여 관리 복잡도가 증가합니다.

**분리의 핵심 이유:**

1. **실행 주기의 차이**
   - **API**: 지속 실행 (Long-running) - HTTP 요청 대기 상태로 계속 실행
   - **배치**: 단발성 실행 (Short-lived) - Job 실행 후 자동 종료
   ```bash
   # API: 서버 시작 후 계속 실행
   java -jar commerce-api.jar
   
   # 배치: Job 완료 후 자동 종료
   java -jar commerce-batch.jar \
     --spring.batch.job.names=productRankAggregationJob \
     periodType=WEEKLY targetDate=20241215
   ```

2. **트랜잭션 성격의 차이**
   - **API**: 짧은 트랜잭션 (수백 ms ~ 수초), 다중 요청 동시 처리
   - **배치**: 긴 트랜잭션 (수분 ~ 수시간), Chunk 단위 순차 처리, 재시작 가능

3. **장애 대응 방식의 차이**
   - **API**: 즉시 응답 (Circuit Breaker, Retry, Fallback)
   - **배치**: 재시작 가능 (Chunk 단위 재시작, Spring Batch 메타데이터로 재시작 지점 추적)

4. **독립적 실행, 재실행, 관측**
   - **독립 실행**: 배치 실행 시 API 서버 불필요
   - **독립 재실행**: 실패 시 마지막 완료된 Chunk부터 재시작, 멱등성 보장
   - **관측 가능**: Spring Batch 메타데이터로 Job/Step/Chunk 단위 추적

**해결 방안:**
`commerce-batch` 모듈을 별도로 분리하여 독립적인 애플리케이션으로 구성했습니다:
- **독립 실행**: `BatchApplication`을 통해 배치만 독립적으로 실행 가능
- **설정 분리**: `application.yml`에서 배치 전용 설정 관리 (웹 서버 비활성화, Job 자동 실행 비활성화)
- **의존성 최소화**: Kafka, Feign Client, Resilience4j 등 불필요한 의존성 제거
- **도메인 공유**: `com.loopers.domain` 패키지의 도메인은 공유하되, Repository 구현은 모듈별로 분리
- **테스트 전략 분리**: 배치 테스트는 비즈니스 로직 중심의 단위 테스트로 구성

**구조:**
```
commerce-api/
  └─ API 요청 처리, 실시간 랭킹 조회 (Redis)
      ├─ 웹 서버 활성화 (Servlet)
      ├─ Feign Client, Resilience4j
      └─ HTTP 요청 기반 테스트
  
commerce-batch/
  └─ 배치 집계, Materialized View 적재
      ├─ BatchApplication (독립 실행, Job 완료 후 자동 종료)
      ├─ ProductRankJobConfig (Job/Step 구성)
      ├─ 웹 서버 비활성화 (web-application-type: none)
      ├─ Spring Batch 전용 의존성
      └─ 비즈니스 로직 중심 단위 테스트
```

**관련 코드:**
```java
// apps/commerce-batch/src/main/java/com/loopers/BatchApplication.java
@SpringBootApplication(scanBasePackages = "com.loopers")
@EnableJpaRepositories(basePackages = "com.loopers.infrastructure")
@EntityScan(basePackages = "com.loopers.domain")
public class BatchApplication {
    public static void main(String[] args) {
        // Job 완료 후 자동 종료
        System.exit(SpringApplication.exit(SpringApplication.run(BatchApplication.class, args)));
    }
}

// apps/commerce-batch/src/main/resources/application.yml
spring:
  main:
    web-application-type: none # 배치 전용이므로 웹 서버 불필요
  batch:
    jdbc:
      initialize-schema: always # Spring Batch 메타데이터 테이블 자동 생성
    job:
      enabled: false # 명령줄에서 수동 실행하므로 자동 실행 비활성화
```

**분리의 효과:**
- ✅ **관리 복잡도 감소**: 설정, Job/Step 구성, 테스트 전략 분리
- ✅ **의존성 최소화**: 배치 모듈에 불필요한 의존성 제거 (Kafka, Feign Client, Resilience4j)
- ✅ **배포 전략 분리**: API는 수평 확장, 배치는 수직 확장
- ✅ **모니터링 분리**: API는 Actuator, 배치는 Spring Batch 메타데이터
- ✅ **장애 격리**: 배치 작업 실패가 API 서비스에 영향 없음

**고민한 점:**
- 모듈을 분리하면 코드 중복이 발생할 수 있지만, 각 모듈의 목적이 다르므로 분리하는 것이 더 명확하다고 판단했습니다.
- 도메인은 공유하되, Repository 구현은 모듈별로 분리하여 각 모듈의 필요에 맞게 최적화했습니다. 예를 들어, 배치 모듈에서는 대량 조회에 최적화된 Repository를 구현했습니다.

---

### 4. 배치 테스트 전략: 비즈니스 로직 중심의 단위 테스트

**배경 및 설계 의도:**
멘토링 세션에서 배치 전체를 exec해서 잘 실행되는지를 확인하는 것보다 그 안에 있는 processor같은 의미있는 비즈니스 로직에 대한 테스트로 처리하는 게 낫다는 조언을 받았습니다. 따라서 배치 전체 실행 테스트 대신, 비즈니스 로직이 있는 컴포넌트에 대한 단위 테스트에 초점을 두었습니다:
- **Reader/Processor/Writer 단위 테스트**: 각 컴포넌트의 비즈니스 로직(집계, 점수 계산, 랭킹 부여 등)을 Mock을 사용하여 격리된 환경에서 검증
- **배치 전체 실행 테스트는 제외**: 배치 전체를 실행하는 통합 테스트는 작성하지 않음
- **핵심 로직 검증**: 메트릭 집계, 점수 계산, TOP 100 필터링 등 핵심 로직만 독립적으로 검증

**테스트 예시:**
```java
// apps/commerce-batch/src/test/java/com/loopers/infrastructure/batch/rank/ProductRankScoreAggregationWriterTest.java
@ExtendWith(MockitoExtension.class)
class ProductRankScoreAggregationWriterTest {
    @Mock
    private ProductRankScoreRepository productRankScoreRepository;
    
    @InjectMocks
    private ProductRankScoreAggregationWriter writer;
    
    @Test
    void aggregatesMetricsByProductId() throws Exception {
        // Chunk 내에서 같은 product_id를 가진 메트릭을 집계하는 로직 검증
        // ...
    }
    
    @Test
    void calculatesScoreWithCorrectWeights() throws Exception {
        // 점수 계산 로직 검증 (가중치: 좋아요 0.3, 판매량 0.5, 조회수 0.2)
        // ...
    }
}
```

**고민한 점:**
- 각 컴포넌트의 핵심 로직을 독립적으로 검증하면, 변경 시 영향 범위를 명확히 파악할 수 있습니다.
- 비즈니스 로직 중심의 단위 테스트로 구성하면 테스트가 단순해지고 실행 시간도 짧아집니다. 또한 각 컴포넌트의 책임이 명확해져 유지보수가 쉬워집니다.


## ✅ Checklist

### Spring Batch
- [x] **Spring Batch Job을 작성하고, 파라미터 기반으로 동작시킬 수 있다**
  - Job 파라미터: `periodType`(WEEKLY/MONTHLY), `targetDate`(yyyyMMdd)
  - `apps/commerce-batch/src/main/java/com/loopers/infrastructure/batch/rank/ProductRankJobConfig.java`

- [x] **Chunk Oriented Processing (Reader/Processor/Writer) 기반의 배치 처리를 구현했다**
  - Chunk 크기: 100
  - Step 1: Reader(페이징 조회) → Processor(Pass-through) → Writer(집계 및 저장)
  - Step 2: Reader(전체 조회) → Processor(TOP 100 선정) → Writer(저장)
  - `apps/commerce-batch/src/main/java/com/loopers/infrastructure/batch/rank/ProductRankJobConfig.java`

- [x] **집계 결과를 저장할 Materialized View의 구조를 설계하고 올바르게 적재했다**
  - 테이블: `mv_product_rank` (period_type으로 주간/월간 구분)
  - 저장 방식: `delete + insert` (TOP 100만 저장)
  - `apps/commerce-batch/src/main/java/com/loopers/domain/rank/ProductRank.java`

### Ranking API
- [x] **API가 일간, 주간, 월간 랭킹을 제공하며 조회해야 하는 형태에 따라 적절한 데이터를 기반으로 랭킹을 제공한다**
  - 일간: Redis ZSET에서 조회
  - 주간/월간: Materialized View에서 조회
  - `GET /api/v1/rankings?date=yyyyMMdd&period=WEEKLY&size=20&page=1`
  - `apps/commerce-api/src/main/java/com/loopers/interfaces/api/ranking/RankingV1Controller.java`
  - `apps/commerce-api/src/main/java/com/loopers/application/ranking/RankingService.java`

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
  * 상품 순위 조회에 기간 선택 옵션 추가 (일간/주간/월간)
  * 일간 순위는 Redis에서, 주간/월간 순위는 최적화된 데이터 저장소에서 제공
  * 페이지네이션을 지원하는 상품 순위 조회 기능 추가

* **개선**
  * 순위 조회 실패 시 자동으로 이전 데이터로 안정적 제공
  * 배치 처리 기반 상품 메트릭 수집 및 순위 계산 시스템 구축

<sub>✏️ Tip: You can customize this high-level summary in your review settings.</sub>

<!-- end of auto-generated comment: release notes by coderabbit.ai -->
