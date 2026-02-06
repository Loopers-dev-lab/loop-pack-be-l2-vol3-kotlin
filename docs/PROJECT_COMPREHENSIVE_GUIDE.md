# Loopers Kotlin Spring Template - 완전한 프로젝트 재구축 가이드

이 문서는 이 프로젝트를 처음 보는 사람이 모든 파일과 설정을 이해하고, 프로젝트를 처음부터 다시 만들 수 있도록 작성된 상세 가이드입니다.

---

## 목차
1. [프로젝트 개요](#1-프로젝트-개요)
2. [핵심 아키텍처 개념](#2-핵심-아키텍처-개념)
3. [빌드 시스템 상세 설명](#3-빌드-시스템-상세-설명)
4. [모듈 구조 상세 설명](#4-모듈-구조-상세-설명)
5. [인프라 환경 구성](#5-인프라-환경-구성)
6. [애플리케이션 코드 구조](#6-애플리케이션-코드-구조)
7. [테스트 전략과 구성](#7-테스트-전략과-구성)
8. [처음부터 프로젝트 재구축하기](#8-처음부터-프로젝트-재구축하기)
9. [각 파일의 역할과 목적](#9-각-파일의-역할과-목적)
10. [개발 워크플로우](#10-개발-워크플로우)

---

## 1. 프로젝트 개요

### 1.1 프로젝트 정체성
- **프로젝트명**: loopers-kotlin-spring-template
- **그룹 ID**: com.loopers
- **목적**: Kotlin 기반 Spring Boot 멀티모듈 템플릿 제공
- **빌드 도구**: Gradle with Kotlin DSL
- **언어**: Kotlin 2.0.20
- **JVM 버전**: Java 21

### 1.2 핵심 기술 스택

#### 프레임워크
- **Spring Boot**: 3.4.4
- **Spring Cloud**: 2024.0.1
- **Spring Dependency Management Plugin**: 1.1.7

#### 데이터 레이어
- **Database**: MySQL 8.0
- **ORM**: JPA/Hibernate
- **Query Builder**: QueryDSL with Kapt
- **Connection Pool**: HikariCP (Spring Boot 기본)
- **Cache**: Redis 7.0 (Master-Replica 구조)

#### 메시징
- **Message Broker**: Apache Kafka 3.5.1 (KRaft 모드)
- **Kafka Admin UI**: Provectus Kafka UI

#### 테스팅
- **Test Framework**: JUnit 5 Platform
- **Mocking**: SpringMockK 4.0.2, Mockito 5.14.0, Mockito-Kotlin 5.4.0
- **Test Data**: Instancio JUnit 5.0.2
- **Containers**: Testcontainers (MySQL, Redis, Kafka)

#### 코드 품질
- **Linter**: ktlint 1.0.1 (Gradle Plugin 12.1.2)
- **Coverage**: JaCoCo
- **Pre-commit Hook**: ktlintCheck 자동 실행

#### 모니터링
- **Metrics**: Micrometer + Prometheus Registry
- **Dashboard**: Grafana
- **Health Checks**: Spring Boot Actuator

#### API 문서화
- **OpenAPI**: SpringDoc OpenAPI 2.7.0
- **Swagger UI**: 내장 (개발 환경에서만 활성화)

---

## 2. 핵심 아키텍처 개념

### 2.1 3-Tier 모듈 구조 (Three-Tier Module Architecture)

이 프로젝트는 엄격하게 정의된 3계층 모듈 아키텍처를 따릅니다.

#### Tier 1: `apps/` - 실행 가능한 애플리케이션
**목적**: Spring Boot 애플리케이션으로 실제로 실행되는 모듈

**특징**:
- `@SpringBootApplication` 애노테이션이 있는 메인 클래스 포함
- BootJar 활성화, 일반 Jar 비활성화
- modules와 supports를 의존성으로 가져와 사용
- 각자 독립적인 application.yml 설정 파일 보유

**포함된 모듈**:
1. `commerce-api`: REST API 서버 (웹 엔드포인트 제공)
2. `commerce-batch`: 배치 처리 애플리케이션
3. `commerce-streamer`: 이벤트 스트리밍/처리 애플리케이션

#### Tier 2: `modules/` - 재사용 가능한 인프라 설정
**목적**: 도메인과 무관한, 재사용 가능한 인프라 구성 모듈

**원칙**:
- 특정 비즈니스 로직이나 도메인 구현에 종속되지 않음
- 순수하게 기술적인 설정과 구성만 담당
- 다른 프로젝트에서도 그대로 가져다 쓸 수 있어야 함

**특징**:
- 일반 Jar 활성화, BootJar 비활성화
- Configuration 클래스와 유틸리티 제공
- 각자 독립적인 YAML 설정 파일 제공 (예: jpa.yml, redis.yml)

**포함된 모듈**:
1. `jpa`: JPA/Hibernate 설정, BaseEntity, QueryDSL 설정
2. `redis`: Redis 설정 (Master-Replica 구조)
3. `kafka`: Kafka Producer/Consumer 설정

#### Tier 3: `supports/` - 부가 기능 유틸리티
**목적**: 횡단 관심사(Cross-cutting Concerns)와 보조 기능 제공

**특징**:
- 일반 Jar 활성화, BootJar 비활성화
- 애플리케이션 전반에 걸쳐 사용되는 공통 기능 제공

**포함된 모듈**:
1. `jackson`: JSON 직렬화 커스터마이징
2. `logging`: 로깅 설정 (Logback)
3. `monitoring`: Actuator + Prometheus 메트릭

### 2.2 Layered Architecture (apps 내부 구조)

`apps/` 디렉토리의 각 애플리케이션은 다음과 같은 계층 구조를 따릅니다:

```
src/main/kotlin/com/loopers/
├── interfaces/           # API 레이어
│   └── api/
│       ├── ApiResponse.kt
│       ├── ApiControllerAdvice.kt
│       └── example/
│           ├── ExampleV1Controller.kt      # REST 컨트롤러
│           ├── ExampleV1ApiSpec.kt         # API 명세 인터페이스
│           └── ExampleV1Dto.kt             # 요청/응답 DTO
│
├── application/          # 애플리케이션 서비스 레이어
│   └── example/
│       ├── ExampleFacade.kt                # 서비스 오케스트레이션
│       └── ExampleInfo.kt                  # 내부 전달용 데이터 클래스
│
├── domain/               # 도메인 레이어
│   └── example/
│       ├── ExampleModel.kt                 # 도메인 모델 (엔티티)
│       ├── ExampleService.kt               # 도메인 서비스
│       └── ExampleRepository.kt            # 리포지토리 인터페이스
│
├── infrastructure/       # 인프라 레이어
│   └── example/
│       ├── ExampleJpaRepository.kt         # JPA 리포지토리
│       └── ExampleRepositoryImpl.kt        # 리포지토리 구현체
│
└── support/              # 애플리케이션별 지원 기능
    └── error/
        ├── ErrorType.kt
        └── CoreException.kt
```

**계층 간 호출 흐름**:
```
Controller → Facade → Service → Repository
     ↓          ↓         ↓          ↓
   DTO       Info     Model      Entity
```

**각 계층의 책임**:

1. **interfaces/api**: 외부와의 통신 담당
   - HTTP 요청/응답 처리
   - DTO 변환
   - API 명세 구현

2. **application**: 애플리케이션 로직 오케스트레이션
   - 여러 도메인 서비스 조율
   - DTO ↔ Domain Model 변환
   - 트랜잭션 경계 설정

3. **domain**: 핵심 비즈니스 로직
   - 도메인 모델에 비즈니스 규칙 구현
   - 도메인 서비스에서 복잡한 비즈니스 로직 처리
   - 리포지토리 인터페이스 정의 (구현은 infrastructure에서)

4. **infrastructure**: 외부 시스템 연동
   - 데이터베이스 접근 구현
   - 외부 API 호출
   - 메시징 시스템 연동

5. **support**: 애플리케이션별 유틸리티
   - 예외 처리
   - 공통 로직

---

## 3. 빌드 시스템 상세 설명

### 3.1 Gradle 프로젝트 구조

#### 루트 프로젝트 파일들

**settings.gradle.kts**
- **목적**: 멀티모듈 프로젝트 구조 정의
- **주요 내용**:
  ```kotlin
  rootProject.name = "loopers-kotlin-spring-template"

  // 포함된 모듈 선언
  include(
      ":apps:commerce-api",
      ":apps:commerce-streamer",
      ":apps:commerce-batch",
      ":modules:jpa",
      ":modules:redis",
      ":modules:kafka",
      ":supports:jackson",
      ":supports:logging",
      ":supports:monitoring",
  )

  // 플러그인 관리
  pluginManagement {
      // 버전은 gradle.properties에서 주입
      val kotlinVersion: String by settings
      val springBootVersion: String by settings

      repositories {
          maven { url = uri("https://repo.spring.io/milestone") }
          maven { url = uri("https://repo.spring.io/snapshot") }
          gradlePluginPortal()
      }

      // 플러그인 버전 해석 전략
      resolutionStrategy {
          eachPlugin {
              when (requested.id.id) {
                  "org.jetbrains.kotlin.jvm" -> useVersion(kotlinVersion)
                  "org.springframework.boot" -> useVersion(springBootVersion)
                  // 기타 플러그인들...
              }
          }
      }
  }
  ```

**gradle.properties**
- **목적**: 프로젝트 전체에서 사용할 버전 및 설정 중앙화
- **주요 내용**:
  ```properties
  # 프로젝트 그룹
  projectGroup=com.loopers

  # 언어 및 프레임워크 버전
  kotlinVersion=2.0.20
  springBootVersion=3.4.4
  springDependencyManagementVersion=1.1.7
  springCloudDependenciesVersion=2024.0.1

  # 플러그인 버전
  ktLintPluginVersion=12.1.2
  ktLintVersion=1.0.1

  # 라이브러리 버전
  springDocOpenApiVersion=2.7.0
  springMockkVersion=4.0.2
  mockitoVersion=5.14.0
  mockitoKotlinVersion=5.4.0
  instancioJUnitVersion=5.0.2
  slackAppenderVersion=1.6.1

  # Kotlin 데몬 JVM 설정 (빌드 성능 최적화)
  kotlin.daemon.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=512m
  ```

**build.gradle.kts (루트)**
- **목적**: 모든 서브모듈에 공통으로 적용되는 설정 정의
- **주요 섹션**:

1. **Git Hash 기반 버전 관리**:
   ```kotlin
   fun getGitHash(): String {
       return runCatching {
           providers.exec {
               commandLine("git", "rev-parse", "--short", "HEAD")
           }.standardOutput.asText.get().trim()
       }.getOrElse { "init" }
   }
   ```
   - 프로젝트 버전이 지정되지 않으면 현재 Git 커밋 해시를 버전으로 사용
   - Git이 없는 환경에서는 "init"을 기본값으로 사용

2. **플러그인 적용**:
   ```kotlin
   plugins {
       kotlin("jvm")                              // Kotlin JVM 지원
       kotlin("kapt")                             // Kotlin Annotation Processing
       kotlin("plugin.spring") apply false        // Kotlin Spring 플러그인 (서브모듈에서 적용)
       id("org.springframework.boot") apply false // Spring Boot (서브모듈에서 적용)
       id("io.spring.dependency-management")      // Spring 의존성 관리
       id("org.jlleitschuh.gradle.ktlint") apply false // ktlint (서브모듈에서 적용)
   }
   ```

3. **Java & Kotlin 컴파일러 설정**:
   ```kotlin
   java {
       toolchain {
           languageVersion = JavaLanguageVersion.of(21)
       }
   }

   kotlin {
       compilerOptions {
           jvmToolchain(21)
           freeCompilerArgs.addAll("-Xjsr305=strict")  // Null-safety 강화
       }
   }
   ```

4. **모든 프로젝트 공통 설정**:
   ```kotlin
   allprojects {
       group = projectGroup
       version = if (version == DEFAULT_VERSION) getGitHash() else version

       repositories {
           mavenCentral()
       }
   }
   ```

5. **서브프로젝트 공통 설정**:
   ```kotlin
   subprojects {
       // 모든 서브모듈에 플러그인 적용
       apply(plugin = "org.jetbrains.kotlin.jvm")
       apply(plugin = "org.jetbrains.kotlin.kapt")
       apply(plugin = "org.jetbrains.kotlin.plugin.spring")
       apply(plugin = "org.springframework.boot")
       apply(plugin = "io.spring.dependency-management")
       apply(plugin = "jacoco")
       apply(plugin = "org.jlleitschuh.gradle.ktlint")

       // Spring Cloud BOM 임포트
       dependencyManagement {
           imports {
               mavenBom("org.springframework.cloud:spring-cloud-dependencies:${project.properties["springCloudDependenciesVersion"]}")
           }
       }

       // 모든 모듈 공통 의존성
       dependencies {
           // Kotlin 기본
           runtimeOnly("org.springframework.boot:spring-boot-starter-validation")
           implementation("org.jetbrains.kotlin:kotlin-reflect")
           implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

           // Spring 기본
           implementation("org.springframework.boot:spring-boot-starter")

           // JSON 직렬화
           implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
           implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

           // 테스트 공통 의존성
           testRuntimeOnly("org.junit.platform:junit-platform-launcher")
           testRuntimeOnly("com.mysql:mysql-connector-j")
           testImplementation("org.springframework.boot:spring-boot-starter-test")
           testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
           testImplementation("com.ninja-squad:springmockk:${project.properties["springMockkVersion"]}")
           testImplementation("org.mockito:mockito-core:${project.properties["mockitoVersion"]}")
           testImplementation("org.mockito.kotlin:mockito-kotlin:${project.properties["mockitoKotlinVersion"]}")
           testImplementation("org.instancio:instancio-junit:${project.properties["instancioJUnitVersion"]}")

           // Testcontainers
           testImplementation("org.springframework.boot:spring-boot-testcontainers")
           testImplementation("org.testcontainers:testcontainers")
           testImplementation("org.testcontainers:junit-jupiter")
       }
   }
   ```

6. **모듈별 Jar/BootJar 설정**:
   ```kotlin
   // 기본: 모든 모듈은 일반 Jar 생성, BootJar 비활성화
   tasks.withType(Jar::class) { enabled = true }
   tasks.withType(BootJar::class) { enabled = false }

   // apps/ 아래 모듈만 BootJar 생성, 일반 Jar 비활성화
   configure(allprojects.filter { it.parent?.name.equals("apps") }) {
       tasks.withType(Jar::class) { enabled = false }
       tasks.withType(BootJar::class) { enabled = true }
   }
   ```
   - **일반 Jar**: 라이브러리로 사용되는 모듈 (modules, supports)
   - **BootJar**: 실행 가능한 Spring Boot 애플리케이션 (apps)

7. **테스트 설정**:
   ```kotlin
   tasks.test {
       maxParallelForks = 1                           // 테스트 병렬 실행 제한 (Testcontainers 안정성)
       useJUnitPlatform()                             // JUnit 5 사용
       systemProperty("user.timezone", "Asia/Seoul")  // 타임존 설정
       systemProperty("spring.profiles.active", "test") // 테스트 프로파일 활성화
       jvmArgs("-Xshare:off")                         // 클래스 데이터 공유 비활성화 (안정성)
   }
   ```

8. **JaCoCo 코드 커버리지 설정**:
   ```kotlin
   tasks.withType<JacocoReport> {
       mustRunAfter("test")
       executionData(fileTree(layout.buildDirectory.asFile).include("jacoco/*.exec"))
       reports {
           xml.required = true   // XML 리포트 (CI/CD에서 사용)
           csv.required = false
           html.required = false
       }
   }
   ```

9. **ktlint 설정**:
   ```kotlin
   configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
       version.set(properties["ktLintVersion"] as String)
   }
   ```

10. **모듈 컨테이너 Task 비활성화**:
    ```kotlin
    // apps, modules, supports는 실제 모듈이 아닌 컨테이너이므로 Task 비활성화
    project("apps") { tasks.configureEach { enabled = false } }
    project("modules") { tasks.configureEach { enabled = false } }
    project("supports") { tasks.configureEach { enabled = false } }
    ```

### 3.2 모듈별 build.gradle.kts

#### apps/commerce-api/build.gradle.kts
```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.jpa")  // JPA 엔티티에 대한 Kotlin 지원
}

dependencies {
    // 내부 모듈 의존성
    implementation(project(":modules:jpa"))
    implementation(project(":modules:redis"))
    implementation(project(":supports:jackson"))
    implementation(project(":supports:logging"))
    implementation(project(":supports:monitoring"))

    // 웹 관련
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.properties["springDocOpenApiVersion"]}")

    // QueryDSL
    kapt("com.querydsl:querydsl-apt::jakarta")

    // 테스트 픽스처 (테스트용 유틸리티)
    testImplementation(testFixtures(project(":modules:jpa")))
    testImplementation(testFixtures(project(":modules:redis")))
}
```

**의도**:
- commerce-api는 REST API 서버이므로 spring-boot-starter-web 필요
- JPA, Redis 모듈을 사용하므로 해당 모듈 의존성 추가
- QueryDSL을 사용하므로 kapt로 Q클래스 생성
- 테스트에서 데이터베이스 정리 등을 위해 testFixtures 사용

#### modules/jpa/build.gradle.kts
```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.jpa")  // JPA 엔티티에 대한 no-arg 생성자 자동 생성
    `java-test-fixtures`                   // testFixtures 기능 활성화
}

dependencies {
    // JPA
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    // QueryDSL
    api("com.querydsl:querydsl-jpa::jakarta")
    kapt("com.querydsl:querydsl-apt::jakarta")

    // MySQL JDBC 드라이버
    runtimeOnly("com.mysql:mysql-connector-j")

    // Testcontainers for MySQL
    testImplementation("org.testcontainers:mysql")

    // testFixtures 의존성
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testFixturesImplementation("org.testcontainers:mysql")
}
```

**의도**:
- `api()` 사용: 이 모듈을 의존하는 모듈에게도 JPA 의존성 전파
- `java-test-fixtures` 플러그인: 테스트용 유틸리티를 다른 모듈에서 재사용 가능하게 함
- QueryDSL: 타입 안전한 쿼리 빌더 제공

### 3.3 Gradle Wrapper

프로젝트는 Gradle Wrapper를 사용하여 Gradle 버전을 고정합니다.

```bash
./gradlew --version  # Gradle 버전 확인
./gradlew build      # 전체 빌드
```

**이유**:
- 개발자마다 다른 Gradle 버전을 사용하는 문제 방지
- CI/CD 환경에서 일관된 빌드 보장

---

## 4. 모듈 구조 상세 설명

### 4.1 apps/commerce-api

**목적**: 커머스 도메인의 REST API 제공

**디렉토리 구조**:
```
commerce-api/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── kotlin/com/loopers/
    │   │   ├── CommerceApiApplication.kt      # Spring Boot 메인 클래스
    │   │   ├── interfaces/api/                # API 레이어
    │   │   ├── application/                   # 애플리케이션 레이어
    │   │   ├── domain/                        # 도메인 레이어
    │   │   ├── infrastructure/                # 인프라 레이어
    │   │   └── support/                       # 애플리케이션별 지원 기능
    │   └── resources/
    │       └── application.yml                # 애플리케이션 설정
    └── test/
        └── kotlin/com/loopers/
            ├── CommerceApiContextTest.kt      # 스프링 컨텍스트 로딩 테스트
            ├── domain/                        # 도메인 단위 테스트
            └── interfaces/api/                # E2E API 테스트
```

**application.yml 구조**:
```yaml
server:
  shutdown: graceful                           # 그레이스풀 셧다운
  tomcat:
    threads:
      max: 200                                 # 최대 워커 스레드 (동시 요청 처리 수)
      min-spare: 10                            # 최소 유지 스레드
    connection-timeout: 1m                     # 연결 타임아웃
    max-connections: 8192                      # 최대 동시 연결 수
    accept-count: 100                          # 대기 큐 크기
    keep-alive-timeout: 60s                    # Keep-Alive 타임아웃
  max-http-request-header-size: 8KB            # HTTP 헤더 최대 크기

spring:
  main:
    web-application-type: servlet              # Servlet 기반 웹 애플리케이션
  application:
    name: commerce-api
  profiles:
    active: local                              # 기본 프로파일
  config:
    import:                                    # 외부 설정 파일 임포트
      - jpa.yml
      - redis.yml
      - logging.yml
      - monitoring.yml

springdoc:
  use-fqn: true                                # Fully Qualified Name 사용
  swagger-ui:
    path: /swagger-ui.html

---
# local, test 프로파일에서만 Swagger 활성화
spring:
  config:
    activate:
      on-profile: local, test

---
# prd 프로파일에서는 Swagger 비활성화
spring:
  config:
    activate:
      on-profile: prd

springdoc:
  api-docs:
    enabled: false
```

**주요 파일 설명**:

1. **CommerceApiApplication.kt**:
   ```kotlin
   @SpringBootApplication
   class CommerceApiApplication

   fun main(args: Array<String>) {
       runApplication<CommerceApiApplication>(*args)
   }
   ```
   - Spring Boot 애플리케이션 진입점

2. **ApiResponse.kt**:
   - 모든 API 응답의 표준 포맷 정의
   - 성공/실패 응답 일관성 유지

3. **ApiControllerAdvice.kt**:
   - 전역 예외 처리
   - CoreException을 HTTP 응답으로 변환

4. **ExampleModel.kt**:
   ```kotlin
   @Entity
   @Table(name = "example")
   class ExampleModel(
       name: String,
       description: String,
   ) : BaseEntity() {
       var name: String = name
           protected set  // 외부에서 직접 수정 불가

       var description: String = description
           protected set

       init {
           // 생성 시점 검증
           if (name.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
           if (description.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "설명은 비어있을 수 없습니다.")
       }

       fun update(newDescription: String) {
           // 변경 메서드에서 검증
           if (newDescription.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "설명은 비어있을 수 없습니다.")
           this.description = newDescription
       }
   }
   ```
   - **의도**: 도메인 모델에 비즈니스 규칙 구현 (Rich Domain Model)
   - `protected set`: 외부에서 직접 변경 불가, 메서드를 통해서만 변경 가능
   - `init` 블록: 생성 시점 불변식(invariant) 검증
   - `update()`: 변경 메서드에서 비즈니스 규칙 검증

### 4.2 apps/commerce-batch

**목적**: 배치 처리 작업 실행

**특징**:
- Spring Batch 기반 (필요 시 의존성 추가)
- 주기적인 데이터 처리, 집계, 정리 작업 수행
- 웹 서버 없이 실행

### 4.3 apps/commerce-streamer

**목적**: 실시간 이벤트 스트리밍 처리

**특징**:
- Kafka Consumer로 동작
- 이벤트 기반 비즈니스 로직 처리
- 웹 서버 없이 실행

### 4.4 modules/jpa

**목적**: JPA/Hibernate 설정 및 공통 엔티티 제공

**주요 파일**:

1. **jpa.yml**:
   ```yaml
   spring:
     jpa:
       open-in-view: false                     # OSIV 비활성화 (명시적 트랜잭션 관리)
       generate-ddl: false
       show-sql: false
       hibernate:
         ddl-auto: none
       properties:
         hibernate:
           default_batch_fetch_size: 100       # N+1 문제 해결 (IN 쿼리 배치 사이즈)
           timezone.default_storage: NORMALIZE_UTC  # DB에 UTC로 저장
           jdbc.time_zone: UTC

   datasource:
     mysql-jpa:
       main:
         driver-class-name: com.mysql.cj.jdbc.Driver
         jdbc-url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}
         username: ${MYSQL_USER}
         password: "${MYSQL_PWD}"
         pool-name: mysql-main-pool
         maximum-pool-size: 40                 # 최대 커넥션 풀 크기
         minimum-idle: 30                      # 최소 유휴 커넥션
         connection-timeout: 3000              # 커넥션 획득 대기시간 (3초)
         validation-timeout: 5000              # 커넥션 유효성 검사 시간 (5초)
         keepalive-time: 0
         max-lifetime: 1800000                 # 커넥션 최대 생존시간 (30분)
         leak-detection-threshold: 0           # 커넥션 누수 감지 (0 = 비활성화)
         initialization-fail-timeout: 1        # DB 연결 실패 시 즉시 예외
         data-source-properties:
           rewriteBatchedStatements: true      # Batch INSERT 최적화

   ---
   spring.config.activate.on-profile: local

   spring:
     jpa:
       show-sql: true                          # 로컬에서는 SQL 로그 출력
       hibernate:
         ddl-auto: create                      # 로컬에서는 자동 스키마 생성

   datasource:
     mysql-jpa:
       main:
         jdbc-url: jdbc:mysql://localhost:3306/loopers
         username: application
         password: application

   ---
   spring.config.activate.on-profile: test

   spring:
     jpa:
       show-sql: true
       hibernate:
         ddl-auto: create                      # 테스트에서도 자동 스키마 생성

   datasource:
     mysql-jpa:
       main:
         maximum-pool-size: 10                 # 테스트에서는 작은 풀 사이즈
         minimum-idle: 5
   ```

2. **BaseEntity.kt**:
   ```kotlin
   @MappedSuperclass
   abstract class BaseEntity {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       val id: Long = 0

       @Column(name = "created_at", nullable = false, updatable = false)
       lateinit var createdAt: ZonedDateTime
           protected set

       @Column(name = "updated_at", nullable = false)
       lateinit var updatedAt: ZonedDateTime
           protected set

       @Column(name = "deleted_at")
       var deletedAt: ZonedDateTime? = null
           protected set

       open fun guard() = Unit  // 서브클래스에서 오버라이드하여 검증 로직 추가

       @PrePersist
       private fun prePersist() {
           guard()
           val now = ZonedDateTime.now()
           createdAt = now
           updatedAt = now
       }

       @PreUpdate
       private fun preUpdate() {
           guard()
           updatedAt = ZonedDateTime.now()
       }

       fun delete() {
           deletedAt ?: run { deletedAt = ZonedDateTime.now() }
       }

       fun restore() {
           deletedAt?.let { deletedAt = null }
       }
   }
   ```
   - **의도**: 모든 엔티티의 공통 필드(id, createdAt, updatedAt, deletedAt) 자동 관리
   - Soft Delete 패턴 구현
   - `@PrePersist`, `@PreUpdate`로 타임스탬프 자동 설정

3. **DataSourceConfig.kt**:
   ```kotlin
   @Configuration
   class DataSourceConfig {
       @Bean
       @ConfigurationProperties(prefix = "datasource.mysql-jpa.main")
       fun mySqlMainHikariConfig(): HikariConfig = HikariConfig()

       @Primary
       @Bean
       fun mySqlMainDataSource(@Qualifier("mySqlMainHikariConfig") hikariConfig: HikariConfig) =
           HikariDataSource(hikariConfig)
   }
   ```
   - **의도**: YAML 설정을 HikariCP 설정으로 변환
   - `@ConfigurationProperties`로 타입 안전한 설정 바인딩

4. **QueryDslConfig.kt**:
   - QueryDSL의 `JPAQueryFactory` 빈 생성
   - 타입 안전한 쿼리 작성 지원

5. **testFixtures/**:
   - `DatabaseCleanUp.kt`: 테스트 간 데이터베이스 초기화
   - `MySqlTestContainersConfig.kt`: Testcontainers MySQL 설정

### 4.5 modules/redis

**목적**: Redis Master-Replica 구조 설정

**주요 파일**:

1. **redis.yml**:
   ```yaml
   spring:
     data:
       redis:
         repositories:
           enabled: false  # Redis Repository 사용 안 함 (RedisTemplate만 사용)

   datasource:
     redis:
       database: 0
       master:
         host: ${REDIS_MASTER_HOST}
         port: ${REDIS_MASTER_PORT}
       replicas:
         - host: ${REDIS_REPLICA_1_HOST}
           port: ${REDIS_REPLICA_1_PORT}

   ---
   spring.config.activate.on-profile: local, test

   datasource:
     redis:
       master:
         host: localhost
         port: 6379
       replicas:
         - host: localhost
           port: 6380
   ```

2. **RedisConfig.kt**:
   ```kotlin
   @Configuration
   @EnableConfigurationProperties(RedisProperties::class)
   class RedisConfig(
       private val redisProperties: RedisProperties,
   ) {
       @Primary
       @Bean
       fun defaultRedisConnectionFactory(): LettuceConnectionFactory {
           val (database, master, replicas) = redisProperties
           return lettuceConnectionFactory(database, master, replicas) {
               readFrom(ReadFrom.REPLICA_PREFERRED)  // 읽기는 Replica 우선
           }
       }

       @Qualifier(CONNECTION_MASTER)
       @Bean
       fun masterRedisConnectionFactory(): LettuceConnectionFactory {
           val (database, master, replicas) = redisProperties
           return lettuceConnectionFactory(database, master, replicas) {
               readFrom(ReadFrom.MASTER)  // 쓰기는 항상 Master
           }
       }

       @Primary
       @Bean
       fun defaultRedisTemplate(
           lettuceConnectionFactory: LettuceConnectionFactory,
       ): RedisTemplate<*, *> {
           return RedisTemplate<String, String>()
               .defaultRedisTemplate(lettuceConnectionFactory)
       }

       @Qualifier(REDIS_TEMPLATE_MASTER)
       @Bean
       fun masterRedisTemplate(
           @Qualifier(CONNECTION_MASTER) lettuceConnectionFactory: LettuceConnectionFactory,
       ): RedisTemplate<*, *> {
           return RedisTemplate<String, String>()
               .defaultRedisTemplate(lettuceConnectionFactory)
       }
   }
   ```
   - **의도**:
     - 기본 RedisTemplate: Replica 우선 읽기 (읽기 성능 향상)
     - Master RedisTemplate: 항상 Master에서 읽기/쓰기 (강한 일관성 필요 시)

### 4.6 modules/kafka

**목적**: Kafka Producer/Consumer 설정

**kafka.yml**:
```yaml
spring:
  kafka:
    bootstrap-servers: ${BOOTSTRAP_SERVERS}
    client-id: ${spring.application.name}
    properties:
        spring.json.add.type.headers: false   # JSON 타입 헤더 비활성화
        request.timeout.ms: 20000
        retry.backoff.ms: 500
        auto:
          create.topics.enable: false         # 토픽 자동 생성 비활성화
          register.schemas: false
          offset.reset: latest                # 최신 오프셋부터 읽기
        use.latest.version: true
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      retries: 3
    consumer:
      group-id: loopers-default-consumer
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-serializer: org.apache.kafka.common.serialization.ByteArrayDeserializer
      properties:
        enable-auto-commit: false             # 수동 커밋
    listener:
      ack-mode: manual                        # 수동 ACK

---
spring.config.activate.on-profile: local, test

spring:
  kafka:
    bootstrap-servers: localhost:19092
    admin:
      properties:
        bootstrap.servers: kafka:9092
```

**의도**:
- Producer는 JSON 직렬화로 메시지 전송
- Consumer는 수동 커밋으로 메시지 처리 보장
- 로컬 환경에서는 localhost:19092로 접속

### 4.7 supports/jackson

**목적**: Jackson JSON 직렬화 커스터마이징

**JacksonConfig.kt**:
- ObjectMapper 커스터마이징
- 날짜/시간 포맷 설정
- Null 처리 정책 등

### 4.8 supports/logging

**목적**: 로깅 설정

**logging.yml**:
```yaml
logging.config: classpath:logback/logback.xml
```

- Logback 설정 파일 경로 지정
- Slack Appender 등 커스텀 Appender 설정 가능

### 4.9 supports/monitoring

**목적**: Actuator + Prometheus 메트릭

**monitoring.yml**:
- Actuator 엔드포인트 노출 설정
- Prometheus 메트릭 활성화
- 커스텀 메트릭 정의

---

## 5. 인프라 환경 구성

### 5.1 Docker Compose 구조

프로젝트는 두 개의 Docker Compose 파일로 인프라를 분리합니다:

#### 5.1.1 docker/infra-compose.yml

**목적**: 애플리케이션 실행에 필요한 핵심 인프라 서비스

**서비스**:

1. **MySQL 8.0**:
   ```yaml
   mysql:
     image: mysql:8.0
     ports:
       - "3306:3306"
     environment:
       - MYSQL_ROOT_PASSWORD=root
       - MYSQL_USER=application
       - MYSQL_PASSWORD=application
       - MYSQL_DATABASE=loopers
       - MYSQL_CHARACTER_SET=utf8mb4
       - MYSQL_COLLATE=utf8mb4_general_ci
     volumes:
       - mysql-8-data:/var/lib/mysql
   ```
   - **의도**:
     - utf8mb4로 이모지 등 모든 유니코드 지원
     - Named Volume으로 데이터 영속성 보장

2. **Redis Master**:
   ```yaml
   redis-master:
     image: redis:7.0
     container_name: redis-master
     ports:
       - "6379:6379"
     volumes:
       - redis_master_data:/data
     command:
       [
         "redis-server",
         "--appendonly", "yes",  # AOF 영속성 활성화
         "--save", "",           # RDB 스냅샷 비활성화
         "--latency-monitor-threshold", "100",  # 100ms 이상 걸리는 명령어 모니터링
       ]
     healthcheck:
       test: ["CMD", "redis-cli", "-p", "6379", "PING"]
       interval: 5s
       timeout: 2s
       retries: 10
   ```
   - **의도**:
     - AOF(Append-Only File): 모든 쓰기 명령어 로깅, 데이터 손실 최소화
     - RDB 비활성화: AOF만 사용하여 메모리 오버헤드 감소
     - Health Check: Replica가 Master 준비 완료 후 시작되도록 보장

3. **Redis Readonly (Replica)**:
   ```yaml
   redis-readonly:
     image: redis:7.0
     container_name: redis-readonly
     depends_on:
       redis-master:
         condition: service_healthy  # Master가 준비된 후 시작
     ports:
       - "6380:6379"
     volumes:
       - redis_readonly_data:/data
     command:
       [
         "redis-server",
         "--appendonly", "yes",
         "--appendfsync", "everysec",           # 1초마다 디스크 동기화
         "--replicaof", "redis-master", "6379", # Master 복제
         "--replica-read-only", "yes",          # 읽기 전용
         "--latency-monitor-threshold", "100",
       ]
   ```
   - **의도**:
     - 읽기 부하 분산
     - Master 장애 시 Failover 가능
     - 읽기 전용 모드로 데이터 무결성 보장

4. **Kafka (KRaft 모드)**:
   ```yaml
   kafka:
     image: bitnamilegacy/kafka:3.5.1
     container_name: kafka
     ports:
       - "9092:9092"    # 내부 통신
       - "19092:19092"  # 외부 접속
     environment:
       - KAFKA_CFG_NODE_ID=1
       - KAFKA_CFG_PROCESS_ROLES=broker,controller  # KRaft 모드 (Zookeeper 불필요)
       - KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,PLAINTEXT_HOST://:19092,CONTROLLER://:9093
       - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:19092
       - KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT,CONTROLLER:PLAINTEXT
       - KAFKA_CFG_INTER_BROKER_LISTENER_NAME=PLAINTEXT
       - KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER
       - KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@kafka:9093
       - KAFKA_CFG_OFFSETS_TOPIC_REPLICATION_FACTOR=1        # 로컬용 복제 계수
       - KAFKA_CFG_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1
       - KAFKA_CFG_TRANSACTION_STATE_LOG_MIN_ISR=1
     volumes:
       - kafka-data:/bitnami/kafka
     healthcheck:
       test: ["CMD", "bash", "-c", "kafka-topics.sh --bootstrap-server localhost:9092 --list || exit 1"]
       interval: 10s
       timeout: 5s
       retries: 10
   ```
   - **의도**:
     - **KRaft 모드**: Zookeeper 없이 Kafka만으로 클러스터 관리 (Kafka 2.8+)
     - **PLAINTEXT**: 로컬 개발용이므로 암호화 없음 (프로덕션에서는 SSL/SASL 사용)
     - **리스너 분리**:
       - `PLAINTEXT://:9092`: 컨테이너 간 통신 (Docker 네트워크 내부)
       - `PLAINTEXT_HOST://:19092`: 호스트에서 접속 (localhost:19092)
       - `CONTROLLER://:9093`: KRaft 컨트롤러 통신

5. **Kafka UI**:
   ```yaml
   kafka-ui:
     image: provectuslabs/kafka-ui:latest
     container_name: kafka-ui
     depends_on:
       kafka:
         condition: service_healthy
     ports:
       - "9099:8080"
     environment:
       KAFKA_CLUSTERS_0_NAME: local
       KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
   ```
   - **의도**: 브라우저에서 Kafka 토픽, 메시지, 컨슈머 그룹 모니터링

**실행**:
```bash
docker-compose -f ./docker/infra-compose.yml up -d
```

#### 5.1.2 docker/monitoring-compose.yml

**목적**: 모니터링 스택 (메트릭 수집 및 시각화)

**서비스**:

1. **Prometheus**:
   ```yaml
   prometheus:
     image: prom/prometheus
     ports:
       - "9090:9090"
     volumes:
       - ./grafana/prometheus.yml:/etc/prometheus/prometheus.yml
   ```
   - **의도**:
     - Actuator에서 노출된 메트릭을 주기적으로 수집
     - 시계열 데이터베이스에 저장

2. **Grafana**:
   ```yaml
   grafana:
     image: grafana/grafana
     ports:
       - "3000:3000"
     volumes:
       - ./grafana/provisioning:/etc/grafana/provisioning
     environment:
       - GF_SECURITY_ADMIN_USER=admin
       - GF_SECURITY_ADMIN_PASSWORD=admin
   ```
   - **의도**:
     - Prometheus 데이터를 시각화
     - 대시보드로 애플리케이션 상태 모니터링

**실행**:
```bash
docker-compose -f ./docker/monitoring-compose.yml up -d
```

**접속**:
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)

### 5.2 Prometheus 설정 (docker/grafana/prometheus.yml)

```yaml
global:
  scrape_interval: 15s  # 15초마다 메트릭 수집

scrape_configs:
  - job_name: 'spring-boot-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']  # 호스트의 Spring Boot 앱
```

**의도**:
- Spring Boot Actuator의 `/actuator/prometheus` 엔드포인트에서 메트릭 수집
- `host.docker.internal`: Docker 컨테이너에서 호스트 머신 접근

### 5.3 Grafana Datasource 설정 (docker/grafana/provisioning/datasources/datasource.yml)

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
```

**의도**:
- Grafana가 자동으로 Prometheus를 데이터 소스로 추가
- Provisioning: 수동 설정 없이 자동 구성

---

## 6. 애플리케이션 코드 구조

### 6.1 도메인 모델 (Domain Model)

**ExampleModel.kt** (앞서 설명한 내용 반복):
- Rich Domain Model 패턴
- 비즈니스 로직을 도메인 모델에 응집
- `protected set`으로 불변성 보장
- 변경 메서드를 통한 검증

### 6.2 리포지토리 패턴 (Repository Pattern)

**도메인 레이어**:
```kotlin
// domain/example/ExampleRepository.kt
interface ExampleRepository {
    fun find(id: Long): ExampleModel?
    fun save(model: ExampleModel): ExampleModel
}
```

**인프라 레이어**:
```kotlin
// infrastructure/example/ExampleJpaRepository.kt
interface ExampleJpaRepository : JpaRepository<ExampleModel, Long>

// infrastructure/example/ExampleRepositoryImpl.kt
@Repository
class ExampleRepositoryImpl(
    private val jpaRepository: ExampleJpaRepository,
) : ExampleRepository {
    override fun find(id: Long): ExampleModel? {
        return jpaRepository.findById(id).orElse(null)
    }

    override fun save(model: ExampleModel): ExampleModel {
        return jpaRepository.save(model)
    }
}
```

**의도**:
- **인터페이스 분리**: 도메인은 JPA에 의존하지 않음
- **구현체 교체 가능**: JPA → MyBatis로 변경 시 인프라 레이어만 수정
- **테스트 용이성**: 리포지토리를 Mock으로 대체 가능

### 6.3 서비스 레이어 (Service Layer)

```kotlin
// domain/example/ExampleService.kt
@Component
class ExampleService(
    private val exampleRepository: ExampleRepository,
) {
    @Transactional(readOnly = true)
    fun getExample(id: Long): ExampleModel {
        return exampleRepository.find(id)
            ?: throw CoreException(errorType = ErrorType.NOT_FOUND, customMessage = "[id = $id] 예시를 찾을 수 없습니다.")
    }
}
```

**의도**:
- 단일 도메인에 대한 비즈니스 로직 처리
- `@Transactional(readOnly = true)`: 읽기 전용 트랜잭션 최적화
- 도메인 예외 발생

### 6.4 파사드 레이어 (Facade Layer)

```kotlin
// application/example/ExampleFacade.kt
@Component
class ExampleFacade(
    private val exampleService: ExampleService,
) {
    fun getExample(id: Long): ExampleInfo {
        val example = exampleService.getExample(id)
        return ExampleInfo(
            id = example.id,
            name = example.name,
            description = example.description,
        )
    }
}
```

**의도**:
- 여러 도메인 서비스 조율
- 도메인 모델 → 애플리케이션 DTO 변환
- 트랜잭션 경계 설정 (필요 시)

### 6.5 컨트롤러 레이어 (Controller Layer)

**API 명세 인터페이스**:
```kotlin
// interfaces/api/example/ExampleV1ApiSpec.kt
@Tag(name = "Example API", description = "예시 API")
interface ExampleV1ApiSpec {
    @Operation(summary = "예시 조회", description = "ID로 예시를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공"),
            ApiResponse(responseCode = "404", description = "예시를 찾을 수 없음"),
        ]
    )
    fun getExample(exampleId: Long): ApiResponse<ExampleV1Dto.ExampleResponse>
}
```

**컨트롤러 구현**:
```kotlin
// interfaces/api/example/ExampleV1Controller.kt
@RestController
@RequestMapping("/api/v1/examples")
class ExampleV1Controller(
    private val exampleFacade: ExampleFacade,
) : ExampleV1ApiSpec {
    @GetMapping("/{exampleId}")
    override fun getExample(
        @PathVariable(value = "exampleId") exampleId: Long,
    ): ApiResponse<ExampleV1Dto.ExampleResponse> {
        return exampleFacade.getExample(exampleId)
            .let { ExampleV1Dto.ExampleResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
```

**DTO**:
```kotlin
// interfaces/api/example/ExampleV1Dto.kt
class ExampleV1Dto {
    @Schema(description = "예시 응답")
    data class ExampleResponse(
        @Schema(description = "ID")
        val id: Long,
        @Schema(description = "이름")
        val name: String,
        @Schema(description = "설명")
        val description: String,
    ) {
        companion object {
            fun from(info: ExampleInfo): ExampleResponse {
                return ExampleResponse(
                    id = info.id,
                    name = info.name,
                    description = info.description,
                )
            }
        }
    }
}
```

**의도**:
- **API 명세 분리**: Swagger 문서와 구현 분리
- **DTO 변환**: 파사드에서 받은 Info를 API Response DTO로 변환
- **표준 응답**: ApiResponse로 일관된 응답 포맷 제공

### 6.6 예외 처리 (Exception Handling)

**CoreException.kt**:
```kotlin
class CoreException(
    val errorType: ErrorType,
    val customMessage: String? = null,
) : RuntimeException(customMessage ?: errorType.message)
```

**ErrorType.kt**:
```kotlin
enum class ErrorType(
    val httpStatus: HttpStatus,
    val code: String,
    val message: String,
) {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    // ...
}
```

**ApiControllerAdvice.kt**:
```kotlin
@RestControllerAdvice
class ApiControllerAdvice {
    @ExceptionHandler(CoreException::class)
    fun handleCoreException(e: CoreException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .status(e.errorType.httpStatus)
            .body(ApiResponse.error(e.errorType.code, e.message ?: e.errorType.message))
    }

    // 기타 예외 핸들러...
}
```

**의도**:
- 도메인에서 발생한 예외를 HTTP 응답으로 변환
- 일관된 에러 응답 형식 제공

---

## 7. 테스트 전략과 구성

### 7.1 테스트 계층

이 프로젝트는 다음과 같은 테스트 계층을 사용합니다:

1. **단위 테스트 (Unit Test)**: 도메인 모델, 서비스 로직
2. **통합 테스트 (Integration Test)**: 리포지토리, 데이터베이스 연동
3. **E2E 테스트 (End-to-End Test)**: API 전체 흐름

### 7.2 테스트 설정

**build.gradle.kts (테스트 관련 설정)**:
```kotlin
tasks.test {
    maxParallelForks = 1                           // Testcontainers 안정성 위해 순차 실행
    useJUnitPlatform()
    systemProperty("user.timezone", "Asia/Seoul")  // 일관된 타임존
    systemProperty("spring.profiles.active", "test")
    jvmArgs("-Xshare:off")
}
```

### 7.3 Testcontainers 설정

**modules/jpa/src/testFixtures/kotlin/com/loopers/testcontainers/MySqlTestContainersConfig.kt**:
```kotlin
@TestConfiguration
class MySqlTestContainersConfig {
    companion object {
        private val mysqlContainer = MySQLContainer<Nothing>("mysql:8.0").apply {
            withDatabaseName("test")
            withUsername("test")
            withPassword("test")
            start()
        }
    }

    @Bean
    @Primary
    fun testDataSource(): DataSource {
        return HikariDataSource().apply {
            jdbcUrl = mysqlContainer.jdbcUrl
            username = mysqlContainer.username
            password = mysqlContainer.password
        }
    }
}
```

**의도**:
- 실제 MySQL 컨테이너를 띄워서 테스트
- 테스트 격리: 각 테스트마다 스키마 재생성
- CI/CD 환경에서도 동일한 테스트 환경 보장

### 7.4 DatabaseCleanUp 유틸리티

**modules/jpa/src/testFixtures/kotlin/com/loopers/utils/DatabaseCleanUp.kt**:
```kotlin
@Component
class DatabaseCleanUp(
    private val entityManager: EntityManager,
) {
    @Transactional
    fun truncateAllTables() {
        val tables = entityManager.metamodel.entities
            .map { it.name }

        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate()
        tables.forEach { table ->
            entityManager.createNativeQuery("TRUNCATE TABLE $table").executeUpdate()
        }
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate()
    }
}
```

**의도**:
- 테스트 간 데이터 격리
- 외래 키 제약 무시하고 모든 테이블 데이터 삭제

### 7.5 E2E 테스트 예시

**ExampleV1ApiE2ETest.kt**:
```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExampleV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val exampleJpaRepository: ExampleJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("GET /api/v1/examples/{id}")
    @Nested
    inner class Get {
        @DisplayName("존재하는 예시 ID를 주면, 해당 예시 정보를 반환한다.")
        @Test
        fun returnsExampleInfo_whenValidIdIsProvided() {
            // arrange
            val exampleModel = exampleJpaRepository.save(ExampleModel(name = "예시 제목", description = "예시 설명"))
            val requestUrl = "/api/v1/examples/${exampleModel.id}"

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ExampleV1Dto.ExampleResponse>>() {}
            val response = testRestTemplate.exchange(requestUrl, HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isEqualTo(exampleModel.id) },
                { assertThat(response.body?.data?.name).isEqualTo(exampleModel.name) },
                { assertThat(response.body?.data?.description).isEqualTo(exampleModel.description) },
            )
        }
    }
}
```

**테스트 패턴 (3A)**:
1. **Arrange**: 테스트 데이터 준비
2. **Act**: 실제 API 호출
3. **Assert**: 응답 검증

**의도**:
- 실제 HTTP 요청으로 전체 레이어 테스트
- 데이터베이스까지 포함한 통합 테스트
- 테스트 후 데이터 정리

---

## 8. 처음부터 프로젝트 재구축하기

이 섹션에서는 이 프로젝트를 완전히 처음부터 재구축하는 과정을 단계별로 설명합니다.

### 8.1 사전 요구사항 설치

**필수 도구**:
1. **JDK 21**: Java 개발 키트
   ```bash
   # macOS (Homebrew)
   brew install openjdk@21

   # Ubuntu
   sudo apt install openjdk-21-jdk
   ```

2. **Docker & Docker Compose**: 인프라 컨테이너 실행
   ```bash
   # macOS
   brew install --cask docker

   # Ubuntu
   sudo apt install docker.io docker-compose
   ```

3. **Git**: 버전 관리
   ```bash
   # macOS
   brew install git

   # Ubuntu
   sudo apt install git
   ```

### 8.2 단계 1: 프로젝트 초기화

#### 1.1 프로젝트 디렉토리 생성
```bash
mkdir loopers-kotlin-spring-template
cd loopers-kotlin-spring-template
```

#### 1.2 Git 초기화
```bash
git init
```

#### 1.3 .gitignore 생성
```
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IntelliJ IDEA
.idea/
*.iml
*.iws
*.ipr
out/

# macOS
.DS_Store

# Logs
*.log

# Test
**/test-results/
```

### 8.3 단계 2: Gradle 프로젝트 설정

#### 2.1 Gradle Wrapper 생성
```bash
# Gradle이 설치되어 있다면
gradle wrapper --gradle-version 8.5

# 또는 Spring Initializr에서 생성한 프로젝트의 wrapper 복사
```

#### 2.2 gradle.properties 작성
```properties
projectGroup=com.loopers
kotlinVersion=2.0.20
ktLintPluginVersion=12.1.2
ktLintVersion=1.0.1
springBootVersion=3.4.4
springDependencyManagementVersion=1.1.7
springCloudDependenciesVersion=2024.0.1
springDocOpenApiVersion=2.7.0
springMockkVersion=4.0.2
mockitoVersion=5.14.0
mockitoKotlinVersion=5.4.0
instancioJUnitVersion=5.0.2
slackAppenderVersion=1.6.1
kotlin.daemon.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=512m
```

#### 2.3 settings.gradle.kts 작성
```kotlin
rootProject.name = "loopers-kotlin-spring-template"

include(
    ":apps:commerce-api",
    ":apps:commerce-streamer",
    ":apps:commerce-batch",
    ":modules:jpa",
    ":modules:redis",
    ":modules:kafka",
    ":supports:jackson",
    ":supports:logging",
    ":supports:monitoring",
)

pluginManagement {
    val kotlinVersion: String by settings
    val springBootVersion: String by settings
    val springDependencyManagementVersion: String by settings
    val ktLintPluginVersion: String by settings

    repositories {
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://repo.spring.io/snapshot") }
        gradlePluginPortal()
    }

    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "org.jetbrains.kotlin.jvm" -> useVersion(kotlinVersion)
                "org.jetbrains.kotlin.kapt" -> useVersion(kotlinVersion)
                "org.jetbrains.kotlin.plugin.spring" -> useVersion(kotlinVersion)
                "org.jetbrains.kotlin.plugin.jpa" -> useVersion(kotlinVersion)
                "org.springframework.boot" -> useVersion(springBootVersion)
                "io.spring.dependency-management" -> useVersion(springDependencyManagementVersion)
                "org.jlleitschuh.gradle.ktlint" -> useVersion(ktLintPluginVersion)
            }
        }
    }
}
```

#### 2.4 루트 build.gradle.kts 작성
(앞서 설명한 내용 참고)

### 8.4 단계 3: 모듈 디렉토리 구조 생성

```bash
# apps 모듈
mkdir -p apps/commerce-api/src/main/{kotlin/com/loopers,resources}
mkdir -p apps/commerce-api/src/test/kotlin/com/loopers
mkdir -p apps/commerce-batch/src/main/{kotlin/com/loopers,resources}
mkdir -p apps/commerce-streamer/src/main/{kotlin/com/loopers,resources}

# modules
mkdir -p modules/jpa/src/main/{kotlin/com/loopers/config/jpa,resources}
mkdir -p modules/jpa/src/main/kotlin/com/loopers/domain
mkdir -p modules/jpa/src/testFixtures/kotlin/com/loopers/{utils,testcontainers}
mkdir -p modules/redis/src/main/{kotlin/com/loopers/config/redis,resources}
mkdir -p modules/redis/src/testFixtures/kotlin/com/loopers/{utils,testcontainers}
mkdir -p modules/kafka/src/main/{kotlin/com/loopers/config/kafka,resources}

# supports
mkdir -p supports/jackson/src/main/kotlin/com/loopers/config/jackson
mkdir -p supports/logging/src/main/resources
mkdir -p supports/monitoring/src/main/resources

# docker
mkdir -p docker/grafana/provisioning/datasources

# .githooks
mkdir -p .githooks
```

### 8.5 단계 4: 각 모듈의 build.gradle.kts 작성

#### apps/commerce-api/build.gradle.kts
```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.jpa")
}

dependencies {
    implementation(project(":modules:jpa"))
    implementation(project(":modules:redis"))
    implementation(project(":supports:jackson"))
    implementation(project(":supports:logging"))
    implementation(project(":supports:monitoring"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.properties["springDocOpenApiVersion"]}")

    kapt("com.querydsl:querydsl-apt::jakarta")

    testImplementation(testFixtures(project(":modules:jpa")))
    testImplementation(testFixtures(project(":modules:redis")))
}
```

#### modules/jpa/build.gradle.kts
```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.jpa")
    `java-test-fixtures`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("com.querydsl:querydsl-jpa::jakarta")
    kapt("com.querydsl:querydsl-apt::jakarta")
    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation("org.testcontainers:mysql")

    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testFixturesImplementation("org.testcontainers:mysql")
}
```

(나머지 모듈도 유사하게 작성)

### 8.6 단계 5: 설정 파일 작성

#### apps/commerce-api/src/main/resources/application.yml
(앞서 설명한 내용 참고)

#### modules/jpa/src/main/resources/jpa.yml
(앞서 설명한 내용 참고)

#### modules/redis/src/main/resources/redis.yml
(앞서 설명한 내용 참고)

#### modules/kafka/src/main/resources/kafka.yml
(앞서 설명한 내용 참고)

### 8.7 단계 6: 핵심 코드 작성

#### BaseEntity.kt
(앞서 설명한 내용 참고)

#### DataSourceConfig.kt
(앞서 설명한 내용 참고)

#### RedisConfig.kt
(앞서 설명한 내용 참고)

#### CommerceApiApplication.kt
```kotlin
package com.loopers

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CommerceApiApplication

fun main(args: Array<String>) {
    runApplication<CommerceApiApplication>(*args)
}
```

### 8.8 단계 7: Docker Compose 파일 작성

#### docker/infra-compose.yml
(앞서 설명한 내용 참고)

#### docker/monitoring-compose.yml
(앞서 설명한 내용 참고)

### 8.9 단계 8: Git Hooks 설정

#### .githooks/pre-commit
```bash
#!/bin/bash

GIT_DIR=$(git rev-parse --show-toplevel)
$GIT_DIR/gradlew ktlintCheck
```

#### Makefile
```makefile
init:
	git config core.hooksPath .githooks
	chmod +x .githooks/pre-commit
```

### 8.10 단계 9: 초기 빌드 및 실행

#### 9.1 Pre-commit Hook 설정
```bash
make init
```

#### 9.2 인프라 서비스 시작
```bash
docker-compose -f ./docker/infra-compose.yml up -d
```

#### 9.3 프로젝트 빌드
```bash
./gradlew build
```

#### 9.4 애플리케이션 실행
```bash
./gradlew :apps:commerce-api:bootRun
```

#### 9.5 API 문서 확인
브라우저에서 http://localhost:8080/swagger-ui.html 접속

### 8.11 단계 10: 첫 커밋
```bash
git add .
git commit -m "Initial project setup with multi-module architecture"
```

---

## 9. 각 파일의 역할과 목적

### 9.1 루트 디렉토리 파일

| 파일명 | 역할 | 상세 설명 |
|--------|------|-----------|
| `settings.gradle.kts` | 멀티모듈 프로젝트 구조 정의 | 포함된 모듈 선언, 플러그인 버전 관리 |
| `build.gradle.kts` | 루트 프로젝트 빌드 설정 | 모든 서브모듈에 공통 적용되는 설정 |
| `gradle.properties` | 프로젝트 전역 속성 | 버전 정보, JVM 옵션 등 |
| `gradlew`, `gradlew.bat` | Gradle Wrapper 스크립트 | Gradle 버전 고정 및 실행 |
| `gradle/wrapper/gradle-wrapper.jar` | Gradle Wrapper 바이너리 | Gradle 다운로드 및 실행 |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle Wrapper 설정 | Gradle 버전 및 다운로드 URL |
| `Makefile` | 개발 편의 스크립트 | Git hooks 설정 등 |
| `.gitignore` | Git 무시 파일 목록 | 빌드 산출물, IDE 설정 등 제외 |
| `.githooks/pre-commit` | Git Pre-commit Hook | 커밋 전 ktlint 자동 실행 |
| `CLAUDE.md` | Claude Code 가이드 | AI 어시스턴트를 위한 프로젝트 설명 |

### 9.2 모듈별 주요 파일

#### apps/commerce-api

| 파일 경로 | 역할 |
|-----------|------|
| `build.gradle.kts` | 모듈 의존성 정의 |
| `src/main/kotlin/com/loopers/CommerceApiApplication.kt` | Spring Boot 메인 클래스 |
| `src/main/resources/application.yml` | 애플리케이션 설정 |
| `src/main/kotlin/com/loopers/interfaces/api/ApiResponse.kt` | 표준 API 응답 포맷 |
| `src/main/kotlin/com/loopers/interfaces/api/ApiControllerAdvice.kt` | 전역 예외 처리 |
| `src/main/kotlin/com/loopers/support/error/CoreException.kt` | 도메인 예외 클래스 |
| `src/main/kotlin/com/loopers/support/error/ErrorType.kt` | 에러 타입 정의 |

#### modules/jpa

| 파일 경로 | 역할 |
|-----------|------|
| `build.gradle.kts` | JPA 관련 의존성 |
| `src/main/resources/jpa.yml` | JPA/Hibernate 설정 |
| `src/main/kotlin/com/loopers/domain/BaseEntity.kt` | 공통 엔티티 필드 |
| `src/main/kotlin/com/loopers/config/jpa/DataSourceConfig.kt` | DataSource 빈 생성 |
| `src/main/kotlin/com/loopers/config/jpa/JpaConfig.kt` | JPA 설정 |
| `src/main/kotlin/com/loopers/config/jpa/QueryDslConfig.kt` | QueryDSL 설정 |
| `src/testFixtures/kotlin/com/loopers/utils/DatabaseCleanUp.kt` | 테스트용 DB 정리 |
| `src/testFixtures/kotlin/com/loopers/testcontainers/MySqlTestContainersConfig.kt` | Testcontainers 설정 |

#### modules/redis

| 파일 경로 | 역할 |
|-----------|------|
| `build.gradle.kts` | Redis 관련 의존성 |
| `src/main/resources/redis.yml` | Redis 설정 |
| `src/main/kotlin/com/loopers/config/redis/RedisConfig.kt` | Redis 빈 생성 |
| `src/main/kotlin/com/loopers/config/redis/RedisProperties.kt` | Redis 속성 바인딩 |
| `src/main/kotlin/com/loopers/config/redis/RedisNodeInfo.kt` | Redis 노드 정보 |
| `src/testFixtures/kotlin/com/loopers/utils/RedisCleanUp.kt` | 테스트용 Redis 정리 |
| `src/testFixtures/kotlin/com/loopers/testcontainers/RedisTestContainersConfig.kt` | Testcontainers 설정 |

#### modules/kafka

| 파일 경로 | 역할 |
|-----------|------|
| `build.gradle.kts` | Kafka 관련 의존성 |
| `src/main/resources/kafka.yml` | Kafka 설정 |
| `src/main/kotlin/com/loopers/config/kafka/KafkaConfig.kt` | Kafka 빈 생성 |

#### supports/jackson

| 파일 경로 | 역할 |
|-----------|------|
| `build.gradle.kts` | Jackson 관련 의존성 |
| `src/main/kotlin/com/loopers/config/jackson/JacksonConfig.kt` | ObjectMapper 커스터마이징 |

#### supports/logging

| 파일 경로 | 역할 |
|-----------|------|
| `build.gradle.kts` | 로깅 관련 의존성 |
| `src/main/resources/logging.yml` | Logback 설정 경로 |

#### supports/monitoring

| 파일 경로 | 역할 |
|-----------|------|
| `build.gradle.kts` | Actuator 관련 의존성 |
| `src/main/resources/monitoring.yml` | Actuator 엔드포인트 설정 |

### 9.3 Docker 관련 파일

| 파일 경로 | 역할 |
|-----------|------|
| `docker/infra-compose.yml` | 인프라 서비스 (MySQL, Redis, Kafka) |
| `docker/monitoring-compose.yml` | 모니터링 스택 (Prometheus, Grafana) |
| `docker/grafana/prometheus.yml` | Prometheus 스크랩 설정 |
| `docker/grafana/provisioning/datasources/datasource.yml` | Grafana 데이터 소스 자동 구성 |

---

## 10. 개발 워크플로우

### 10.1 로컬 개발 환경 시작

#### 1단계: 인프라 서비스 시작
```bash
# MySQL, Redis, Kafka 시작
docker-compose -f ./docker/infra-compose.yml up -d

# 서비스 상태 확인
docker-compose -f ./docker/infra-compose.yml ps

# 로그 확인
docker-compose -f ./docker/infra-compose.yml logs -f
```

#### 2단계: 애플리케이션 실행
```bash
# commerce-api 실행
./gradlew :apps:commerce-api:bootRun

# 또는 IDE에서 CommerceApiApplication.kt 실행
```

#### 3단계: API 테스트
- Swagger UI: http://localhost:8080/swagger-ui.html
- Kafka UI: http://localhost:9099

### 10.2 새 기능 개발 워크플로우

#### 예시: 사용자 관리 기능 추가

**1단계: 도메인 모델 작성**
```kotlin
// apps/commerce-api/src/main/kotlin/com/loopers/domain/user/UserModel.kt
@Entity
@Table(name = "users")
class UserModel(
    email: String,
    name: String,
) : BaseEntity() {
    var email: String = email
        protected set

    var name: String = name
        protected set

    init {
        require(email.contains("@")) { "유효한 이메일이 아닙니다." }
        require(name.isNotBlank()) { "이름은 비어있을 수 없습니다." }
    }

    fun changeName(newName: String) {
        require(newName.isNotBlank()) { "이름은 비어있을 수 없습니다." }
        this.name = newName
    }
}
```

**2단계: 리포지토리 작성**
```kotlin
// domain/user/UserRepository.kt
interface UserRepository {
    fun find(id: Long): UserModel?
    fun findByEmail(email: String): UserModel?
    fun save(user: UserModel): UserModel
}

// infrastructure/user/UserJpaRepository.kt
interface UserJpaRepository : JpaRepository<UserModel, Long> {
    fun findByEmail(email: String): UserModel?
}

// infrastructure/user/UserRepositoryImpl.kt
@Repository
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository,
) : UserRepository {
    override fun find(id: Long): UserModel? = jpaRepository.findById(id).orElse(null)
    override fun findByEmail(email: String): UserModel? = jpaRepository.findByEmail(email)
    override fun save(user: UserModel): UserModel = jpaRepository.save(user)
}
```

**3단계: 서비스 작성**
```kotlin
// domain/user/UserService.kt
@Component
class UserService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createUser(email: String, name: String): UserModel {
        if (userRepository.findByEmail(email) != null) {
            throw CoreException(ErrorType.BAD_REQUEST, "이미 존재하는 이메일입니다.")
        }

        val user = UserModel(email, name)
        return userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun getUser(id: Long): UserModel {
        return userRepository.find(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.")
    }
}
```

**4단계: 파사드 작성**
```kotlin
// application/user/UserFacade.kt
@Component
class UserFacade(
    private val userService: UserService,
) {
    fun createUser(email: String, name: String): UserInfo {
        val user = userService.createUser(email, name)
        return UserInfo.from(user)
    }

    fun getUser(id: Long): UserInfo {
        val user = userService.getUser(id)
        return UserInfo.from(user)
    }
}

// application/user/UserInfo.kt
data class UserInfo(
    val id: Long,
    val email: String,
    val name: String,
) {
    companion object {
        fun from(user: UserModel): UserInfo {
            return UserInfo(
                id = user.id,
                email = user.email,
                name = user.name,
            )
        }
    }
}
```

**5단계: 컨트롤러 작성**
```kotlin
// interfaces/api/user/UserV1ApiSpec.kt
@Tag(name = "User API")
interface UserV1ApiSpec {
    @Operation(summary = "사용자 생성")
    fun createUser(request: UserV1Dto.CreateUserRequest): ApiResponse<UserV1Dto.UserResponse>

    @Operation(summary = "사용자 조회")
    fun getUser(userId: Long): ApiResponse<UserV1Dto.UserResponse>
}

// interfaces/api/user/UserV1Controller.kt
@RestController
@RequestMapping("/api/v1/users")
class UserV1Controller(
    private val userFacade: UserFacade,
) : UserV1ApiSpec {
    @PostMapping
    override fun createUser(@RequestBody request: UserV1Dto.CreateUserRequest): ApiResponse<UserV1Dto.UserResponse> {
        return userFacade.createUser(request.email, request.name)
            .let { UserV1Dto.UserResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{userId}")
    override fun getUser(@PathVariable userId: Long): ApiResponse<UserV1Dto.UserResponse> {
        return userFacade.getUser(userId)
            .let { UserV1Dto.UserResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}

// interfaces/api/user/UserV1Dto.kt
class UserV1Dto {
    @Schema(description = "사용자 생성 요청")
    data class CreateUserRequest(
        @Schema(description = "이메일")
        val email: String,
        @Schema(description = "이름")
        val name: String,
    )

    @Schema(description = "사용자 응답")
    data class UserResponse(
        @Schema(description = "ID")
        val id: Long,
        @Schema(description = "이메일")
        val email: String,
        @Schema(description = "이름")
        val name: String,
    ) {
        companion object {
            fun from(info: UserInfo): UserResponse {
                return UserResponse(
                    id = info.id,
                    email = info.email,
                    name = info.name,
                )
            }
        }
    }
}
```

**6단계: 테스트 작성**
```kotlin
// domain/user/UserModelTest.kt (단위 테스트)
class UserModelTest {
    @Test
    fun `유효한 이메일과 이름으로 사용자를 생성할 수 있다`() {
        // given & when
        val user = UserModel(email = "test@example.com", name = "홍길동")

        // then
        assertThat(user.email).isEqualTo("test@example.com")
        assertThat(user.name).isEqualTo("홍길동")
    }

    @Test
    fun `이메일 형식이 올바르지 않으면 예외가 발생한다`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            UserModel(email = "invalid-email", name = "홍길동")
        }
    }
}

// interfaces/api/UserV1ApiE2ETest.kt (E2E 테스트)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    fun `사용자를 생성하고 조회할 수 있다`() {
        // arrange
        val createRequest = UserV1Dto.CreateUserRequest(
            email = "test@example.com",
            name = "홍길동"
        )

        // act: 사용자 생성
        val createResponse = testRestTemplate.postForEntity(
            "/api/v1/users",
            createRequest,
            object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
        )

        // assert: 생성 성공
        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.OK)
        val createdUser = createResponse.body?.data!!
        assertThat(createdUser.email).isEqualTo("test@example.com")

        // act: 사용자 조회
        val getResponse = testRestTemplate.exchange(
            "/api/v1/users/${createdUser.id}",
            HttpMethod.GET,
            HttpEntity<Any>(Unit),
            object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
        )

        // assert: 조회 성공
        assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(getResponse.body?.data?.email).isEqualTo("test@example.com")
    }
}
```

### 10.3 빌드 및 테스트

#### 전체 빌드
```bash
./gradlew build
```

#### 특정 모듈 빌드
```bash
./gradlew :apps:commerce-api:build
```

#### 테스트 실행
```bash
# 전체 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :apps:commerce-api:test

# 커버리지 포함
./gradlew test jacocoTestReport
```

#### Lint 체크
```bash
# 전체 프로젝트
./gradlew ktlintCheck

# 자동 수정
./gradlew ktlintFormat
```

### 10.4 Git 커밋

```bash
# Pre-commit hook이 자동으로 ktlintCheck 실행
git add .
git commit -m "feat: 사용자 관리 기능 추가"

# ktlint 실패 시 자동 수정
./gradlew ktlintFormat
git add .
git commit -m "feat: 사용자 관리 기능 추가"
```

### 10.5 모니터링 활성화

```bash
# 모니터링 스택 시작
docker-compose -f ./docker/monitoring-compose.yml up -d

# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000 (admin/admin)
```

**Grafana 대시보드 설정**:
1. Grafana 접속 (http://localhost:3000)
2. 좌측 메뉴에서 "Dashboards" 클릭
3. "New" → "Import" 클릭
4. Spring Boot 대시보드 ID 입력 (예: 12900)
5. Prometheus 데이터 소스 선택
6. "Import" 클릭

---

## 11. 트러블슈팅

### 11.1 빌드 실패

**증상**: `./gradlew build` 실패

**해결 방법**:
1. Java 버전 확인:
   ```bash
   java -version  # 21이어야 함
   ```

2. Gradle 캐시 정리:
   ```bash
   ./gradlew clean
   rm -rf ~/.gradle/caches
   ```

3. 의존성 다시 다운로드:
   ```bash
   ./gradlew build --refresh-dependencies
   ```

### 11.2 데이터베이스 연결 실패

**증상**: `Could not open JDBC Connection for transaction`

**해결 방법**:
1. MySQL 컨테이너 상태 확인:
   ```bash
   docker-compose -f ./docker/infra-compose.yml ps
   ```

2. MySQL 로그 확인:
   ```bash
   docker-compose -f ./docker/infra-compose.yml logs mysql
   ```

3. 컨테이너 재시작:
   ```bash
   docker-compose -f ./docker/infra-compose.yml restart mysql
   ```

### 11.3 Redis 연결 실패

**증상**: `Unable to connect to Redis`

**해결 방법**:
1. Redis 컨테이너 상태 확인:
   ```bash
   docker-compose -f ./docker/infra-compose.yml ps redis-master redis-readonly
   ```

2. Redis Health Check:
   ```bash
   docker exec -it redis-master redis-cli PING
   # 응답: PONG
   ```

### 11.4 Kafka 연결 실패

**증상**: `TimeoutException: Topic ... not present in metadata`

**해결 방법**:
1. Kafka 컨테이너 상태 확인:
   ```bash
   docker-compose -f ./docker/infra-compose.yml ps kafka
   ```

2. Kafka 토픽 목록 확인:
   ```bash
   docker exec -it kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
   ```

3. 토픽 수동 생성:
   ```bash
   docker exec -it kafka kafka-topics.sh --bootstrap-server localhost:9092 --create --topic test-topic --partitions 3 --replication-factor 1
   ```

### 11.5 ktlint 실패

**증상**: Git 커밋 시 pre-commit hook 실패

**해결 방법**:
```bash
# 자동 수정
./gradlew ktlintFormat

# 다시 커밋
git add .
git commit -m "..."
```

---

## 12. 프로덕션 배포 고려사항

### 12.1 환경별 설정 분리

**application.yml**:
```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}  # 환경변수로 프로파일 선택
```

**환경별 설정**:
- `local`: 로컬 개발
- `test`: 자동화 테스트
- `dev`: 개발 서버
- `qa`: QA 서버
- `prd`: 프로덕션 서버

### 12.2 보안 설정

**민감 정보 외부화**:
```yaml
# 환경변수 사용
datasource:
  mysql-jpa:
    main:
      jdbc-url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}
      username: ${MYSQL_USER}
      password: ${MYSQL_PWD}
```

**Kubernetes Secret 사용**:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mysql-secret
type: Opaque
data:
  username: YXBwbGljYXRpb24=  # base64 인코딩
  password: YXBwbGljYXRpb24=
```

### 12.3 성능 최적화

**HikariCP 튜닝**:
```yaml
datasource:
  mysql-jpa:
    main:
      maximum-pool-size: 40       # CPU 코어 수 * 10
      minimum-idle: 30
      connection-timeout: 3000
      max-lifetime: 1800000       # 30분
```

**JVM 옵션**:
```bash
java -Xms2g -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar commerce-api.jar
```

### 12.4 모니터링

**필수 메트릭**:
- JVM 메모리 사용량
- GC 빈도 및 지연시간
- 데이터베이스 커넥션 풀 사용률
- API 응답 시간
- 에러율

**알림 설정**:
- 에러율 > 1%
- API 응답 시간 > 1초
- 커넥션 풀 사용률 > 90%

---

## 13. 추가 리소스

### 13.1 공식 문서

- **Spring Boot**: https://spring.io/projects/spring-boot
- **Kotlin**: https://kotlinlang.org/docs/home.html
- **QueryDSL**: http://querydsl.com/
- **Testcontainers**: https://testcontainers.com/
- **Kafka**: https://kafka.apache.org/documentation/

### 13.2 유용한 도구

- **IntelliJ IDEA**: Kotlin 개발 최적화 IDE
- **Postman**: API 테스트
- **DataGrip**: 데이터베이스 관리
- **RedisInsight**: Redis 모니터링

---

## 결론

이 문서는 Loopers Kotlin Spring Template 프로젝트의 모든 파일, 설정, 아키텍처를 상세하게 설명합니다. 이 가이드를 따라하면 프로젝트를 완전히 이해하고 처음부터 재구축할 수 있습니다.

**핵심 원칙 요약**:
1. **3-Tier 모듈 구조**: apps (실행), modules (인프라), supports (유틸리티)
2. **Layered Architecture**: interfaces → application → domain → infrastructure
3. **도메인 중심 설계**: 비즈니스 로직을 도메인 모델에 응집
4. **테스트 가능한 설계**: 계층 분리, 인터페이스 활용
5. **명시적 설정**: 의도가 드러나는 설정 파일 작성

이 템플릿은 확장 가능하고 유지보수하기 쉬운 엔터프라이즈 애플리케이션을 빠르게 시작할 수 있는 기반을 제공합니다.
