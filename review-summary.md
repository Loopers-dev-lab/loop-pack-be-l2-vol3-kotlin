# PR #21 Review Summary — 결제 시스템 통합

> 분석 일시: 2026-03-19
> 리뷰어: Gemini Code Assist, CodeRabbit
> 총 항목: 35건 (Gemini 4 + CodeRabbit 31)

---

## 집계

| 분류 | 건수 | 비율 |
|------|------|------|
| AGREE (수정 필요) | 9건 | 26% |
| TRADEOFF (판단 필요) | 13건 | 37% |
| DISMISS (불필요/기처리) | 13건 | 37% |

---

## AGREE — 수정 필요 (9건)

대부분 **테스트 보강** + **코드 품질 개선**. 리스크 낮음.

| # | 파일 | 요약 | 근거 |
|---|------|------|------|
| CR-5 | `OrderTest.kt:324-349` | CREATED→markFailed 실패 케이스 테스트 누락 | 잘못된 상태 전이 방지 회귀 감지에 필요 |
| CR-6 | `OrderTest.kt:253-322` | markPaid/markFailed 멱등성 테스트 누락 | 멱등성 로직이 구현돼 있으나 검증 테스트 없음 |
| CR-10 | `FakePaymentPgProcessor.kt:19-22` | Fake 마스킹이 도메인 마스킹과 불일치 | Payment.maskCardNo 변경 시 테스트 격차 유발. raw 기록 또는 도메인 유틸 호출로 통일 필요 |
| CR-14 | `PgPaymentResult.kt:8-12` | require 이중 부정 가독성 개선 | `status != SUCCESS \|\| !isNullOrBlank` → `if (SUCCESS) require(!isNullOrBlank)` 변환으로 의도 명확화 |
| CR-22 | `GetOrderUseCase.kt:31` | PaymentRepository 파라미터 Long→OrderId 불일치 | OrderRepository/OrderItemRepository는 OrderId 사용, PaymentRepository만 Long. 도메인 타입 일관성 확보 필요 |
| CR-24 | `HandlePaymentCallbackUseCaseTest.kt:147-168` | Order NOT_FOUND, transactionKey 불일치 테스트 누락 | UseCase에 Order 미존재 예외 경로 있으나 테스트 없음 |
| CR-25 | `HandlePaymentCallbackUseCaseTest.kt:122-144` | 멱등성 테스트가 비현실적 상태 조합 | Payment=SUCCESS + Order=PENDING_PAYMENT는 운영에서 발생 불가. Payment=SUCCESS + Order=PAID가 현실적 |
| CR-28 | `RecoverPaymentUseCaseTest.kt:155-156` | `!!` → `requireNotNull`로 테스트 실패 메시지 개선 | 8곳에서 !! 사용. NPE 대신 의미 있는 실패 메시지 제공 |
| CR-30 | `RequestPaymentUseCaseTest.kt:93-198` | 실패 시나리오에서 부작용(PG 호출, 상태 변경) 미검증 | 예외만 확인하고 PG 미호출/상태 미변경 검증 없음. 중복 과금 회귀 방지에 유효 |

---

## TRADEOFF — 판단 필요 (13건)

4개 그룹으로 분류. **그룹 단위로 수용/기각 판단 권장.**

### 그룹 1: 카드번호 보안 방어 (CR-15, CR-18, CR-21)

현재 직렬화 경로는 없으나, `data class copy()`나 구조적 로깅(Jackson)으로 cardNo가 노출될 이론적 가능성.

| # | 파일 | 요약 | 근거 |
|---|------|------|------|
| CR-15 | `PgFeignClient.kt:39-49` | data class copy()로 cardNo 노출 가능성 | 인프라 내부 DTO로 외부 직렬화 경로 없음. 방어적 프로그래밍 관점 유효하나 현재 위험 낮음 |
| CR-18 | `PgPaymentRequest.kt:5-13` | 카드번호 @JsonIgnore 추가 | 도메인 내부 객체, HTTP 응답 경로 없음. 구조적 로깅(Jackson) 시 노출 가능성은 이론적 |
| CR-21 | `PaymentCommand.kt:4-12` | Command 객체 cardNo Jackson 직렬화 노출 | Application 내부 커맨드, 직렬화 경로 없음. #15, #18과 동일 패턴 |

> **판단 포인트**: 현재 위험 제로 vs 미래 방어. 수용 시 3곳에 `@JsonIgnore` 또는 `@get:JsonIgnore` 추가.
>
> **결정**: [x] 기각 → RD-016 기록

### 그룹 2: 로깅/모니터링 강화 (CR-1, CR-17, CR-27, G-T0)

실 PG 연동 시 일괄 처리 가능한 운영 편의성 개선.

