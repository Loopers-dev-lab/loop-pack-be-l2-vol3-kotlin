# AI 리뷰 트레이드오프 결정 기록

이 문서는 AI 리뷰어(CodeRabbit, Gemini)가 반복 지적하지만, 프로젝트에서 의도적으로 다른 방향을 선택하거나 전제 조건 하에 수용한 사안을 기록한다.
`/review-pr` 스킬의 analyze 페이즈에서 자동 참조하여 이미 결정된 사안의 반복 검토를 방지한다.

> **아카이브**: RD-001 ~ RD-040(라운드 1~9)은 [`docs/review/review-decisions-1.md`](review/review-decisions-1.md)로 분리되어 있다. `/review-pr analyze`가 키워드 매칭으로 `ALREADY_DECIDED`를 식별할 때 두 파일을 모두 참조해야 한다.

## 분류 체계

- **DISMISSED**: 요구사항/스펙 준수 또는 현행 유지 결정. 제안을 수용하지 않으며, "수용 시 회귀 발생" 또는 "이른 추상화" 등 명확한 반대 근거를 갖는다. 본문 `한계` 필드에 재검토 트리거를 기록한다.
- **RISK_ACCEPTED**: 지적 자체는 사실이며 코드상 취약점이 실재하나, 운영 전제(예: 단일 인스턴스, 내부망) 하에서 수용. 전제가 깨지면 즉시 재검토 대상. "문제 없음"이 아니라 "범위상 수용된 리스크"임을 명시한다.
- **AGREED**: 제안을 수용하여 수정 예정 또는 완료. 구현 태스크는 `plan.md`에 연결하며, 본문 `구현 방향` 필드에 접근 방법을 요약한다.

---

## DISMISSED

