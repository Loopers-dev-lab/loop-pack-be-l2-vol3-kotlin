# Phase: Review

프로덕션 코드 리뷰 페이즈. commerce-api 4레이어 아키텍처 기준.

> "코드가 말하게 하라. 주석이 필요하다면 코드가 충분히 명확하지 않은 것이다."

## 입력

`$ARGUMENTS`가 주어지면 해당 파일/클래스를 리뷰한다.
주어지지 않으면 `git diff`로 현재 변경사항 전체를 리뷰한다.

## 절차

1. **변경 범위 파악**: 대상 파일을 읽고 변경 의도 파악
2. **레이어 CLAUDE.md 참조**: 변경된 파일이 속한 레이어의 CLAUDE.md를 읽고 규칙 확인
   - `apps/commerce-api/src/main/kotlin/com/loopers/domain/CLAUDE.md`
   - `apps/commerce-api/src/main/kotlin/com/loopers/application/CLAUDE.md`
   - `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/CLAUDE.md`
   - `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/CLAUDE.md`
3. **리뷰 수행**: 체크리스트 기준으로 검토
4. **결과 보고**: 심각도별 분류

## 리뷰 체크리스트

### 단순함 (Simplicity)

- [ ] 가장 단순한 해결책인가? 불필요한 추상화는 없는가?
- [ ] 중복이 없는가? (DRY)
- [ ] 이름이 의도를 명확히 드러내는가?
- [ ] 메서드가 단일 책임인가? 너무 길지 않은가?

### 버그 & 안정성

- [ ] NPE 가능성 (`!!` 사용, nullable 미처리)
- [ ] 동시성 문제 (공유 상태, 스레드 안전성)
- [ ] 리소스 누수 (Connection, Stream 미해제)
- [ ] 엣지 케이스 누락 (빈 리스트, null, 경계값)

### 4레이어 아키텍처 (interfaces → application → domain ← infrastructure)

- [ ] **의존 방향 위반**: domain이 infrastructure/application/interfaces를 참조하지 않는지
- [ ] **Domain에 Spring import 금지**: `org.springframework.*` import가 domain 계층에 없는지
- [ ] **Controller → UseCase 경유**: Controller가 Domain Service를 직접 호출하지 않는지
- [ ] **Controller의 Domain 참조 금지**: Controller가 Domain 객체(Command, VO, Enum, Service)를 직접 참조하지 않는지
- [ ] **Application에서 Infrastructure 구현체 직접 참조 금지**: Repository 인터페이스만 사용
- [ ] **@Transactional**: Application 계층(UseCase)에서만 사용

### 애그리거트 캡슐화

- [ ] **@AggregateRootOnly**: 루트가 아닌 Entity의 상태 변경 메서드에 부착되어 있는지
- [ ] **@OptIn(AggregateRootOnly::class)**: 루트에서만 사용하고, UseCase/외부 Service에서 Opt-In으로 우회하지 않는지

### 데이터 접근 & JPA

- [ ] **JPQL/@Query 금지**: QueryDSL 또는 Spring Data JPA 메서드명 쿼리만 사용
- [ ] **fetch join + paging 동시 사용 금지**: N+1 해결 시 @BatchSize, @EntityGraph, 별도 쿼리 분리 대안 사용
- [ ] **@Transactional 전파**: readOnly 속성 전파 규칙이 올바른지

### 에러 처리

- [ ] **CoreException 단일 클래스**: 개별 Exception 클래스를 새로 만들지 않았는지
- [ ] **ErrorType 사용**: INTERNAL_ERROR, BAD_REQUEST, NOT_FOUND, CONFLICT, UNAUTHORIZED

### 코딩 컨벤션

- [ ] 네이밍 컨벤션 준수 (`*UseCase`, `*Repository`, `*Entity`)
- [ ] `data class` + `val` 불변성 원칙
- [ ] null-safety (`!!` 사용 지양, nullable 처리)

### Tidy First

- [ ] 구조적 변경과 행위적 변경이 섞여 있지 않은지

## 심각도 분류

| 등급 | 의미 | 조치 |
|------|------|------|
| **CRITICAL** | 버그 또는 데이터 손실 위험 | 반드시 수정 |
| **WARNING** | 잠재적 문제 또는 컨벤션 위반 | 수정 권장 |
| **INFO** | 개선 제안 | 선택적 적용 |

## 출력 형식

```
### [CRITICAL] 파일명:라인번호 — 요약
설명과 수정 제안

### [WARNING] 파일명:라인번호 — 요약
설명과 수정 제안

### [INFO] 파일명:라인번호 — 요약
설명과 수정 제안
```

마지막에 전체 요약 (CRITICAL/WARNING/INFO 개수) 포함.

## 다음 페이즈

review 완료 후 `test-review` 페이즈로 진행한다.
