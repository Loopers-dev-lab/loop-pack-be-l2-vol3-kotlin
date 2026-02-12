# CLAUDE.md

이 파일은 Claude Code가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 언어 규칙

- 응답, 코드 주석, 커밋 메시지, 문서화: 한국어
- 변수명/함수명/클래스명: 영어

## Commands

```bash
./gradlew build                        # 전체 빌드
./gradlew :apps:commerce-api:build     # 특정 모듈 빌드
./gradlew :apps:commerce-api:bootRun   # 애플리케이션 실행
./gradlew test                         # 전체 테스트
./gradlew ktlintCheck                  # 린트 체크
./gradlew ktlintFormat                 # 린트 자동 수정
./gradlew ktlintCheck test             # 커밋 전 최종 검증
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

### 레이어드 아키텍처 (apps 내부)

```
interfaces/api/    → Controller, ApiSpec(OpenAPI 인터페이스), Dto
application/       → Facade(오케스트레이션), Info(레이어 간 데이터 전달)
domain/            → Entity, Service(@Component), Repository(인터페이스), Value Object
infrastructure/    → RepositoryImpl(구현체), JpaRepository
support/error/     → CoreException, ErrorType
```

요청 흐름: `Controller → Facade(Service) → Repository(interface) → RepositoryImpl → JpaRepository`

### 핵심 패턴

**Entity**: `BaseEntity` 상속 (id, createdAt, updatedAt, deletedAt 자동관리, soft delete 지원). 프로퍼티는 `protected set`. 도메인 검증은
`init` 블록에서 `CoreException` throw.

**Value Object**: 생성 시점에 자가 검증. `init` 블록에서 규칙 위반 시 `CoreException` throw. Entity 필드는 String으로 유지하되 생성/변경 시 VO로 검증.

**에러 처리**: `CoreException(errorType: ErrorType, customMessage: String?)`. ErrorType enum: `INTERNAL_ERROR(500)`,
`BAD_REQUEST(400)`, `NOT_FOUND(404)`, `CONFLICT(409)`, `UNAUTHORIZED(401)`.

**API 응답**: 모든 응답은 `ApiResponse<T>` 래퍼 사용. `ApiResponse.success(data)` / `ApiResponse.fail(errorCode, message)`.
`ApiControllerAdvice`에서 전역 예외 처리.

**Repository**: 도메인 레이어에 인터페이스 정의 → infrastructure에서 구현. JPA Repository는 `JpaRepository<Entity, Long>` 상속.

**DTO 변환**: Info/Dto에 `companion object { fun from(...) }` 팩토리 메서드 사용.

## 테스트 패턴

- `@Nested` + `@DisplayName`(한국어) BDD 스타일, **3A 원칙** (Arrange → Act → Assert)
- **통합 테스트**: `@SpringBootTest`, `@AfterEach`에서 `databaseCleanUp.truncateAllTables()`
- **E2E 테스트**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
- MySQL/Redis는 TestContainers 자동 구동 (프로파일: `test`), 타임존: `Asia/Seoul`
- 상세 테스트 작성 절차는 `/red`, `/e2e` 스킬 참고

## 개발 방법론: TDD (Kent Beck) + Tidy First

### 증강 코딩 원칙

- **대원칙**: 방향성 및 주요 의사 결정은 개발자에게 제안만 하며, 최종 승인된 사항을 기반으로 작업 수행
- **임의 작업 금지**: 반복적 동작, 요청하지 않은 기능 구현, 테스트 삭제를 임의로 진행하지 않는다
- **설계 주도권**: AI는 임의판단하지 않고 방향성을 제안할 수 있으나, 개발자 승인 후 수행

### TDD 사이클: Red → Green → Refactor

- **Red** → **Green** → **Refactor** 순서를 반드시 따른다
- 구조적 변경과 행위적 변경을 절대 같은 커밋에 섞지 않는다 (Tidy First)
- 둘 다 필요하면 구조적 변경을 먼저 수행한다
- 각 단계의 상세 절차는 `/red`, `/green`, `/refactor` 스킬 참고

### 코드 품질 기준

- 중복을 철저히 제거한다
- 이름과 구조로 의도를 명확히 표현한다
- 의존성을 명시적으로 드러낸다
- 메서드는 작게, 단일 책임으로 유지한다
- 가능한 가장 단순한 해결책을 사용한다

## 작업 환경

- **OS**: Windows (절대 Linux/Unix 전용 명령어 사용 금지)
- `chmod`, `ln -s`, `grep`(Bash), `sed`, `awk`, `cat`, `head`, `tail` 등 Unix 명령어 사용하지 말 것
- Windows 호환 명령어 또는 Claude Code 전용 도구(Read, Edit, Write, Grep, Glob 등)를 사용할 것

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
- 커밋 상세 절차는 `/commit` 스킬 참고
- PR 제목: `[N주차] 제출 내용`, 리뷰 포인트 필수 작성
