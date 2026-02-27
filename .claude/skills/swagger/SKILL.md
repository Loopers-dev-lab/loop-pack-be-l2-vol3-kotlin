---
name: swagger
description: API 문서화 작업 시 아래 Swagger 규칙을 적용하세요.
---

API 문서화 작업 시 아래 Swagger 규칙을 적용하세요.

> 참고 구현: `ExampleV1ApiSpec.kt`, `ExampleV1Controller.kt`, `ExampleV1Dto.kt`

### 1. ApiSpec 인터페이스에 Swagger 어노테이션 분리

Swagger 어노테이션(`@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`)은 **ApiSpec 인터페이스**에 작성하고, Controller는 Spring 어노테이션(`@GetMapping`, `@PathVariable` 등)만 작성한다.

```kotlin
// ApiSpec 인터페이스 — Swagger 어노테이션 담당
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

// Controller — Spring 어노테이션만
@RestController
class XxxV1Controller(...) : XxxV1ApiSpec { ... }
```

### 2. @Tag (ApiSpec 인터페이스 레벨)

```kotlin
@Tag(name = "Brand V1 API", description = "대고객 브랜드 API")
interface BrandV1ApiSpec { ... }

@Tag(name = "Brand Admin V1 API", description = "어드민 브랜드 API")
interface BrandAdminV1ApiSpec { ... }
```

### 3. @Operation (각 엔드포인트)

`summary` (한줄 요약) + `description` (상세 설명)을 작성한다.

```kotlin
@Operation(summary = "브랜드 조회", description = "브랜드 ID로 브랜드를 조회합니다.")
fun getBrand(brandId: Long): ApiResponse<BrandV1Dto.BrandResponse>
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
        SwaggerApiResponse(responseCode = "200", description = "브랜드 등록 성공"),
        SwaggerApiResponse(responseCode = "400", description = "잘못된 요청 (브랜드명 빈 값)"),
        SwaggerApiResponse(responseCode = "409", description = "이미 존재하는 브랜드명"),
    ],
)
```

> `io.swagger.v3.oas.annotations.responses.ApiResponse`는 프로젝트의 `ApiResponse`와 이름이 충돌하므로 `SwaggerApiResponse`로 alias하여 사용한다.

### 5. @Schema (DTO 클래스 및 필드)

Request/Response DTO 클래스와 각 필드에 `description` + `example`을 작성한다.

```kotlin
@Schema(description = "브랜드 등록 요청")
data class CreateRequest(
    @Schema(description = "브랜드명", example = "나이키")
    val name: String,
    @Schema(description = "브랜드 설명", example = "스포츠 브랜드")
    val description: String?,
)
```

### 6. @Parameter (요청 헤더 및 경로 변수)

ApiSpec 인터페이스의 메서드 파라미터에 `@Parameter`를 작성한다.

```kotlin
// ApiSpec 인터페이스에서
fun getBrand(
    @Parameter(description = "브랜드 ID", required = true)
    brandId: Long,
): ApiResponse<BrandV1Dto.BrandResponse>
```

### 7. 인증 헤더 문서화

인증이 필요한 API는 ApiSpec에 헤더 파라미터를 명시한다.

```kotlin
// 대고객 인증
fun getMyInfo(
    @Parameter(description = "로그인 ID", required = true)
    loginId: String,
    @Parameter(description = "비밀번호", required = true)
    password: String,
): ApiResponse<UserV1Dto.UserInfoResponse>

// 어드민 인증
fun createBrand(
    @Parameter(description = "어드민 LDAP 인증", required = true)
    ldap: String,
    request: BrandV1Dto.CreateRequest,
): ApiResponse<BrandV1Dto.BrandResponse>
```
