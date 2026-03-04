# Phase: Go (자동 실행)

plan.md를 읽고 다음 checkpoint까지 미완료 항목들을 연속으로 Red → Green TDD 사이클 수행한다.

## 절차

1. `plan.md`를 읽는다
    - 파일이 없으면 "plan.md가 없습니다. `/plan` 커맨드로 먼저 작성해주세요."라고 안내하고 중단한다
2. 진행 상황을 요약한다: `완료: N개 / 미완료: M개 / 다음 checkpoint까지: K개`
    - checkpoint가 없으면 미완료 항목 1개만 대상으로 한다 (하위 호환)
3. 다음 checkpoint까지의 미완료 항목들을 순서대로 진행:
    a. 항목 알림: "진행 중: {항목 내용}"
    b. **Red**: 해당 항목의 실패하는 테스트를 작성한다
       - 3A 원칙: Arrange(준비) → Act(실행) → Assert(검증)
       - `@Nested` + `@DisplayName`(한국어) BDD 스타일
    c. 테스트가 실패하는지 확인한다:
       ```
       ./gradlew :apps:commerce-api:test --tests "해당테스트클래스.해당테스트메서드"
       ```
    d. **Green**: 테스트를 통과시키기 위한 최소한의 코드만 구현한다
       - 오버엔지니어링 금지, 기존 프로젝트 패턴 준수
    e. 해당 테스트 통과 확인
    f. `plan.md`에서 해당 항목을 `- [x]`로 체크한다
    g. 다음 항목으로 (checkpoint까지 반복)
4. Checkpoint 도달 시 자가 검증:
    a. `./gradlew ktlintFormat` (자동 수정)
    b. `./gradlew ktlintCheck` (린트 확인, 실패 시 자가 수정 시도)
    c. `./gradlew test` (전체 테스트)
5. 검증 통과 시 표준 보고:

   ```
   ═══════════════════════════════════════
    CHECKPOINT REACHED — AWAITING REVIEW
   ═══════════════════════════════════════

   ## Change
   - 완료한 항목 목록 (체크리스트)
   - 생성/수정한 파일 목록

   ## Validation
   - ktlintCheck: 통과/실패
   - 전체 테스트: N개 통과 / N개 실패

   ## Risk/Ambiguity
   - 임의 결정한 네이밍, 가정한 비즈니스 로직 등
   - 없으면 "Perfectly aligned with spec"

   ## Next
   - 다음 checkpoint까지 항목 미리보기
   - 남은 미완료 항목 수
   ```

6. 검증 실패 시: 자가 수정 1회 시도 → 재실패 시 실패 보고 후 중단

## checkpoint가 없는 plan.md

- `--- checkpoint ---` 구분선이 없으면 1개 항목 단위로 동작 (하위 호환)
- 이 경우에도 lint 포함 검증과 표준 보고 포맷은 동일하게 적용

## 주의사항

- 전체 테스트가 실패하면 plan.md를 체크하지 않고 실패 내용을 보고한다
- null-safety 준수, println 금지
