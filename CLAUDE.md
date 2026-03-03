# CLAUDE.md

이 파일은 Claude Code가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 언어 규칙

- 응답, 코드 주석, 커밋 메시지, 문서화: 한국어
- 변수명/함수명/클래스명: 영어

## 상호작용 규칙

- **질문 vs 실행 구분**: 개념/설계/아키텍처 질문에는 코드 변경 없이 답변만 한다. 코드를 grep하거나 수정하지 않는다. 코드 수정은 "~해줘", "~수정해줘", "~구현해줘" 등 명시적 실행 요청이 있을 때만 수행한다
- **수정 의도 확인**: 코드 수정/삭제 전 의도를 확인한다. 특히 어노테이션/검증 관련 변경은 ADD/REMOVE/MOVE 중 무엇인지, 어떤 파일(Controller? ApiSpec? Entity?)에 대한 것인지 반드시 확인 후 착수한다
- **문서화된 규칙 준수**: CLAUDE.md에 문서화된 규칙은 non-negotiable이다. 'out of scope'로 무시하거나 건너뛰지 않는다. 규칙이 불합리하다고 판단되면 무시하지 말고 개발자에게 이의를 제기한다
- **완료 전 검증 필수**: 작업 완료를 선언하기 전에 반드시 `./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test`를 실행하여 검증한다. 검증 없이 "완료했습니다"라고 말하지 않는다
- **환각 금지**: 존재하지 않는 API, 패키지, 파일 경로, 설정 옵션을 지어내지 않는다. 확실하지 않으면 먼저 확인한다
- **피드백 반영**: 개발자가 실수를 지적하면 `MEMORY.md`에 교훈을 기록한다. 2회 이상 반복되면 `.claude/rules/corrections.md`로 승격하고 개발자에게 알린다

## Commands

```bash
./gradlew build                        # 전체 빌드
./gradlew :apps:commerce-api:build     # 특정 모듈 빌드
./gradlew :apps:commerce-api:bootRun   # 애플리케이션 실행
./gradlew test                         # 전체 테스트
./gradlew ktlintCheck                  # 린트 체크
./gradlew ktlintFormat                 # 린트 자동 수정
./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test  # 커밋 전 최종 검증 (kapt 충돌 방지를 위해 분리 실행)
```

단일 테스트 실행:

```bash
./gradlew :apps:commerce-api:test --tests "패키지.클래스명"
./gradlew :apps:commerce-api:test --tests "패키지.클래스명.메서드명"
```

## 아키텍처

Kotlin + Spring Boot 3.4.4 + JDK 21 멀티모듈 프로젝트.

### 모듈 구조

