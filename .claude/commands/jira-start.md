# JIRA 티켓 작업 시작

JIRA 티켓 작업을 시작합니다. 새로운 브랜치를 생성하고 전환합니다.

## 작업 순서

1. 사용자로부터 JIRA 티켓 번호를 받습니다 (command args로 전달됨)

2. JIRA API를 통해 티켓 정보를 조회하고 요약(summary)을 가져옵니다:
   - `mcp__atlassian__getJiraIssue` 도구 사용
   - 티켓 요약을 기반으로 간단한 영어 kebab-case 설명 생성

3. main 브랜치에서 최신 코드를 가져옵니다:
   ```bash
   git checkout volume-n
   git pull origin volume-n
   ```
   volume-n 에서 n은 브랜치에 있는 volume-n 중 가장 큰 수

4. 새 브랜치를 생성하고 전환합니다 (티켓 유형은 항상 feat):
   ```bash
   git checkout -b feat/[티켓번호]-[AI가-생성한-설명]
   ```
   예: `git checkout -b feat/SCRUM-40-implement-login-api`

5. 브랜치 생성 완료를 확인하고 사용자에게 알립니다:
   ```bash
   git branch --show-current
   ```

6. 사용자에게 작업을 시작할 수 있다고 안내합니다.

## 예시

사용자 실행:
```
/jira-start SCRUM-40
```

JIRA 티켓 조회 결과:
- 티켓 번호: SCRUM-40
- 요약: "사용자 로그인 API 구현"

AI가 생성한 브랜치명:
```bash
git checkout volume-n
git pull origin volume-n
git checkout -b feat/SCRUM-40-implement-user-login-api
```
