# Phase Green: 최소 구현으로 테스트 통과

TDD Green Phase. 현재 실패 중인 테스트를 통과시키기 위한 최소한의 코드만 구현한다.
오버엔지니어링 금지. 레이어드 아키텍처 패턴을 준수한다.

## 절차

1. **실패 원인 파악**: 현재 실패 테스트를 확인한다.
   - 컴파일 에러: 존재하지 않는 클래스/메서드/프로퍼티
   - assertion 실패: 잘못된 반환값, 예외 미발생 등
   - 예외: 구현 로직 오류

2. **최소 구현 작성**: 테스트를 통과시키는 데 필요한 코드만 작성한다.
   레이어드 아키텍처 패턴을 따른다:
   - **Domain Model / Entity**: `BaseEntity` 상속, `protected set`, `init` 블록 검증
   - **Value Object**: 생성 시 자가 검증, `CoreException` throw
   - **Repository**: 도메인 인터페이스 정의 → infrastructure 구현체
   - **Domain Service**: `@Component`, Repository 인터페이스 의존
   - **UseCase(Facade)**: 오케스트레이션, `@Transactional`, Info 객체로 데이터 전달
   - **Controller**: `ApiResponse` 래퍼, ApiSpec 인터페이스 구현, `@Validated`

3. **해당 테스트 실행**: 구현한 테스트가 통과하는지 확인한다.
   ```bash
   ./gradlew :apps:commerce-api:test --tests "패키지.클래스명.메서드명"
   ```

4. **전체 테스트 실행**: 기존 테스트가 깨지지 않았는지 확인한다.
   ```bash
   ./gradlew :apps:commerce-api:test
   ```

5. **보고**: Phase 완료 보고 형식으로 결과를 보고한다.

## 규칙

- 오버엔지니어링 금지 — 테스트를 통과시키는 데 필요한 코드만 작성한다.
- DTO 변환은 `companion object { fun from(...) }` 팩토리 메서드를 사용한다.
- 에러 처리는 `CoreException(errorType, customMessage)` 단일 클래스를 사용한다.
- null-safety 준수, println 금지.
- `--fix` 모드: 버그 해결에 필요한 최소한의 수정만 한다. 인접 코드를 건드리지 않는다.

## 완료 보고

```
## Green Phase 완료

- **테스트**: 패키지.클래스명.메서드명
- **결과**: 통과
- **변경 파일**: 변경된 파일 목록 (경로 포함)
- **다음 단계**: Refactor Phase — [개선 가능한 사항 1~3줄 요약] 또는 "구조 개선 불필요"
```
