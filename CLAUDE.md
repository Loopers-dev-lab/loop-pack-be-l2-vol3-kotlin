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

## 기술 주의사항

- **allOpen 플러그인**: `plugin.spring`은 @Component/@Service 등을 open하지만, `plugin.jpa`는 no-arg 생성자만 생성하고 allOpen은 아님. **@Entity 클래스는 final**이다. 주장 전 반드시 allOpen 설정(build.gradle.kts)과 디컴파일 결과를 확인한다
- **kapt + ktlint 태스크 충돌**: `./gradlew ktlintCheck test`를 한 번에 실행하면 실패한다. 반드시 분리 실행 (Commands 섹션 참고)
- **JPQL/NativeQuery 금지**: @Query 어노테이션 사용 금지. QueryDSL 또는 Spring Data JPA 메서드명 쿼리 사용
- **fetch join + paging 호환 불가**: N+1 해결 시 fetch join과 paging 동시 사용 금지. @BatchSize, @EntityGraph, 별도 쿼리 분리 대안 사용
- **@Transactional 전파**: readOnly 속성의 전파 규칙, REQUIRES_NEW의 동작 방식을 정확히 이해하고 적용

## 테스트 패턴

→ `apps/commerce-api/src/test/CLAUDE.md` (3A 원칙, Fake Repository, TestContainers 등)

## 개발 방법론: TDD (Kent Beck) + Tidy First

### 증강 코딩 원칙

- **대원칙**: 방향성 및 주요 의사 결정은 개발자에게 제안만 하며, 최종 승인된 사항을 기반으로 작업 수행
- **임의 작업 금지**: 반복적 동작, 요청하지 않은 기능 구현, 테스트 삭제를 임의로 진행하지 않는다
- **금지 행동 엄수**: 사용자가 "하지 마라"고 한 행동은 어떤 형태로든 시도하지 않는다 (예: "테스트 돌리지 마" → 테스트 실행 금지)
- **git commit/push 제한**: 명시적으로 커밋/푸시를 요청할 때만 실행한다. 커밋 메시지만 요청하면 메시지 텍스트만 제공한다
- **에이전트 최소 위임**: 파일 읽기만 필요하면 Read 도구를 직접 사용한다. explore 에이전트는 탐색 범위가 불명확할 때만 사용. 서브에이전트 프롬프트에 코드 전문을 복사하지 않고 파일 경로만 전달한다
- **기존 분석 활용**: 이전 분석 결과(Gemini, Codex 등)를 참조하라고 하면 해당 출력을 직접 사용한다. 독자적으로 재분석하지 않는다
- **설계 주도권**: AI는 임의판단하지 않고 방향성을 제안할 수 있으나, 개발자 승인 후 수행

> 코딩 행동 원칙(외과적 변경, 단순함 우선, 목표 중심 실행, 점진적 실행, 실패 대응 등)은 `.claude/rules/behavior.md` 참조

### TDD 사이클: Red → Green → Refactor

- **Red** → **Green** → **Refactor** 순서를 반드시 따른다
- 구조적 변경과 행위적 변경을 절대 같은 커밋에 섞지 않는다 (Tidy First)
- 둘 다 필요하면 구조적 변경을 먼저 수행한다
- 각 단계의 상세 절차는 `/red`, `/green`, `/refactor` 스킬 참고
- **plan 작성 시에도 TDD 형식을 따른다**: 각 구현 항목을 `[RED] 테스트 → [GREEN] 구현` 쌍으로 구성한다. Fake Repository 생성 항목도 포함한다

### 병렬 작업 워크플로우

생산성 = 속도 × 인지 안정성 × 병렬 처리량. 인지 부하를 통제한 상태에서 유지 가능한 처리량을 높인다.

**Phase 1 — 수렴 (Converge)**: 요구사항 정제 → 설계 확정. 이 구간에서 Q&A를 집중하여 모호함을 전부 해소한다.
**Phase 2 — 발산 (Diverge)**: plan.md 기반 위임 + 체크포인트 검수. 에이전트는 자가 검증(lint+test) 후 보고한다.

#### 서브에이전트 위임 원칙

