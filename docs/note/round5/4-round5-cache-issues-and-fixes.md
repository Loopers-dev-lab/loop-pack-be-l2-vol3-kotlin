# 캐시 설계 리뷰: 발견된 문제와 해결 방향

architect 에이전트로 읽기 최적화 설계를 리뷰한 결과, 핵심 문제 2가지를 발견했다.
각 문제에 대해 대안을 비교하고 선택 근거를 정리한다.

---

## 1. Thundering Herd 방지: @Cacheable sync=true

### 문제

`GetProductsUseCase.kt`의 `@Cacheable`에 `sync` 옵션이 없다.

캐시 미스가 발생하면, 같은 키에 대한 동시 요청 100개가 **전부** DB를 조회한다 (Thundering Herd).
캐시 적중률이 30% 이하로 떨어지면 DB 부하가 급증하고, 커넥션 풀이 고갈된다.

### sync=true가 하는 일

```
# sync 없을 때 (현재)
같은 키로 100개 동시 요청 → 100개 전부 DB 조회 → 100개 전부 Redis에 put

# sync=true
같은 키로 100개 동시 요청 → 1개만 DB 조회 → 결과를 Redis에 put → 99개는 캐시에서 반환
```

Spring Cache의 `sync=true`는 같은 캐시 키에 대해 **JVM 단위 락**을 건다.
하나의 스레드만 캐시 로딩(DB 조회)을 수행하고, 나머지 스레드는 결과가 나올 때까지 대기한다.

### 대안 비교

| 방법                             | 설명                           | 장점                 | 단점                                  |
|--------------------------------|------------------------------|--------------------|-------------------------------------|
| **sync=true**                  | Spring Cache 내장 기능. JVM 단위 락 | 설정 1줄 추가, 즉시 효과    | 분산 환경에서는 서버마다 1개씩 DB 조회 (서버 3대면 3개) |
| Redis 분산 락 (Redisson)          | Redis 기반 글로벌 락               | 서버 N대에서도 1개만 DB 조회 | Redisson 의존성 추가, 락 관리 복잡            |
| 사전 캐시 워밍                       | 배포/TTL 만료 전에 미리 캐시 적재        | 캐시 미스 자체를 방지       | 정렬×페이지×브랜드 조합이 너무 많아 현실적이지 않음       |
| 로컬 캐시 (Caffeine) + Redis 2단 캐시 | 로컬에서 1차 캐시, Redis에서 2차       | 네트워크 비용 절감         | 캐시 일관성 관리 복잡, 무효화 전파 필요             |

### 선택: sync=true

- 설정 1줄(`sync = true`)로 Thundering Herd를 방지한다
- 서버 3대 기준으로 최악의 경우에도 100개 → 3개로 DB 부하를 97% 감소시킨다
- 분산 락(Redisson)은 현재 규모에서 과잉 설계. 서버 수가 수십 대로 늘어날 때 검토한다
- 사전 워밍은 정렬(3) × 페이지(N) × 브랜드(50+) 조합이 너무 다양해서 비현실적이다

### 관련 영향: DB 커넥션 풀 경합 해소

sync=true로 동시 DB 조회가 줄면, 좋아요 비관적 락과 조회 API가 같은 커넥션 풀을 공유하는 문제도 간접적으로 완화된다.
캐시 미스 시 Redis에 대한 put 쓰기도 1개로 줄어들어, Redis master 부하도 감소한다.

---

## 2. 트랜잭션 커밋 전 캐시 갱신: 정합성 붕괴

### 문제

`UpdateProductUseCase`, `AddLikeUseCase`, `RemoveLikeUseCase` 모두 `@Transactional` 내부에서 Redis에 쓰기를 한다.

```
1. DB에 save (아직 커밋 안 됨)
2. Redis에 캐시 갱신 (즉시 반영)
3. 이후 트랜잭션 롤백 발생
→ Redis에는 갱신된 데이터, DB에는 이전 데이터 = 불일치
```

### 대안 비교

| 방법                                            | 설명                      | 장점                            | 단점                                           |
|-----------------------------------------------|-------------------------|-------------------------------|----------------------------------------------|
| **@TransactionalEventListener(AFTER_COMMIT)** | 트랜잭션 커밋 확인 후 이벤트로 캐시 갱신 | Spring 표준, 롤백 시 자동 무시, 관심사 분리 | 이벤트 클래스 추가 필요                                |
| TransactionSynchronizationManager.afterCommit | 수동 콜백 등록                | 이벤트 없이 직접 제어 가능               | 콜백 코드가 UseCase에 섞여 가독성 저하                    |
| REQUIRES_NEW 별도 트랜잭션                          | 캐시 갱신을 새 트랜잭션에서 실행      | 트랜잭션 격리                       | 메인 트랜잭션 롤백돼도 캐시 트랜잭션은 이미 커밋 → **정합성 문제 그대로** |
| 캐시 갱신 재시도 (Outbox 패턴)                         | DB에 이벤트 저장 → 비동기 처리     | 완벽한 정합성                       | 현재 규모에서 과잉, 인프라 복잡도 증가                       |

### 선택: @TransactionalEventListener(AFTER_COMMIT)

- 트랜잭션이 **커밋된 후에만** 이벤트가 발행된다. 롤백 시 이벤트 자체가 발행되지 않는다.
- `REQUIRES_NEW`는 별도 트랜잭션을 열어도 메인 롤백과 무관하게 실행되므로 정합성 문제가 **그대로 남는다**.
- `TransactionSynchronizationManager`도 동일한 효과지만, 콜백 코드가 UseCase 비즈니스 로직에 섞여 가독성이 떨어진다.
- Outbox 패턴은 완벽하지만, 단순 캐시 갱신에 메시지 큐까지 도입하는 건 현재 규모에서 과잉이다.

### 구현: 이벤트 기반 캐시 갱신

UseCase에서 직접 캐시를 조작하는 대신, 도메인 이벤트를 발행하고 리스너가 커밋 후 캐시를 갱신한다.

```
[기존] UseCase → @Transactional 내부 → productCacheRepository.save/evict (커밋 전 실행)
[변경] UseCase → @Transactional 내부 → eventPublisher.publishEvent() → 커밋 후 → Listener → productCacheRepository.save/evict
```
