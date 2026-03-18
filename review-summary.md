# PR #19 CodeRabbit 2차 리뷰 검토 결과

> 3f646d8 커밋(1차 리뷰 반영) 이후 CodeRabbit 재리뷰 (2026-03-18)
> Actionable 8건 + Nitpick 3건 = 총 11건

## 카테고리 범례

- **D**: 수정 필요 (actionable) → `plan.md` CP15-17에 반영
- **E**: 트레이드오프/기각 → 사유 기술

---

## D. 수정 필요 (8건)

### D1. `.gitignore` `scripts/` 제거 [Actionable]
- **파일**: `.gitignore:49`
- **왜 나왔나**: `scripts/` 패턴이 트리 전체에서 scripts 디렉토리를 무시한다. 소스 코드 내부의 scripts 디렉토리까지 영향.
- **수정 방향**: 라인 제거. 루트 scripts/는 이미 프로젝트에서 분리 완료.

### D2. RecoverAllPaymentsUseCase: CoreException 외 예외 누락 [Actionable]
- **파일**: `RecoverAllPaymentsUseCase.kt:25-29`
- **왜 나왔나**: 1차 리뷰(A9)에서 Exception 전체 catch → CoreException만 catch로 좁혔는데, 네트워크/DB 예외 등으로 배치가 중단될 수 있다.
- **수정 방향**: CoreException catch (log.warn) 유지 + Exception catch 추가 (log.error + 스택트레이스). 배치 중단 방지.

### D3. RequestPaymentUseCase: CardType.valueOf 원본 예외 유실 [Actionable]
- **파일**: `RequestPaymentUseCase.kt:52-56`
- **왜 나왔나**: `IllegalArgumentException`을 잡아서 `CoreException`으로 래핑하지만 원본 예외가 cause로 체이닝되지 않아 디버깅 정보가 손실.
- **수정 방향**: `.also { it.initCause(e) }` 추가

### D4. PgClientImpl: PgResultStatus.SUCCESS 하드코딩 [Actionable]
- **파일**: `PgClientImpl.kt:41-45`
- **왜 나왔나**: PG 응답에 `meta.result`와 `data.status` 필드가 존재하지만, 현재 코드는 HTTP 200이면 무조건 SUCCESS를 반환. PG가 HTTP 200으로 실패를 반환할 수 있다.
- **수정 방향**: `meta.result` 검사 + `data.transactionKey` 비어있으면 에러 처리

### D5. HandlePaymentCallbackUseCase: 불필요한 Order 락 점유 [Nitpick]
- **파일**: `HandlePaymentCallbackUseCase.kt:19-26`
- **왜 나왔나**: `isProcessable` 검사 전에 Order 락을 획득하여, 이미 처리된 중복 콜백에서도 Order 트랜잭션이 대기한다.
- **수정 방향**: `isProcessable` 검사를 Order 락 획득 전으로 이동

### D6. PaymentEntity: (status, id) 복합 인덱스 누락 [Actionable]
- **파일**: `PaymentRepositoryImpl.kt:54-58` → `PaymentEntity.kt`
- **왜 나왔나**: `findByStatusIn`이 status 필터 + id 정렬을 사용하지만 복합 인덱스가 없어 풀 테이블 스캔 가능.
- **수정 방향**: `@Table(indexes = [Index(...)])` 추가

### D7. PaymentTest: 비표준 카드번호 형식 테스트 누락 [Nitpick]
- **파일**: `PaymentTest.kt:38-51`
- **왜 나왔나**: `maskCardNo`에 하이픈 없는 카드번호 폴백 로직(`"*".repeat(n) + last4`)이 있지만 테스트가 없다.
- **수정 방향**: 하이픈 없는 카드번호 테스트 추가

### D8. PgClientImpl fallback: 로깅 부실 [Actionable]
- **파일**: `PgClientImpl.kt:60-62`
- **왜 나왔나**: 상태 조회 실패 시 `e.message`만 기록하고 예외 타입/스택트레이스가 없어 장애 원인 분석 어려움.
- **수정 방향**: 예외 타입 명시 + 스택트레이스 포함 로깅 (TIMEOUT 기본값은 유지)

