---
name: organize-sessions
description:
  지정된 날짜의 Claude Code 세션들을 읽어서 프로젝트 루트에 Daily Note(마크다운)로 정리한다.
  인자 없으면 어제, YYYY-MM-DD/yesterday/today/last-week 지정 가능.
---

# Claude Code Session Organizer

지정된 날짜의 Claude Code 세션들을 읽어서 Daily Note로 정리한다.

## Arguments (사용자 입력)

- 인자 없음: **어제** 세션을 정리
- 날짜 지정 시:
  - `YYYY-MM-DD` 형식: 해당 날짜
  - `yesterday`: 어제
  - `today`: 오늘
  - `last-week`: 최근 7일 (날짜별로 각각 파일 생성)

사용자 입력: $ARGUMENTS

## 실행 절차

### 1. 날짜 결정

사용자 입력을 파싱하여 대상 날짜를 결정한다. 인자가 없으면 어제 날짜를 사용한다.
모든 시간은 **KST(UTC+9)** 기준이다.

### 2. 세션 데이터 수집

Claude Code는 두 곳에 세션 데이터를 저장한다:

**세션 인덱스 파일:**
```
~/.claude/sessions/{pid}.json
```
각 파일은 다음 구조:
```json
{
  "pid": 117512,
  "sessionId": "uuid-here",
  "cwd": "/home/user/study",
  "startedAt": 1773709575647
}
```
- `startedAt`은 Unix timestamp (밀리초) → KST로 변환하여 날짜 필터링에 사용

**세션 트랜스크립트 파일:**
```
~/.claude/projects/{프로젝트경로}/{sessionId}.jsonl
```
- 프로젝트 경로는 cwd의 `/`를 `-`로 치환한 형태 (예: `/home/user/study` → `-home-user-study`)
- 세션 인덱스의 `sessionId`와 트랜스크립트 파일명이 매칭됨

**수집 방법:**

1. `~/.claude/projects/` 하위의 `.jsonl` 파일들을 파일 수정 시간으로 필터링
2. `agent-*` 접두사 파일은 서브에이전트 세션이므로 제외
3. 필터링된 세션의 `.jsonl` 트랜스크립트를 분석 대상으로 선정
4. 프로젝트 경로에서 프로젝트명을 추출한다

### 3. 세션 내용 분석

Claude Code 트랜스크립트는 **JSONL 형식**이다. 한 줄에 하나의 JSON 객체:

```json
{"type":"user","message":{"role":"user","content":"사용자 메시지"},"timestamp":"2026-03-17T02:24:32.244Z","sessionId":"uuid","cwd":"/path"}
{"type":"assistant","message":{"role":"assistant","content":[{"type":"text","text":"응답"}]},"timestamp":"..."}
```

주요 필드:
- `type`: `"user"` | `"assistant"` | `"file-history-snapshot"` 등
- `message.content`: 문자열 또는 배열. 배열인 경우 `type: "text"`인 항목의 `text` 필드 추출
- `timestamp`: ISO 8601 형식 (UTC) → KST 변환 필요
- `message.model`: assistant 메시지에 사용된 모델명 (예: `"claude-opus-4-6"`)

**분석 방법:**

파일이 클 수 있으므로 효율적으로 읽는다:
- `type: "user"`인 줄만 필터링
- `<system-reminder>`, `<local-command`, `<command-name>`, `<task-notification>` 등 시스템 메시지 제외
- 실제 사용자 입력 메시지만 추출하여 세션 흐름 파악
- 첫 번째 assistant 메시지에서 모델명 추출
- 전체 사용자 메시지 흐름을 3-5줄로 요약

### 4. Daily Note 작성

아래 포맷으로 마크다운 노트를 생성한다:

```markdown
---
date: {{YYYY-MM-DD}}
tags: [claude-sessions, daily-note]
---

# Claude Code Sessions - {{YYYY-MM-DD}}

## Sessions

### {{세션 요약 제목}}
- **시간**: {{HH:MM}}~{{HH:MM}} (KST)
- **프로젝트**: {{프로젝트명}}
- **모델**: {{사용된 모델명}}
- **주제**: {{첫 번째 user 메시지 기반 한줄 요약}}
- **주요 내용**:
  - {{핵심 포인트 1}}
  - {{핵심 포인트 2}}
  - {{핵심 포인트 3}}

---

(세션이 여러 개면 반복)

## Today's Takeaways
- {{전체 세션에서 배운 점이나 주요 성과 1-3줄 요약}}
```

### 5. 파일 저장

저장 경로: 프로젝트 루트
```
./sessions/{{YYYY-MM-DD}}.md
```

- `sessions/` 폴더가 없으면 생성한다.
- 이미 해당 날짜 파일이 있으면 기존 내용 뒤에 `---` 구분선과 함께 추가한다.

### 6. 완료 보고

정리한 세션 수, 대상 프로젝트들, 저장 경로를 보고하고, 해당 날짜에 세션이 없었으면 그 사실을 알린다.

## 참고

- 모든 시간은 KST(UTC+9) 기준으로 표시
- 한국어로 작성
- `agent-*` 파일은 서브에이전트 세션이므로 메인 세션만 정리 대상
- 트랜스크립트가 큰 경우 grep/head/tail로 효율적으로 읽는다
