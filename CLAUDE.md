# CLAUDE.md

이 파일은 Claude Code가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 언어 규칙

- 응답, 코드 주석, 커밋 메시지, 문서화: 한국어
- 변수명/함수명/클래스명: 영어

## Commands

### 빌드 및 실행

```bash
./gradlew build                        # 전체 빌드
./gradlew :apps:commerce-api:build     # 특정 모듈 빌드
./gradlew :apps:commerce-api:bootRun   # 애플리케이션 실행
```

### Red Phase — 테스트 작성 후 실패 확인

```bash
# 단일 테스트 클래스 실행
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.user.UserTest"

# 단일 테스트 메서드 실행
./gradlew :apps:commerce-api:test --tests "com.loopers.domain.user.UserTest.특정메서드명"
```

### Green Phase — 구현 후 전체 테스트 통과 확인

```bash
./gradlew test
```

### Refactor Phase — 리팩토링 후 린트 + 전체 테스트

```bash
./gradlew ktlintFormat   # 린트 자동 수정
./gradlew ktlintCheck    # 린트 체크
./gradlew test           # 전체 테스트 재확인
```

### 커밋 전 최종 검증

```bash
./gradlew ktlintCheck test   # 린트 + 테스트 한 번에 실행
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

요청 흐름: `Controller → Facade → Service → Repository(interface) → RepositoryImpl → JpaRepository`

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

- **단위 테스트**: `@Nested` + `@DisplayName`(한국어) 조합으로 BDD 스타일 구성
- **통합 테스트**: `@SpringBootTest`, `@AfterEach`에서 `databaseCleanUp.truncateAllTables()` 호출
- **E2E 테스트**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
- 테스트 시 MySQL/Redis는 TestContainers로 자동 구동 (프로파일: `test`)
- 테스트 타임존: `Asia/Seoul`
- 모든 테스트는 **3A 원칙**: Arrange(준비) → Act(실행) → Assert(검증)

## 개발 방법론: TDD (Kent Beck) + Tidy First

### 증강 코딩 원칙

- **대원칙**: 방향성 및 주요 의사 결정은 개발자에게 제안만 하며, 최종 승인된 사항을 기반으로 작업 수행
- **임의 작업 금지**: 반복적 동작, 요청하지 않은 기능 구현, 테스트 삭제를 임의로 진행하지 않는다
- **설계 주도권**: AI는 임의판단하지 않고 방향성을 제안할 수 있으나, 개발자 승인 후 수행

### TDD 사이클: Red → Green → Refactor

1. **Red**: 요구사항을 만족하는 실패 테스트를 먼저 작성한다 (한 번에 하나씩)
2. **Green**: 테스트를 통과시키기 위한 최소한의 코드를 구현한다. 오버엔지니어링 금지
3. **Refactor**: 테스트가 통과한 후에만 리팩토링한다
    - 불필요한 private 함수 지양, 객체지향적 코드 작성
    - unused import 제거, 성능 최적화
    - 모든 테스트 케이스가 통과해야 함

### 변경 유형 분리 (Tidy First)

- **구조적 변경**: 동작을 바꾸지 않는 코드 재배치 (이름 변경, 메서드 추출, 코드 이동)
- **행위적 변경**: 실제 기능 추가/수정
- 구조적 변경과 행위적 변경을 절대 같은 커밋에 섞지 않는다
- 둘 다 필요하면 구조적 변경을 먼저 수행한다

### 커밋 규칙

- 모든 테스트가 통과하고, 린터 경고가 없을 때만 커밋
- 하나의 논리적 작업 단위로 커밋
- 커밋 메시지에 구조적/행위적 변경 여부를 명시
- 크고 드문 커밋보다 작고 빈번한 커밋을 지향

### 코드 품질 기준

- 중복을 철저히 제거한다
- 이름과 구조로 의도를 명확히 표현한다
- 의존성을 명시적으로 드러낸다
- 메서드는 작게, 단일 책임으로 유지한다
- 가능한 가장 단순한 해결책을 사용한다

## 설계 방법론 (Week 2)

### 설계 원칙

- **요구사항을 그대로 믿지 않는다** — "무엇을 만들까?"가 아니라 "어떤 문제를 해결하는가?"로 재해석
- **애매한 부분을 숨기지 않는다** — 결정되지 않은 부분을 명시적으로 드러내고 질문한다
- **코드보다 의도, 책임, 경계를 우선한다** — 구현 전에 생각해야 할 것을 끌어낸다
- **다이어그램은 근거와 해석을 함께 제시한다** — 왜 이 다이어그램이 필요한지, 어떤 포인트를 봐야 하는지

### 유비쿼터스 언어

도메인 용어를 코드, 문서, API 명세에서 동일하게 사용한다. 새 용어 도입 전 반드시 정의와 맥락을 공유한다.

### 다이어그램 도구

Mermaid 문법으로 작성한다:

- **시퀀스 다이어그램**: 책임 분리, 호출 순서, 트랜잭션 경계 확인
- **클래스 다이어그램**: 도메인 책임, 의존 방향, 응집도 확인
- **ERD**: 영속성 구조, 관계의 주인, 정규화 여부

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

### 브랜치 생성

```bash
git checkout main && git pull origin main
git checkout -b feature/round2-design
```

### 커밋 메시지

| 접두사         | 용도              |
|-------------|-----------------|
| `feat:`     | 새 기능 추가         |
| `refactor:` | 동작 변경 없는 구조적 변경 |
| `fix:`      | 버그 수정           |
| `test:`     | 테스트 추가/수정       |
| `docs:`     | 문서 변경           |
| `chore:`    | 설정, 빌드 스크립트 변경  |

### PR 규칙

- PR 제목: `[2주차] 설계 문서 제출`
- 리뷰 포인트 필수 작성
- 불필요한 코드/디버깅 로그 제거
