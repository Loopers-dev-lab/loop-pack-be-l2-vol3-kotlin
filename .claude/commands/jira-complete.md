# JIRA 티켓 완료

JIRA 티켓을 완료 처리합니다.

## 작업 순서

1. 현재 브랜치명에서 JIRA 티켓 번호를 추출한다 (예: `feat/SCRUM-10-...` → `SCRUM-10`)

2. JIRA 티켓 요구사항 충족 여부를 검증한다:
   - `mcp__atlassian__getJiraIssue`로 티켓의 description(요구사항)을 조회한다
   - 요구사항의 각 항목을 현재 구현 코드 및 테스트와 대조한다
   - 충족/미충족 항목을 체크리스트로 정리한다
   - **미충족 항목이 있으면 완료 처리를 중단하고, 누락된 요구사항 목록을 개발자에게 보고한다**
   - 모든 항목이 충족되면 다음 단계로 진행한다

3. 테스트 결과를 JIRA 티켓 코멘트에 추가:
   - 테스트 결과 요약 작성
   - JIRA API를 통해 코멘트 추가:
     ```
     mcp__atlassian__addCommentToJiraIssue
     - cloudId: (Atlassian Cloud ID)
     - issueIdOrKey: (티켓 번호)
     - commentBody: (테스트 결과 마크다운)
     ```

4. JIRA 티켓 완료 처리:
   - 사용 가능한 전환(transition) 조회:
     ```
     mcp__atlassian__getTransitionsForJiraIssue
     ```
   - "완료" 상태로 전환:
     ```
     mcp__atlassian__transitionJiraIssue
     - transition: {"id": "완료 전환 ID"}
     ```

5. 작업 완료 요약:
   ```
   ✅ JIRA 티켓에 테스트 결과 코멘트가 추가되었습니다.
   ✅ JIRA 티켓이 "완료" 상태로 변경되었습니다.

   새로운 작업을 시작하려면 /jira-start 명령어를 사용하세요.
   ```
