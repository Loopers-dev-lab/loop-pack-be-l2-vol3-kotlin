---
name: design
version: 1.0.0
description: "요구사항 분석부터 설계 리뷰까지 전체 설계 사이클을 단계별로 실행한다."
argument-hint: "[요구사항 또는 PRD 경로] [--phase requirements|sequence|class|erd|review] [--status] [--resume] [--quick]"
allowed-tools:
  - Read
  - Write
  - Edit
  - Glob
  - Grep
  - Bash(mkdir *)
  - Bash(ls *)
---

오케스트레이터. 설계 사이클의 각 Phase를 순서대로 실행하고 상태를 관리한다.

항상 한국어로 응답한다.

## 스킬 참조 경로

이 스킬의 파일들은 `.claude/skills/design/` 하위에 위치한다.
Phase 파일을 Read할 때 절대 경로를 사용한다.

Phase 진입 시 반드시 해당 Phase 파일을 Read한 후 실행한다:
```
Read(`<프로젝트 루트>/.claude/skills/design/phases/phase-{name}.md`)
```

## 인자

- `ARGS[0]` (선택): 요구사항 설명 또는 PRD 문서 경로
- `--phase requirements|sequence|class|erd|review`: 특정 Phase만 실행
- `--status`: 현재 진행 상황 조회. 다른 플래그와 함께 사용 불가.
- `--resume`: 중단된 작업 재개. state.md의 current-phase에서 재시작.
- `--quick`: requirements → review만 실행 (sequence/class/erd 다이어그램 생략)

ARGS[0]이 없고 `--status`도 `--resume`도 없으면 다음을 응답:
"분석할 요구사항이나 PRD 경로를 입력해주세요. 예: `/design [기능 설명]` 또는 `/design docs/requirements.md`"

## Phase 개요

| Phase | 파일 | 산출물 | 사용자 확인 |
|-------|------|--------|------------|
| requirements | phase-requirements.md | `docs/design/01-requirements.md` | Q&A 후 확인 |
| sequence | phase-sequence.md | `docs/design/02-sequence-diagrams.md` | 저장 후 확인 |
| class | phase-class.md | `docs/design/03-class-diagram.md` | 저장 후 확인 |
| erd | phase-erd.md | `docs/design/04-erd.md` | 저장 후 확인 |
| review | phase-review.md | 리뷰 보고서 (인라인) | 최종 보고 |

### --quick 경로

중간 다이어그램(sequence/class/erd)을 생략하고 핵심만 실행:
```
정상: requirements → sequence → class → erd → review
quick: requirements → review
```
`--quick` 사용 시 review Phase에서 존재하는 문서만 검증한다.

## Phase 라우팅

`--phase`가 지정되면 해당 Phase만 실행한다:
- `--phase requirements`: requirements 실행. 완료 후 다음 Phase 진행 여부 확인.
- `--phase sequence`: `docs/design/01-requirements.md`가 있으면 참고. sequence 실행.
- `--phase class`: `docs/design/02-sequence-diagrams.md`가 있으면 참고. class 실행.
- `--phase erd`: `docs/design/03-class-diagram.md`가 있으면 참고. erd 실행.
- `--phase review`: 존재하는 모든 설계 문서를 대상으로 review 실행.

`--phase` 없이 실행하면 requirements부터 순차적으로 모든 Phase를 실행한다.
각 Phase 완료 후 사용자에게 "다음 Phase(XXX)로 진행할까요?" 확인 후 계속한다.

## --status 동작

`--status`가 지정되면 파이프라인을 실행하지 않고 현재 상태만 출력한다:

1. `.design/state.md`를 탐색한다.
2. state.md가 없으면: "진행 중인 설계 작업이 없습니다." 출력 후 종료.
3. state.md가 있으면 다음을 출력:
   ```
   ## 설계 파이프라인 상태
   - 작업: {subject}
   - 현재 Phase: {current-phase} ({status})
   - 플래그: {flags}
   - 시작: {started}

   ### Phase 진행
   - requirements: {status}
   - sequence: {status}
   - class: {status}
   - erd: {status}
   - review: {status}
   ```
4. 출력 후 종료.

## 상태 관리 (state.md)

파이프라인 진행 상태를 `.design/state.md`에 기록하여 세션 재개를 지원한다.

**state.md 구조:**
```yaml
subject: "쿠폰 발급 기능"
current-phase: sequence
status: in_progress
flags: ""
started: 2026-03-04T10:00:00
phases:
  requirements: completed
  sequence: in_progress
  class: pending
  erd: pending
  review: pending
artifacts:
  requirements: docs/design/01-requirements.md
  sequence: docs/design/02-sequence-diagrams.md
  class: docs/design/03-class-diagram.md
  erd: docs/design/04-erd.md
```

**갱신 규칙:**
- Phase 진입 시: `current-phase: {name}`, `phases.{name}: in_progress`로 갱신.
- Phase 완료 시: `phases.{name}: completed`로 갱신.
- `--resume` 시: `current-phase`에서 재개한다.
- 새 파이프라인 시작 시 기존 state.md를 덮어쓴다.

## 산출물 저장 위치

| 문서 | 경로 |
|------|------|
| 요구사항 명세 | `docs/design/01-requirements.md` |
| 시퀀스 다이어그램 | `docs/design/02-sequence-diagrams.md` |
| 클래스 다이어그램 | `docs/design/03-class-diagram.md` |
| ERD | `docs/design/04-erd.md` |

## 플래그 충돌 검증

- `--status`는 단독 사용. 다른 플래그와 함께 사용하면 에러.
- `--resume`은 단독 사용. `--phase`와 함께 사용하면 에러.
- `--quick`과 `--phase`는 동시 사용 불가.

## Phase 진행 원칙

1. 각 Phase 진입 전 반드시 해당 Phase 파일을 Read한다.
2. Phase 완료 후 산출물을 `docs/design/`에 저장한다.
3. 저장 후 사용자에게 다음 Phase 진행 여부를 확인한다.
4. 사용자가 수정을 요청하면 현재 Phase를 재실행한다.
5. 에러 발생 시 조용히 무시하지 않고 사용자에게 보고한다.

## 보고 포맷

각 Phase 완료 시 다음 형식으로 보고한다:
```
## [Phase명] 완료

**산출물**: {파일 경로}
**핵심 내용**: {2~3줄 요약}

다음 Phase: {다음 Phase명} — 진행할까요?
```

최종 review Phase 완료 시:
```
## 설계 사이클 완료

| Phase | 산출물 | 상태 |
|-------|--------|------|
| requirements | 01-requirements.md | 완료 |
| sequence | 02-sequence-diagrams.md | 완료 |
| class | 03-class-diagram.md | 완료 |
| erd | 04-erd.md | 완료 |
| review | (인라인 보고) | 완료 |

**다음 단계**: 설계 리뷰 결과를 바탕으로 구현을 시작하거나, `/verify-docs`로 코드와 문서 정합성을 확인할 수 있습니다.
```