---

## E. 트레이드오프 / 기각 (3건)

### E1. `setScale(0, UNNECESSARY)` → `HALF_UP` 변경 제안 [기각]
- **파일**: `RequestPaymentUseCase.kt:61`
- **리뷰 요지**: `UNNECESSARY`는 소수점이 있으면 ArithmeticException을 던진다. `HALF_UP`으로 안전하게 반올림하라.
- **프로젝트 판단**: **1차 리뷰(A1)에서 우리가 의도적으로 선택한 안전장치.** `BigDecimal.toLong()` 정밀도 손실 → `setScale(0, UNNECESSARY).toLong()`으로 수정한 것. KRW는 소수점 단위가 없으므로, totalPrice에 소수점이 있다면 상류 버그다. `HALF_UP`은 버그를 조용히 숨긴다. 코드에 의도 주석을 추가하여 향후 리뷰에서 같은 제안이 반복되지 않도록 한다.

### E2. RecoverAllPaymentsUseCase: BATCH_SIZE 루프 처리 제안 [부분 수용]
- **파일**: `RecoverAllPaymentsUseCase.kt:20-22`
- **리뷰 요지**: 고정 50건이면 대량 적체 시 처리 지연. 루프로 전부 처리하거나 설정 가능하게.
- **프로젝트 판단**: 루프는 장시간 실행·OOM·스케줄러 스레드 블로킹 위험. 고정 배치 + 주기적 스케줄러가 더 안전한 회복탄력성 패턴. **BATCH_SIZE를 `@Value`로 설정 가능하게** 만드는 것만 수용.

### E3. PgClientImpl fallback: catch-all 예외 세분화 제안 [부분 수용]
- **파일**: `PgClientImpl.kt:56-63`
- **리뷰 요지**: 모든 예외를 TIMEOUT으로 처리하면 4xx(FAILED)와 I/O 오류를 구분하지 못한다. 예외별 분기 필요.
- **프로젝트 판단**: 이중 fallback(PG 호출 실패 → 상태 조회도 실패)에서 예외를 전파하면 결제가 미정의 상태에 빠진다. TIMEOUT은 "모르겠으니 recovery scheduler가 재확인"이라는 **가장 안전한 기본값.** 로깅 개선만 수용.

---

# PR #20 CodeRabbit/Gemini 리뷰 검토 결과

> PR #20 초회 리뷰 (2026-03-18)
> CodeRabbit 32건 (Critical 5 + Major 24 + Minor 3) + Gemini 2건 = 총 34건

## 카테고리 범례
- **F**: 수정 필요 → `plan.md` CP18-22에 반영
- **G**: 트레이드오프/기각
- **H**: 기결정 (docs/review-decisions.md 참조)
- **I**: 범위 밖 / .json 제외

---

## F. 수정 필요 (19건)

### F1. 락 순서 불일치 — 교착 상태 위험 [CodeRabbit · Critical]
- **파일**: `RequestPaymentUseCase` vs `HandlePaymentCallbackUseCase`/`RecoverPaymentUseCase`/`PaymentPgProcessor`
- **왜 나왔나**: RequestPaymentUseCase만 Order→Payment 순서, 나머지 3곳은 Payment→Order. 재결제+콜백 동시 발생 시 교착 가능.
- **수정 방향**: RequestPaymentUseCase를 Payment→Order 순서로 통일

### F2. OrderRepositoryImpl 비관적 락 타임아웃 없음 [CodeRabbit · Major]
- **파일**: `OrderRepositoryImpl.kt:27-28`
- **왜 나왔나**: PaymentJpaRepository는 3초 타임아웃 설정. OrderJpaRepository는 무제한 대기.
- **수정 방향**: `@QueryHints` 타임아웃 3초 추가

