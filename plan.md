# 리뷰 개선점 반영 계획

# PR #19 CodeRabbit 2차 리뷰 반영 계획

> 3f646d8 커밋 이후 CodeRabbit 재리뷰 (2026-03-18)
> Actionable 8건 + Nitpick 3건 중 수용 8건, 트레이드오프 3건
> 전체 분석: `review-summary.md` "2차 리뷰" 섹션 참고

## CP15. 콜백/결제 요청 안전성 개선

- [ ] `HandlePaymentCallbackUseCase`: `isProcessable` 검사를 Order 락 획득 **전**으로 이동 — 중복 콜백 시 불필요한 Order 락 점유 방지
- [ ] `RequestPaymentUseCase:55`: `throw CoreException(...)` → `.also { it.initCause(e) }` 추가 — 원본 IllegalArgumentException 보존
- [ ] `RequestPaymentUseCase:61`: `setScale(0, UNNECESSARY)` 옆에 주석 추가 — KRW 소수점 불변식 의도 설명

## CP16. PG 연동 견고화

- [ ] `PgClientImpl.requestPayment`: `meta.result` 검사 + `data.transactionKey` 비어있으면 에러 — SUCCESS 하드코딩 제거
- [ ] `PgClientImpl.requestPaymentFallback`: 상태 조회 실패 시 로깅에 예외 타입/메시지 명시 (TIMEOUT 기본값은 유지)
- [ ] `RecoverAllPaymentsUseCase`: `CoreException` catch 유지 + `Exception` catch 추가 (log.error + 스택트레이스)
- [ ] `RecoverAllPaymentsUseCase`: BATCH_SIZE를 `@Value`로 설정 가능하게 변경

## CP17. 인덱스 + 테스트 + 정리

- [ ] `PaymentEntity`: `@Table(indexes = [Index(name = "idx_payment_status_id", columnList = "status,id")])` 추가
- [ ] `PaymentTest`: 하이픈 없는 카드번호(`1234567890123456`) 마스킹 테스트 추가
- [ ] `.gitignore:49`: `scripts/` 라인 제거

## 최종 검증 (3차)

- [ ] ktlintCheck: PASS
- [ ] 전체 테스트: BUILD SUCCESSFUL
