TDD Refactor Phase: 모든 테스트가 통과하는 상태에서 코드 품질을 개선한다.

리팩토링 대상/방향: $ARGUMENTS

## 절차

1. 전체 테스트가 통과하는 상태인지 확인: `./gradlew test`
    - 실패하면 리팩토링 중단, 보고
2. `$ARGUMENTS`가 있으면 해당 대상 우선 처리, 없으면 개선점 제안
3. 한 번에 하나의 리팩토링 수행:
    - 중복 제거
    - 네이밍 개선
    - 메서드 추출 (단일 책임)
    - unused import 제거
4. 린트 수정: `./gradlew ktlintFormat`
5. 린트 체크: `./gradlew ktlintCheck`
6. 전체 테스트 재확인: `./gradlew test`
7. 결과 보고

## 규칙

- 리팩토링은 **구조적 변경만** (동작 변경 금지)
- 매 단계마다 테스트 실행
- 테스트 실패 시 변경을 되돌리고 보고