| # | 파일 | 요약 | 근거 |
|---|------|------|------|
| CR-1 | `RedisCleanUp.kt:11-18` | catch 블록에 error 로그 추가 | testFixtures 코드라 운영 영향 없음. 테스트 인프라 디버깅 시 유용하나 우선순위 낮음 |
| CR-17 | `application.yml:33-35` | Feign 로거 레벨 환경별 분리 | 프로필별 분리 합리적이나 시뮬레이터 환경에서 우선순위 낮음 |
| CR-27 | `PgStatusQueryClient.kt:21-32` | 다중 null 반환 경로 구분 불가 | 경로별 로그 추가로 디버깅 개선 가능. RD-012(예외 세분화)와 같은 맥락 |
| G-T0 | `build.gradle.kts` | Resilience4j 버전을 Version Catalogs로 중앙 관리 | 현재 project.properties로 일부 중앙화. Version Catalogs는 chore 수준 |

> **판단 포인트**: 시뮬레이터 단계에서 운영 로깅 투자 가치가 있는가?
>
> **결정**: [x] 기각 → RD-017 기록

### 그룹 3: 도메인 모델 일관성 (CR-7, CR-20, G-T2)

도메인 설계 관점의 일관성/명확성 개선.

| # | 파일 | 요약 | 근거 |
|---|------|------|------|
| CR-7 | `Order.kt:48-53` | markPendingPayment 멱등성 미처리 | markPaid/markFailed는 멱등인데 markPendingPayment만 예외 발생. "PENDING 재요청은 버그 신호" vs 일관성 |
| CR-20 | `OrderInfo.kt:15-16` | paymentStatus String?→enum 타입 안정성 | Application DTO에서 String 사용은 Interfaces 계층과의 유연한 매핑 의도. enum 전환 시 매핑 복잡도 증가 |
| G-T2 | `RecoverAllPaymentsUseCase.kt` | recoveredCount 반환값 의미 모호 (시도 수 vs 성공 수) | 로그에서는 구분하고 있어 운영 관점 문제 없음. 호출자가 관리자 API 1곳 |

> **판단 포인트**: 현재 동작에 문제 없으나 코드 의도 명확화 관점
>
> **결정**: [x] 수용 → CP23, CP24에 반영

### 그룹 4: 테스트 확장 (CR-9, CR-19, CR-31)

현재 scope 밖이거나 추가 검토가 필요한 테스트 관련 항목.

| # | 파일 | 요약 | 근거 |
|---|------|------|------|
| CR-9 | `PgStatusQueryClient.kt:15-16` | @CircuitBreaker fallbackMethod 미정의 | CallNotPermittedException 전파 경로 검토 필요. RecoverPaymentUseCase의 try-catch가 현재 보호 |
| CR-19 | `GetOrderUseCaseTest.kt:123-180` | REQUESTED/TIMEOUT 중간 상태 테스트 추가 | 테스트 커버리지 확장은 유효하나 현재 scope 외. 복구 스케줄러 테스트에서 간접 검증됨 |
| CR-31 | `PaymentFlowIntegrationTest.kt:112-126` | afterCommit payload를 버리고 하드코딩 값으로 재호출 | 계약 불일치를 숨길 수 있으나, 현재 통합 테스트의 의도(단계별 검증)와 Fake 구조 고려 필요 |

> **판단 포인트**: 현재 라운드 scope에 포함할 것인가?
>
> **결정**: [x] 수용 → CP27에 반영

---

## DISMISS — 불필요/기처리 (13건)

| # | 리뷰어 | 파일 | 요약 | 사유 |
|---|--------|------|------|------|
| CR-2 | CodeRabbit | `pre-tool-guard.sh:9` | sed 공백 경로 처리 | RD-002 참조 |
| CR-3 | CodeRabbit | `.claude/config.json:29-32` | bin/out/ 패턴 추가 | 프로젝트에서 미사용 |
| CR-4 | CodeRabbit | `.claude/config.json:33-37` | 타임아웃 키에 단위 명시 | JSON 주석은 비표준 |
| CR-8 | CodeRabbit | `application.yml:42-45` | localhost HTTPS 강제 | RD-004 참조 |
| CR-11 | CodeRabbit | `DeleteProductUseCaseTest.kt:56-57` | 2줄 중복 헬퍼 추출 | 과잉 추상화 (행동원칙 §4) |
| CR-12 | CodeRabbit | `pre-tool-guard.sh:2` | set -e 미사용 | 의도적 설계 |
| CR-13 | CodeRabbit | `.claude/config.json:24` | *secret* 패턴 명시성 | 실질적 오탐 없음 |
| CR-16 | CodeRabbit | `PaymentStatus.kt:3-8` | 상태 전이 규칙 enum에 명시 | 도메인 모델이 이미 관리 |
| CR-23 | CodeRabbit | `PaymentCallbackController.kt:21-34` | 콜백 HMAC/IP 보안 | RD-003 참조 |
| CR-26 | CodeRabbit | `PaymentTest.kt:38-64` | 마스킹 엣지 케이스 | Controller 검증으로 보장 |
| CR-29 | CodeRabbit | `round6-seed.yaml:41` | Resilience 임계값 외부화 | application.yml로 이미 완료 |
| G-T1 | Gemini | `HandlePaymentCallbackUseCase.kt` | 락 전 사전 체크 | TOCTOU 문제로 오히려 위험 |
| G-T3 | Gemini | `FakePaymentPgProcessor.kt` | Fake 마스킹 불일치 | 테스트 대역 목적상 불필요 |
