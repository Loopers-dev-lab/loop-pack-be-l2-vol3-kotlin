---
name: tdd
description: Red → Green → Refactor TDD 사이클을 오케스트레이션한다. 각 Phase를 순서대로 진행하며, 사이클을 반복할 수 있다.
argument-hint: "[요구사항 설명] [--phase red|green|refactor|e2e|http] [--cycle N] [--fix] [--go]"
allowed-tools:
  - Read
  - Write
  - Edit
  - Glob
  - Grep
  - Bash(./gradlew *)
  - Bash(git diff *)
  - Bash(git status *)
  - AskUserQuestion
---

요구사항: $ARGUMENTS

## 오케스트레이터 역할

이 스킬은 Red → Green → Refactor 사이클을 순서대로 실행하는 오케스트레이터다.
각 Phase에 진입할 때 해당 Phase 파일을 Read하여 지침을 따른다.

## 플래그

- `--phase red|green|refactor` — 특정 Phase만 단독 실행
- `--cycle N` — N번째 사이클부터 시작 (기본값: 1)
- `--fix` — 버그 수정 모드: 재현 테스트(Red) → 최소 수정(Green), Refactor 생략
- `--go` — plan.md 기반 자동 실행. 다음 checkpoint까지 Red → Green 사이클을 연속 수행
- `--phase e2e` — E2E 테스트 작성 (구현 완료 후)
- `--phase http` — IntelliJ HTTP Client 파일 작성 (구현 완료 후)

플래그가 없으면 전체 사이클(Red → Green → Refactor)을 실행한다.

## 스킬 참조 경로

Phase 파일은 이 스킬의 `phases/` 디렉토리에 위치한다.
Phase 진입 시 항상 절대 경로로 Read한다:

```
/home/user/dev/.claude/skills/tdd/phases/phase-red.md
/home/user/dev/.claude/skills/tdd/phases/phase-green.md
/home/user/dev/.claude/skills/tdd/phases/phase-refactor.md
/home/user/dev/.claude/skills/tdd/phases/phase-e2e.md
/home/user/dev/.claude/skills/tdd/phases/phase-http.md
/home/user/dev/.claude/skills/tdd/phases/phase-go.md
```

## 실행 흐름

### 전체 사이클 (플래그 없음)

```
1. 요구사항 분석
2. [Red]     Read(phase-red.md)     → 실패 테스트 작성 → 실패 확인
3. [Green]   Read(phase-green.md)   → 최소 구현 → 통과 확인
4. [Refactor] Read(phase-refactor.md) → 구조 개선 → 테스트 재확인
5. 사이클 완료 보고
6. 추가 요구사항이 있으면 다음 사이클로 반복
```

### `--phase red` 단독

Read(phase-red.md) → 절차 실행 → Red Phase 완료 보고 후 종료.

### `--phase green` 단독

현재 실패 중인 테스트를 파악한다 (git diff, 테스트 실행 등).
Read(phase-green.md) → 절차 실행 → Green Phase 완료 보고 후 종료.

### `--phase refactor` 단독

모든 테스트 통과 상태를 먼저 확인한다.
Read(phase-refactor.md) → 절차 실행 → Refactor Phase 완료 보고 후 종료.

### `--go` 자동 실행

plan.md를 기반으로 다음 checkpoint까지 Red → Green 사이클을 연속 수행한다.
Read(phase-go.md) → 절차 실행 → Checkpoint 보고 후 종료.

### `--phase e2e` 단독

Read(phase-e2e.md) → E2E 테스트 작성 → 결과 보고 후 종료.

### `--phase http` 단독

Read(phase-http.md) → HTTP 파일 작성 → 결과 보고 후 종료.

### `--fix` 버그 수정 모드

```
1. 결함 분석: 증상, 재현 조건, 기대 동작 파악
2. [Red]   Read(phase-red.md)   → 버그 재현 테스트 작성 → 실패 확인
3. [Green] Read(phase-green.md) → 최소한의 버그 수정 → 통과 확인
4. Refactor 생략 (구조 개선은 별도 작업으로 제안)
5. 전체 테스트로 사이드 이펙트 확인
6. 결과 보고
```

## Phase 전환 규칙

- **Red → Green**: 테스트가 실패하는 것을 확인한 후에만 진행한다.
  실패를 확인하지 않으면 Green을 시작하지 않고 Red를 다시 실행한다.
- **Green → Refactor**: 테스트가 통과하는 것을 확인한 후에만 진행한다.
  통과를 확인하지 않으면 Refactor를 시작하지 않고 Green을 계속한다.
- **Refactor → 완료 또는 다음 Red**: 리팩토링 후 테스트가 여전히 통과하는 것을 확인한다.
  실패하면 변경을 되돌리고 원인을 보고한다.

## 각 Phase 완료 보고 형식

```
## [Red|Green|Refactor] Phase 완료

- **테스트**: 패키지.클래스명.메서드명
- **결과**: 실패 (컴파일 에러 / assertion 실패) | 통과 | 구조 개선 완료
- **다음 단계**: [다음 Phase 설명 또는 완료]
```

## 사이클 완료 보고 형식

```
## TDD 사이클 #N 완료

- **요구사항**: ...
- **테스트**: 추가된 테스트 목록
- **구현**: 변경된 파일 목록
- **리팩토링**: 수행한 개선 사항 (없으면 "없음")
- **다음 사이클**: 추가 요구사항 설명 또는 "완료"
```

## 중요 규칙

- `/tdd`는 전체 사이클 오케스트레이터다. `--phase red|green|refactor`로 개별 Phase도 단독 실행할 수 있다.
- 구조적 변경(Refactor)과 행위적 변경(Red+Green)을 절대 같은 단계에 섞지 않는다 (Tidy First).
- 각 Phase는 반드시 해당 Phase 파일을 Read한 후 실행한다. 기억에 의존하지 않는다.
- 테스트 검증 명령: `./gradlew :apps:commerce-api:test --tests "패키지.클래스명"`
- 전체 테스트: `./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test`
