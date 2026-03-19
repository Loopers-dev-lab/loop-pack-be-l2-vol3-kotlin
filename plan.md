# PR #21 리뷰 반영 계획

> PR #21 Gemini + CodeRabbit 리뷰 (2026-03-19)
> 총 35건: AGREE 9건, TRADEOFF 수용 6건, TRADEOFF 기각 7건, DISMISS 13건
> 전체 분석: `review-summary.md` 참고

<!-- 기각 사유 (리뷰 반복 지적 대응용)
  그룹 1 — 카드번호 @JsonIgnore (CR-15,18,21): 기각. RD-016.
    PgFeignClient DTO, PgPaymentRequest, PaymentCommand 모두 인프라/Application 내부 객체.
    HTTP 응답 직렬화 경로가 존재하지 않으며, 민감정보 마스킹은 도메인 모델에서 처리 완료.

  그룹 2 — 로깅/모니터링 강화 (CR-1,17,27,G-T0): 기각. RD-017.
    시뮬레이터 단계에서 운영 로깅 투자는 과잉. RedisCleanUp은 testFixtures 코드.
    Feign 로거 분리, null 경로 로깅, Version Catalogs 전환은 실 PG 연동 시 일괄 처리.
-->

## CP23. Order 도메인 모델 + 테스트

- [x] `Order.kt`: markPendingPayment 멱등성 처리 — markPaid/markFailed와 일관성 확보 (CR-7)
- [x] `OrderTest.kt`: CREATED→markFailed 실패 케이스 테스트 추가 — 잘못된 상태 전이 회귀 감지 (CR-5)
- [x] `OrderTest.kt`: markPaid/markFailed 멱등성 테스트 추가 — 구현된 멱등성 로직 검증 (CR-6)

## CP24. Payment 도메인 타입 일관성

- [x] `PaymentRepository`: 파라미터 Long→OrderId 통일 — OrderRepository/OrderItemRepository와 일관성 확보 (CR-22)
- [x] `OrderInfo.kt`: paymentStatus String?→enum 타입 변환 — 타입 안정성 확보 (CR-20)
- [x] `RecoverAllPaymentsUseCase.kt`: recoveredCount 반환값 의미 명확화 (시도 수 vs 성공 수 구분) (G-T2)

## CP25. 코드 가독성 + Fake 정합성

- [x] `PgPaymentResult.kt`: require 이중부정 가독성 개선 — if (SUCCESS) require(!isNullOrBlank) 패턴으로 변환 (CR-14)
- [x] `FakePaymentPgProcessor.kt`: 마스킹 로직을 도메인 유틸 호출로 통일 — Payment.maskCardNo 변경 시 격차 방지 (CR-10)

## CP26. 결제 흐름 테스트 보강

- [x] `HandlePaymentCallbackUseCaseTest.kt`: Order NOT_FOUND, transactionKey 불일치 음성 테스트 추가 (CR-24)
- [x] `HandlePaymentCallbackUseCaseTest.kt`: 멱등성 테스트를 현실적 상태 조합으로 수정 (Payment=SUCCESS + Order=PAID) (CR-25)
- [x] `RequestPaymentUseCaseTest.kt`: 실패 시나리오에서 PG 미호출/상태 미변경 부작용 검증 추가 (CR-30)

## CP27. 복구/통합 테스트 + Resilience

- [x] `RecoverPaymentUseCaseTest.kt`: !!→requireNotNull 변환 — 의미 있는 실패 메시지 제공 (CR-28)
- [x] `PgStatusQueryClient.kt`: @CircuitBreaker fallbackMethod 정의 — CallNotPermittedException 전파 경로 명확화 (CR-9)
- [x] `GetOrderUseCaseTest.kt`: REQUESTED/TIMEOUT 중간 상태 테스트 추가 (CR-19)
- [x] `PaymentFlowIntegrationTest.kt`: afterCommit payload 계약 검증 개선 (CR-31)

## 최종 검증

- [x] ktlintCheck: PASS
- [x] 전체 테스트: BUILD SUCCESSFUL
