# Interfaces 레이어

API 진입점. Controller → Dto 변환 → UseCase 호출.

## Controller

- OpenAPI 명세는 `ApiSpec` 인터페이스로 분리 (예: `ProductV1ApiSpec`)
- Controller는 ApiSpec을 구현하여 엔드포인트를 제공
- 모든 응답은 `ApiResponse<T>` 래퍼 사용

## Dto

- Response Dto: `companion object { fun from(info: XxxInfo): XxxResponse }` 팩토리 메서드로 Info DTO에서 변환
- Request Dto → Command 변환은 Controller에서 수행
- Response Dto ← Info DTO(Application) 변환은 Controller에서 수행
- Controller는 Domain 객체를 직접 참조하지 않으므로, Response Dto는 반드시 Info DTO(Application)에서 변환한다

## 계층 건너뛰기 금지

- 모든 요청은 예외 없이 UseCase를 통과해야 한다.

## 인증

`AuthInterceptor`가 `AuthenticateUserUseCase`를 호출하여 헤더 기반 인증 처리.

## 페이지네이션

`PageResult<T>.toSpringPage()` 확장함수로 도메인 타입 → Spring `Page<T>` 변환을 Controller에서 수행.

## 검증 전략

### ApiSpec 인터페이스
- **모든 Bean Validation 어노테이션은 ApiSpec 인터페이스에 선언한다**
- Controller 구현체에는 검증 어노테이션을 중복 선언하지 않는다 (Spring의 MethodValidationInterceptor가 인터페이스 어노테이션을 상속)
- PathVariable ID 파라미터: `@Positive` 필수
- 페이지네이션 파라미터: `page`에 `@PositiveOrZero`, `size`에 `@Positive @Max(100)`
- RequestBody: `@Valid` 선언
- 페이지 크기 기본값: Controller에서 `@RequestParam(defaultValue = "20")`

### Controller 구현체
- 클래스 레벨에 `@Validated` 필수 (없으면 Bean Validation이 동작하지 않음)
- 메서드 파라미터에 검증 어노테이션을 직접 붙이지 않는다 (ApiSpec에서 상속)
- `@RequestBody`, `@PathVariable`, `@RequestParam` 등 바인딩 어노테이션만 선언

### Request DTO
- `@field:NotBlank`, `@field:NotNull` 등으로 필수 필드 검증
- 숫자 필드: `@field:Positive`, `@field:Min`, `@field:Max` 등 범위 검증
- 모든 입력 필드에 적절한 제약 어노테이션 부착

### API 응답 타입 규칙
- ApiSpec의 반환 타입에 Application 계층 타입(xxxInfo)을 직접 노출하지 않는다
- 반드시 Interfaces 계층의 Response DTO를 정의하고 변환한다
