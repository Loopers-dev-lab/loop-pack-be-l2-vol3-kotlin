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
