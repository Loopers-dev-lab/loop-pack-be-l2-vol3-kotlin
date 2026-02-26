# Layer & Class Responsibility

## Layer 의존성 방향

```mermaid
graph LR
    subgraph Presentation["Presentation Layer"]
        direction TB
        P_CTRL["Controller"]
        P_SPEC["ApiSpec"]
        P_DTO["Dto\n(Request / Response)"]
        P_RES["ApiResponse\nApiControllerAdvice"]
        P_AUTH["Interceptor\nArgumentResolver\nAnnotation"]
    end

    subgraph Application["Application Layer"]
        direction TB
        A_FACADE["Facade\n(@Transactional)"]
        A_SVC["Service"]
        A_CMD["Command"]
        A_INFO["Info"]
        A_ERR["ApplicationException\nApplicationErrorType"]
        A_ASPECT["DomainExceptionTranslator\n(@Aspect)"]
        A_AUTH_SVC["AuthService"]
    end

    subgraph Domain["Domain Layer"]
        direction TB
        D_MODEL["Model\n(data class)"]
        D_VO["VO\n(value class)"]
        D_REPO["Repository\n(interface)"]
        D_DS["PasswordEncoder\n(interface)"]
        D_ENUM["Enum\n(Status 등)"]
        D_COND["SearchCondition\nPageQuery / PageResult"]
        D_VALID["Validator"]
        D_ERR["CoreException\nErrorType"]
    end

    subgraph Infrastructure["Infrastructure Layer"]
        direction TB
        I_JPA_MODEL["JpaModel\n(@Entity)"]
        I_JPA_REPO["JpaRepository\n(Spring Data JPA)"]
        I_REPO_IMPL["RepositoryImpl"]
        I_DS_IMPL["BcryptPasswordEncoder"]
        I_CONFIG["Config\n(CacheConfig 등)"]
    end

    Presentation -->|"depends on"| Application
    Application -->|"depends on"| Domain
    Infrastructure -->|"implements"| Domain
```

## Class Suffix별 책임

```mermaid
graph TD
    subgraph Presentation["Presentation Layer"]
        direction LR
        DTO["<b>Dto</b>\nRequest: HTTP 요청 바인딩 + @Valid\nResponse: HTTP 응답 직렬화"]
        CTRL["<b>Controller</b>\nHTTP 라우팅\nDto ↔ Command/Info 변환"]
        SPEC["<b>ApiSpec</b>\nSwagger 문서 인터페이스"]
        ADVICE["<b>ApiControllerAdvice</b>\nApplicationException → HTTP 응답"]
        INTERCEPTOR["<b>Interceptor</b>\n인증 헤더 검증\nAuthService(Application) 호출"]
    end

    subgraph Application["Application Layer"]
        direction LR
        FACADE["<b>Facade</b>\n@Transactional 유일한 소유자\nService 호출 조합\nModel → Info 변환"]
        SVC["<b>Service</b>\n비즈니스 로직 조합\nRepository 호출\n@Transactional 없음"]
        CMD["<b>Command</b>\nFacade/Service 입력\ndata class"]
        INFO["<b>Info</b>\nFacade 출력 → Controller에 전달\nDomain 타입 노출 안 함\n(primitive/String 변환)"]
        APP_EX["<b>ApplicationException</b>\nPresentation이 받는 유일한 예외"]
    end

    subgraph Domain["Domain Layer"]
        direction LR
        MODEL["<b>Model</b>\n순수 불변 data class\ncopy() 기반 비즈니스 메서드\n외부 의존 없음"]
        VO["<b>VO</b>\n@JvmInline value class\n필드 단위 도메인 검증 규칙\nof() 팩토리에서만 검증"]
        REPO["<b>Repository</b>\ninterface (포트)\nDomain 자체 타입만 사용"]
        DS["<b>Domain Service</b>\ninterface (포트)\n도메인이 필요한 기술 계약"]
        ENUM_D["<b>Enum</b>\n도메인 상태값\nBrandStatus, OrderStatus 등"]
        COND["<b>SearchCondition / PageQuery</b>\nDomain 자체 조회 스펙\n외부 프레임워크 의존 없음"]
        VALID["<b>Validator</b>\nVO로 불가능한 복합 규칙\n(예: RawPassword)"]
        CORE_EX["<b>CoreException / ErrorType</b>\n도메인 비즈니스 예외"]
    end

    subgraph Infrastructure["Infrastructure Layer"]
        direction LR
        JPA_MODEL["<b>JpaModel</b>\n@Entity, BaseEntity 상속\ntoModel() / from() 매핑\nDB 컬럼 매핑 전담"]
        JPA_REPO["<b>JpaRepository</b>\nSpring Data JPA interface\nJpaModel 대상"]
        REPO_IMPL["<b>RepositoryImpl</b>\nRepository 구현체\nModel ↔ JpaModel 변환\nPageQuery ↔ Pageable 변환"]
        DS_IMPL["<b>Domain Service Impl</b>\nDomain Service 구현체\n(예: BcryptPasswordEncoder)"]
    end

    Presentation -->|"depends on"| Application
    Application -->|"depends on"| Domain
    Infrastructure -->|"implements"| Domain
```

