---
description: 코드 변경 시 아키텍처 규칙 준수 검증
globs: "apps/**/*.kt"
---

# 아키텍처 준수 규칙

## 코드 변경 전 Pre-flight

코드를 변경하기 전에 반드시 수행한다:

1. 변경 대상 레이어의 CLAUDE.md를 읽는다 (root CLAUDE.md의 "레이어별 가이드" 참조)
2. 해당 레이어의 의존 방향 규칙을 확인한다
3. 변경이 어떤 아키텍처 규칙에 근거하는지 명확히 한다

## 레이어 의존 방향 위반 검출 패턴

아래 패턴이 코드에 존재하면 아키텍처 위반이다:

- Domain 계층에서 Spring 프레임워크 타입 import (`org.springframework.*`)
  - 예외: `@Transactional`은 Application 계층에서만 사용
- Domain 계층에서 JPA Entity 직접 참조
- Application 계층에서 Infrastructure 구현체 직접 참조 (Repository 인터페이스만 허용)
- Interfaces(Controller) 계층에서 Domain Service 직접 호출 (UseCase를 통해야 함)
  - 예외: AuthInterceptor는 AuthenticateUserUseCase 직접 호출 허용

## 변경 완료 후 Self-Audit

코드 변경 후 아래를 점검한다:

- Domain 계층에 Spring import가 새로 추가되지 않았는가?
- 비즈니스 로직이 Domain Model 내부에 위치하는가?
- Repository 인터페이스가 Domain 계층에 정의되어 있는가?
