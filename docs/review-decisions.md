# AI 리뷰 트레이드오프 결정 기록

이 문서는 AI 리뷰어(CodeRabbit, Gemini)가 반복 지적하지만, 프로젝트에서 의도적으로 다른 방향을 선택한 사안을 기록한다.
`/review-pr` 스킬의 analyze 페이즈에서 자동 참조하여 이미 결정된 사안의 반복 검토를 방지한다.

---

## RD-001. JPA Entity equals/hashCode 미구현
- **keywords**: `equals`, `hashCode`, `BaseEntity`, `identity`, `JPA proxy`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각
- **근거**: 도메인 모델에서 VO로 identity를 관리하고, JPA Entity는 인프라 구현체. 컬렉션에 엔티티를 직접 넣는 패턴 미사용. auto-generated ID 기반 equals/hashCode는 `id = 0`인 새 엔티티 간 동등성 문제를 오히려 유발.
- **최종 업데이트**: 2026-03-17

## RD-002. Hook 상대경로 보안
- **keywords**: `hook`, `settings.json`, `상대경로`, `relative path`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각
- **근거**: Claude Code는 항상 프로젝트 루트에서 실행. 비이슈.
- **최종 업데이트**: 2026-03-17

## RD-003. 콜백 엔드포인트 서명/IP 검증
- **keywords**: `callback`, `signature`, `HMAC`, `IP whitelist`, `콜백 보안`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각 (현 단계)
- **근거**: PG 시뮬레이터 + 로컬 네트워크. 실제 PG 연동 시 HMAC 서명 검증 필수이나 시뮬레이터 단계에서는 오버엔지니어링. TODO 기록.
- **최종 업데이트**: 2026-03-17

## RD-004. PG URL localhost 기본값
- **keywords**: `localhost`, `PG URL`, `기본값`, `환경변수`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각
- **근거**: 학습 프로젝트, Docker Compose 기반 로컬 개발이 주 환경. 기본값 제거 시 불편. 실 배포는 환경별 프로필로 분리.
- **최종 업데이트**: 2026-03-17

## RD-005. Feign 타임아웃 p95/p99 근거
- **keywords**: `timeout`, `Feign`, `p95`, `p99`, `지연`, `latency`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각 (현 단계)
- **근거**: 시뮬레이터 기반이라 실제 PG 응답 분포 데이터 없음. 현재 1s/3s는 합리적 초기값. 실 PG 연동 시 측정 후 조정.
- **최종 업데이트**: 2026-03-17

## RD-006. 스케줄러 분산 락 (ShedLock)
- **keywords**: `scheduler`, `distributed lock`, `ShedLock`, `분산 락`, `다중 인스턴스`
- **리뷰어**: CodeRabbit
- **repeat_count**: 3
- **최종 결정**: 기각 (현 단계)
- **근거**: 단일 인스턴스 배포. RecoverPaymentUseCase가 비관적 락으로 중복 방지. QueueScheduler도 동일 전제. 스케일아웃 시 도입 필요하나 premature.
- **최종 업데이트**: 2026-04-02

## RD-007. TIMEOUT 시 Order 상태 미갱신 (비대칭)
- **keywords**: `TIMEOUT`, `Order`, `markFailed`, `비대칭`, `상태 미갱신`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각 (의도적 설계)
- **근거**: TIMEOUT은 "PG 응답 불명확 → 복구 스케줄러가 재확인" 상태. Order를 PENDING_PAYMENT로 유지해야 복구 흐름 정상 동작. FAILED 전환 시 복구 대상에서 빠짐.
- **최종 업데이트**: 2026-03-17

## RD-008. Payment.updatedAt 상태 전환 시 도메인 모델 미갱신
- **keywords**: `updatedAt`, `상태 전환`, `도메인 모델`, `@PreUpdate`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각
- **근거**: BaseEntity의 `@PreUpdate`가 JPA save 시 자동 갱신. 도메인 모델의 updatedAt은 읽기 전용 필드.
- **최종 업데이트**: 2026-03-17

## RD-009. Retry 정책 범위 (IOException 포함)
- **keywords**: `retry`, `IOException`, `중복 승인`, `멱등`, `idempotency`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 유지 (현 단계)
- **근거**: 시뮬레이터는 멱등 처리(동일 orderId 중복 요청 시 기존 결제 반환). 실 PG 연동 시 멱등키 기반 정책으로 전환 필요.
- **최종 업데이트**: 2026-03-17

## RD-010. setScale UNNECESSARY vs HALF_UP
- **keywords**: `setScale`, `UNNECESSARY`, `HALF_UP`, `RoundingMode`, `BigDecimal`, `KRW`
- **리뷰어**: CodeRabbit
- **repeat_count**: 2
- **최종 결정**: 기각
- **근거**: 1차 리뷰에서 `BigDecimal.toLong()` 정밀도 손실을 `setScale(0, UNNECESSARY)`로 수정한 의도적 안전장치. KRW는 소수점 단위가 없으므로 소수점 존재 시 상류 버그. HALF_UP은 버그를 숨김.
- **최종 업데이트**: 2026-03-18

