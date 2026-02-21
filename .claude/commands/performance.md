성능 최적화 작업 시 아래 가이드를 적용하세요.

## JPA/Hibernate
- N+1 문제가 발생할 수 있는 양방향 @OneToOne을 지양한다.
- N+1 문제는 `fetch join`, `@BatchSize`로 해결한다.
- 전체 조회를 지양하고, `Pageable`을 적용하거나 필요한 컬럼만 Projection한다.

## QueryDSL
- 전체 Entity를 조회하지 말고, 필요한 필드만 Projection하여 DTO로 반환한다.

## Stream API
- 동일한 조건으로 스트림을 여러 번 순회하지 말고, 한 번 필터링한 결과를 재사용한다.

## 성능 주의사항
- 조회 메서드에는 `@Transactional(readOnly = true)`를 적용한다.
- Lazy Loading은 트랜잭션 범위 안에서만 접근하여 `LazyInitializationException`을 방지한다.
- 자주 조회하는 컬럼에 인덱스가 존재하는지 확인한다.
