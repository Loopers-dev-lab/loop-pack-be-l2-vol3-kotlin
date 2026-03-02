# 예외 처리 가이드

## 아키텍처 개요

```
ApiControllerAdvice (전역 핸들러)
        ▲
ApiResponse (일관된 응답 포맷)
        ▲
CoreException (모든 도메인이 직접 사용)
        ▲
ErrorCode (CommonErrorCode, UserErrorCode, BrandErrorCode, ...)
```

## ErrorCode 정의

**네이밍:** `{도메인}_{에러유형}` (예: `USER_NOT_FOUND`, `DUPLICATE_LOGIN_ID`)

**코드 분리 기준:**
| 상황 | 결정 |
|------|------|
| 클라이언트 분기 필요 없음 | 코드 통합, 메시지로 구분 |
| 클라이언트가 다른 처리 필요 | 코드 분리 |
| 모니터링/알람 집계 필요 | 코드 분리 |

## 예외 사용 패턴

모든 도메인이 `CoreException(ErrorCode)`를 직접 사용한다. 도메인별 예외 클래스(UserException 등)는 만들지 않는다.

```kotlin
// Domain에서 — 비즈니스 규칙 위반
throw CoreException(UserErrorCode.INVALID_LOGIN_ID_FORMAT)
throw CoreException(ProductErrorCode.INSUFFICIENT_STOCK)

// UseCase에서 — DB 조회 실패, 비즈니스 규칙
val brand = brandRepository.findActiveByIdOrNull(id)
    ?: throw CoreException(BrandErrorCode.BRAND_NOT_FOUND)

if (brandRepository.existsActiveByName(name)) {
    throw CoreException(BrandErrorCode.DUPLICATE_BRAND_NAME)
}
```

## 예외 발생 위치

| 위치 | 예외 종류 | 예시 |
|------|----------|------|
| Domain (Entity/VO) | 도메인 규칙 위반 | 로그인ID 형식 오류, 재고 부족 |
| UseCase | DB 조회 필요한 비즈니스 규칙 | 중복 로그인ID, 엔티티 미존재 |
| ArgumentResolver | 인증 실패 | 헤더 누락, 잘못된 비밀번호 |

## 보안 원칙

**인증 실패 시 상세 원인을 노출하지 않음:**
```kotlin
// ✅ 좋음 - 동일한 에러코드로 통일
throw CoreException(UserErrorCode.AUTHENTICATION_FAILED)  // 사용자 없음
throw CoreException(UserErrorCode.AUTHENTICATION_FAILED)  // 비밀번호 틀림

// ❌ 나쁨 - 사용자 존재 여부 노출
throw CoreException(UserErrorCode.USER_NOT_FOUND)   // 아이디가 없음을 알려줌
throw CoreException(UserErrorCode.WRONG_PASSWORD)    // 아이디는 맞다는 것을 알려줌
```

## 테스트에서 예외 검증

> 참조: `domain/user/UserTest.kt`

```kotlin
val exception = assertThrows<CoreException> {
    User.create(loginId = "test!", ...)  // 특수문자 포함
}
assertThat(exception.errorCode).isEqualTo(UserErrorCode.INVALID_LOGIN_ID_FORMAT)
```

## E2E 테스트에서 에러 응답 검증

```kotlin
assertAll(
    { assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT) },
    { assertThat(response.body?.meta?.errorCode).isEqualTo(UserErrorCode.DUPLICATE_LOGIN_ID.code) },
)
```
