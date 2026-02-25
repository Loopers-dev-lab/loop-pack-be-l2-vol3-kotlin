# 기술 주의사항 (Kotlin / Spring / JPA)

- **allOpen 플러그인**: `plugin.spring`은 `@Component`/`@Service` 등을 open하지만, `plugin.jpa`는 no-arg 생성자만 생성하고 allOpen은 아님. **`@Entity` 클래스는 final**이다. Kotlin의 protected/final 동작에 대해 주장하기 전에 반드시 allOpen 설정(`build.gradle.kts`)과 디컴파일 결과를 확인한다
- **kapt + ktlint 태스크 충돌**: kapt이 생성하는 소스 디렉토리를 ktlint가 참조하면서 Gradle 태스크 순서 충돌(`implicit dependency`)이 발생한다. **`./gradlew ktlintCheck test`를 한 번에 실행하면 실패한다.** 반드시 `ktlintCheck`와 `test`를 분리 실행해야 한다:
  ```bash
  ./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test
  ```
- **JPQL/NativeQuery 금지**: `@Query` 어노테이션을 사용한 JPQL이나 NativeQuery를 제안하지 않는다. QueryDSL(`JPAQueryFactory`) 또는 Spring Data JPA 메서드명 쿼리로 해결한다
- **fetch join + paging 호환 불가**: N+1 문제 해결 시 fetch join과 paging을 동시에 사용하는 방안을 제안하지 않는다. `@BatchSize`, `@EntityGraph`, 별도 쿼리 분리 등 대안을 사용한다
- **@Transactional 전파**: readOnly 속성의 전파 규칙, REQUIRES_NEW의 동작 방식을 정확히 이해하고 적용한다
