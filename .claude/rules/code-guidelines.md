# 코드 가이드라인

## Never Do

- 실제 동작하지 않는 코드, 불필요한 Mock 데이터를 이용한 구현 금지
- null-safety 하지 않게 코드 작성 금지
- println 코드 남기지 않는다

## Recommendation

- 실제 API를 호출해 확인하는 E2E 테스트 코드 작성
- 재사용 가능한 객체 설계
- 성능 최적화에 대한 대안 및 제안
- 개발 완료된 API는 `http/*.http` 파일에 분류하여 작성

## Priority

1. 실제 동작하는 해결책만 고려
2. null-safety, thread-safety 고려
3. 테스트 가능한 구조로 설계
4. 기존 코드 패턴 분석 후 일관성 유지

## 검증 전략 (Validation Strategy)

### 계층별 검증 책임

#### 1. Interfaces 계층 (ApiSpec / Controller / DTO)

##### ApiSpec 인터페이스
- **모든 Bean Validation 어노테이션은 ApiSpec 인터페이스에 선언한다**
- Controller 구현체에는 검증 어노테이션을 중복 선언하지 않는다 (Spring의 MethodValidationInterceptor가 인터페이스 어노테이션을 상속)
- PathVariable ID 파라미터: `@Positive` 필수
- 페이지네이션 파라미터: `page`에 `@PositiveOrZero`, `size`에 `@Positive @Max(100)`
- RequestBody: `@Valid` 선언

##### Controller 구현체
- 클래스 레벨에 `@Validated` 필수 (없으면 Bean Validation이 동작하지 않음)
- 메서드 파라미터에 검증 어노테이션을 직접 붙이지 않는다 (ApiSpec에서 상속)
- `@RequestBody`, `@PathVariable`, `@RequestParam` 등 바인딩 어노테이션만 선언

##### Request DTO
- `@field:NotBlank`, `@field:NotNull` 등으로 필수 필드 검증
- 숫자 필드: `@field:Positive`, `@field:Min`, `@field:Max` 등 범위 검증
- 모든 입력 필드에 적절한 제약 어노테이션 부착 (도메인까지 나쁜 데이터가 도달하지 않도록)

#### 2. Application 계층 (UseCase)
- Interfaces에서 이미 검증된 값을 신뢰한다
- 비즈니스 규칙에 의한 검증만 수행 (예: "이미 사용된 쿠폰", "재고 부족")
- 페이지네이션 파라미터 재검증은 하지 않는다

#### 3. Domain 계층 (Model / VO)
- **도메인 불변식은 생성 시점에 강제한다** (`init {}` 블록 또는 `require()`)
- 금액 관련: 음수 불가, 상한 초과 불가 (예: discountAmount >= 0 && discountAmount <= originalPrice)
- 수량 관련: 음수 불가
- 상태 관련: 상태-필드 일관성 (예: USED 상태면 usedAt != null)
- VO의 `init {}` 블록에서 `require()`로 즉시 검증

### API 응답 타입 규칙
- ApiSpec의 반환 타입에 Application 계층 타입(xxxInfo)을 직접 노출하지 않는다
- 반드시 Interfaces 계층의 Response DTO를 정의하고 변환한다
- 이유: Application 계층 변경이 API 스키마에 전파되는 것을 차단

### Self-Audit 체크리스트
코드 변경 후 아래를 점검한다:
- [ ] 새로 추가한 PathVariable에 ApiSpec에서 `@Positive`가 있는가?
- [ ] 새로 추가한 페이지네이션 파라미터에 `@PositiveOrZero`, `@Positive @Max(100)`이 있는가?
- [ ] Controller에 `@Validated`가 클래스 레벨에 있는가?
- [ ] Request DTO의 모든 필드에 적절한 제약 어노테이션이 있는가?
- [ ] 도메인 모델 생성 시 불변식이 검증되는가?
- [ ] ApiSpec 반환 타입에 Application 타입이 직접 노출되지 않는가?

## 페이지네이션 및 정렬 규칙

### 정렬 기준 필수

- 페이지네이션 쿼리에는 **반드시 안정적인 정렬 기준을 명시**한다
- 정렬 기준이 없으면 동시 생성/삭제 시 같은 데이터가 여러 페이지에 나타나거나 누락된다
- 기본 정렬: `Sort.by(Sort.Direction.DESC, "id")` (PK 기준 내림차순)
- 비즈니스 정렬이 필요한 경우에도 마지막에 `id`를 추가하여 안정성 보장

### Infrastructure 계층 구현 패턴

```kotlin
// 올바른 패턴: 정렬 기준 명시
val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))

// 잘못된 패턴: 정렬 기준 없음
val pageable = PageRequest.of(page, size)
```

### 페이지 크기 제한

- API 파라미터 검증: ApiSpec에서 `@Positive @Max(100)` 적용
- 기본값: Controller에서 `@RequestParam(defaultValue = "20")`

### Domain Repository 인터페이스

- `page: Int, size: Int` 파라미터 + `PageResult<T>` 반환
- 정렬 파라미터는 Infrastructure 계층에서 결정 (Domain은 정렬 세부사항을 모른다)
