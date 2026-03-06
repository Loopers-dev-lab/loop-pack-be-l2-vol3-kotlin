---
name: ship
description: |
  출시 워크플로우: 커밋 → PR → 핸드오프.
  /ship [--phase commit|pr|handoff] [--all]
---

# Ship Pipeline

출시 워크플로우 오케스트레이터.

## 사용법

```
/ship [--phase commit|pr|handoff] [--all]
```

## 페이즈 흐름

```
commit → pr → handoff
```

## 옵션

- 기본 실행 (옵션 없음): commit만 수행
- `--phase commit`: 커밋 생성
- `--phase pr`: commit + pr 순차 실행
- `--phase handoff`: 핸드오프 노트만 작성
- `--all`: commit → pr → handoff 전체 실행

## 페이즈 요약

| 페이즈 | 설명 | 산출물 |
|-------|------|--------|
| commit | Tidy First 변경 분류 + 커밋 생성 | git commit |
| pr | 커밋 이력 기반 PR 생성 | GitHub PR |
| handoff | 세션 핸드오프 노트 작성 | `.claude/handoff.md` |

## 실행 로직

1. `$ARGUMENTS`에서 `--phase`와 `--all` 옵션을 파싱한다
2. 대상 페이즈의 `phases/phase-<name>.md`를 읽고 실행한다
3. 기본 실행은 commit만 수행한다
4. `--phase pr`은 commit → pr을 순차 실행한다
5. `--all`은 commit → pr → handoff 전체를 순차 실행한다
