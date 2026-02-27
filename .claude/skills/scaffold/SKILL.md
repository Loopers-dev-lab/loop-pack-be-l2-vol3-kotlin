---
name: scaffold
description: 새 도메인 API의 레이어별 보일러플레이트를 생성합니다.
---

새 도메인 API의 레이어별 보일러플레이트를 생성합니다.

사용자에게 도메인 이름과 필요한 엔드포인트를 확인한 후, 아래 구조에 맞게 파일을 생성하세요.

## 생성 파일 목록

도메인 이름을 `Xxx`로 표기합니다.

| 레이어 | 파일 경로 | 어노테이션 |
|--------|----------|-----------|
| Controller | `interfaces/api/{domain}/XxxV1Controller.kt` | `@RestController`, `@Tag` |
| DTO | `interfaces/api/{domain}/XxxV1Dto.kt` | `@Schema` |
| Service | `application/{domain}/XxxService.kt` | `@Component`, `@Transactional` |
| Facade | `application/{domain}/XxxFacade.kt` | `@Component` (2+ 서비스 조합 시에만 생성) |
| Info | `application/{domain}/XxxInfo.kt` | - |
| Repository 인터페이스 | `domain/{domain}/XxxRepository.kt` | 없음 |
| Entity | `domain/{domain}/Xxx.kt` | `@Entity`, `@Table` |
| Command | `domain/{domain}/XxxCommand.kt` | - |
| RepositoryImpl | `infrastructure/{domain}/XxxRepositoryImpl.kt` | `@Component` |
| JpaRepository | `infrastructure/{domain}/XxxJpaRepository.kt` | - |

## 베이스 패키지

```
com.loopers
```

## 패턴 규칙

### Controller
- Swagger 어노테이션을 Controller에 직접 작성 (ApiSpec 인터페이스 분리 없이)
- `io.swagger.v3.oas.annotations.responses.ApiResponse`는 `SwaggerApiResponse`로 alias
- 응답은 `ApiResponse<T>`로 래핑

```kotlin
@Tag(name = "Xxx V1 API", description = "Xxx 관련 API")
@RestController
@RequestMapping("/api/v1/xxxs")
class XxxV1Controller(
    private val xxxFacade: XxxFacade,
) {
    @Operation(summary = "...", description = "...")
    @ApiResponses(value = [...])
    @GetMapping("/{id}")
    fun getXxx(@PathVariable id: Long): ApiResponse<XxxV1Dto.XxxResponse> {
        return xxxFacade.getXxx(id)
            .let { XxxV1Dto.XxxResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
```

### DTO
- `XxxV1Dto` 일반 class로 네임스페이스 역할
- 내부에 Request/Response를 `data class`로 정의
- Request: `toCommand()` 메서드로 domain Command 변환
- Response: `companion object { fun from(info) }` 팩토리 메서드

### Facade
- `@Component`, Service 호출 후 Info 객체로 변환

### Info
- `data class`, `companion object { fun from(entity) }` 팩토리 메서드

### Service
- `@Component`
- 조회: `@Transactional(readOnly = true)`
- 쓰기: `@Transactional`
- 예외: `CoreException(ErrorType.XXX, "메시지")`
- null 처리: Elvis 연산자 `?:` + throw

### Repository
- 인터페이스: `domain/{domain}/` 패키지, 어노테이션 없음
- 구현체: `infrastructure/{domain}/` 패키지, `@Component`
- `findByIdOrNull` (Spring Data Kotlin 확장) 사용

### Entity
- `BaseEntity()` 상속 (id, createdAt, updatedAt, deletedAt 자동 관리)
- 프로퍼티: `protected set`으로 외부 직접 수정 금지
- `init` 블록에서 생성 시점 유효성 검증
- 상태 변경은 도메인 메서드로만 허용
- 상수는 `companion object`에 `private const val`

### Command
- `data class`, 요청 전달 객체