1. **메인 컨텍스트 절약**: 코드 탐색/구현/검증은 서브에이전트에 위임하고, 메인은 오케스트레이션과 개발자 대화에 집중한다
2. **위임 단위는 작게**: 에이전트 1개당 변경 파일 5개 이하, 항목 5개 이하. 대형 작업은 레이어별/도메인별로 분할하여 여러 sonnet에 위임한다
3. **대형 작업은 plan 먼저**: 5개 이상 파일을 변경하는 작업은 먼저 탐색 에이전트로 영향 범위를 파악하고, 파일 겹침 없는 서브태스크로 분할한 후 병렬 위임한다
4. **서브에이전트 보고를 맹신하지 않는다**: CP 완료 보고 후 메인에서 Grep/Read로 핵심 변경이 실제 반영되었는지 확인한다. 확인 전까지 서브에이전트를 종료하지 않는다. 미반영 시 같은 에이전트에 재반영을 지시한다(resume). 전체 테스트는 최종 완료 전 1회만 실행한다.
5. **실패 시 원인을 좁혀서 위임**: 실패 로그를 메인에서 먼저 확인하고, 원인을 특정한 후 수정을 위임한다. 막연히 "10개 실패 고쳐줘"로 던지지 않는다
6. **모델 선택**: sonnet이 기본. opus는 복잡한 분석/설계 판단이 필요할 때만 사용한다. 대량 파일 변경은 opus 1개보다 sonnet 여러 개가 낫다
7. **탐색은 1회만**: 동일한 파일을 여러 탐색 에이전트가 중복 읽지 않는다. 탐색 결과는 메인에서 공유하고 실행 에이전트에 필요한 부분만 전달한다
8. **탐색 범위 제한**: 열린 탐색(빈틈 분석, 전체 조사 등)은 현재 작업에 직결되는 범위로 한정한다. "모든 가능한 개선점"을 찾지 않는다
9. **컨텍스트 파일 전달**: 서브에이전트 위임 시 작업 대상 레이어의 CLAUDE.md 경로와 `.claude/rules/` 파일 경로를 프롬프트에 명시하여 읽도록 지시한다

#### 서브에이전트 결과 검증 원칙

서브에이전트의 "완료" 보고를 맹신하지 않는다. **오케스트레이터가 직접 파일 변경을 검증**한다.

1. 서브에이전트 완료 후, Grep/Read로 핵심 변경이 실제 파일에 반영되었는지 확인한다
2. 확인 전까지 서브에이전트를 종료하지 않는다. 미반영 시 같은 에이전트에 재반영을 지시한다(resume)
3. 서브에이전트 프롬프트에도 "Edit 후 반드시 Read로 재확인하고, 변경이 없으면 재시도" 지시를 기본 포함한다

#### Self-Validation 원칙

보고 전에 할 수 있는 검증은 전부 수행한다.

1. `ktlintFormat` → `ktlintCheck` (포맷 + 린트)
2. `./gradlew test` (전체 테스트)
3. 통과 상태에서만 보고. 실패 시 자가 수정 시도.

#### 보고 포맷

모든 작업 보고에 아래 구조를 따른다.

- **Change**: 무엇을 변경했는지 (3줄 요약)
- **Validation**: 어떤 검증을 통과했는지
- **Risk/Ambiguity**: 개발자가 판단해야 할 모호한 부분. 임의 결정한 네이밍, 가정한 비즈니스 로직 등을 반드시 명시. 없으면 "Perfectly aligned with spec".

## 작업 환경

- **OS**: Windows 또는 WSL (Ubuntu)
- Windows 환경에서는 Linux/Unix 전용 명령어(`chmod`, `ln -s` 등) 사용 금지
- WSL 환경에서는 Unix 명령어 사용 가능하나, 파일 조작은 Claude Code 전용 도구(Read, Edit, Write, Grep, Glob 등)를 우선 사용할 것

## 주의사항

### Never Do
- 실제 동작하지 않는 코드, 불필요한 Mock 데이터를 이용한 구현 금지
- null-safety 하지 않게 코드 작성 금지
- println 코드 남기지 않는다

### Recommendation
- 실제 API를 호출해 확인하는 E2E 테스트 코드 작성
- 재사용 가능한 객체 설계
- 성능 최적화에 대한 대안 및 제안
- 개발 완료된 API는 `http/*.http` 파일에 분류하여 작성

### Priority
1. 실제 동작하는 해결책만 고려
2. null-safety, thread-safety 고려
3. 테스트 가능한 구조로 설계
4. 기존 코드 패턴 분석 후 일관성 유지

## 브랜치 및 PR 규칙

- 브랜치: `main`에서 분기 (예: `feature/round2-design`)
- 커밋 접두사: `feat:` | `refactor:` | `fix:` | `test:` | `docs:` | `chore:`
- 커밋 상세 절차는 `/ship` 스킬 참고
- 멀티 커밋이 필요하면 `/ship`에서 구조적/행위적 변경을 자동 분리하여 커밋한다
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
