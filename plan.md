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

---

# PR #20 CodeRabbit/Gemini 리뷰 반영 계획

> PR #20 초회 리뷰 (2026-03-18)
> CodeRabbit 32건 + Gemini 2건 = 총 34건
> 수용 19건, 기결정 6건, 범위밖 4건, 트레이드오프 4건, .json 제외 1건
> 전체 분석: `review-summary.md` "PR #20" 섹션 참고

## CP18. 락 순서 통일 + Order 락 타임아웃 ⚡ HIGH

- [ ] `RequestPaymentUseCase`: 락 순서를 Payment→Order로 변경 (HandlePaymentCallbackUseCase/RecoverPaymentUseCase/PaymentPgProcessor와 통일 — 교착 상태 방지)
- [ ] `OrderJpaRepository`: `findWithLockById`에 `@QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))` 추가

## CP19. PG 연동 안전성 ⚡ HIGH

- [ ] `PgStatusQueryClient`: `firstOrNull()` → 복수 transaction 시 최신(마지막) 것을 선택하도록 변경
- [ ] `RecoverPaymentUseCase:75-77`: afterCommit catch에 예외 객체(`e`) 추가하여 스택 트레이스 로깅
- [ ] `build.gradle.kts`: resilience4j-spring-boot3 버전 명시 (현재 사용 중인 버전 고정)
- [ ] `PgFeignPaymentRequest`: `idempotencyKey: String? = null` → `idempotencyKey: String` non-null

## CP20. 민감정보 보호 + 입력 검증

- [ ] `HandlePaymentCallbackUseCase:32`: 예외 메시지에서 transactionKey를 마스킹 (앞 4자리만 남기거나 orderId 중심 메시지로 변경)
- [ ] `PgPaymentRequest`: data class → toString() override 추가 (cardNo 마스킹)
- [ ] `PaymentDto.Request`: cardType 필드를 enum으로 변경하거나 `@Pattern` validator 추가 — 컨트롤러에서 400 차단
- [ ] `PaymentRepositoryImpl.findByStatusIn`: limit 범위 검증 (1~500 상한, 범위 밖이면 기본값)

## CP21. 테스트 품질 개선

- [ ] `FakePaymentPgProcessor`: 저장 시 카드번호를 마스킹 형태로 변환
- [ ] `FakePaymentRepository`: `save()` 시 객체 복사본 저장 (참조 공유 방지)
- [ ] `RecoverAllPaymentsUseCase`: recoveredCount → attempted/recovered 분리 또는 결과 enum 반환
- [ ] `RequestPaymentUseCaseTest`: 성공 케이스에서 PG 프로세서 호출 횟수/인자 검증 추가
- [ ] `HandlePaymentCallbackUseCaseTest`: 실패 사유(reason) 전파 검증 추가
- [ ] `PaymentFlowIntegrationTest`: after-commit 실행 경로 검증

## CP22. 빌드/인프라 정리

- [ ] `build.gradle.kts`: `benchmarkTest` 태스크 등록 (Tag("benchmark") 테스트 별도 실행)
- [ ] `.claude/hooks/pre-tool-guard.sh`: `git -C` → `git --git-dir` 수정
- [ ] `RedisCleanUp`: flushDb 예외 처리 + 실패 시 로깅

## 최종 검증 (4차)

- [ ] ktlintCheck: PASS
- [ ] 전체 테스트: BUILD SUCCESSFUL
