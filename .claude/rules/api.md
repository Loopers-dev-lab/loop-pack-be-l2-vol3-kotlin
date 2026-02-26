---
paths:
  - "apps/commerce-api/src/main/kotlin/**/interfaces/api/**/*.kt"
---

# API Layer 규칙

## Controller

- Facade만 호출 (Service 직접 호출 금지)
- `@Valid`로 입력 검증
- `ApiPaths` 상수 사용
- `ApiResponse.success()`로 응답 래핑

```kotlin
@RestController
@RequestMapping(ApiPaths.Users.BASE)
class UserV1Controller(
    private val userFacade: UserFacade,  // Facade만
) {
    @PostMapping
    fun register(@Valid @RequestBody request: RegisterRequest): ApiResponse<UserResponse> {
        val userInfo = userFacade.register(...)
        return ApiResponse.success(UserResponse.from(userInfo))
    }
}
```

## DTO

- `from()` 메서드는 **단순 매핑만** (변환 로직 금지)
- 검증 어노테이션은 Domain 규칙과 일치
- Request DTO에서 Command 변환은 `toCommand()` 메서드 사용

```kotlin
// ✅ 좋음
fun from(info: UserInfo) = UserResponse(
    name = info.name,  // 이미 Facade에서 마스킹됨
)

// ❌ 나쁨
fun from(info: UserInfo) = UserResponse(
    name = MaskingUtils.maskLastCharacter(info.name),  // DTO에서 변환 금지
)
```

### DTO 파일 구성 규칙

Request와 Response는 **항상 별도 파일로 분리**한다.

**파일 네이밍:** ktlint 규칙(단일 클래스 파일은 클래스명 = 파일명)을 따른다.

- **Request**: 여러 클래스를 모으므로 `{Domain}V1Request.kt` (ktlint 통과)
- **Response**: 보통 단일 클래스이므로 클래스명과 동일하게 `{Domain}Response.kt`

```
interfaces/api/{domain}/
    ├── {Domain}Response.kt         ← Response DTO (단일 클래스)
    └── {Domain}V1Controller.kt

interfaces/api/admin/{domain}/
    ├── Admin{Domain}V1Request.kt   ← Request DTO들 (여러 클래스)
    ├── Admin{Domain}Response.kt    ← Response DTO (단일 클래스)
    └── Admin{Domain}V1Controller.kt
```

**클래스 네이밍:** 파일 안에 top-level data class로 선언. wrapper class 없음.

```kotlin
// AdminBrandV1Request.kt (여러 Request 클래스)
data class AdminBrandRegisterRequest(val name: String)
data class AdminBrandUpdateRequest(val name: String)

// AdminBrandResponse.kt (단일 Response 클래스)
data class AdminBrandResponse(val id: Long, val name: String)
```

**Response만 있는 경우** (고객 API 등 Request가 없을 때): Response 파일만 생성.

```
interfaces/api/brand/
    ├── BrandResponse.kt            ← Response만
    └── BrandV1Controller.kt
```

## 인증

- `@CurrentUserId`로 userId(Long) 받음
- Entity가 아닌 ID 전달 (Detached Entity 방지)