### RD-041. `period` 파라미터 대소문자 엄격 정책 (lowercase만 허용)
- **keywords**: `period`, `lowercase`, `RankingPeriod.from`, `RankingV1Period.from`, `대소문자`, `명확성 우선`, `400 BAD_REQUEST`
- **리뷰어**: CodeRabbit (PR #38 #2, #4 / PR #39 CR-3, CR-6)
- **repeat_count**: 2
- **최종 결정**: 기각 (요구사항 명시 정책 준수)
- **근거**: CR은 `RankingPeriod.from(value)`에서 `value`를 `lowercase()`로 정규화하여 `DAILY`/`Daily`/`daily`를 모두 허용하라고 제안. 그러나 `docs/requirements/round10-requirements-analysis.md:423`이 "URL 파라미터 값: 소문자 daily/weekly/monthly 고정. 대소문자 섞인 값(Daily, WEEKLY 등)은 400 BAD_REQUEST. **명확성 우선**"이라고 명시적으로 결정한 사안이다. `plan.md:156`의 E2E 테스트 `period=Daily → 400 BAD_REQUEST`가 이미 통과 중이며, 제안 수용 시 스펙 및 기존 테스트 회귀가 발생한다. 엄격 정책은 "클라이언트 입력 표준화 강제" 목적이며 관대한 매칭은 이와 상충한다.
- **한계**: 없음. 엄격 정책은 의도적 선택이며 현재 구현이 스펙과 일치한다.
- **최종 업데이트**: 2026-04-15

### RD-042. weekly/monthly 응답 `rank`는 MV `rank_no` 원본 + 응답 시점 필터링으로 페이지 축소 허용
- **keywords**: `rank`, `rank_no`, `MV`, `filtered rank`, `페이지 축소`, `응답 시점 필터링`, `semantics`
- **리뷰어**: CodeRabbit (PR #38 #1)
- **repeat_count**: 1
- **최종 결정**: 기각 (요구사항 명시 정책 준수)
- **근거**: CR은 `GetRankingUseCase.executeWeekly/executeMonthly`가 비가시 상품 필터 후 `row.rank`(MV 원본)를 그대로 반환하여 daily 경로(`page*size+index+1`로 재부여)와 semantics가 다르다고 지적하며 filtered rank 재부여를 제안. 그러나 `docs/requirements/round10-requirements-analysis.md:241-244`는 "비활성/삭제 상품은 응답 시점에 필터링한다... 응답 필터링으로 인해 페이지 크기가 모자라도 **그대로 허용한다** (over-fetch나 재정렬 없이 자연 축소)"를 **daily·weekly·monthly 공통 정책**으로 고정했다. 또한 `docs/requirements/round10-requirements-analysis.md:301`은 "`content[].rank`: daily는 페이지 오프셋 기반(1-based), **weekly/monthly는 MV에 사전 계산된 `rank_no`**"라고 명시했다. 제안 수용 시 스펙 위반에 해당한다. weekly/monthly의 raw rank는 "배치 스냅샷의 원본 순위"라는 의미를 보존한다.
- **한계**: 프런트가 "첫 번째 아이템 = 1위"를 전제로 UI를 만들면 혼란 여지 존재. 해결은 API 문서에서 "weekly/monthly rank는 배치 스냅샷 raw" 명시.
- **최종 업데이트**: 2026-04-14

### RD-043. 랭킹 가중치(0.1/0.2/0.7) 공유 상수 미추출 — 현행 하드코딩 유지
- **keywords**: `랭킹 가중치`, `RankingWeight`, `공유 상수`, `WeeklyRankingQueryDao`, `MonthlyRankingQueryDao`, `commerce-streamer`
- **리뷰어**: Gemini Code Assist (PR #38 #10, #11)
- **repeat_count**: 1
- **최종 결정**: 기각 (현행 유지)
- **근거**: Gemini는 `WeeklyRankingQueryDao.kt:20-22`, `MonthlyRankingQueryDao.kt:20-22`, 그리고 `commerce-streamer` 실시간 점수 계산 경로에 같은 가중치(0.1/0.2/0.7)가 하드코딩되어 있다며 공유 상수 추출을 제안. 그러나 두 모듈(`commerce-batch`, `commerce-streamer`) 간에 공유할 도메인 모듈이 없고 batch는 streamer 도메인을 참조할 수 없다. 각 모듈에 별도 `RankingWeight` 상수 클래스를 두는 부분 수용도 가능하지만, 3개 상수를 3곳에서만 쓰는 상태에서의 추출은 "Rule of Three" 관점에서 이른 추상화다. 향후 가중치 조정/외부 설정화 요구가 생길 때 공유 모듈 신설과 함께 도입하는 것이 맞다.
- **한계**: 가중치 불일치 시 회귀 위험 있음. 수기 교차 확인이 필요한 상태 유지.
- **최종 업데이트**: 2026-04-14

### RD-044. BatchRankingController 동기 `jobLauncher.run` — 현 단계 acceptable
- **keywords**: `BatchRankingController`, `jobLauncher.run`, `동기`, `async JobLauncher`, `TaskExecutor`, `HTTP 블로킹`
- **리뷰어**: Gemini Code Assist (PR #38 #6)
- **repeat_count**: 1
- **최종 결정**: 기각 (현 단계 acceptable, Nice-to-Have 후순위)
- **근거**: Gemini는 `jobLauncher.run`이 HTTP 스레드를 블로킹하므로 `SimpleAsyncTaskExecutor` 기반 비동기 JobLauncher를 권장. 그러나 현재 Top 100 집계는 소규모(수초 이내)로 예상되며, scheduler 프로파일 단일 인스턴스 + 운영자 수동 백필 용도라 동시성/처리량 요구가 낮다. Job 완료까지 블로킹되는 편이 오히려 HTTP 응답에 최종 `BatchStatus`를 그대로 담을 수 있어 운영 관측성이 좋다. 비동기 전환 시 응답 바디는 `STARTED`만 남아 상태 확인을 위한 별도 polling API가 필요해진다.
- **한계**: Top 100 규모가 커지거나 집계 쿼리 비용이 증가하면 재검토 필요. 실측 지표 기반으로 결정.
- **최종 업데이트**: 2026-04-14

### RD-046. commerce-batch scheduler 프로파일 엔드포인트 무인증 — 학습 프로젝트 + 내부망 전제 유지
- **keywords**: `BatchRankingController`, `/internal/batch/ranking`, `Spring Security`, `SecurityFilterChain`, `내부망`, `포트 8082`, `API Key`
- **리뷰어**: CodeRabbit (PR #38 #15, Major)
- **repeat_count**: 1
- **최종 결정**: 기각 (학습 프로젝트 전제, RD-003과 동일 논지)
- **근거**: CR은 scheduler 프로파일이 포트 8082로 열릴 때 `BatchRankingController`가 무인증이라 임의 요청으로 랭킹 재집계가 가능하다고 지적. 그러나 (1) 학습 프로젝트 + Docker Compose 기반 로컬 개발이 주 환경, (2) 실 배포 시 인그레스/로드밸런서에서 포트 8082 차단 또는 내부망 바인딩으로 보호, (3) 애플리케이션 레이어 Spring Security 도입은 `@Profile("scheduler")` 한정 설정이나 `WebSecurityCustomizer` 등 비용이 premature. RD-003 콜백 HMAC 검증 기각과 동일 논지. 향후 실 운영으로 전환 시 인증 정책을 일괄 도입.
- **한계**: 내부망 가정이 깨지면 즉시 재검토. 실제 배포 시 네트워크 경로와 바인딩 주소를 재점검.
- **최종 업데이트**: 2026-04-14

### RD-047. `commerce-batch` `spring.batch.job.enabled: false` 전역 기본값 — 의도적 설계 유지
- **keywords**: `spring.batch.job.enabled`, `application.yml`, `one-shot`, `CommandLineRunner`, `RankingJobScheduler`, `web-application-type`, `a873b42`
- **리뷰어**: CodeRabbit (PR #38 #14, Critical)
- **repeat_count**: 1
- **최종 결정**: 기각 (의도적 설계)
- **근거**: CR은 `application.yml:17`의 `spring.batch.job.enabled: false` 전역 기본값 때문에 non-scheduler 환경에서 one-shot 배치(`--job.name=...`) 실행 경로가 전무하다고 지적하며 "기본 true, scheduler 프로파일에서만 false"로 전도할 것을 제안. 그러나 직전 커밋 `a873b42 refactor(batch): job.enabled 기본 비활성화 + @ConditionalOnProperty 제거`가 이 방향을 **명시적으로** 결정한 사안이다. `RankingJobScheduler`가 `@Scheduled` 안에서 `jobLauncher.run`을 수동으로 호출하는 구조라 `spring.batch.job.enabled: true`로 두면 애플리케이션 기동 시 모든 Job이 자동 실행되어 스케줄러 설계와 충돌한다. 또한 non-scheduler 환경은 `web-application-type: none`으로 기동 즉시 종료되며, one-shot 실행 경로는 설계상 `scheduler` 프로파일 + HTTP trigger로 단일화했다.
- **한계**: CLI 기반 one-shot 실행 요구가 실제 생기면 `JobLauncherApplicationRunner` + 프로파일별 오버라이드를 도입해야 한다. 현재는 운영 요구 없음.
- **최종 업데이트**: 2026-04-14

### RD-049. `GetRankingUseCase.executeWeekly`/`executeMonthly` 공통 헬퍼 추출 — 현행 유지
- **keywords**: `executeWeekly`, `executeMonthly`, `GetRankingUseCase`, `코드 중복`, `제네릭 헬퍼`, `WeeklyProductRank`, `MonthlyProductRank`
- **리뷰어**: Gemini Code Assist (PR #39 G-2, Medium)
- **repeat_count**: 1
- **최종 결정**: 기각 (현행 유지)
  - **근거**: Gemini는 `GetRankingUseCase.kt:145`에서 `executeWeekly`와 `executeMonthly`가 저장소와 period key 계산 방식만 다르고 로직이 동일하다며 공통 인터페이스 + 제네릭 헬퍼 추출을 제안. 그러나 `WeeklyProductRank`/`MonthlyProductRank` 공통 인터페이스를 도메인 레이어에 도입하려면 도메인 모델 변경이 수반된다. 현재 2개 메서드는 변경 빈도가 낮고, 명시적 분리가 의도를 더 명확하게 전달한다. "Rule of Three" 관점에서도 2개 구현에 대한 추상화는 이른 최적화다.
- **한계**: 향후 분기별(quarterly) 등 새로운 period 추가 시 재검토 필요.
- **최종 업데이트**: 2026-04-15

### RD-050. `findOrCreate`/`findOrCreateDaily` race condition 처리 헬퍼 추출 — 현행 유지
- **keywords**: `findOrCreate`, `findOrCreateDaily`, `race condition`, `제네릭 헬퍼`, `UpdateProductMetricsUseCase`, `코드 중복`
- **리뷰어**: Gemini Code Assist (PR #39 G-3, Medium)
- **repeat_count**: 1
- **최종 결정**: 기각 (현행 유지)
- **근거**: Gemini는 `UpdateProductMetricsUseCase.kt:179`에서 `findOrCreate`와 `findOrCreateDaily`의 race condition 처리 로직이 동일하다며 `<T>` 제네릭 헬퍼로 추출을 제안. 그러나 제네릭 헬퍼는 `find: () -> T?`, `create: () -> T`, `save: (T) -> T` 람다를 파라미터로 받는 구조가 되어 원본 로직보다 가독성이 오히려 낮아진다. `findOrCreate`는 누적 메트릭, `findOrCreateDaily`는 일별 메트릭으로 도메인 의미가 다르며, 현재 2개 메서드로 의도가 명확하다. RD-049와 마찬가지로 추상화 비용이 이익보다 크다.
- **한계**: RD-048 2차 개정으로 `ProductMetricsInitializer` 자체가 폐기되고 `findOrCreate`/`findOrCreateDaily`는 `UpdateProductMetricsUseCase` private 헬퍼로 복원됨. `products` 행 pessimistic lock이 race를 원천 차단하여 `DataIntegrityViolationException` catch 경로도 소멸했으므로, 헬퍼 추출 동기(중복된 race 처리 로직)는 사라진 상태. 2개 private 메서드는 내부 구현으로만 유지하며, 향후 분기별 등 새로운 period 축 추가 시 재검토.
- **최종 업데이트**: 2026-04-15 (RD-048 2차 개정에 따른 문맥 갱신)

---

## RISK_ACCEPTED

### RD-045. `run.id` 기반 JobInstance 중복 실행 차단 부재 — 단일 인스턴스 전제 하 수용
- **keywords**: `run.id`, `JobInstance`, `동시 실행`, `JobExplorer`, `409`, `단일 인스턴스`, `BatchRankingController`, `RankingJobScheduler`, `ShedLock`
- **리뷰어**: CodeRabbit (PR #38 #8, Critical)
- **repeat_count**: 1
- **최종 결정**: 수용된 리스크 (RISK_ACCEPTED). 지적한 취약점은 코드상 실재하나, 단일 인스턴스 전제와 요구사항 §10.7의 `baseDate + run.id` 채택 하에서 수용. "문제 없음"이 아니라 "범위상 수용된 리스크"다.
- **근거**: CR은 `BatchRankingController.kt:71`과 `RankingJobScheduler.kt:47` 양쪽의 `buildParams`가 `System.currentTimeMillis()`를 `run.id`로 넣어 같은 `baseDate`로 호출해도 매번 새 JobInstance가 생성되어 동시 실행을 차단하지 못한다고 지적. **지적 자체는 사실이며, 코드 수준의 동시 실행 방지 장치는 없다.** 다만 본 프로젝트 운영 전제 하에서 수용 가능하다: (1) 배치 서버는 단일 인스턴스 + failover 모델로 운영, 멀티 인스턴스는 파티셔닝 스텝이나 ShedLock 같은 분산 락이 필수(RD-006 참조), (2) Spring `@Scheduled`는 단일 스레드 `ThreadPoolTaskScheduler`로 직렬 실행되므로 같은 스케줄러 내 중복 호출 불가, (3) `BatchRankingController`는 운영자 수동 백필용으로 스케줄러 실행 중 동시 호출 빈도가 실질 0이다. `docs/requirements/round10-requirements-analysis.md:456`도 `baseDate + run.id` 조합을 "동일 기간 재실행 식별자"로 명시적으로 채택했다. 초기에는 DISMISSED로 기록했으나 "문제 없음" 프레이밍이 과장이라는 피드백을 반영하여 RISK_ACCEPTED로 재분류한다.
- **재검토 트리거**: 다음 중 **어느 하나라도 성립하면 즉시 재검토**.
  - (a) 멀티 인스턴스 스케줄러 배포 (파드 복제, 블루/그린 중복 기동 등)
  - (b) HTTP trigger를 외부 스케줄러(크론/워크플로우 엔진 등)로 상시 노출하여 동시 호출 빈도가 관측되는 경우
  - (c) 스케줄러와 HTTP trigger 동시 호출 빈도가 모니터링 지표로 측정되는 시점
- **조치 계획**: 위 트리거 발생 시 `JobExplorer.findRunningJobExecutions(jobName)` 기반 선행 검사 + 409 반환 또는 ShedLock 도입. 요구사항 §10.7도 변경 필요.
- **최종 업데이트**: 2026-04-15

---

## AGREED

### RD-048. `UpdateProductMetricsUseCase` race condition — `products` 행 pessimistic lock 앵커 (TRADEOFF → AGREED 승격, 2차 개정)
- **keywords**: `findOrCreate`, `findOrCreateDaily`, `DataIntegrityViolationException`, `pessimistic lock`, `PESSIMISTIC_WRITE`, `ProductLockEntity`, `ProductLockRepository`, `cross-topic race`, `rollback-only`, `UnexpectedRollbackException`, `UpdateProductMetricsUseCase`, `lost update`
- **리뷰어**: Gemini Code Assist (PR #39 G-1, High)
- **repeat_count**: 1
- **최종 결정**: **수용 (AGREED, 2차 개정)**.
  - **1차 (2026-04-15 오전)**: TRADEOFF → AGREED 승격. `ProductMetricsInitializer`(신규 `@Component`) + `@Transactional(propagation = REQUIRES_NEW)` 분리.
  - **2차 (2026-04-15 오후, 현재)**: **구현 방향 전면 교체**. REQUIRES_NEW 분리는 요구사항 §5 "동일 `@Transactional` 경계" 문면과 충돌(seed row가 부모 TX 바깥에서 커밋되어 롤백 경로에서 잔존). `products` 행을 `PESSIMISTIC_WRITE` lock anchor로 삼아 (a) 요구사항 원문 유지, (b) DIVEx 경로 소멸, (c) catalog-events ↔ order-events cross-topic race 차단을 한 번에 해결한다. 구현 태스크는 `plan.md` Step 2 CP12-B 2차 개정 체크리스트에 연결.
- **근거 (1차 승격)**: Gemini는 `UpdateProductMetricsUseCase.kt:163-179`에서 `DataIntegrityViolationException` catch 후 같은 트랜잭션 내 재조회 시도가 부모 `@Transactional` 컨텍스트를 rollback-only 상태로 만들어 `UnexpectedRollbackException`을 유발할 수 있다고 지적. 초기에는 "Fake 기반 단위 검증 + Kafka retry/DLT fallback"으로 기각했으나 재평가 결과 런타임 정합성 리스크가 실재한다:
  1. `KafkaConfig.kt:88`에서 consumer concurrency=3으로 동일 상품 경합이 현실적.
  2. Spring/Hibernate의 `HibernateJpaDialect`가 `PersistenceException` → `DataIntegrityViolationException` 변환 과정에서 이미 현재 트랜잭션을 rollback-only로 마킹함. 본 RD 초기 본문의 `한계`도 이를 인정하고 있었다.
  3. `handleCatalogEvent`/`handleOrderEvent`의 후속 `productMetricsRepository.save` 호출이 flush 시점에 `UnexpectedRollbackException`을 던지거나 stale entity를 리턴하는 경로가 가능.
  4. Kafka consumer retry + DLT로 최종 정합은 유지되지만, 관측 가능한 런타임 오류와 DLT 적재는 운영 노이즈로 누적된다. 회피 비용이 수용 비용보다 크다.
  5. 초기 기각은 "스코프 회피(리팩터 취향)"였으며, 런타임 정합성 리스크는 리팩터 선택 문제가 아니다.
- **근거 (2차 개정)**: 1차 REQUIRES_NEW 방안을 본 RD와 `review-summary.md`에 반영한 직후, 개발자 재검토 피드백에서 세 가지 문제가 제기되어 방향을 전면 교체한다.
  6. **cross-topic race 미차단**: Kafka partition key가 `productId`여도 `catalog-events`와 `order-events`는 **별도 토픽**이므로 서로 다른 consumer 스레드가 같은 `productId`를 동시에 처리할 수 있다. Kafka 파티셔닝만으로는 cross-topic race를 차단할 수 없고, DB 차원의 공통 lock anchor가 필수. REQUIRES_NEW 분리는 seed 생성 경합만 우회할 뿐 cross-topic race 자체는 방치한다.
  7. **요구사항 §5 문면 위반**: `docs/requirements/round10-requirements-analysis.md:228` — "commerce-streamer는 기존 누적 `product_metrics` 갱신과 **동일 `@Transactional` 경계 안에서** 일간 집계 테이블도 함께 저장한다. 이렇게 해야 두 테이블이 같은 `event_handled` 멱등 키로 보호된다." REQUIRES_NEW로 seed row를 분리하면 부모 TX 롤백 시에도 seed row는 커밋된 채 잔존 → "동일 경계 + `event_handled` 보호" 조건의 원자성이 깨진다. 실용적 영향은 작지만(0 카운터 seed row) 문면상 부합하지 않음.
  8. **lost update는 별개 문제**: REQUIRES_NEW나 native upsert는 "insert 경합"을 완화할 뿐 `find → increment → save` 패턴의 lost update(동시 두 TX가 같은 카운터 값을 읽고 +1 → 두 번 증가가 한 번만 반영)를 막지 못한다. `SELECT ... FOR UPDATE` 기반 pessimistic lock은 lost update까지 자동으로 차단한다.
- **구현 방향 (2차 개정)**:
  - `ProductLockEntity`(신규, commerce-streamer) — `@Entity @Table(name = "products")`에 `@Id val id: Long` 단일 컬럼만 매핑. `BaseEntity` 미상속으로 dirty check 시 `updatedAt` touch 리스크 차단. commerce-api의 `ProductJpaEntity`와는 서로 다른 모듈·다른 엔티티 클래스로 JPA 물리 충돌 없음(같은 테이블 두 엔티티 매핑은 JPA 관점에서 합법).
  - `ProductLockRepository`(domain 인터페이스, commerce-streamer) — `fun findByIdForUpdate(id: Long): Long?` 시그니처. `ProductLockEntity`(infrastructure)를 domain 인터페이스에 노출하지 않음 — 레이어 의존 방향 준수.
  - `ProductLockJpaRepository`(Spring Data) — `@Lock(LockModeType.PESSIMISTIC_WRITE) fun findWithLockById(id: Long): ProductLockEntity?`. 프로젝트 `@Query` 금지 규칙을 위반하지 않으면서 derived method에 락 힌트만 덮어씌우는 형태.
  - `ProductLockRepositoryImpl`(infrastructure) — JPA repo 어댑터로 `entity.id`를 `Long?`으로 변환 반환.
  - `UpdateProductMetricsUseCase`:
    - `handleCatalogEvent` / `handleOrderEvent`에 `@Transactional(isolation = Isolation.READ_COMMITTED)` 적용. MySQL REPEATABLE READ 기본값에서 MVCC 스냅샷이 트랜잭션 첫 읽기 시점에 확정되어 T2가 락 획득 후에도 `findByProductId` null 반환 → unique constraint violation을 일으키는 문제 해결. READ_COMMITTED에서는 각 읽기가 최신 커밋 데이터를 봄.
    - `handleCatalogEvent` / `handleOrderEvent` 진입부: `existsByEventId` 체크 직후 → `productLockRepository.findByIdForUpdate(productId)` 호출 → null 반환 시(상품 부재 edge case) `log.warn` + `eventHandledRepository.save(...)` + return. non-null이면 기존 경로 진행.
    - **lock 획득 직후 post-check (double-checked locking, 사후 fix 반영)**: 락 획득 후에 `existsByEventId`를 재확인하여 동일 `eventId`가 Kafka 동시 재전달로 병렬 도달한 경우 T2가 조기 종료되도록 한다. 이는 단순 런타임 노이즈 제거가 아니라 **정합성 수정**이다 — `EventHandledEntity`는 `@Id val eventId: String` 단일 PK이므로 `save()` 중복 호출이 PK 충돌을 일으켜 자연 차단될 것으로 기대했으나, Spring Data JPA의 `SimpleJpaRepository.save()`는 PK가 non-null이면 `persist` 대신 **`merge`로 fallback**하여 같은 eventId에 대한 두 번째 호출이 조용히 update로 처리되고 카운터가 중복 증가한다(`UpdateProductMetricsUseCaseConcurrencyIT`의 "동일 eventId 중복 도달 시나리오"에서 실증). post-check는 이 merge 경로 자체에 도달하지 못하게 만들어 문제를 원천 차단한다. READ_COMMITTED 격리 수준과 결합하여 T1 commit 이후 T2가 lock을 획득한 시점의 재조회가 최신 커밋을 보는 것이 핵심.
    - `findOrCreate` / `findOrCreateDaily`는 private 헬퍼로 복원. DIVEx catch 블록 제거(lock이 race를 원천 차단하므로 불필요).
    - `ProductMetricsInitializer` + 전용 테스트(`ProductMetricsInitializerTest`)는 **삭제**.
  - 테스트 구조:
    - `FakeProductLockRepository`(testFixtures) — 상품 존재/부재 제어 API + 호출 기록용 counter.
    - `UpdateProductMetricsUseCaseTest`: 락 호출 발생 검증 + 상품 부재 skip 경로 + 기존 카운터/가중치/after-commit 테스트 유지.
    - **`UpdateProductMetricsUseCaseConcurrencyIT`(신규 필수)**: `@SpringBootTest` + TestContainers. 두 시나리오를 포함한다. (1) 두 스레드가 동일 `productId`로 cross-topic 이벤트(`PRODUCT_VIEWED` + `PAYMENT_COMPLETED`)를 동시 실행 → 직렬화되어 최종 카운터가 합산값과 일치함을 검증. (2) 두 스레드가 **동일 `eventId`**로 `handleCatalogEvent`를 동시 실행 → post-check에 의해 T2가 skip되어 `viewCount == 1` + 예외 미발생임을 검증(`ConcurrentLinkedQueue<Throwable>`로 exception을 명시 수집). 이 통합 테스트가 "lock이 실제로 Spring 프록시 경로에서 걸린다"는 사실까지 함께 보장하며, 단위 테스트만으로는 `@Lock` 어노테이션의 프록시 적용 여부 및 Spring Data save()의 merge 경로를 검증할 수 없다.
- **잔여 리스크 / 모니터링**:
  - hot product의 cross-topic 이벤트는 직렬화되므로 consumer lag가 증가할 수 있음. 락 보유 시간 ≈ 수 ms로 정상 범위. 이상 시 consumer lag + `products` lock wait time을 관측해 재검토.
  - `products` 행 부재 edge case(드물지만 가능, 예: 상품 삭제 직후 지연된 이벤트): 위 skip 경로 + `log.warn`으로 흡수. DLT 대신 `event_handled` 저장으로 Kafka retry 무한 루프 차단.
  - `products` 테이블 DDL 변경이 발생하면 `ProductLockEntity`는 `id` 컬럼만 매핑하므로 컬럼 추가/삭제에는 안전. 테이블명 변경 시 commerce-streamer 빌드가 깨짐 — 그 시점에 명시적 대응.
- **연결 태스크**: `plan.md` Step 2 CP12-B (2차 개정 체크리스트 — forward refactor로 1차 구현 제거 + lock anchor 도입). 사후 fix로 발견된 post-check도 같은 단일 의도(RD-048 race condition fix)이므로 한 커밋에 함께 담는다.
- **최종 업데이트**: 2026-04-15 (2차 개정 + post-check 사후 fix: Spring Data JPA `save() merge` 함정 차단)
