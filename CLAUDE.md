# CLAUDE.md

이 문서는 Claude Code가 프로젝트를 이해하는 데 필요한 컨텍스트를 제공합니다.

## 프로젝트 개요

**loopers-kotlin-spring-template** - Kotlin + Spring Boot 기반의 멀티 모듈 커머스 백엔드 템플릿 프로젝트

## 기술 스택

### 핵심 기술
| 기술 | 버전 |
|------|------|
| Kotlin | 2.0.20 |
| Java | 21 |
| Spring Boot | 3.4.4 |
| Spring Cloud | 2024.0.1 |

### 주요 라이브러리
| 라이브러리 | 버전 | 용도 |
|------------|------|------|
| SpringDoc OpenAPI | 2.7.0 | API 문서화 |
| QueryDSL | (Spring Boot 관리) | 타입 안전한 JPA 쿼리 |
| Jackson | (Spring Boot 관리) | JSON 직렬화 |
| Micrometer | (Spring Boot 관리) | 모니터링/메트릭 |

### 테스트 라이브러리
| 라이브러리 | 버전 |
|------------|------|
| SpringMockK | 4.0.2 |
| Mockito | 5.14.0 |
| Mockito-Kotlin | 5.4.0 |
| Instancio | 5.0.2 |
| Testcontainers | (Spring Boot 관리) |

### 코드 품질
| 도구 | 버전 |
|------|------|
| ktlint | 1.0.1 |
| ktlint Gradle Plugin | 12.1.2 |
| JaCoCo | (Gradle 기본) |

## 모듈 구조

```
loopers-kotlin-spring-template/
├── apps/                          # 실행 가능한 애플리케이션
│   ├── commerce-api/              # REST API 서버 (Web + Actuator + OpenAPI)
│   ├── commerce-batch/            # Spring Batch 애플리케이션
│   └── commerce-streamer/         # Kafka Consumer 애플리케이션
├── modules/                       # 핵심 인프라 모듈
│   ├── jpa/                       # JPA + QueryDSL + MySQL
│   ├── redis/                     # Spring Data Redis
│   └── kafka/                     # Spring Kafka
└── supports/                      # 공통 지원 모듈
    ├── jackson/                   # Jackson 설정
    ├── logging/                   # 로깅 설정 (Slack Appender 포함)
    └── monitoring/                # Prometheus 메트릭
```

### 모듈 의존성

```
commerce-api     → jpa, redis, jackson, logging, monitoring
commerce-batch   → jpa, redis, jackson, logging, monitoring
commerce-streamer→ jpa, redis, kafka, jackson, logging, monitoring
```

## 인프라 (Docker Compose)

| 서비스 | 이미지 | 포트 |
|--------|--------|------|
| MySQL | mysql:8.0 | 3306 |
| Redis Master | redis:7.0 | 6379 |
| Redis Readonly | redis:7.0 | 6380 |
| Kafka (KRaft) | bitnamilegacy/kafka:3.5.1 | 9092, 19092 |
| Kafka UI | provectuslabs/kafka-ui | 9099 |
| Prometheus | prom/prometheus | 9090 |
| Grafana | grafana/grafana | 3000 |

## 빌드 & 실행 명령어

```bash
# 프로젝트 초기화 (git hooks 설정)
make init

# 빌드
./gradlew build

# 테스트
./gradlew test

# ktlint 검사
./gradlew ktlintCheck

# ktlint 자동 수정
./gradlew ktlintFormat

# 인프라 실행
docker compose -f docker/infra-compose.yml up -d

# 모니터링 스택 실행
docker compose -f docker/monitoring-compose.yml up -d
```

## 프로젝트 컨벤션

### 패키지 구조 (commerce-api 기준)
```
com.loopers/
├── application/       # Facade 계층 (유스케이스 조합)
├── domain/            # 도메인 로직 (Service, Model, Repository 인터페이스)
├── infrastructure/    # Repository 구현체, 외부 연동
├── interfaces/        # Controller, DTO, API Spec
└── support/           # 예외, 유틸리티
```

### 테스트 설정
- 타임존: `Asia/Seoul`
- 프로파일: `test`
- Testcontainers: MySQL, Redis, Kafka

### 커밋 전 체크
- `.githooks/pre-commit`에 설정된 훅 실행
- ktlint 검사 포함

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

## 도메인 & 객체 설계 전략
- 도메인 객체는 비즈니스 규칙을 캡슐화해야 합니다.
- 애플리케이션 서비스는 서로 다른 도메인을 조립해, 도메인 로직을 조정하여 기능을 제공해야 합니다.
- 규칙이 여러 서비스에 나타나면 도메인 객체에 속할 가능성이 높습니다.
- 각 기능에 대한 책임과 결합도에 대해 개발자의 의도를 확인하고 개발을 진행합니다.

## 아키텍처, 패키지 구성 전략
- 본 프로젝트는 레이어드 아키텍처를 따르며, DIP (의존성 역전 원칙) 을 준수합니다.
- API request, response DTO와 응용 레이어의 DTO는 분리해 작성하도록 합니다.
- 패키징 전략은 4개 레이어 패키지를 두고, 하위에 도메인 별로 패키징하는 형태로 작성합니다.
  - 예시
    > /interfaces/api (interface 레이어 - API)
      /application/.. (application 레이어 - 도메인 레이어를 조합해 사용 가능한 기능을 제공)
      /domain/.. (domain 레이어 - 도메인 객체 및 엔티티, Repository 인터페이스가 위치)
      /infrastructure/.. (infrastructure 레이어 - JPA, Redis 등을 활용해 Repository 구현체를 제공)
