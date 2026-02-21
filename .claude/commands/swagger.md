API 문서화 작업 시 아래 Swagger 규칙을 적용하세요.

> 참고 구현: `MemberV1Controller.kt`, `MemberV1Dto.kt`

### 1. Controller에 직접 어노테이션

`@Tag`, `@Operation`, `@ApiResponses`를 Controller 클래스에 직접 작성한다. (별도 ApiSpec 인터페이스 분리 없이)

```kotlin
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
```

### 2. @Tag (Controller 클래스 레벨)

```kotlin
@Tag(name = "도메인 V1 API", description = "도메인 관련 API")
@RestController
@RequestMapping("/api/v1/도메인")
class XxxV1Controller(...)
```

### 3. @Operation (각 엔드포인트)

`summary` (한줄 요약) + `description` (상세 설명)을 작성한다.

```kotlin
@Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
```

### 4. @ApiResponses (각 엔드포인트)

가능한 HTTP 응답 코드별로 `responseCode` + `description`을 작성한다.

- **200**: 성공 케이스
- **400**: 잘못된 요청 (구체적 사유를 괄호로 명시)
- **401**: 인증 실패
- **404**: 리소스 없음
- **409**: 충돌 (중복 등)

```kotlin
@ApiResponses(
    value = [
        SwaggerApiResponse(responseCode = "200", description = "회원가입 성공"),
        SwaggerApiResponse(responseCode = "400", description = "잘못된 요청 (비밀번호 8자 미만, 로그인 ID 10자 초과 등)"),
        SwaggerApiResponse(responseCode = "409", description = "이미 존재하는 로그인 ID"),
    ],
)
```

> `io.swagger.v3.oas.annotations.responses.ApiResponse`는 프로젝트의 `ApiResponse`와 이름이 충돌하므로 `SwaggerApiResponse`로 alias하여 사용한다.

### 5. @Schema (DTO 클래스 및 필드)

Request/Response DTO 클래스와 각 필드에 `description` + `example`을 작성한다.

```kotlin
@Schema(description = "회원가입 요청")
data class SignUpRequest(
    @Schema(description = "로그인 ID (최대 10자)", example = "testuser1")
    val loginId: String,
    @Schema(description = "비밀번호 (최소 8자, 영문+숫자+특수문자 포함)", example = "Password1!")
    val password: String,
)
```

### 6. @Parameter (요청 헤더 및 경로 변수)

`@RequestHeader`, `@PathVariable` 파라미터에 `description` + `required`를 작성한다.

```kotlin
@Parameter(description = "로그인 ID", required = true)
@RequestHeader("X-Loopers-LoginId") loginId: String,
```