## RD-011. RecoverAllPaymentsUseCase 배치 루프 처리
- **keywords**: `BATCH_SIZE`, `루프`, `loop`, `페이지네이션`, `배치 복구`
- **리뷰어**: CodeRabbit
- **repeat_count**: 2
- **최종 결정**: 부분 수용 (BATCH_SIZE @Value 전환만)
- **근거**: 루프는 장시간 실행·OOM·스케줄러 스레드 블로킹 위험. 고정 배치 + 주기적 스케줄러가 안전한 회복탄력성 패턴.
- **최종 업데이트**: 2026-03-18

## RD-012. PgClientImpl fallback catch-all 예외 세분화
- **keywords**: `fallback`, `catch-all`, `TIMEOUT`, `예외 세분화`, `상태 조회`
- **리뷰어**: CodeRabbit
- **repeat_count**: 2
- **최종 결정**: 부분 수용 (로깅 개선만)
- **근거**: 이중 fallback에서 예외 전파 시 결제가 미정의 상태에 빠짐. TIMEOUT은 "모르겠으니 recovery scheduler가 재확인"이라는 가장 안전한 기본값.
- **최종 업데이트**: 2026-03-18

## RD-013. afterCommit PG 호출 예외 시 REQUESTED 잔류
- **keywords**: `afterCommit`, `REQUESTED`, `상태 전이`, `PG 예외`, `고착`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각 (의도적 설계)
- **근거**: afterCommit 패턴의 의도적 트레이드오프. Resilience4j fallback이 TIMEOUT 반환, recovery scheduler가 REQUESTED/TIMEOUT 주기적 픽업. 동기 PG 호출은 DB 트랜잭션 내 외부 I/O로 더 위험.
- **최종 업데이트**: 2026-03-18

## RD-014. afterCommit 무한 재시도 (retryCount/backoff 부재)
- **keywords**: `afterCommit`, `무한 재시도`, `retryCount`, `backoff`, `플러딩`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각 (현 단계)
- **근거**: retryCount 도입은 Payment 도메인 모델 변경(필드 추가) 필요. 단일 인스턴스+시뮬레이터 환경에서 premature. 실 PG 연동 시 구현.
- **최종 업데이트**: 2026-03-18

## RD-015. 중복 SUCCESS 콜백 시 Order 자동 보정
- **keywords**: `중복 콜백`, `SUCCESS`, `PENDING_PAYMENT`, `자동 보정`, `불일치`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각 (의도적 설계)
- **근거**: payment=SUCCESS + order=PENDING 비정상 상태의 자동 보정은 예상치 못한 상태 변경 유발. 별도 보정 API/배치로 처리하는 것이 안전.
- **최종 업데이트**: 2026-03-18

## RD-016. 카드번호 @JsonIgnore 방어적 직렬화 차단
- **keywords**: `cardNo`, `@JsonIgnore`, `data class copy`, `직렬화`, `민감정보`
- **리뷰어**: CodeRabbit (CR-15, CR-18, CR-21)
- **repeat_count**: 1
- **최종 결정**: 기각 (현재 위험 없음)
- **근거**: PgFeignClient DTO, PgPaymentRequest, PaymentCommand 모두 인프라/Application 내부 객체로 HTTP 응답 직렬화 경로가 없다. data class copy()나 구조적 로깅(Jackson)으로의 노출은 이론적 가능성일 뿐 현재 코드에서 해당 경로가 존재하지 않음. 민감정보 보호는 CP20에서 마스킹으로 처리 완료.
- **최종 업데이트**: 2026-03-19

## RD-017. 로깅/모니터링 강화 및 Version Catalogs 전환
- **keywords**: `로깅`, `Feign 로거`, `Version Catalogs`, `null 경로`, `RedisCleanUp`
- **리뷰어**: CodeRabbit (CR-1, CR-17, CR-27), Gemini (G-T0)
- **repeat_count**: 1
- **최종 결정**: 기각 (시뮬레이터 단계 과잉)
- **근거**: RedisCleanUp은 testFixtures 코드로 운영 영향 없음. Feign 로거 환경별 분리와 PgStatusQueryClient null 경로 로깅은 실 PG 연동 시 일괄 처리가 합리적. Version Catalogs 전환은 chore 수준으로 현재 project.properties 방식에 기능적 문제 없음.
- **최종 업데이트**: 2026-03-19

## RD-018. 빈 문자열 인증 헤더 방어 (OptionalAuthInterceptor)
- **keywords**: `빈 문자열`, `blank`, `헤더`, `OptionalAuthInterceptor`, `takeIf`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각
- **근거**: HTTP 클라이언트(브라우저, Feign 등)가 빈 문자열 헤더를 보내는 실 발생 경로 없음. YAGNI.
- **최종 업데이트**: 2026-03-23

