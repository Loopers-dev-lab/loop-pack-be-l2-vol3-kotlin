# CLAUDE.md

이 파일은 Claude Code가 이 저장소의 코드를 이해하고 작업하는 데 필요한 가이드를 제공합니다.

## 프로젝트 개요

Loopers에서 제공하는 Spring + Kotlin 멀티 모듈 템플릿 프로젝트입니다. 커머스 도메인 애플리케이션을 위한 기반 구조를 제공합니다.

## 기술 스택 및 버전

| 구분 | 기술 | 버전 |
|------|------|------|
| Language | Kotlin | 2.0.20 |
| JDK | Java | 21 |
| Framework | Spring Boot | 3.4.4 |
| Dependency Management | Spring Dependency Management | 1.1.7 |
| Cloud | Spring Cloud Dependencies | 2024.0.1 |
| Build Tool | Gradle (Kotlin DSL) | - |
| Linting | ktlint | 1.0.1 |
| API Docs | SpringDoc OpenAPI | 2.7.0 |

### 테스트 라이브러리

| 라이브러리 | 버전 |
|-----------|------|
| spring-mockk | 4.0.2 |
| mockito-core | 5.14.0 |
| mockito-kotlin | 5.4.0 |
| instancio-junit | 5.0.2 |
| Testcontainers | Spring Boot 관리 |

## 모듈 구조

```
Root (loopers-kotlin-spring-template)
├── apps/                    # 실행 가능한 SpringBootApplication
│   ├── commerce-api        # REST API 애플리케이션
│   ├── commerce-batch      # 배치 처리 애플리케이션
│   └── commerce-streamer   # Kafka 컨슈머 애플리케이션
├── modules/                 # 재사용 가능한 설정 모듈 (도메인 비의존적)
│   ├── jpa                 # JPA/QueryDSL 설정
│   ├── redis               # Redis 설정
│   └── kafka               # Kafka 설정
└── supports/                # 부가 기능 애드온 모듈
    ├── jackson             # Jackson 직렬화 설정
    ├── logging             # 로깅 설정
    └── monitoring          # Prometheus/Grafana 모니터링
```

## 주요 명령어

```bash
# 초기 설정 (pre-commit 훅 설정)
make init

# 테스트 실행
./gradlew test

# ktlint 검사
./gradlew ktlintCheck

# ktlint 포맷팅
./gradlew ktlintFormat

# 프로젝트 빌드
./gradlew build

# commerce-api 로컬 실행
./gradlew :apps:commerce-api:bootRun

# 로컬 인프라 실행 (MySQL 등)
docker-compose -f ./docker/infra-compose.yml up

# 모니터링 스택 실행 (Prometheus, Grafana - localhost:3000)
docker-compose -f ./docker/monitoring-compose.yml up
```

## 아키텍처 패턴

### 패키지 구조 (commerce-api)

```
com.loopers
├── application/           # Facade 패턴, 유즈케이스 조합
├── domain/               # 비즈니스 로직, 엔티티, 서비스
├── infrastructure/       # Repository 구현체, 외부 연동
├── interfaces/           # 컨트롤러, API 스펙, DTO
│   └── api/
└── support/              # 공통 관심사 (에러, 유틸)
```

### 핵심 패턴

- **Facade 패턴**: `application/` 레이어에서 도메인 서비스 조합
- **Repository 패턴**: `domain/`에서 인터페이스 정의, `infrastructure/`에서 구현
- **API Spec 인터페이스**: OpenAPI 문서화를 위해 컨트롤러가 스펙 인터페이스 구현
- **CoreException**: `ErrorType` enum을 사용한 중앙 집중식 에러 처리

### 테스트 전략

- **단위 테스트**: `*Test.kt` - 도메인 로직, 순수 함수
- **통합 테스트**: `*IntegrationTest.kt` - Testcontainers를 활용한 서비스 레이어 테스트
- **E2E 테스트**: `*E2ETest.kt` - MockMvc를 활용한 전체 API 테스트

테스트 설정:
- 프로필: `test`
- 타임존: `Asia/Seoul`
- Testcontainers: MySQL, Redis

## 코드 스타일

- ktlint 규칙 준수 (pre-commit 훅으로 강제)
- Kotlin 관용구 사용 (data class, sealed class, extension function)
- 불변성 선호
- 읽기 전용 트랜잭션: `@Transactional(readOnly = true)`

## 개발 규칙
### 진행 Workflow - 증강 코딩
- **대원칙** : 방향성 및 주요 의사 결정은 개발자에게 제안만 할 수 있으며, 최종 승인된 사항을 기반으로 작업을 수행.
- **중간 결과 보고** : AI 가 반복적인 동작을 하거나, 요청하지 않은 기능을 구현, 테스트 삭제를 임의로 진행할 경우 개발자가 개입.
- **설계 주도권 유지** : AI 가 임의판단을 하지 않고, 방향성에 대한 제안 등을 진행할 수 있으나 개발자의 승인을 받은 후 수행.

### 개발 Workflow - TDD (Red > Green > Refactor)
- 모든 테스트는 3A 원칙으로 작성할 것 (Arrange - Act - Assert)
#### 1. Red Phase : 실패하는 테스트 먼저 작성
- 요구사항을 만족하는 기능 테스트 케이스 작성
- 테스트 예시
#### 2. Green Phase : 테스트를 통과하는 코드 작성
- Red Phase 의 테스트가 모두 통과할 수 있는 코드 작성
- 오버엔지니어링 금지
#### 3. Refactor Phase : 불필요한 코드 제거 및 품질 개선
- 불필요한 private 함수 지양, 객체지향적 코드 작성
- unused import 제거
- 성능 최적화
- 모든 테스트 케이스가 통과해야 함

## 주의사항
### 1. Never Do
- 실제 동작하지 않는 코드, 불필요한 Mock 데이터를 이요한 구현을 하지 말 것
- null-safety 하지 않게 코드 작성하지 말 것 (Java 의 경우, Optional 을 활용할 것)
- println 코드 남기지 말 것

### 2. Recommendation
- 실제 API 를 호출해 확인하는 E2E 테스트 코드 작성
- 재사용 가능한 객체 설계
- 성능 최적화에 대한 대안 및 제안
- 개발 완료된 API 의 경우, `.http/**.http` 에 분류해 작성

### 3. Priority
1. 실제 동작하는 해결책만 고려
2. null-safety, thread-safety 고려
3. 테스트 가능한 구조로 설계
4. 기존 코드 패턴 분석 후 일관성 유지