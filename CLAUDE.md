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
