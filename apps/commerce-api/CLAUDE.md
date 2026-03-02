# Commerce API 아키텍처

## 레이어 구조 & 의존 방향

```
interfaces/  →  application/  →  domain/  ←  infrastructure/
```

**DIP**: `Application → Domain ← Infrastructure`. Domain은 어디에도 의존하지 않는다.

## 패키지 전략

계층 패키지 하위에 도메인별로 패키징한다 (예: `domain/catalog/`, `infrastructure/catalog/`).

## 요청 흐름 (Strict Layered Architecture)

- **모든 Controller**: Controller → **UseCase** → Repository / Domain Service. 예외 없음. UseCase가 Repository를 직접 호출하는 것이
  기본이며, 원자적 얽힘이 있는 경우에만 Domain Service를 경유한다.
- 인증: **AuthInterceptor** → `AuthenticateUserUseCase` → UserRepository
- Controller는 Domain 객체(Command, VO, Enum, Service)를 절대 직접 참조하지 않는다.
- Domain Enum은 도메인에 유지. Interfaces DTO에 API 전용 Enum을 선언하고 매핑.

## 에러 처리

비즈니스 예외는 개별 Exception 클래스를 만들지 않고, `CoreException(errorType: ErrorType, customMessage: String?)` 단일 클래스만 사용한다.

ErrorType: `INTERNAL_ERROR(500)`, `BAD_REQUEST(400)`, `NOT_FOUND(404)`, `CONFLICT(409)`, `UNAUTHORIZED(401)`.

## API 응답

모든 응답은 `ApiResponse<T>` 래퍼 사용. `ApiResponse.success(data)` / `ApiResponse.fail(errorCode, message)`.
`ApiControllerAdvice`에서 전역 예외 처리.

## 설계 결정 기록

주요 설계 결정 및 근거는 `docs/note/round3-decisions.md`에 기록한다.
