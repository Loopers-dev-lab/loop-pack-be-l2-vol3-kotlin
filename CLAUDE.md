# CLAUDE.md

## 프로젝트 개요

**loopers-kotlin-spring-template** — Kotlin + Spring Boot 기반 멀티모듈 커머스 백엔드 템플릿 프로젝트

## 기술 스택 및 버전

| 기술                             | 버전                    |
|--------------------------------|-----------------------|
| Kotlin                         | 2.0.20                |
| Java                           | 21                    |
| Spring Boot                    | 3.4.4                 |
| Spring Cloud                   | 2024.0.1              |
| Spring Dependency Management   | 1.1.7                 |
| QueryDSL                       | jakarta               |
| SpringDoc OpenAPI              | 2.7.0                 |
| ktLint                         | 1.0.1 (plugin 12.1.2) |
| Micrometer (Prometheus, Brave) | Spring Boot 관리        |
| Testcontainers                 | Spring Boot 관리        |

## 모듈 구조

```
loopers-kotlin-spring-template/
├── apps/                          # 실행 가능한 애플리케이션 (BootJar)
│   ├── commerce-api/              # REST API 서버 (Spring MVC, Swagger)
│   ├── commerce-batch/            # Spring Batch 배치 서버
│   └── commerce-streamer/         # Kafka Consumer 스트리밍 서버
│
├── modules/                       # 재사용 인프라 모듈 (Library Jar)
│   ├── jpa/                       # JPA + QueryDSL + MySQL DataSource 설정
│   ├── redis/                     # Redis Master-Replica 설정
│   └── kafka/                     # Kafka Producer/Consumer 설정
│
├── supports/                      # 횡단 관심사 모듈 (Library Jar)
│   ├── jackson/                   # Jackson ObjectMapper 설정
│   ├── logging/                   # Logback + Slack Appender 설정
│   └── monitoring/                # Actuator + Prometheus 메트릭 설정
│
├── build.gradle.kts               # 루트 빌드 (공통 의존성, 서브프로젝트 설정)
├── settings.gradle.kts            # 모듈 포함 및 플러그인 버전 관리
└── gradle.properties              # 버전 프로퍼티
```

## 패키지 구조 (Layered Architecture)

```
com.loopers
├── application/       # Facade 계층 (유스케이스 조합)
├── domain/            # 도메인 모델 및 비즈니스 로직
├── infrastructure/    # Repository 구현체
├── interfaces/        # API Controller / Kafka Consumer
└── support/           # 에러 처리 (CoreException, ErrorType)
```

## 빌드 및 실행

```bash
# 전체 빌드
./gradlew build

# 특정 앱 실행
./gradlew :apps:commerce-api:bootRun
./gradlew :apps:commerce-batch:bootRun --args='--job.name=demoJob'
./gradlew :apps:commerce-streamer:bootRun

# 테스트 (Testcontainers 사용, Docker 필요)
./gradlew test

# 코드 스타일 검사
./gradlew ktlintCheck

# 코드 스타일 자동 포맷
./gradlew ktlintFormat
```

## 테스트

- JUnit 5 + SpringMockK + Mockito Kotlin + Instancio
- Testcontainers로 MySQL, Redis, Kafka 격리 테스트
- 테스트 프로파일: `test` (자동 적용, `spring.profiles.active=test`)
- 타임존: `Asia/Seoul`
- `testFixtures`로 MySqlTestContainersConfig, RedisTestContainersConfig, DatabaseCleanUp, RedisCleanUp 제공

## 프로파일

- `local` — 로컬 개발 (show-sql, DDL auto-create)
- `test` — 테스트 (Testcontainers, DDL auto-create)
- `dev`, `qa`, `prd` — 환경변수 기반 설정 (`MYSQL_HOST`, `REDIS_MASTER_HOST`, `BOOTSTRAP_SERVERS` 등)

## 주요 인프라 설정

- **DB**: MySQL (HikariCP 커넥션 풀, 배치 rewrite 지원)
- **Redis**: Master-Replica 구성
- **Kafka**: JSON 직렬화, Manual ACK, auto-commit 비활성화
- **모니터링**: Prometheus 메트릭 (`/actuator/prometheus`, 포트 8081), Liveness/Readiness probe
- **로깅**: Logback + Slack Appender

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

## 작업 환경
- **OS**: Windows (절대 Linux/Unix 전용 명령어 사용 금지)
- `chmod`, `ln -s`, `grep`(Bash), `sed`, `awk`, `cat`, `head`, `tail` 등 Unix 명령어 사용하지 말 것
- Windows 호환 명령어 또는 Claude Code 전용 도구(Read, Edit, Write, Grep, Glob 등)를 사용할 것
- 경로 구분자는 `\` 또는 `/` 모두 가능하나, Windows 경로 형식 우선

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
