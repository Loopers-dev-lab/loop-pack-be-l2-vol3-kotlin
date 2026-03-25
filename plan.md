# PR #27 CodeRabbit 리뷰 반영

## 개요

PR #27 CodeRabbit 리뷰 분석 결과 AGREE 18건, DISMISS 4건 = 총 22건 (인라인 5 Major + outside-diff 1 + Minor 16).
DISMISS 4건 중 2건은 이전 PR #26에서 기각된 반복 지적 (RD-019, RD-023).
AGREE 18건을 4개 그룹으로 분류하여 수정.

## TRADEOFF — 결정 완료

- [x] **[CR-9] .coderabbit.yaml path_filters**: 무시 (현행 유지). develop에 .coderabbit.yaml 머지 후 자동 해소
- [x] **[CR-16, CR-21] docs/design 수정**: 개발자 지시에 따라 수정 진행

## 영향 범위

```text
Group A (production): CatalogOutbox, OrderEventConsumer, CouponIssueRequest,
                      KafkaOutboxEventPublisher, GetProductsUseCase, OutboxRelaySchedulerTest
Group B (test):       UpdateProductMetricsUseCaseTest
Group C (skills/docs): sync-config, design, resume-context, plan, tdd, ship SKILL.md
Group D (design):     04-erd.md, 03-class-diagram.md
```

---

## Group A: Production 코드 변경 (6건)

### A-1. CatalogOutbox eventId 빈 값 검증 [CR-2]
- [x] `apps/commerce-api/.../domain/outbox/model/CatalogOutbox.kt` — init 블록에 `eventId.isBlank()` 검증 추가

### A-2. OrderEventConsumer 빈 값/비양수 검증 강화 [CR-5]
- [x] `apps/commerce-streamer/.../interfaces/consumer/OrderEventConsumer.kt` — eventId/eventType `require(isNotBlank())`, productId `require(> 0)` 추가

### A-3. CouponIssueRequest requestId 검증 [CR-15]
- [x] `apps/commerce-streamer/.../domain/coupon/model/CouponIssueRequest.kt` — init 블록에 `require(requestId.isNotBlank())` 검증 추가

### A-4. KafkaOutboxEventPublisher 예외 처리 보강 [CR-17]
- [x] `apps/commerce-api/.../infrastructure/outbox/KafkaOutboxEventPublisher.kt` — ExecutionException(cause unwrap), TimeoutException(명시적 래핑) catch 추가

### A-5. GetProductsUseCase 미사용 userId 제거 [CR-18]
- [x] `apps/commerce-api/.../application/catalog/product/GetProductsUseCase.kt` — userId 파라미터 제거
- [x] `apps/commerce-api/.../interfaces/api/product/ProductV1Controller.kt` — userId 전달 제거
- [x] ApiSpec 인터페이스의 userId 파라미터는 유지 (인증 목적)
- [x] 테스트 이미 userId 미사용 — 수정 불필요

### A-6. OutboxRelaySchedulerTest 빈 함수 블록 [CR-19]
- [x] `apps/commerce-api/.../test/.../OutboxRelaySchedulerTest.kt` — `{}` → `= Unit`

---

## Group B: 테스트 코드 변경 (1건)

### B-1. UpdateProductMetricsUseCaseTest EventHandled 검증 [CR-20]
- [x] `apps/commerce-streamer/.../test/.../UpdateProductMetricsUseCaseTest.kt` — 알 수 없는 이벤트 처리 시 EventHandled 저장 assertion 추가 (catalog + order 모두)

---

## Group C: 스킬/설정 문서 변경 (9건)

### C-1. sync-config SKILL.md `git add -A` 안전성 [CR-1]
- [x] `.claude/skills/sync-config/SKILL.md:114` — `git add -A` → 동기화 대상 파일만 명시적 staging

### C-2. design SKILL.md `--phase` 인자 목록 [CR-6]
- [x] `.claude/skills/design/SKILL.md:42` — `|verify-docs` 추가

### C-3. design SKILL.md 호출 커맨드 통일 [CR-13]
- [x] `.claude/skills/design/SKILL.md:187` — `/verify-docs` → `/design --phase verify-docs`

### C-4. phase-red.md 테스트 이름 규칙 통일 [CR-11]
- [x] `.claude/skills/tdd/phases/phase-red.md:58-61` — `동작을 한국어로 설명한다` → `동작을 설명한다`

### C-5. MD040 코드 블록 언어 태그 일괄 수정 [CR-7, CR-8, CR-10, CR-12, CR-14]
- [x] `.claude/skills/resume-context/SKILL.md:15,44` — `bash`, `markdown`
- [x] `plan.md:17` — `text` (이미 수정됨)
- [x] `.claude/skills/design/phases/phase-verify-docs.md:40,62` — `markdown`
- [x] `.claude/skills/ship/phases/phase-commit.md:23` — `markdown`
- [x] `.claude/skills/plan/SKILL.md:109` — `markdown`

---

## Group D: 설계 문서 변경 (2건)

### D-1. ERD 인덱스 문서 불일치 [CR-16]
- [x] `docs/design/04-erd.md:495` — `(status, created_at)` → `(published, id)` 통일

### D-2. class-diagram 섹션 번호 충돌 [CR-21]
- [x] `docs/design/03-class-diagram.md:1099+` — Round 7 섹션 8-12 → 13-17로 변경, `(Round 7)` 접미사 추가
