# DIP 강제 전략: 모듈 분리 vs ArchUnit

## 문제 인식

`apps/commerce-api` 안에 `domain`과 `infrastructure`가 같은 Gradle 모듈에 있어서, 컴파일 타임에 `domain → infrastructure` 방향의 잘못된 의존을 막을 수 없는 상태이다. 패키지 컨벤션으로만 DIP를 유지하고 있는 셈이다.

```
apps/commerce-api/  (하나의 Gradle 모듈)
├── domain/         ← infrastructure를 import 안 하고 있지만, 해도 컴파일 통과
├── infrastructure/
├── application/
└── interfaces/
```

## 모듈 분리의 장점

- **컴파일러 수준의 DIP 강제** — domain 모듈이 infrastructure를 물리적으로 참조 불가
- 도메인 순수성이 구조적으로 보장됨
- 의존성 방향 위반 시 빌드가 깨지므로 실수 방지
- 각 레이어의 독립적 테스트, 빌드 가능

## 현재 구조에서의 핵심 충돌

모듈 분리를 시도할 때 한 가지 핵심적인 충돌이 있다:

```kotlin
// domain/user/User.kt
@Entity
@Table(name = "users")
class User(...) : BaseEntity()  // BaseEntity는 modules:jpa에 있음
```

User 엔티티가 이미 `@Entity`, `@Table` 등 JPA 어노테이션에 의존하고 있고, `BaseEntity`도 `modules:jpa`에 있다. 즉, **domain 계층이 이미 JPA에 결합된 상태**이다.

모듈을 분리해도 domain 모듈이 `modules:jpa`에 의존해야 하므로, 진정한 의미의 "순수 도메인 분리"는 달성하기 어렵다.

완전한 분리를 하려면 도메인 객체와 JPA 엔티티를 별도로 두고 매핑해야 하는데, 이는 매핑 보일러플레이트가 상당하다.

| 구분 | 현재 (JPA Entity = Domain Entity) | 완전 분리 (Domain Entity + JPA Entity) |
|------|----------------------------------|---------------------------------------|
| 보일러플레이트 | 없음 | 매핑 코드 필요 |
| 도메인 순수성 | △ JPA 어노테이션 의존 | ✅ 완전 순수 |
| 실용성 | ✅ 높음 | ❌ 오버헤드 큼 |
| 적합 시점 | 현재 규모 | 대규모 프로젝트 |

## 방식 비교

### 방법 1: Gradle 모듈 분리 (물리적 강제)

```
apps/commerce-api/
├── domain/          ← 독립 모듈, infrastructure 의존성 없음
├── infrastructure/  ← domain 모듈만 의존
├── application/     ← domain 의존
└── interfaces/      ← application 의존
```

| 관점 | 평가 |
|------|------|
| DIP 강제력 | ✅ 컴파일 타임에 위반 불가 |
| 도입 비용 | ❌ 높음 (Gradle 설정, 테스트 설정 복잡도 증가) |
| 현재 구조 호환 | ❌ JPA Entity = Domain Entity 구조와 충돌 |
| 모듈 수 증가 | ❌ 현재 8개 → 도메인마다 4개씩 증가 |

### 방법 2: ArchUnit 아키텍처 테스트 (논리적 강제)

```kotlin
@AnalyzeClasses(packages = ["com.loopers"])
class ArchitectureTest {

    @ArchTest
    val `domain은 infrastructure에 의존하지 않는다` = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("..infrastructure..")
}
```

| 관점 | 평가 |
|------|------|
| DIP 강제력 | ✅ 테스트 타임에 위반 감지 (TDD 프로세스에서는 항상 실행) |
| 도입 비용 | ✅ 매우 낮음 (의존성 추가 + 테스트 클래스 하나) |
| 현재 구조 호환 | ✅ 변경 없이 적용 가능 |
| 확장성 | ✅ 레이어 간 규칙을 자유롭게 추가 가능 |

## 결론: ArchUnit 우선 도입, 모듈 분리는 시기를 판단하여 전환

### 현재는 ArchUnit이 적합한 이유

1. **도메인이 아직 1개(user)** — 모듈 분리의 ROI가 낮다
2. **JPA Entity = Domain Entity 구조** — 모듈만 분리하면 "domain 모듈이 JPA에 의존하는데 infrastructure만 못 보는" 어중간한 상태가 된다
3. **TDD 프로세스에서 테스트를 항상 실행** — ArchUnit도 충분한 강제력을 가진다
4. **CLAUDE.md 원칙: "가능한 가장 단순한 해결책"** — 빌드 복잡도 증가 없이 의존성 방향을 검증할 수 있다

### 모듈 분리가 의미 있어지는 시점

- 도메인이 3~4개 이상으로 늘어나고
- 도메인 간 독립 배포/빌드가 필요해지거나
- JPA Entity와 Domain Entity를 분리할 만큼 복잡도가 높아졌을 때

이 시점에서 domain 모듈을 분리하면 ROI가 높아진다.

## ArchUnit 도입 시 검증 가능한 규칙 예시

```kotlin
// 레이어 간 의존성 방향
val `domain은 infrastructure에 의존하지 않는다`
val `domain은 interfaces에 의존하지 않는다`
val `application은 infrastructure에 의존하지 않는다`

// 순환 참조 방지
val `패키지 간 순환 의존이 없다`
```
