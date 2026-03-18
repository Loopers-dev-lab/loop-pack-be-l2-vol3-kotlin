# 리뷰 개선점 반영 계획

> 4개 에이전트 리뷰 결과 기반. 이미 확정된 설계 결정 재논의 제외.

## CP1. 중복 결제 방지 강화 + Order 비관적 락 ✅

- [x] SUCCESS 상태 주문에 재결제 요청 시 CONFLICT
- [x] FAILED 상태 주문에 재결제 허용
- [x] OrderRepository에 findByIdForUpdate 추가 (Domain + Infra + Fake)
- [x] RequestPaymentUseCase에서 Order 조회를 findByIdForUpdate로 변경
- [x] HandlePaymentCallbackUseCase에서도 findByIdForUpdate로 변경
- [x] 테스트 추가 (SUCCESS→CONFLICT, FAILED→재결제)

## CP2. `?: return` 무음 실패 제거 + 상태 불일치 방어 ✅

- [x] PaymentPgProcessorImpl: ?: return → ?: throw CoreException
- [x] FAILED 분기: Order 선조회 후 Payment 상태 변경
- [x] PaymentRecoveryProcessorImpl: PG 트랜잭션 null 시 log.info 추가
- [x] RequestPaymentUseCase afterCommit: try-catch + log.error 추가

## CP3. callbackUrl Domain 제거 + processPayment 시그니처 정리 ✅

- [x] PgPaymentRequest에서 callbackUrl 제거
- [x] PgClientImpl이 PgProperties에서 callbackUrl 주입하여 합성
- [x] PaymentCommand.RequestPayment에서 callbackUrl 제거
- [x] PaymentController에서 callbackUrl 전달 로직 제거
- [x] processPayment 시그니처: command → 필요값만(paymentId, orderId, amount, cardType, cardNo)
- [x] FakePaymentPgProcessor 동기화
- [x] 테스트 수정 완료

## CP4. RecoverPaymentUseCase 분리 + PaymentRecoveryProcessor 제거 ✅

- [x] RecoverPaymentUseCase → execute(orderId): Boolean, @Transactional
- [x] RecoverAllPaymentsUseCase 신규 → execute(): Int, @Transactional 없음
- [x] PaymentRecoveryProcessor 인터페이스 + Impl 삭제
- [x] PaymentAdminController 의존성 변경
- [x] PaymentRecoveryScheduler 의존성 변경
- [x] 테스트 분리/수정 + false positive 수정 (FakePgClient orderId별 응답)

## CP5. ApiSpec 추가 + 검증 보강 ✅

- [x] PaymentCallbackApiSpec 생성 (spec/ 패키지)
- [x] PaymentAdminApiSpec 생성 (spec/ 패키지)
- [x] Controller에서 검증 어노테이션 제거 (ApiSpec 상속)
- [x] cardType 검증: CardType.valueOf try-catch → BAD_REQUEST

## CP6. 데드코드 제거 + 소소한 정리 ✅

- [x] PaymentRepository.updateStatusConditionally 제거 (인터페이스 + 구현 + Fake)
- [x] HandlePaymentCallbackUseCaseTest에서 도메인 메서드로 대체
- [x] findByStatusIn 정렬 기준 추가: Sort.by(ASC, "id")
- [x] Payment.maskCardNo를 private으로 변경

## 최종 검증 ✅

- [x] ktlintCheck: PASS
- [x] 전체 테스트: BUILD SUCCESSFUL

---

# PR #19 Gemini/CodeRabbit 리뷰 반영 계획

> 전체 리뷰 분석: `review-summary.md` 참고
> pg-simulator 관련 20건은 모듈 분리 후 반영 (현재 제외)
> 트레이드오프/이견 9건은 review-summary.md C 섹션 참고

## CP7. 카드번호 보안 강화 (민감정보 노출 차단) ✅

- [x] `PaymentCommand.RequestPayment`: toString() override — cardNo 마스킹
- [x] `PgFeignPaymentRequest`: toString() override — cardNo 마스킹
- [x] `Payment.maskCardNo`: parts.size != 4 시 원문 반환 → 강제 마스킹 (마지막 4자리만 보존)
- [x] `application.yml`: pgClient에 `logger-level: none` 명시

## CP8. Resilience4j 설정 정합성 ✅

- [x] `application.yml`: `circuitBreakerAspectOrder: 1`, `retryAspectOrder: 2` 추가
- [x] `application.yml`: TimeLimiter `pg-payment-request` 설정 제거 (Feign 타임아웃으로 대체)
- [x] `PgClientImpl.requestPaymentFallback`: FeignException 4xx → FAILED 분기 추가

## CP9. 결제 금액 안전성 + PgPaymentResult 검증 ✅

- [x] `RequestPaymentUseCase`: `BigDecimal.toLong()` → `setScale(0, RoundingMode.UNNECESSARY).toLong()`
- [x] `PgPaymentResult`: init 블록에 상태별 필수값 검증 (SUCCESS → transactionKey 필수)

## CP10. 콜백 안전성 (멱등성 + transactionKey 검증) ✅

- [x] `HandlePaymentCallbackUseCase`: 콜백 수신 시 저장된 transactionKey와 비교 검증
- [x] `Order.markPaid/markFailed`: 이미 목표 상태이면 early return (멱등성)
- [x] `Payment.markSuccess/markFailed/markTimeout`: 이미 목표 상태이면 early return
- [x] 기존 테스트 수정: 멱등성 반영 (assertThrows → 무부작용 검증)
- 참고: `markPendingPayment`는 중복 결제 방지 관문이므로 멱등성 미적용

## CP11. 복구 로직 견고화 ✅

- [x] `RecoverAllPaymentsUseCase`: Exception → CoreException만 catch, 예상 밖 예외 재전파
- [x] `RecoverAllPaymentsUseCase`: BATCH_SIZE=50 적용
- [x] `PaymentRepositoryImpl`: 비관적 락에 `@QueryHints` 타임아웃 3초 추가

## CP12. 관측성 개선 (로깅 + 주석) ✅

- [x] `PaymentPgProcessor`: PG 응답 수신 시 구조화 로깅 (paymentId, orderId, status, reason)
- [x] `application.yml` Retry 설정에 주석 추가: 시뮬레이터 멱등 전제, 실 PG 연동 시 정책 분리 필요

## CP13. 테스트 보강 ✅

- [x] `GetOrderUseCaseTest`: Payment 연동 시나리오 추가 (결제 완료/미존재/실패)

## CP14. pg-simulator git 추적 제거 (로컬 전용) ✅

- [x] `settings.gradle.kts`에서 pg-simulator include 제거
- [x] `git rm -r --cached apps/pg-simulator/` — 추적만 끊고 로컬 파일 유지
- [x] `.git/info/exclude`에 `apps/pg-simulator/` 추가
- [x] `.coderabbit.yaml`에서 pg-simulator 제외 설정 제거

## 최종 검증 ✅

- [x] ktlintCheck: PASS
- [x] 전체 테스트: BUILD SUCCESSFUL
