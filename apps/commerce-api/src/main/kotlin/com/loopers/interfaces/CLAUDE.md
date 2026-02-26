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
