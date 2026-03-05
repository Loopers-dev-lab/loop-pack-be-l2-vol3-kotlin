# Commerce API — Module-Specific Rules

> 이 파일은 `apps/commerce-api` 작업 시에만 로딩된다. 프로젝트 공통 규칙은 루트 `AGENTS.md` 참조.

## Package Structure

```text
com.loopers.
├── application/<domain>/           # UseCase, Result DTO, Command
├── domain/<domain>/                # Domain Model, Repository interface, Port interface
├── infrastructure/<domain>/        # Entity, RepositoryImpl, JpaRepository
├── interfaces/
│   └── api/                        # ApiResponse, ApiControllerAdvice
│       └── <domain>/               # Controller, ApiSpec, Request, Response
└── support/                        # Cross-cutting (CoreException, ErrorType, Config)
```

### Package Naming Convention

```text
com.loopers.<layer>.<domain>
```

Example: `com.loopers.domain.user`, `com.loopers.infrastructure.user`

## Layer Dependencies

- `interfaces` depends on `application`
- `application` depends on `domain`
- `infrastructure` depends on `domain`
- `domain` has **no outward dependencies** (defines interfaces only)

## API-Specific Patterns

- **Controller 변환 체인**: `.let { Response.from(it) }.let { ApiResponse.success(it) }`
- **ApiResponse 성공**: `{ "meta": { "result": "SUCCESS", "errorCode": null, "message": null, "errors": null }, "data": {...} }`
- **ApiResponse 실패 (validation)**: `{ "meta": { "result": "FAIL", "errorCode": "BAD_REQUEST", "message": "입력값이 올바르지 않습니다.", "errors": { "fieldName": ["에러 메시지"] } }, "data": null }`
- **ApiControllerAdvice**: Global exception handling via `CoreException` + `ErrorType`

## Naming Rules (API-Specific)

- **Controller**: `{Domain}V{Version}Controller` implements `{Domain}V{Version}ApiSpec` (예: `UserV1Controller`)
- **ApiSpec**: `{Domain}V{Version}ApiSpec` — Swagger 어노테이션 분리용 인터페이스
- **Request**: `{Domain}V{Version}Request` container + inner class (예: `UserAuthV1Request.SignUp`)
- **Response**: `{Domain}V{Version}Response` container + inner class (예: `UserAuthV1Response.SignUp`)
- **Command**: `{Domain}Command` container + inner class (예: `UserAuthCommand.SignUp`)
- **버전 관리**: 클래스명에 V{Version} 포함, 패키지 경로에는 버전 없음

## Build & Test (Module-Scoped)

- 빌드: `./gradlew clean :apps:commerce-api:build`
- 테스트: `./gradlew :apps:commerce-api:test`
- 린트: `./gradlew :apps:commerce-api:ktlintCheck`
- 포맷: `./gradlew :apps:commerce-api:ktlintFormat`

## 패턴 레퍼런스

새로운 도메인 구현 시 아래 파일들의 패턴을 따른다 (경로는 `apps/commerce-api` 기준):

| 패턴                   | 참조 파일                                                                                                  |
|----------------------|--------------------------------------------------------------------------------------------------------|
| Controller + ApiSpec | `src/main/kotlin/com/loopers/interfaces/api/user/auth/UserAuthV1Controller.kt`, `UserAuthV1ApiSpec.kt` |
| Request              | `src/main/kotlin/com/loopers/interfaces/api/user/auth/UserAuthV1Request.kt`                            |
| Response             | `src/main/kotlin/com/loopers/interfaces/api/user/auth/UserAuthV1Response.kt`                           |
| UseCase              | `src/main/kotlin/com/loopers/application/user/auth/UserSignUpUseCase.kt`                               |
| Port Interface       | `src/main/kotlin/com/loopers/domain/user/UserPasswordHasher.kt`                                        |
| Result DTO           | `src/main/kotlin/com/loopers/application/user/auth/UserResult.kt`                                      |
| Command              | `src/main/kotlin/com/loopers/application/user/auth/UserAuthCommand.kt`                                 |
| Domain Model         | `src/main/kotlin/com/loopers/domain/user/User.kt`                                                      |
| Repository Interface | `src/main/kotlin/com/loopers/domain/user/UserRepository.kt`                                            |
| Entity               | `src/main/kotlin/com/loopers/infrastructure/user/UserEntity.kt`                                        |
| Mapper               | `src/main/kotlin/com/loopers/infrastructure/user/UserMapper.kt`                                        |
| Repository Impl      | `src/main/kotlin/com/loopers/infrastructure/user/UserRepositoryImpl.kt`                                |
| JPA Repository       | `src/main/kotlin/com/loopers/infrastructure/user/UserJpaRepository.kt`                                 |
| Unit Test (Domain)   | `src/test/kotlin/com/loopers/domain/user/UserTest.kt`                                                  |
| Unit Test (UseCase)  | `src/test/kotlin/com/loopers/application/user/auth/UserSignUpUseCaseTest.kt`                           |
| Integration Test     | `src/test/kotlin/com/loopers/infrastructure/user/UserRepositoryIntegrationTest.kt`                     |
| E2E Test             | `src/test/kotlin/com/loopers/interfaces/api/user/auth/UserAuthV1SignUpE2ETest.kt`                      |
| Architecture Test    | `src/test/kotlin/com/loopers/support/arch/ArchitectureTest.kt`                                         |
