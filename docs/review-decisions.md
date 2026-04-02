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
- **repeat_count**: 1
- **최종 결정**: 기각 (현 단계)
- **근거**: 단일 인스턴스 배포. RecoverPaymentUseCase가 비관적 락으로 중복 방지. 스케일아웃 시 도입 필요하나 premature.
- **최종 업데이트**: 2026-03-17

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
