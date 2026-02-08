TDD Red Phase: 요구사항에 대한 실패하는 테스트를 작성한다.

요구사항: $ARGUMENTS

## 절차

1. 요구사항을 분석하고, 어떤 종류의 테스트가 적절한지 판단한다:
    - **단위 테스트**: 도메인 로직, Entity 검증, VO, 순수 비즈니스 규칙
    - **통합 테스트**: Service + Repository 연동, DB 관련 로직
    - **E2E 테스트**: Controller → Facade → Service → Repository 전체 흐름
2. 기존 테스트 코드의 패턴을 참고해 일관성을 유지한다
3. 3A 원칙: Arrange(준비) → Act(실행) → Assert(검증)
4. `@Nested` + `@DisplayName`(한국어) BDD 스타일
5. 테스트를 실행해 실패하는지 확인한다:
   ```
   ./gradlew :apps:commerce-api:test --tests "해당테스트클래스.해당테스트메서드"
   ```
6. 실패 결과를 보고한다:
    - 테스트 클래스명, 메서드명
    - 실패 사유 (컴파일 에러 / assertion 실패 / 예외 등)
    - 다음 단계(Green)에서 구현해야 할 내용 요약

## 규칙

- 한 번에 하나의 테스트만 작성
- 테스트 이름은 동작을 설명하도록 한국어로 작성
- VO 검증은 도메인 단위 테스트에서만 — 상위 계층에서 동일 VO 규칙을 중복 테스트하지 않는다
