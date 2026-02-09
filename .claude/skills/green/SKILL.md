---
name: green
description:
  TDD Green Phase. 현재 실패 중인 테스트를 통과시키기 위한 최소한의 코드만 구현한다.
  오버엔지니어링 금지. 레이어드 아키텍처 패턴을 준수한다.
---

## 절차

1. 현재 실패 중인 테스트를 파악한다
2. 실패 원인을 분석한다 (컴파일 에러 / assertion 실패 / 예외 등)
3. 테스트를 통과시키기 위한 **최소한의 코드만** 작성한다
4. 레이어드 아키텍처 패턴을 따른다:
    - **Entity**: `BaseEntity` 상속, `protected set`, `init` 블록 검증
    - **Value Object**: 생성 시 자가 검증, `CoreException` throw
    - **Repository**: 도메인 인터페이스 → infrastructure 구현체
    - **Service**: `@Component`, Repository 인터페이스 의존
    - **Facade**: 오케스트레이션, Info 객체로 데이터 전달
    - **Controller**: `ApiResponse` 래퍼, ApiSpec 인터페이스 구현
5. 해당 테스트 실행 후 전체 테스트를 실행한다:
   ```
   ./gradlew :apps:commerce-api:test --tests "해당테스트클래스.해당테스트메서드"
   ./gradlew test
   ```
6. 결과를 보고한다

## 규칙

- 오버엔지니어링 금지 — 테스트를 통과시키는 데 필요한 코드만
- DTO 변환은 `companion object { fun from(...) }` 팩토리 메서드
- 에러 처리는 `CoreException(errorType, customMessage)`
- null-safety 준수, println 금지
