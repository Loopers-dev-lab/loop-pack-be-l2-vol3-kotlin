---
name: daily-report
description:
  Claude Code 세션 기록(대화/의사결정) + Git 로그(코드 변경)를 결합하여
  Gemini OAuth로 데일리 회고를 생성한다. 인자 없으면 어제, YYYY-MM-DD/yesterday/today 지정 가능.
---

# Daily Report Generator

세션 트랜스크립트와 Git 로그를 결합하여 Gemini 기반 데일리 회고를 생성한다.

## 사용법

```
/daily-report [날짜]
```

- 인자 없음: 어제
- `yesterday`: 어제
- `today`: 오늘
- `YYYY-MM-DD`: 특정 날짜

## 실행

아래 명령을 실행한다:

```bash
python3 scripts/daily-report.py $ARGUMENTS
```

## 전제 조건

- Gemini CLI 설치 및 OAuth 로그인 완료 (`~/.gemini/oauth_creds.json` 존재)
- npm 전역 설치 (`npm root -g`로 Gemini CLI 경로 탐색)

## 출력

- Obsidian Vault `Daily Notes/YYYY-MM-DD.md`에 저장 (기본 경로: `/mnt/c/Users/hello/Documents/Obsidian Vault/Daily Notes/`)
- frontmatter: `date`, `tags: [daily-report, claude-sessions]`
