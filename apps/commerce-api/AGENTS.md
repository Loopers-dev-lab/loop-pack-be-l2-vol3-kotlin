# Commerce API — Module-Specific Rules

> 이 파일은 `apps/commerce-api` 작업 시에만 로딩된다. 프로젝트 공통 규칙은 루트 `AGENTS.md` 참조.

## Package Structure

```
com.loopers.
├── application/<domain>/           # UseCase, Result DTO, Command
├── domain/<domain>/                # Domain Model, Repository interface, Port interface
├── infrastructure/<domain>/        # Entity, RepositoryImpl, JpaRepository
├── interfaces/
│   └── api/                        # ApiResponse, ApiControllerAdvice
│       └── <domain>/               # Controller, ApiSpec, Dto
└── support/                        # Cross-cutting (CoreException, ErrorType, Config)
```

### Package Naming Convention

```
com.loopers.<layer>.<domain>
```

Example: `com.loopers.domain.user`, `com.loopers.infrastructure.user`

## Layer Dependencies

- `interfaces` depends on `application`
- `application` depends on `domain`
- `infrastructure` depends on `domain`
- `domain` has **no outward dependencies** (defines interfaces only)

## API-Specific Patterns

- **Controller 변환 체인**: `.let { Dto.from(it) }.let { ApiResponse.success(it) }`
- **ApiResponse**: `{ "meta": { "result": "SUCCESS", "errorCode": null, "message": null }, "data": {...} }` 형태의 표준 응답 래퍼
- **ApiControllerAdvice**: Global exception handling via `CoreException` + `ErrorType`

## Naming Rules (API-Specific)

- **Controller**: `{Domain}V{Version}Controller` implements `{Domain}V{Version}ApiSpec` (예: `UserV1Controller`)
- **ApiSpec**: `{Domain}V{Version}ApiSpec` — Swagger 어노테이션 분리용 인터페이스
- **DTO**: `{Domain}V{Version}Dto` container + inner class (예: `UserV1Dto.SignUpResponse`)
- **버전 관리**: 클래스명에 V{Version} 포함, 패키지 경로에는 버전 없음

## 패턴 레퍼런스

새로운 도메인 구현 시 아래 파일들의 패턴을 따른다 (경로는 `apps/commerce-api` 기준):

| 패턴                        | 참조 파일                                                                                       |
|---------------------------|---------------------------------------------------------------------------------------------|
| Controller + ApiSpec      | `src/main/kotlin/com/loopers/interfaces/api/user/UserV1Controller.kt`, `UserV1ApiSpec.kt`   |
| DTO                       | `src/main/kotlin/com/loopers/interfaces/api/user/UserV1Dto.kt`                              |
| UseCase                   | `src/main/kotlin/com/loopers/application/user/UserSignUpUseCase.kt`                         |
| Port Interface            | `src/main/kotlin/com/loopers/domain/user/UserPasswordHasher.kt`                             |
| Result DTO                | `src/main/kotlin/com/loopers/application/user/UserResult.kt`                                |
| Command                   | `src/main/kotlin/com/loopers/application/user/model/UserSignUpCommand.kt`                   |
| Domain Model              | `src/main/kotlin/com/loopers/domain/user/User.kt`                                          |
| Repository Interface      | `src/main/kotlin/com/loopers/domain/user/UserRepository.kt`                                 |
| Entity                    | `src/main/kotlin/com/loopers/infrastructure/user/UserEntity.kt`                             |
| Repository Impl           | `src/main/kotlin/com/loopers/infrastructure/user/UserRepositoryImpl.kt`                     |
| JPA Repository            | `src/main/kotlin/com/loopers/infrastructure/user/UserJpaRepository.kt`                      |
| Unit Test (Domain)        | `src/test/kotlin/com/loopers/domain/user/UserTest.kt`                                      |
| Unit Test (UseCase)       | `src/test/kotlin/com/loopers/application/user/UserSignUpUseCaseTest.kt`                     |
| Integration Test          | `src/test/kotlin/com/loopers/infrastructure/user/UserRepositoryIntegrationTest.kt`          |
| E2E Test                  | `src/test/kotlin/com/loopers/interfaces/api/user/UserV1SignUpE2ETest.kt`                    |
| Architecture Test         | `src/test/kotlin/com/loopers/support/arch/ArchitectureTest.kt`                              |