- **apps/**: 실행 가능한 Spring Boot 애플리케이션 (commerce-api, commerce-batch, commerce-streamer)
- **modules/**: 인프라 설정 모듈 (jpa, redis, kafka) — `testFixtures` 제공
- **supports/**: 부가 기능 모듈 (jackson, logging, monitoring)

### 레이어별 가이드

작업 대상 레이어의 CLAUDE.md를 **반드시** 먼저 읽는다:

- `apps/commerce-api/CLAUDE.md` — 레이어 의존방향, 요청 흐름, 에러/응답 패턴
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/CLAUDE.md` — Domain Model, VO, Command, Repository 인터페이스, Domain Service
- `apps/commerce-api/src/main/kotlin/com/loopers/application/CLAUDE.md` — Facade, @Transactional, DTO 변환
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/CLAUDE.md` — JPA Entity, 매핑, Repository 구현
- `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/CLAUDE.md` — Controller, ApiSpec, Dto, 인증

## 기술 주의사항 / 테스트 패턴

→ `.claude/rules/kotlin-spring-jpa.md` (kapt 충돌, allOpen, JPQL 금지 등)
→ `.claude/rules/test-patterns.md` (3A 원칙, Fake Repository, TestContainers 등)

## 개발 방법론: TDD (Kent Beck) + Tidy First

### 증강 코딩 원칙

- **대원칙**: 방향성 및 주요 의사 결정은 개발자에게 제안만 하며, 최종 승인된 사항을 기반으로 작업 수행
- **임의 작업 금지**: 반복적 동작, 요청하지 않은 기능 구현, 테스트 삭제를 임의로 진행하지 않는다
- **금지 행동 엄수**: 사용자가 "하지 마라"고 한 행동은 어떤 형태로든 시도하지 않는다 (예: "테스트 돌리지 마" → 테스트 실행 금지)
- **git commit/push 제한**: 명시적으로 커밋/푸시를 요청할 때만 실행한다. 커밋 메시지만 요청하면 메시지 텍스트만 제공한다
- **에이전트 최소 위임**: 파일 읽기만 필요하면 Read 도구를 직접 사용한다. explore 에이전트는 탐색 범위가 불명확할 때만 사용. 서브에이전트 프롬프트에 코드 전문을 복사하지 않고 파일 경로만 전달한다
- **기존 분석 활용**: 이전 분석 결과(Gemini, Codex 등)를 참조하라고 하면 해당 출력을 직접 사용한다. 독자적으로 재분석하지 않는다
- **설계 주도권**: AI는 임의판단하지 않고 방향성을 제안할 수 있으나, 개발자 승인 후 수행
- **가정 명시**: 불확실한 부분은 가정을 명시적으로 나열하고 질문한다. 조용히 하나를 선택하지 않는다
- **모호함 표면화**: 해석이 여러 개 가능하면 선택지와 영향도를 함께 제시한다. 혼란스러우면 멈추고 무엇이 혼란스러운지 명명한다
- **목표 중심 실행**: 작업을 검증 가능한 목표로 변환한다. "버그 수정" → "재현 테스트 작성 후 통과시켜라", "리팩토링" → "리팩토링 전후로 테스트가 통과하는지 확인하라". 강한 성공 기준이 있으면 자율적으로 반복하고, 약한 기준이면 확인을 구한다

### 수술적 변경 원칙

요청받은 것만 변경한다. 변경된 모든 줄은 사용자의 요청으로 추적 가능해야 한다.

- 인접한 코드, 주석, 포맷을 "개선"하지 않는다
- 깨지지 않은 것을 리팩토링하지 않는다
- 본인이 다르게 했을지라도 기존 스타일을 따른다
- 관련 없는 dead code를 발견하면 언급만 하고 삭제하지 않는다
- 내 변경으로 인해 사용되지 않게 된 import/변수/함수만 정리한다

### TDD 사이클: Red → Green → Refactor

- **Red** → **Green** → **Refactor** 순서를 반드시 따른다
- 구조적 변경과 행위적 변경을 절대 같은 커밋에 섞지 않는다 (Tidy First)
- 둘 다 필요하면 구조적 변경을 먼저 수행한다
- 각 단계의 상세 절차는 `/red`, `/green`, `/refactor` 스킬 참고
- **plan 작성 시에도 TDD 형식을 따른다**: 각 구현 항목을 `[RED] 테스트 → [GREEN] 구현` 쌍으로 구성한다. Fake Repository 생성 항목도 포함한다

### 코드 품질 기준

- 중복을 철저히 제거한다
- 이름과 구조로 의도를 명확히 표현한다
- 의존성을 명시적으로 드러낸다
- 메서드는 작게, 단일 책임으로 유지한다
- 가능한 가장 단순한 해결책을 사용한다

### 실패 대응

- 에러 발생 시 증상만 고치지 말고 **근본 원인**을 분석한다
- 같은 명령을 재시도하기 전에 왜 실패했는지 먼저 이해한다
- 방향이 틀렸으면 고치려 하지 말고 과감히 버리고 다시 작성한다. 매몰 비용에 집착하지 않는다

### 병렬 작업 워크플로우

→ `.claude/rules/parallel-workflow.md` (서브에이전트 위임 원칙, Self-Validation, 보고 포맷)

## 작업 환경

- **OS**: Windows 또는 WSL (Ubuntu)
- Windows 환경에서는 Linux/Unix 전용 명령어(`chmod`, `ln -s` 등) 사용 금지
- WSL 환경에서는 Unix 명령어 사용 가능하나, 파일 조작은 Claude Code 전용 도구(Read, Edit, Write, Grep, Glob 등)를 우선 사용할 것

## 주의사항

→ `.claude/rules/code-guidelines.md` (Never Do, Recommendation, Priority)

## 브랜치 및 PR 규칙

- 브랜치: `main`에서 분기 (예: `feature/round2-design`)
- 커밋 접두사: `feat:` | `refactor:` | `fix:` | `test:` | `docs:` | `chore:`
- 커밋 상세 절차는 `/commit` 스킬 참고
- 멀티 커밋 계획 파일이 있으면 자체 분석 루프 없이 해당 계획을 바로 실행한다 (`/commit-plan` 스킬 참고)
- 로컬 전용 파일(review-plan.md, 개인 메모, .omc/ 하위 파일 등)은 staging 전 개발자에게 확인한다
- PR 제목: `[N주차] 제출 내용`, 리뷰 포인트 필수 작성

## 세션 관리

- 장시간 세션보다 짧고 집중된 세션을 지향한다
- 세션 종료 전 `/handoff` 스킬로 핸드오프 노트를 남길 수 있다
- 다음 세션 시작 시 `.claude/handoff.md`가 있으면 읽고 이어서 작업한다

## 애그리거트 캡슐화 원칙

- 애그리거트 루트가 아닌 객체(Entity)의 상태 변경 메서드는 `@AggregateRootOnly`를 부착하여 외부 노출을 차단한다.
- 루트 객체에서 해당 메서드를 호출할 때는 `@OptIn(AggregateRootOnly::class)`를 사용한다.
- UseCase나 외부 Service에서 Opt-In을 통해 경고를 무시하고 자식 객체를 직접 조작하는 것을 엄격히 금지한다.
