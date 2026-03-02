---
name: commit-plan
description:
  커밋 계획 파일을 읽고 순차적으로 커밋을 실행한다.
  분석 루프 없이 계획대로 즉시 실행한다.
---

## 절차

1. **계획 파일 확인**: 사용자가 지정한 커밋 계획 파일을 읽는다.
   파일이 지정되지 않으면 `git diff --stat`으로 변경 파일을 확인하고, 커밋 분리 계획을 제안한 뒤 승인을 받는다.

2. **사전 검증**: 첫 커밋 실행 전 한 번만 수행한다.
   - `./gradlew ktlintCheck` — 실패 시 `ktlintFormat` 실행 후 재확인
   - `./gradlew test` — 실패 시 중단하고 보고

3. **커밋 순차 실행**: 계획의 각 커밋에 대해:
   a. 해당 커밋에 포함될 파일만 `git add`로 staging
   b. hunk-level staging이 필요하면 `git add -p` 사용
   c. 로컬 전용 파일(review-plan.md, .omc/ 등)은 staging하지 않음
   d. 계획에 명시된 커밋 메시지로 즉시 커밋
   e. **커밋 간 분석/재계획 금지** — 다음 커밋으로 바로 진행

4. **완료 확인**: 모든 커밋 후 `git log --oneline -n <커밋수>`로 결과 확인

## 계획 파일 형식 예시

```markdown
## Commit 1: refactor: Domain VO 정리
- src/main/kotlin/.../domain/Money.kt
- src/main/kotlin/.../domain/Quantity.kt

## Commit 2: feat: 주문 생성 로직 구현
- src/main/kotlin/.../domain/OrderService.kt
- src/test/kotlin/.../domain/OrderServiceTest.kt
```

## 규칙

- 계획에 없는 파일을 임의로 추가하지 않는다
- 커밋 메시지는 계획에 명시된 대로 사용한다 (임의 수정 금지)
- 커밋 사이에 테스트를 돌리지 않는다 (사전 검증에서 이미 통과 확인)
- 문제가 발생하면 즉시 중단하고 보고한다
