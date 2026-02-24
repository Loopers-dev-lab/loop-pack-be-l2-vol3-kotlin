---
description: Kotlin + Spring + JPA 기술적 주의사항
globs: "**/*.kt"
---

# Kotlin / Spring / JPA 기술 주의사항

## allOpen 플러그인

- Spring Boot 프로젝트에서 `allOpen` 플러그인이 `@Entity`, `@MappedSuperclass`, `@Embeddable` 등에 적용된다
- 이로 인해 Kotlin 클래스가 기본 `final`이 아니게 될 수 있다
- `protected`/`final`/`open` 키워드의 실제 동작은 allOpen 설정에 따라 달라진다
- **주장하기 전에 반드시 `build.gradle.kts`의 allOpen 설정을 확인한다**

## kapt / KSP

- kapt은 Kotlin 2.0+과 호환성 문제가 있다 (현재 프로젝트는 Kotlin 2.0 미만)
- QueryDSL QClass 생성에 kapt이 필요하다
- IDE에서 QClass를 인식하지 못해도 Gradle 빌드는 정상 동작할 수 있다
- IDE 인식 문제 해결: `build/generated/source/kapt`을 소스 디렉토리로 등록

## JPA 쿼리 최적화

- **fetch join + paging은 호환 불가**: `HHH000104` 경고와 함께 메모리에서 페이징 처리됨
- N+1 해결 대안:
  - `@BatchSize` (컬렉션 레벨)
  - `@EntityGraph` (단건 조회)
  - 별도 쿼리 분리 후 애플리케이션에서 조합
- `@Transactional(readOnly = true)` 전파: 하위 메서드에서 쓰기 작업 시 예외 발생

## Kotlin 고유 패턴

- `data class`의 `copy()`는 JPA Entity에 사용하지 않는다 (ID 복제 위험)
- `sealed interface`/`sealed class`는 JPA 상속 매핑에 직접 사용할 수 없다
- `companion object`에 팩토리 메서드를 둘 때 JPA 기본 생성자(`protected constructor`)가 필요하다