## 레이어 경계 데이터 변환 흐름

```mermaid
sequenceDiagram
    participant P as Presentation
    participant A as Application
    participant D as Domain
    participant I as Infrastructure

    Note over P: Request Dto
    P->>A: Command (Dto → Command 변환)
    Note over A: Facade (@Transactional)
    A->>A: Service 호출
    Note over A: Service
    A->>D: Model 생성/조회 (Command → Model)
    D->>I: Repository.save(Model)
    Note over I: RepositoryImpl
    I->>I: Model → JpaModel (from)
    I->>I: JpaRepository.save(JpaModel)
    I->>I: JpaModel → Model (toModel)
    I-->>D: Model 반환
    D-->>A: Model 반환
    Note over A: Facade
    A->>A: Model → Info 변환
    A-->>P: Info 반환
    Note over P: Controller
    P->>P: Info → Response Dto 변환
```

## 예외 전파 흐름

```mermaid
sequenceDiagram
    participant D as Domain
    participant A as Application
    participant P as Presentation

    D->>D: CoreException(ErrorType) 발생
    D-->>A: CoreException 전파
    Note over A: DomainExceptionTranslator (@Aspect)
    A->>A: CoreException catch
    A->>A: ApplicationException 변환
    A-->>P: ApplicationException 전파
    Note over P: ApiControllerAdvice
    P->>P: ApplicationException catch
    P->>P: ApplicationErrorType → HttpStatus 매핑
    P->>P: ApiResponse.error() 반환
```

## 의존성 검증

| Layer | 참조 가능 | 참조 불가 |
|-------|----------|----------|
| Presentation | Application | Domain, Infrastructure |
| Application | Domain | Presentation, Infrastructure |
| Domain | 없음 (자기 자신만) | Presentation, Application, Infrastructure |
| Infrastructure | Domain | Presentation, Application |

## Class Suffix 소속 레이어

| Suffix | Layer | import 가능 대상 |
|--------|-------|-----------------|
| Dto (Request/Response) | Presentation | Application (Command, Info) |
| Controller | Presentation | Application (Facade, Command, Info) |
| ApiSpec | Presentation | Application (Info) |
| Interceptor | Presentation | Application (AuthService) |
| Facade | Application | Domain (Model, Repository, VO, Enum, CoreException) |
| Service | Application | Domain (Model, Repository, VO, Enum, CoreException) |
| Command | Application | primitive only (Domain 타입 참조 안 함) |
| Info | Application | primitive only (Domain 타입 노출 안 함) |
| Model | Domain | Domain 내부만 (VO, Enum, CoreException) |
| VO | Domain | 없음 |
| Repository (interface) | Domain | Domain (Model, PageQuery, PageResult, Enum) |
| Validator | Domain | Domain (VO, CoreException) |
| JpaModel | Infrastructure | Domain (Model), JPA (BaseEntity) |
| JpaRepository | Infrastructure | Infrastructure (JpaModel), Spring Data |
| RepositoryImpl | Infrastructure | Domain (Repository, Model), Infrastructure (JpaModel, JpaRepository) |
