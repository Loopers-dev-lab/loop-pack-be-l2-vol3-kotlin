---
name: sync-config
description: |
  현재 프로젝트의 .claude/ 변경사항을 config repo에 선별적으로 반영한다.
  프로젝트 고유 데이터(신뢰 패턴, 규칙 효과 이력 등)는 자동으로 제외한다.
  '설정 동기화', 'config 반영', 'config repo 업데이트' 같은 요청에 트리거한다.
---

# Sync Config

현재 프로젝트의 `.claude/` 설정 변경사항을 config repo에 의미 기반으로 선별 반영하는 스킬.

## 사용법

```bash
/sync-config
```

## 설정

config repo 경로는 `.claude/config.json`의 `configRepo` 필드에서 읽는다.

```json
{ "configRepo": "~/IdeaProjects/claude-code-config" }
```

## 동기화 대상

### 전체 파일 동기화 (Full Sync)

파일 전체를 비교하여 변경 시 반영한다:

| 대상 | config repo 위치 |
|------|-----------------|
| `skills/` (`---` 없는 파일은 전체) | `project/skills/` |
| `agents/` (`---` 없는 파일은 전체) | `project/agents/` |
| `rules/git-workflow.md` | `project/rules/git-workflow.md` |
| `rules/issue-key.md` | `project/rules/issue-key.md` |
| `hooks/pre-tool-guard.sh` | `project/hooks/pre-tool-guard.sh` |
| `settings.json` | `project/settings.json` |
| `settings.local.json` | `project/settings.local.json` |
| `README.md` | `project/README.md` |

### 부분 동기화 (Partial Sync)

`---` 구분선 기준으로 **공통 부분만** 동기화한다. 구분선 이후의 프로젝트 고유 데이터는 제외한다:

| 대상 | 동기화 범위 | 제외 영역 |
|------|-----------|----------|
| `rules/behavior.md` | `---` 구분선 위쪽만 (규칙 텍스트) | 프로젝트 고유 컨벤션, 규칙 효과 이력, 신뢰 패턴 |
| `skills/**/*.md` (`---` 포함 시) | `---` 구분선 위쪽만 (범용 규칙) | 프로젝트 고유 컨벤션 (배치 레이어, 테스트 프레임워크 등) |
| `agents/*.md` (`---` 포함 시) | `---` 구분선 위쪽만 (공통 페르소나) | 프로젝트 고유 체크 항목 |

### 동기화 제외 (Never Sync)

프로젝트 종속 파일은 동기화하지 않는다:

- `rules/architecture.md` — 프로젝트별 아키텍처
- `config.json` — 프로젝트별 빌드 설정
- `handoff.md` — 세션 상태

## 실행 로직

### 1단계: 준비

1. `.claude/config.json`에서 `configRepo` 경로를 읽는다
2. 경로가 없으면 사용자에게 물어본다
3. `~`를 `$HOME`으로 확장한다
4. config repo 디렉토리 존재 여부를 확인한다

### 2단계: 변경 감지

각 동기화 대상에 대해:

1. **Full Sync 대상**: 현재 프로젝트 파일과 config repo 파일을 `diff`로 비교
2. **Partial Sync 대상 (`---` 포함 파일)**:
   - 대상: `rules/behavior.md`, `skills/**/*.md` (`---` 포함 시), `agents/*.md` (`---` 포함 시)
   - 각 파일에서 `---` 구분선 위쪽만 추출
   - config repo 대응 파일의 `---` 위쪽과 비교

변경이 없으면 "변경사항이 없습니다." 출력 후 종료.

### 3단계: 변경 목록 보고

변경된 파일 목록을 보여준다:

```markdown
## 변경 감지

### Full Sync
- [변경됨] skills/tdd/phases/phase-plan.md
- [변경됨] rules/git-workflow.md

### Partial Sync
- [변경됨] rules/behavior.md (규칙 텍스트만, 효과 이력/신뢰 패턴 제외)

### 제외됨
- rules/architecture.md (프로젝트 종속)
- config.json (프로젝트 종속)
```

각 변경 파일의 diff를 간략히 보여준다 (추가/삭제 라인 수 + 핵심 변경 요약).

### 4단계: 사용자 확인

"config repo에 반영할까요?" 확인을 받는다.

### 5단계: 반영

1. **Full Sync**: 파일을 config repo의 `project/` 디렉토리에 복사
2. **Partial Sync (`---` 포함 파일)**:
   - 각 대상 파일에서 `---` 위쪽 내용을 추출
   - config repo 대응 파일의 `---` 위쪽을 교체하고, 아래쪽(프로젝트 고유 영역)은 유지
3. 동기화 대상으로 계산된 파일만 `git add -- <동기화_대상_파일들>`로 스테이징하고 `git status`로 확인
4. 커밋 메시지를 자동 생성 (변경 파일 목록 기반)
5. 사용자 확인 후 커밋

### 6단계: 결과 보고

```markdown
## 반영 완료

- 반영: N개 파일
- 커밋: [커밋 해시] [메시지]
- push는 수동으로 실행하세요: cd [config repo] && git push
```

## behavior.md Partial Sync 상세

config repo의 behavior.md는 아래 구조를 유지한다:

```markdown
# 코딩 행동 원칙
## MUST — ...
(규칙 텍스트)
## SHOULD — ...
(규칙 텍스트)
---
## 규칙 효과 이력
| 규칙 | 위반 이력 | 방지 효과 |
|------|----------|----------|
(빈 행 또는 템플릿)

## 신뢰 패턴
| 패턴 | 근거 | 축소 가능 검증 |
|------|------|--------------|
(빈 행 또는 템플릿)
```

즉, 규칙 텍스트는 프로젝트에서 가져오고, 테이블 구조(양식)만 유지하되 데이터는 비워둔다.

## 핵심 규칙

- 프로젝트 고유 데이터를 config repo에 절대 반영하지 않는다
- 반영 전 반드시 사용자 확인
- push는 자동으로 하지 않는다