### F3. PgStatusQueryClient firstOrNull() [CodeRabbit · Major]
- **파일**: `PgStatusQueryClient.kt:21-39`
- **왜 나왔나**: 복수 transaction 응답 시 첫 번째(오래된 것)를 선택해 잘못된 복구 가능.
- **수정 방향**: 최신(마지막) transaction 선택

### F4. 예외 메시지에 raw transactionKey [CodeRabbit · Major]
- **파일**: `HandlePaymentCallbackUseCase.kt:29-33`
- **왜 나왔나**: ControllerAdvice 응답/로그에 PG 식별자 노출.
- **수정 방향**: orderId 중심 메시지로 변경, transactionKey 마스킹

### F5. afterCommit 스택 트레이스 누락 [CodeRabbit · Minor]
- **파일**: `RecoverPaymentUseCase.kt:75-77`
- **왜 나왔나**: catch에서 e.message만 로깅, 스택 트레이스 없어 디버깅 어려움.
- **수정 방향**: log.warn에 예외 객체(e) 추가

### F6. resilience4j 버전 미고정 [CodeRabbit · Major]
- **파일**: `build.gradle.kts:24-30`
- **왜 나왔나**: Spring BOM에 미포함, floating version 위험.
- **수정 방향**: 명시적 버전 고정

### F7. PgPaymentRequest toString 민감정보 [CodeRabbit · Major]
- **파일**: `PgPaymentRequest.kt:5-10`
- **왜 나왔나**: data class 기본 toString에 cardNo 원문 포함.
- **수정 방향**: toString() override, cardNo 마스킹

### F8. PaymentDto cardType 문자열 검증 [CodeRabbit · Major]
- **파일**: `PaymentDto.kt:9-15`
- **왜 나왔나**: 임의 문자열이 통과, enum 변환 실패 시 500.
- **수정 방향**: enum 직접 사용 또는 validator 추가

### F9. idempotencyKey nullable [CodeRabbit · Major]
- **파일**: `PgFeignPaymentRequest.kt:39-45`
- **왜 나왔나**: 타입이 `String?`이라 null 전달 가능, 중복 결제 위험.
- **수정 방향**: `String` non-null로 변경

### F10. limit 파라미터 검증 [CodeRabbit · Major]
- **파일**: `PaymentRepositoryImpl.kt:54-56`
- **왜 나왔나**: 0 이하/과대값 방어 없음.
- **수정 방향**: 범위 검증 (1~500 상한)

### F11. pre-tool-guard.sh git -C 버그 [CodeRabbit · Major]
- **파일**: `.claude/hooks/pre-tool-guard.sh:12-15`
- **왜 나왔나**: `git -C <.git경로>`는 동작하지 않음. `git --git-dir`을 사용해야.
- **수정 방향**: `git -C` → `git --git-dir` 수정

### F12. FakePaymentPgProcessor 카드번호 [CodeRabbit · Major]
- **파일**: `FakePaymentPgProcessor.kt:4-15`
- **왜 나왔나**: 테스트 더블에 카드번호 원문 저장, 로그 노출 위험.
- **수정 방향**: 마스킹 형태로 저장

### F13. recoveredCount 과대 집계 [CodeRabbit · Major]
- **파일**: `RecoverAllPaymentsUseCase.kt:19-32`
- **왜 나왔나**: `execute()`가 true면 상태 전이 없어도 카운트 증가.
- **수정 방향**: attempted/recovered 분리

### F14. FakePaymentRepository 참조 공유 [CodeRabbit · Major]
- **파일**: `FakePaymentRepository.kt:12-33`
- **왜 나왔나**: save() 후 원본과 저장소가 같은 참조 → 거짓 양성.
- **수정 방향**: copy-on-save 구현

### F15. RequestPaymentUseCaseTest PG 호출 미검증 [CodeRabbit · Major]
- **파일**: `RequestPaymentUseCaseTest.kt:64-87`
- **왜 나왔나**: PG 프로세서 호출 여부/인자를 검증하지 않음.
- **수정 방향**: 호출 횟수/인자 단언 추가