## RD-019. check-then-act 안티패턴 (IssuedCouponRepository)
- **keywords**: `check-then-act`, `exists`, `save`, `saveIfAbsent`, `원자적`, `unique constraint`
- **리뷰어**: CodeRabbit
- **repeat_count**: 2
- **최종 결정**: 기각
- **근거**: DB unique constraint가 실질적 보호 역할. `saveIfAbsent` 원자 연산 도입 시 중복 판단 로직이 도메인에서 인프라로 이동하여 설계 철학과 충돌. ProcessCouponIssueUseCase에 비관적 락(#11) 적용으로 check-then-act 경합 자체가 해소됨.
- **최종 업데이트**: 2026-03-23

## RD-020. ProductMetrics counter 음수 초기값 검증
- **keywords**: `ProductMetrics`, `viewCount`, `likeCount`, `salesCount`, `음수`, `검증`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각
- **근거**: counter 필드는 0 초기값에서 `++`/`--`로만 변경되며 생성자에 음수가 전달될 경로 없음. DB 복원 시에도 음수 데이터 자체가 상위 버그. commerce-streamer는 집계 처리기로 VO 도입 대비 과잉.
- **최종 업데이트**: 2026-03-23

## RD-021. Outbox Relay 중복 배달 가능성 (at-least-once)
- **keywords**: `RelayOutboxUseCase`, `중복 배달`, `at-least-once`, `Kafka publish`, `transaction commit`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각 (의도적 설계)
- **근거**: at-least-once delivery는 Outbox 패턴의 의도된 보장 수준. consumer가 `eventHandled` 테이블로 멱등 처리하므로 중복 배달에 안전. afterCommit publish → DB markPublished 순서가 최소 1회 보장의 표준 패턴.
- **최종 업데이트**: 2026-03-24

## RD-022. Consumer malformed message DLT 처리 (이미 구현)
- **keywords**: `CouponIssueConsumer`, `malformed`, `DLT`, `dead-letter`, `IllegalArgumentException`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각 (이미 구현)
- **근거**: RECORD_LISTENER_DLT 컨테이너 팩토리의 DefaultErrorHandler(FixedBackOff(1000L, 2))가 IllegalArgumentException 포함 모든 예외를 catch하여 3회 시도 후 DLT 발행. PR #25 리뷰 반영에서 구현 완료.
- **최종 업데이트**: 2026-03-24

## RD-023. request not found 시 eventHandled 미기록
- **keywords**: `ProcessCouponIssueUseCase`, `findByRequestId`, `eventHandled`, `무한 재처리`, `offset commit`
- **리뷰어**: CodeRabbit
- **repeat_count**: 2
- **최종 결정**: 기각
- **근거**: Record listener가 정상 반환 시 offset 자동 커밋 → 재배달 없음. eventHandled는 비즈니스 멱등성 보호용이며, 실제 처리된 적 없는 이벤트를 "처리됨"으로 기록하면 의미 왜곡. request 미존재는 데이터 정합성 이슈로 별도 모니터링 대상.
- **최종 업데이트**: 2026-03-24

## RD-024. AFTER_COMMIT 핸들러 통합 테스트
- **keywords**: `AFTER_COMMIT`, `TransactionalEventListener`, `통합 테스트`, `ApplicationEventPublisher`, `phase`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각 (현 단계)
- **근거**: TestContainers + 실제 트랜잭션 인프라 필요. phase 설정은 어노테이션이므로 코드 리뷰로 충분히 검증 가능. 단위 테스트에서 핸들러 로직 자체를 검증하고 있으며, 통합 테스트 추가는 현 단계에서 과잉.
- **최종 업데이트**: 2026-03-25

## RD-025. AFTER_COMMIT 핸들러에 @Transactional(REQUIRES_NEW) 명시
- **keywords**: `AFTER_COMMIT`, `@Transactional`, `REQUIRES_NEW`, `SimpleJpaRepository`, `트랜잭션 경계`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각
- **근거**: AFTER_COMMIT 이후 `catalogOutboxRepository.save()` 호출 시 Spring Data JPA의 `SimpleJpaRepository.save()`에 선언된 `@Transactional`이 자동으로 새 트랜잭션을 생성한다. 핸들러에 별도로 `@Transactional(propagation = REQUIRES_NEW)`를 명시하면 중복 어노테이션이 되며, 프레임워크 동작에 대한 불필요한 의존 표현.
- **최종 업데이트**: 2026-03-25

## RD-026. JDBC batch_size 미설정으로 saveAll 배치 미동작
- **keywords**: `batch_size`, `order_inserts`, `saveAll`, `JDBC 배치`, `IDENTITY`, `jpa.yml`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각 (현 단계)
- **근거**: ① jpa.yml은 전체 엔티티에 영향주는 공유 설정 ② `@GeneratedValue(IDENTITY)` 전략 사용 시 Hibernate INSERT 배치 자체가 비활성화됨 ③ 현재 건수(1~5건/결제)에서 실질 효과 미미. 스케일 확대 시 SEQUENCE 전략 전환과 함께 검토.
- **최종 업데이트**: 2026-03-26

## RD-027. IssuedTokenInfoTest 단일 케이스만 검증
- **keywords**: `IssuedTokenInfoTest`, `toString`, `마스킹`, `ParameterizedTest`, `토큰`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 유지 (현 단계)
- **근거**: 현재 테스트가 핵심 시나리오(일반 토큰 마스킹)를 커버. @ParameterizedTest로 확장은 개선이나, 마스킹은 toString()의 편의 기능이라 현 수준으로 충분.
- **최종 업데이트**: 2026-04-02

## RD-028. QueueSchedulerTest 예외 Repository anonymous object 중복
- **keywords**: `QueueSchedulerTest`, `anonymous object`, `DRY`, `헬퍼 함수`, `failingRepo`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 유지 (현 단계)
- **근거**: 헬퍼 추출은 DRY하나 테스트 코드는 가독성 > 재사용. anonymous object가 2개뿐이고 각각의 예외 시나리오가 다름. 인터페이스 메서드 추가 시 재검토.
- **최종 업데이트**: 2026-04-02

## RD-029. OrderOutbox 팩토리 메서드 / sealed hierarchy
- **keywords**: `OrderOutbox`, `factory method`, `sealed`, `companion object`, `타입 안전성`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 유지 (현 단계)
- **근거**: init 블록 런타임 검증이 충분히 작동. sealed hierarchy 도입은 이벤트 타입이 3개 이상으로 늘어날 때 가치가 커짐. 현재 PAYMENT_COMPLETED/PAYMENT_FAILED 2개뿐.
- **최종 업데이트**: 2026-04-02

## RD-030. CouponOutboxEntity enum.name 영속화
- **keywords**: `enum.name`, `CouponOutboxEntity`, `eventType`, `리네임`, `호환성`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 유지 (현 단계)
- **근거**: enum 상수명 변경은 본질적으로 스키마 변경이며 마이그레이션 동반. `@Enumerated(STRING)` / `.name` 영속화는 JPA/Spring 표준 패턴. 외부 시스템 연동 시 별도 code 프로퍼티 검토.
- **최종 업데이트**: 2026-04-02

## RD-031. 트랜잭션-Redis 분리로 인한 랭킹 점수 드리프트
- **keywords**: `트랜잭션`, `Redis`, `드리프트`, `멱등성`, `ZINCRBY`, `eventId`, `at-least-once`
- **리뷰어**: CodeRabbit (CRITICAL)
- **repeat_count**: 1
- **최종 결정**: 수용
- **근거**: Redis 반영 후 DB 트랜잭션 실패 시 이벤트 재처리로 점수 중복 가산. Redis 갱신을 DB 커밋 이후로 이동하여 해결.
- **최종 업데이트**: 2026-04-06

## RD-032. @Transactional(readOnly) 내 Redis 호출
- **keywords**: `@Transactional`, `readOnly`, `Redis`, `DB 커넥션`, `점유`, `GetProductUseCase`
- **리뷰어**: CodeRabbit (nitpick)
- **repeat_count**: 2 (PR#36 Gemini: GetRankingUseCase 동일 패턴 미적용)
- **최종 결정**: 수용
- **근거**: readOnly 트랜잭션 내 Redis 호출로 DB 커넥션 점유 시간 증가. 트랜잭션 내부(DB 조회)와 외부(Redis 조회, 이벤트 발행) 분리로 해결.
- **최종 업데이트**: 2026-04-09

## RD-033. 랭킹 목록 API vs 상품 상세 API 순위 의미 분리
- **keywords**: `rank`, `ZREVRANK`, `filtered rank`, `상품 상세`, `랭킹 목록`, `불일치`
- **리뷰어**: Codex (P2)
- **repeat_count**: 1
- **최종 결정**: 트레이드오프 (현행 유지)
- **근거**: 랭킹 목록은 비활성 상품을 제외한 "고객 노출 순위"(filtered), 상품 상세는 전체 인기도 내 "raw 순위"(ZREVRANK)로 의미가 다르다. filtered rank를 상품 상세에서도 계산하려면 매 요청마다 상위 항목 전체 순회 + DB active 확인이 필요하여 성능 비용 과대.
- **최종 업데이트**: 2026-04-09

## RD-034. totalElements 캐시와 content 스냅샷 30초 이내 오차 허용
- **keywords**: `totalElements`, `totalCountCache`, `TTL`, `캐시`, `페이지네이션`, `스냅샷 불일치`
- **리뷰어**: CodeRabbit (Major)
- **repeat_count**: 1
- **최종 결정**: 트레이드오프 (현행 유지)
- **근거**: totalElements와 content 모두 "visible(active+미삭제) 상품"이라는 동일한 의미를 기준으로 계산하되, totalElements만 30초 캐시로 stale할 수 있다. 동시 계산하면 캐시 의미가 소실. 페이지네이션 메타데이터의 30초 이내 오차는 허용 범위.
- **주의**: 이 결정은 "같은 의미의 값이 캐시로 잠깐 stale한 것"만 허용한다. ZCOUNT 등 의미가 다른 값(비활성/삭제 상품 포함)을 visible totalElements로 대체하는 근거로 사용 불가.
- **최종 업데이트**: 2026-04-09

## RD-035. 재처리 스케줄러 다중 인스턴스 중복 소비 — 멱등성 기반 대응
- **keywords**: `RetryFailedScoreUpdateScheduler`, `FOR UPDATE SKIP LOCKED`, `다중 인스턴스`, `중복 소비`, `멱등성`
- **리뷰어**: CodeRabbit (Major)
- **repeat_count**: 1
- **최종 결정**: 트레이드오프 (현행 유지, 단일 인스턴스 전제)
- **근거**: (1) incrementScore Lua 스크립트가 eventId 기반 멱등성을 보장하여 점수 중복 가산은 방지. (2) FOR UPDATE SKIP LOCKED는 native query 필요 → 프로젝트 규칙 "JPQL/NativeQuery 금지" 위반. (3) commerce-streamer 단일 인스턴스 환경 전제.
- **한계**: 멱등성은 점수 중복만 막을 뿐, 다중 인스턴스 시 중복 poll로 인한 불필요한 DB/Redis 부하까지는 해결하지 않는다. 다중 인스턴스 확장 시 ShedLock 등 분산 락 도입 필요(RD-006 참고).
- **최종 업데이트**: 2026-04-09

## RD-036. scanTotalVisibleCount O(N) 비용 — 요구사항 의미 보존 우선
- **keywords**: `scanTotalVisibleCount`, `totalElements`, `ZCOUNT`, `비활성 상품`, `성능`, `O(N)`
- **리뷰어**: Gemini (High)
- **repeat_count**: 1
- **최종 결정**: 트레이드오프 (현행 유지)
- **근거**: 요구사항 "삭제/비활성 상태인 상품은 ZSET에 남아있어도 랭킹 API 응답에서 제외한다"가 content뿐 아니라 totalElements에도 적용된다. ZCOUNT(score>0)는 비활성/삭제 상품을 포함하므로 visible totalElements의 의미와 다르다. 현재 scanTotalVisibleCount가 요구사항적으로 올바른 구현이며, 30초 캐시 TTL로 호출 빈도가 제한된다. 대규모 데이터 시 캐시 TTL 연장 또는 별도 active 카운터 유지로 대응.
- **최종 업데이트**: 2026-04-09

## RD-037. LIKE_REMOVED 시 likeCount 감소 여부와 무관한 랭킹 점수 차감
- **keywords**: `LIKE_REMOVED`, `decrementLikeCount`, `랭킹 점수`, `likeCount`, `의도적 분리`, `RankingWeight.LIKE`
- **리뷰어**: CodeRabbit (Critical, PR #36 + PR #37 반복)
- **repeat_count**: 2
- **최종 결정**: 기각 (의도적 분리 정책)
- **근거**: round9 requirements Q4가 "LIKE_REMOVED 이벤트 시 랭킹 점수 -0.2 차감"을 명시한다. 이 구현에서 `ProductMetrics.likeCount`는 랭킹 반영 여부를 판정하는 source of truth가 아니라, Kafka consumer가 유지하는 **보조 projection**이다. 실제 "좋아요가 존재할 때만 LIKE_REMOVED를 발행한다"는 계약은 producer(`RemoveLikeUseCase`)가 보장하며, 좋아요가 없으면 outbox 자체를 만들지 않는다. 따라서 consumer에서 `decrementLikeCount()`의 Boolean 반환값으로 랭킹 차감을 막아 버리면, projection이 일시적으로 틀어진 상황에서 **실제로 발생한 LIKE_REMOVED 신호까지 누락**시킬 수 있다. 현재 설계는 LIKE_ADDED / LIKE_REMOVED를 각각 독립적인 랭킹 신호로 반영하고, projection 카운터는 음수로만 내려가지 않게 보호한다.
- **한계**: producer 계약을 어기고 "실제 좋아요 제거 없이" LIKE_REMOVED가 유입되면 랭킹 점수 드리프트가 발생할 수 있다. 그러나 이는 consumer의 `decrementLikeCount()` Boolean 게이트로 해결할 문제가 아니라, producer 계약 유지 또는 별도 reconciliation으로 다뤄야 한다. 현재 테스트(`좋아요가 없는 상태에서 취소해도 CatalogOutbox에 LIKE_REMOVED가 저장되지 않는다`, `LIKE_REMOVED 이벤트 시 likeCount가 0이어도 랭킹 점수 -0_2가 반영된다`)는 이 계약을 회귀 보호한다.
- **최종 업데이트**: 2026-04-10

## RD-038. RetryFailedScoreUpdateScheduler 스케줄러 레벨 예외 처리·메트릭 — 후순위
- **keywords**: `RetryFailedScoreUpdateScheduler`, `try-catch`, `스케줄러`, `Micrometer`, `메트릭`, `관측성`, `retry.interval-ms`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각 (현 단계 후순위)
- **근거**: 현재는 "재처리 실패가 기능 정합성 이슈인가"를 먼저 보장하는 단계이며, 운영 관측 고도화(스케줄러 전체 try-catch wrapper, 성공/실패/스킵 메트릭 카운터)는 후순위다. Spring `@Scheduled`는 태스크 예외 발생 시 다음 주기에 재실행하므로 기능 정합성에는 영향 없다. 개별 레코드 단위 try-catch는 이미 구현되어 있어 부분 성공이 보장된다. 실 운영 단계에서 관측성 고도화 시 일괄 도입한다.
- **최종 업데이트**: 2026-04-09

## RD-039. OrderEventConsumer validation 실패 시 CoreException + DLT 경유 정책 유지
- **keywords**: `OrderEventConsumer`, `validation`, `log.warn`, `early return`, `DLT`, `CouponIssueConsumer`, `eventId 공백`, `productId`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각 (현 정책 유지)
- **근거**: CR은 `productId <= 0`, `eventId.isBlank()` 같은 deterministic 검증 오류는 재시도해도 성공할 수 없으므로 `CouponIssueConsumer`처럼 `log.warn()` + early return으로 fast-skip하라고 제안. 그러나 PR #36 CP3에서 `OrderEventConsumerTest`의 `ValidationError` Nested에 "검증 실패 시 `CoreException`을 던진다"는 테스트를 **의도적으로 추가**했다. DLT 경유는 운영 가시성(어떤 메시지가 영구 실패했는지 추적) 확보 목적이며, `CouponIssueConsumer`와의 일관성 이슈는 다음 라운드에서 양쪽 Consumer 패턴을 통합 논의할 사안. 수용 시 round9에서 추가한 ValidationError 테스트들을 `verifyNoInteractions` 형태로 모두 재작성해야 하며 정책 방향 전환에 해당.
- **한계**: 일관성 이슈 자체는 유효. 두 Consumer 중 하나의 정책으로 통합하는 결정은 향후 별도 의사결정 라운드에서 다룬다.
- **최종 업데이트**: 2026-04-10

## RD-040. RetryFailedScoreUpdateScheduler save() 실패 시 retryCount 손실 — 수용안 부재로 현 설계 유지
- **keywords**: `RetryFailedScoreUpdateScheduler`, `incrementRetryCount`, `save 실패`, `retryCount 손실`, `무한 재시도`, `루프 전파`
- **리뷰어**: CodeRabbit
- **repeat_count**: 1
- **최종 결정**: 기각 (수용안 자체가 근본 미해결)
- **근거**: CR은 Line 31 `update.incrementRetryCount()` 후 Line 32 `failedScoreUpdateRepository.save(update)`가 실패하면 메모리상 증가된 `retryCount`가 DB에 반영되지 않아 `maxRetryCount` 체크가 우회되고 무한 재시도가 발생할 수 있다고 지적. 시나리오는 정확하다. 그러나 CR이 제안한 "save() 실패도 try-catch + log + continue" 수용안은 **근본 해결이 아니다** — `incrementRetryCount()`가 메모리에만 반영된 채 DB save가 실패하면 continue를 해도 retryCount 증가가 동일하게 유실되며, 다음 주기 조회 시 같은 레코드가 이전 retryCount로 다시 나타난다. 진짜 해결은 (A) 별도 update 쿼리로 retryCount만 증가시키거나 (B) save 자체를 재시도하는 이중 경로 설계가 필요하지만 두 경로 모두 이번 라운드에서 도입할 만한 비용이 아니다. 현 구조(save 실패 시 루프 바깥으로 예외 전파 → 스케줄러가 한 주기 실패 → 다음 주기에 처음부터 재처리)는 `incrementScore` Lua 스크립트의 `eventId` 기반 멱등성에 의존해 점수 드리프트를 방지하므로 데이터 정합성 측면에서 안전하다. save 실패 자체가 DB 장애 상황이라 그 주기 전체가 실패하는 것이 오히려 명시적인 신호다.
- **한계**: 부분 성공 시나리오(일부 record는 incrementScore 성공, 일부는 retryCount 증가 후 save 실패)에서 일부 retryCount 증가가 손실될 가능성은 여전히 존재. 운영에서 실 발생 빈도와 영향도를 측정한 후, 의미 있는 수준이면 (A) 별도 update 쿼리 도입으로 재방문.
- **최종 업데이트**: 2026-04-10

## RD-041. `period` 파라미터 대소문자 엄격 정책 (lowercase만 허용)
- **keywords**: `period`, `lowercase`, `RankingPeriod.from`, `RankingV1Period.from`, `대소문자`, `명확성 우선`, `400 BAD_REQUEST`
- **리뷰어**: CodeRabbit (PR #38 #2, #4)
- **repeat_count**: 1
- **최종 결정**: 기각 (요구사항 명시 정책 준수)
- **근거**: CR은 `RankingPeriod.from(value)`에서 `value`를 `lowercase()`로 정규화하여 `DAILY`/`Daily`/`daily`를 모두 허용하라고 제안. 그러나 `docs/requirements/round10-requirements-analysis.md:423`이 "URL 파라미터 값: 소문자 daily/weekly/monthly 고정. 대소문자 섞인 값(Daily, WEEKLY 등)은 400 BAD_REQUEST. **명확성 우선**"이라고 명시적으로 결정한 사안이다. `plan.md:156`의 E2E 테스트 `period=Daily → 400 BAD_REQUEST`가 이미 통과 중이며, 제안 수용 시 스펙 및 기존 테스트 회귀가 발생한다. 엄격 정책은 "클라이언트 입력 표준화 강제" 목적이며 관대한 매칭은 이와 상충한다.
- **한계**: 없음. 엄격 정책은 의도적 선택이며 현재 구현이 스펙과 일치한다.
- **최종 업데이트**: 2026-04-14

## RD-042. weekly/monthly 응답 `rank`는 MV `rank_no` 원본 + 응답 시점 필터링으로 페이지 축소 허용
- **keywords**: `rank`, `rank_no`, `MV`, `filtered rank`, `페이지 축소`, `응답 시점 필터링`, `semantics`
- **리뷰어**: CodeRabbit (PR #38 #1)
- **repeat_count**: 1
- **최종 결정**: 기각 (요구사항 명시 정책 준수)
- **근거**: CR은 `GetRankingUseCase.executeWeekly/executeMonthly`가 비가시 상품 필터 후 `row.rank`(MV 원본)를 그대로 반환하여 daily 경로(`page*size+index+1`로 재부여)와 semantics가 다르다고 지적하며 filtered rank 재부여를 제안. 그러나 `docs/requirements/round10-requirements-analysis.md:241-244`는 "비활성/삭제 상품은 응답 시점에 필터링한다... 응답 필터링으로 인해 페이지 크기가 모자라도 **그대로 허용한다** (over-fetch나 재정렬 없이 자연 축소)"를 **daily·weekly·monthly 공통 정책**으로 고정했다. 또한 `docs/requirements/round10-requirements-analysis.md:301`은 "`content[].rank`: daily는 페이지 오프셋 기반(1-based), **weekly/monthly는 MV에 사전 계산된 `rank_no`**"라고 명시했다. 제안 수용 시 스펙 위반에 해당한다. weekly/monthly의 raw rank는 "배치 스냅샷의 원본 순위"라는 의미를 보존한다.
- **한계**: 프런트가 "첫 번째 아이템 = 1위"를 전제로 UI를 만들면 혼란 여지 존재. 해결은 API 문서에서 "weekly/monthly rank는 배치 스냅샷 raw" 명시.
- **최종 업데이트**: 2026-04-14

## RD-043. 랭킹 가중치(0.1/0.2/0.7) 공유 상수 미추출 — 현행 하드코딩 유지
- **keywords**: `랭킹 가중치`, `RankingWeight`, `공유 상수`, `WeeklyRankingQueryDao`, `MonthlyRankingQueryDao`, `commerce-streamer`
- **리뷰어**: Gemini Code Assist (PR #38 #10, #11)
- **repeat_count**: 1
- **최종 결정**: 기각 (현행 유지)
- **근거**: Gemini는 `WeeklyRankingQueryDao.kt:20-22`, `MonthlyRankingQueryDao.kt:20-22`, 그리고 `commerce-streamer` 실시간 점수 계산 경로에 같은 가중치(0.1/0.2/0.7)가 하드코딩되어 있다며 공유 상수 추출을 제안. 그러나 두 모듈(`commerce-batch`, `commerce-streamer`) 간에 공유할 도메인 모듈이 없고 batch는 streamer 도메인을 참조할 수 없다. 각 모듈에 별도 `RankingWeight` 상수 클래스를 두는 부분 수용도 가능하지만, 3개 상수를 3곳에서만 쓰는 상태에서의 추출은 "Rule of Three" 관점에서 이른 추상화다. 향후 가중치 조정/외부 설정화 요구가 생길 때 공유 모듈 신설과 함께 도입하는 것이 맞다.
- **한계**: 가중치 불일치 시 회귀 위험 있음. 수기 교차 확인이 필요한 상태 유지.
- **최종 업데이트**: 2026-04-14

## RD-044. BatchRankingController 동기 `jobLauncher.run` — 현 단계 acceptable
- **keywords**: `BatchRankingController`, `jobLauncher.run`, `동기`, `async JobLauncher`, `TaskExecutor`, `HTTP 블로킹`
- **리뷰어**: Gemini Code Assist (PR #38 #6)
- **repeat_count**: 1
- **최종 결정**: 기각 (현 단계 acceptable, Nice-to-Have 후순위)
- **근거**: Gemini는 `jobLauncher.run`이 HTTP 스레드를 블로킹하므로 `SimpleAsyncTaskExecutor` 기반 비동기 JobLauncher를 권장. 그러나 현재 Top 100 집계는 소규모(수초 이내)로 예상되며, scheduler 프로파일 단일 인스턴스 + 운영자 수동 백필 용도라 동시성/처리량 요구가 낮다. Job 완료까지 블로킹되는 편이 오히려 HTTP 응답에 최종 `BatchStatus`를 그대로 담을 수 있어 운영 관측성이 좋다. 비동기 전환 시 응답 바디는 `STARTED`만 남아 상태 확인을 위한 별도 polling API가 필요해진다.
- **한계**: Top 100 규모가 커지거나 집계 쿼리 비용이 증가하면 재검토 필요. 실측 지표 기반으로 결정.
- **최종 업데이트**: 2026-04-14

## RD-045. `run.id` 기반 JobInstance 중복 실행 차단 부재 — 단일 인스턴스 전제 유지
- **keywords**: `run.id`, `JobInstance`, `동시 실행`, `JobExplorer`, `409`, `단일 인스턴스`, `BatchRankingController`, `RankingJobScheduler`
- **리뷰어**: CodeRabbit (PR #38 #8, Critical)
- **repeat_count**: 1
- **최종 결정**: 기각 (단일 인스턴스 전제, RD-006과 동일 논지)
- **근거**: CR은 `buildParams`가 `System.currentTimeMillis()`를 `run.id`로 넣어 같은 `baseDate`로 호출해도 매번 새 JobInstance가 생성되어 동시 실행을 차단하지 못한다고 지적. 시나리오는 정확하다. 그러나 (1) 배치 서버는 실무상 단일 인스턴스 + failover 모델로 운영되며, 멀티 인스턴스는 파티셔닝 스텝이나 ShedLock 같은 분산 락이 필수(RD-006 참조), (2) Spring `@Scheduled`는 단일 스레드 ThreadPoolTaskScheduler로 직렬 실행되므로 같은 스케줄러 내 중복 호출 불가, (3) `BatchRankingController`는 운영자 수동 백필용으로 스케줄러 실행 중 동시 호출 빈도가 실질 0이다. 단일 인스턴스 전제 하에서 HTTP trigger ↔ 스케줄러 겹침은 실발생률이 무시할 수준이다. 요구사항 §10.7도 `run.id`를 "동일 기간 재실행 식별자"로 명시적으로 채택했다.
- **한계**: 멀티 인스턴스 배포 또는 HTTP trigger 빈도가 늘어나면 `JobExplorer.findRunningJobExecutions(jobName)` 기반 선행 검사 + 409 반환 + ShedLock 도입 필요.
- **최종 업데이트**: 2026-04-14

## RD-046. commerce-batch scheduler 프로파일 엔드포인트 무인증 — 학습 프로젝트 + 내부망 전제 유지
- **keywords**: `BatchRankingController`, `/internal/batch/ranking`, `Spring Security`, `SecurityFilterChain`, `내부망`, `포트 8082`, `API Key`
- **리뷰어**: CodeRabbit (PR #38 #15, Major)
- **repeat_count**: 1
- **최종 결정**: 기각 (학습 프로젝트 전제, RD-003과 동일 논지)
- **근거**: CR은 scheduler 프로파일이 포트 8082로 열릴 때 `BatchRankingController`가 무인증이라 임의 요청으로 랭킹 재집계가 가능하다고 지적. 그러나 (1) 학습 프로젝트 + Docker Compose 기반 로컬 개발이 주 환경, (2) 실 배포 시 인그레스/로드밸런서에서 포트 8082 차단 또는 내부망 바인딩으로 보호, (3) 애플리케이션 레이어 Spring Security 도입은 `@Profile("scheduler")` 한정 설정이나 `WebSecurityCustomizer` 등 비용이 premature. RD-003 콜백 HMAC 검증 기각과 동일 논지. 향후 실 운영으로 전환 시 인증 정책을 일괄 도입.
- **한계**: 내부망 가정이 깨지면 즉시 재검토. 실제 배포 시 네트워크 경로와 바인딩 주소를 재점검.
- **최종 업데이트**: 2026-04-14

## RD-047. `commerce-batch` `spring.batch.job.enabled: false` 전역 기본값 — 의도적 설계 유지
- **keywords**: `spring.batch.job.enabled`, `application.yml`, `one-shot`, `CommandLineRunner`, `RankingJobScheduler`, `web-application-type`, `a873b42`
- **리뷰어**: CodeRabbit (PR #38 #14, Critical)
- **repeat_count**: 1
- **최종 결정**: 기각 (의도적 설계)
- **근거**: CR은 `application.yml:17`의 `spring.batch.job.enabled: false` 전역 기본값 때문에 non-scheduler 환경에서 one-shot 배치(`--job.name=...`) 실행 경로가 전무하다고 지적하며 "기본 true, scheduler 프로파일에서만 false"로 전도할 것을 제안. 그러나 직전 커밋 `a873b42 refactor(batch): job.enabled 기본 비활성화 + @ConditionalOnProperty 제거`가 이 방향을 **명시적으로** 결정한 사안이다. `RankingJobScheduler`가 `@Scheduled` 안에서 `jobLauncher.run`을 수동으로 호출하는 구조라 `spring.batch.job.enabled: true`로 두면 애플리케이션 기동 시 모든 Job이 자동 실행되어 스케줄러 설계와 충돌한다. 또한 non-scheduler 환경은 `web-application-type: none`으로 기동 즉시 종료되며, one-shot 실행 경로는 설계상 `scheduler` 프로파일 + HTTP trigger로 단일화했다.
- **한계**: CLI 기반 one-shot 실행 요구가 실제 생기면 `JobLauncherApplicationRunner` + 프로파일별 오버라이드를 도입해야 한다. 현재는 운영 요구 없음.
- **최종 업데이트**: 2026-04-14
