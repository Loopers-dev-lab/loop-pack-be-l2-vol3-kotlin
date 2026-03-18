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