### F16. 실패 사유 전파 미검증 [CodeRabbit · Minor]
- **파일**: `HandlePaymentCallbackUseCaseTest.kt:92-114`
- **왜 나왔나**: reason 입력 후 저장 결과에서 상태만 확인, reason 미검증.
- **수정 방향**: reason 단언 추가

### F17. PaymentFlowIntegrationTest after-commit 우회 [CodeRabbit · Major]
- **파일**: `PaymentFlowIntegrationTest.kt:52-56`
- **왜 나왔나**: afterCommit 실행 경로가 검증되지 않음.
- **수정 방향**: after-commit 실행 검증 추가

### F18. benchmarkTest 태스크 미등록 [CodeRabbit · Major]
- **파일**: `build.gradle.kts:5-9`
- **왜 나왔나**: @Tag("benchmark") 테스트가 제외만 되고 별도 실행 경로 없음.
- **수정 방향**: benchmarkTest 태스크 등록

### F19. RedisCleanUp flushDb 예외 [CodeRabbit · Minor]
- **파일**: `RedisCleanUp.kt:10-12`
- **왜 나왔나**: Redis 정리 실패 시 후속 테스트 격리 불가.
- **수정 방향**: 예외 처리 + 로깅

---

## G. 트레이드오프 / 기각 (4건)

### G1. PG 호출 예외 시 상태 전이 없음 [기각 — 의도적 설계]
- **리뷰 요지**: pgClient.requestPayment() 예외 시 Payment가 REQUESTED에 남아 미복구
- **프로젝트 판단**: afterCommit 설계의 의도적 트레이드오프. Resilience4j fallback이 TIMEOUT을 반환하고, recovery scheduler가 REQUESTED/TIMEOUT을 주기적으로 픽업. 동기 PG 호출은 DB 트랜잭션 내 외부 I/O 유발로 더 위험.

### G2. after-commit 예외 삼킴 → REQUESTED 고착 [기각 — G1과 동일]
- **프로젝트 판단**: 사용자에게 "결제 진행 중" 응답 후 비동기 처리. recovery scheduler가 안전망.

### G3. afterCommit 무한 재시도 (retryCount/backoff 없음) [기각 — 현 단계]
- **리뷰 요지**: 60초마다 무한 재시도, PG 장시간 장애 시 플러딩
- **프로젝트 판단**: retryCount 도입은 Payment 도메인 모델 변경(필드 추가) 필요한 scope 확대. 단일 인스턴스+시뮬레이터 환경에서 premature. 실 PG 연동 시 구현.

### G4. 중복 SUCCESS 콜백 테스트 기대값 [기각 — 의도적 설계]
- **리뷰 요지**: payment=SUCCESS + order=PENDING 불일치 상태에서 콜백이 Order를 PAID로 복구해야
- **프로젝트 판단**: 비정상 상태의 자동 보정은 예상치 못한 상태 변경 유발. 별도 보정 API/배치로 처리하는 것이 안전.

---

## H. 기결정 반복 (6건)

| # | 항목 | 기존 결정 |
|---|------|----------|
| 1 | 콜백 인증 없음 (Controller) | RD-003 |
| 2 | 콜백 무인증 (seed 문서) | RD-003 |
| 3 | Feign 타임아웃 환경별 | RD-005 |
| 4 | PG URL localhost 기본값 | RD-004 |
| 5 | IOException retry | RD-009 |
| 6 | Hook 상대경로 | RD-002 |

## I. 범위 밖 (5건)

| # | 항목 | 사유 |
|---|------|------|
| 1 | seed WHERE 조건 | 요구사항 문서, 코드는 JPA 메서드 쿼리 |
| 2 | seed Fallback 분기 | 요구사항 문서, 이미 구현 완료 |
| 3 | design/03 다이어그램 | docs/design/ 읽기 금지 |
| 4 | design/04 인덱스 | docs/design/ 읽기 금지 |
| 5 | config.json 민감 패턴 | .json 파일 CodeRabbit 리뷰 제외 |
