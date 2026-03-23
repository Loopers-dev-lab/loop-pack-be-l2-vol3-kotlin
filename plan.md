# Step 3 QA 피드백 수정

## 개요

`step3-qa-review.md` 기반 코드 품질 수정.
CRITICAL 4건, WARNING 5건, CUT 1건, 누락 테스트 1건 = 총 11건.

## 병렬 실행 전략

두 모듈은 파일 겹침이 없으므로 완전 병렬 가능.

```
Lane A (streamer): Step 1 → Step 2
Lane B (api):      Step 3 → Step 4 → Step 5
                   ↑ 병렬 실행 ↑
```

cmux 패널 2개로 동시 진행. 각 Lane 완료 후 독립 checkpoint.

## 영향 범위

### commerce-streamer 수정 파일

- `infrastructure/coupon/CouponIssueRequestRepositoryImpl.kt` — Entity 분리 (C1)
- **NEW**: `infrastructure/coupon/CouponIssueRequestEntity.kt` (C1)
- `infrastructure/coupon/CouponRepositoryImpl.kt` — Entity 분리 + orElseThrow (C1, W5)
- **NEW**: `infrastructure/coupon/CouponEntity.kt` (C1)
- `infrastructure/coupon/IssuedCouponRepositoryImpl.kt` — 상수 추출 (C3)
- `interfaces/consumer/CouponIssueConsumer.kt` — 로깅 강화 (W3, W4)
- `test/.../ProcessCouponIssueUseCaseTest.kt` — markFailed 테스트 추가

### commerce-api 수정 파일

- `domain/coupon/model/CouponIssueRequest.kt` — UUID 기본값 제거 + init 제거 (C4, CUT-3)
- `domain/coupon/repository/CouponIssueRequestRepository.kt` — findByRequestIdAndUserId (W1)
- `application/coupon/CouponInfo.kt` — from() 팩토리 제거 (W2)
- `application/coupon/GetCouponIssueStatusUseCase.kt` — userId 추가 (W1)
- `infrastructure/coupon/CouponIssueRequestRepositoryImpl.kt` — JPA 메서드 추가 (W1)
- `interfaces/api/coupon/CouponV1Controller.kt` — userId 전달 (W1)
- `test/.../CouponIssueRequestTest.kt` — init 제거 반영
- `test/.../FakeCouponIssueRequestRepository.kt` — findByRequestIdAndUserId 구현
- `test/.../GetCouponIssueStatusUseCaseTest.kt` — userId 테스트
- `test/.../CouponIssueE2ETest.kt` — 확인/수정

---

## 구현 계획

### Step 1 — commerce-streamer: Entity 파일 분리 + Infrastructure 코드 품질

순수 리팩토링. 행위 변경 없이 파일 구조만 정리.

- [ ] A-1: `CouponIssueRequestRepositoryImpl.kt`에서 `CouponIssueRequestEntity` 클래스를 `CouponIssueRequestEntity.kt`로 분리. RepositoryImpl에는 JpaRepository + Impl만 남김
- [ ] A-2: `CouponRepositoryImpl.kt`에서 `CouponEntity` 클래스를 `CouponEntity.kt`로 분리. RepositoryImpl에는 JpaRepository + Impl만 남김
- [ ] A-3: `CouponRepositoryImpl.save()` — `orElseThrow()` → 의미 있는 예외 메시지 추가
- [ ] A-4: `IssuedCouponRepositoryImpl.kt` — `"AVAILABLE"` 매직 스트링을 companion object 상수로 추출

파일 수: 3 수정 + 2 신규 = 5

--- checkpoint: streamer ktlintCheck + test (기존 테스트 전부 통과 확인) ---

### Step 2 — commerce-streamer: Consumer 로깅 + 누락 테스트

- [ ] B-1: `CouponIssueConsumer` — 페이로드 파싱 실패 시 `log.warn` 추가 (W4)
- [ ] B-2: `CouponIssueConsumer` — 메시지 처리 실패 시 로그에 eventId 등 식별 정보 포함 + 실패 건수 로깅 (W3)
- [ ] B-3: [RED] 쿠폰이 존재하지 않을 때 `markFailed`로 상태 전이된다 → [GREEN] `ProcessCouponIssueUseCaseTest`에 FAILED 경로 테스트 추가

파일 수: 2 (Consumer 1 + Test 1)

--- checkpoint: streamer ktlintCheck + test ---

### Step 3 — commerce-api: Domain/Application 정리

- [ ] C-1: `CouponIssueRequest.kt` — `requestId` 기본값 `UUID.randomUUID()` 제거, 필수 파라미터로 변경 (C4)
- [ ] C-2: `CouponIssueRequest.kt` — `init` 블록(couponId/userId 양수 검증) 제거 + CoreException/ErrorType import 제거 (CUT-3)
- [ ] C-3: `CouponInfo.kt` — `CouponIssueRequestInfo.from()` companion object 제거 + CouponIssueRequest import 제거 (W2)
- [ ] C-4: `CouponIssueRequestTest.kt` — C-1, C-2 변경에 따른 테스트 수정 (requestId 명시 전달, init 검증 테스트 제거)

파일 수: 3 (production 2 + test 1)

--- checkpoint: api ktlintCheck + 대상 테스트 ---

### Step 4 — commerce-api: userId 소유자 검증 (W1)

- [ ] D-1: `CouponIssueRequestRepository` (domain) — `findByRequestIdAndUserId(requestId: String, userId: Long): CouponIssueRequest?` 메서드 추가
- [ ] D-2: `FakeCouponIssueRequestRepository` — `findByRequestIdAndUserId` 구현
- [ ] D-3: [RED] requestId + userId 불일치 시 NOT_FOUND 예외가 발생한다 → [GREEN] `GetCouponIssueStatusUseCase.execute(requestId, userId)` 시그니처 변경 + userId 검증
- [ ] D-4: `CouponIssueRequestRepositoryImpl` — JPA 메서드 `findByRequestIdAndUserId` 추가
- [ ] D-5: `CouponV1Controller.getIssueStatus()` — `@AuthUser userId`를 UseCase에 전달

파일 수: 5 (production 4 + test-fake 1)

--- checkpoint: api ktlintCheck + 대상 테스트 ---

### Step 5 — commerce-api: W1 테스트 수정

- [ ] E-1: `GetCouponIssueStatusUseCaseTest` — userId 포함 시그니처로 테스트 수정 + userId 불일치 시 예외 테스트 추가
- [ ] E-2: `CouponIssueE2ETest` — getIssueStatus 테스트가 기존대로 통과하는지 확인/수정

파일 수: 2 (test 2)

--- checkpoint: api 전체 ktlintCheck + test ---

## 고려사항

- Step 1은 순수 리팩토링(파일 분리)이므로 기존 테스트가 그대로 통과해야 함
- Step 3에서 requestId 기본값 제거 시, 테스트에서 requestId를 생략하는 코드가 있으면 컴파일 에러 → 테스트도 함께 수정
- Step 4의 D-3에서 NOT_FOUND 예외는 "requestId가 없는 경우"와 "userId 불일치" 모두 동일하게 NOT_FOUND 반환 (보안상 존재 여부 노출 방지)
- I1(canIssue 가독성), I2(var→val), I3(미사용 인덱스)은 이번 수정 범위에서 제외 (INFO 등급)
