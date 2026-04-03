# [R8] Redis 기반 주문 대기열 시스템

## Summary

블랙프라이데이 같은 트래픽 폭증 시나리오에서 DB를 보호하면서 유저에게 공정한 대기 경험을 제공하기 위한 대기열 시스템을 구현했습니다.

주문 API 앞에 Redis Sorted Set 기반 대기열을 두고, 스케줄러가 DB 처리량에 맞춰 유저를 순차적으로 내보내는 구조입니다. 기존 주문 API와 Round 7 이벤트 파이프라인은 변경하지 않고, 앞단에 대기열 레이어만 추가했습니다.

**구현 내용:**
- `POST /api/v1/queue/enter`: 대기열 진입 (Redis ZADD)
- `GET /api/v1/queue/position`: 순번 조회 + 토큰 수신 여부 확인
- Lua 스크립트 기반 스케줄러: ZPOPMIN + SET을 원자적으로 실행하여 토큰 발급
- `POST /api/v1/orders`: 기존 API에 토큰 검증 인터셉터 추가
- 주문 커밋 후 토큰 삭제 (`@TransactionalEventListener(AFTER_COMMIT)`)
- Redis 장애 시 Graceful Degradation

**전체 흐름:**
```
유저 → 대기열 진입 → polling으로 순번 확인 → 토큰 발급됨
→ POST /orders (토큰 검증 통과) → 주문 처리 → 토큰 삭제
→ ApplicationEvent + Outbox → Kafka (Round 7 파이프라인 그대로)
```

---

## Review Points

### 1. Rate Limiting이 아닌 대기열을 선택한 이유

**문제 상황:**

대기열 없이 주문 API를 직접 호출하는 부하 테스트(시나리오 A)를 돌려봤습니다.

| VU | 에러율 | p50 | p95 | TPS |
|-----|--------|------|------|-----|
| 200 | 0% | 499ms | 1.09s | 169/s |
| 500 | 0% | 1.33s | 2.69s | 181/s |
| 1000 | 0% | 2.96s | 5.75s | 176/s |

에러는 안 나지만, VU를 올려도 TPS는 170~180 근처에서 더 이상 올라가지 않고 응답시간만 길어집니다. 1000 VU에서 p50이 3초라는 건 유저 절반이 3초 이상 기다린다는 뜻이고, 실서비스에서는 타임아웃이나 새로고침이 발생할 구간입니다.

이 상황에서 두 가지 선택지가 있었습니다:
- **Rate Limiting**: 초과 요청을 429로 거부. 서버는 보호되지만 거절당한 유저는 다시 시도할 수밖에 없음
- **대기열**: 순번과 예상 대기 시간을 알려줘서 유저가 자기 차례를 확인할 수 있게 함

대기열을 선택한 이유는 **총 요청량** 때문입니다. Rate Limiting은 거절된 유저가 "혹시 되나?" 하고 계속 재시도하면서 오히려 요청이 늘어납니다(Retry Storm). 반면 대기열은 순번을 보여주니까 유저가 재시도할 이유가 없고, 같은 유저가 반복 요청을 만들지 않기 때문에 시스템에 들어오는 총 요청량이 구조적으로 줄어듭니다.

Rate Limiting은 대기열 앞단에서 max-queue-size(50,000명) 초과 시 봇/비정상 요청을 걸러내는 용도로 조합 사용합니다.

**고민한 점:**
- 대기열을 써도 이탈하는 유저(토큰 만료)는 존재합니다. 이건 Token Expiry Rate를 모니터링해서 TTL을 조정하는 운영 전략으로 대응해야 합니다.
- Rate Limiting만 쓰면 구현이 간단하지만, "503 → 새로고침 → 503 → 새로고침" 루프가 서버 부하를 오히려 키울 수 있습니다. 대기열은 이 루프를 끊어줍니다.

---

### 2. 대기열 저장소로 Redis Sorted Set을 선택한 이유

**문제 상황:**

대기열을 구현하려면 "누가 먼저 왔는지"를 기록하고, "지금 몇 번째인지"를 빠르게 조회할 수 있어야 합니다. 저장소와 자료구조 선택이 필요했습니다.

**저장소 선택: RDB vs Redis**

| 방식 | 장점 | 단점 |
|------|------|------|
| RDB 테이블 | 트랜잭션 보장, 영속성 | 대기열 조회마다 DB 커넥션 사용 → 대기열이 DB를 보호하려는 건데 대기열 자체가 DB를 쓰면 본말전도 |
| Redis | DB 커넥션을 안 씀, 인메모리라 빠름 | 재시작 시 데이터 유실 |

대기열의 존재 이유가 DB 보호인데, 대기열 자체가 DB 커넥션을 소모하면 의미가 없습니다. Redis를 선택했고, 재시작 시 데이터 유실은 유저가 다시 진입하면 되는 수준이라 감수할 수 있다고 판단했습니다.

**자료구조 선택: List vs Set vs Sorted Set**

| 자료구조 | 순서 보장 | 순번 조회 | 중복 방지 | 판단 |
|---------|:---:|:---:|:---:|------|
| List (LPUSH/RPOP) | O | X (LPOS는 O(N)) | X | 순번 조회가 느리고 중복 진입을 막을 수 없음 |
| Set (SADD) | X | X | O | 순서 자체가 없음 |
| **Sorted Set (ZADD)** | **O (score 기반)** | **O (ZRANK → O(logN))** | **O (member 기준)** | 순서 보장 + 빠른 순번 조회 + 중복 방지를 모두 만족 |

Sorted Set은 진입 시간을 score로 사용해서 순서를 보장하고, ZRANK로 순번을 O(logN)에 조회할 수 있고, 같은 userId를 ZADD하면 중복 추가가 안 됩니다. 대기열에 필요한 세 가지 요구사항을 하나의 자료구조로 해결할 수 있었습니다.

**고민한 점:**
- Sorted Set은 메모리를 List보다 더 씁니다(member+score 저장). 하지만 대기열 max-queue-size가 50,000명이고 member가 userId(Long)이라 메모리 부담은 크지 않습니다.
- ZPOPMIN으로 score가 가장 낮은(가장 먼저 진입한) 유저부터 꺼낼 수 있어서, 스케줄러에서 선입선출 순서를 자연스럽게 보장합니다.

---

### 3. 순번 조회를 Polling 방식으로 구현한 이유

**문제 상황:**

유저가 대기열에 진입한 뒤 "내 차례가 왔는지"를 확인하는 방법이 필요합니다. 세 가지 선택지가 있었습니다.

| 방식 | 동작 | 장점 | 단점 |
|------|------|------|------|
| **Polling** | 클라이언트가 주기적으로 GET 요청 | 구현이 단순, 서버가 상태를 관리할 필요 없음 | 불필요한 요청 발생, 실시간성이 떨어짐 |
| **WebSocket** | 서버가 토큰 발급 시 클라이언트에 push | 실시간 알림 | 서버가 수만 개의 커넥션을 유지해야 함, 연결 끊김 처리 복잡 |
| **SSE (Server-Sent Events)** | 서버→클라이언트 단방향 push | WebSocket보다 가벼움, HTTP 기반 | 서버가 커넥션을 유지해야 하는 건 동일 |

**Polling을 선택한 이유:**

WebSocket이나 SSE는 서버가 클라이언트별로 커넥션을 열어두고 유지해야 합니다. 대기열에 1만 명이 있으면 1만 개의 커넥션을 동시에 관리해야 하는데, 이건 대기열이 해결하려는 문제(서버 리소스 보호)와 모순됩니다. 대기열 때문에 서버 리소스가 더 필요해지면 안 됩니다.

Polling의 단점인 "불필요한 요청"은 동적 polling 주기로 완화했습니다:
- 순번 1~100 (곧 차례): 1초마다 → 빠른 반응
- 순번 101~1000: 3초마다 → 적당한 빈도
- 순번 1001 이상 (한참 남음): 5초마다 → 불필요한 요청 최소화

앞쪽 유저는 자주 확인하고, 뒤쪽 유저는 덜 확인하게 해서 Redis 부하를 줄입니다. 이 주기는 서버가 `suggestedPollIntervalMs`로 응답에 포함해서 클라이언트에게 알려줍니다.

**고민한 점:**
- Polling 주기를 클라이언트가 무시하고 더 자주 요청하면? 현재는 강제하지 않지만, 필요하면 Rate Limiting을 걸 수 있습니다. 다만 순번 조회는 Redis만 쓰고 DB를 안 쓰니까 부하가 크지 않아서, 당장은 서버가 제안하는 수준으로 충분하다고 판단했습니다.
- WebSocket이 더 적합해지는 시점도 있습니다. 대기열 규모가 수십만 명이고, polling 요청 자체가 Redis에 부담이 될 때는 WebSocket으로 전환을 고려해야 합니다. 현재 max-queue-size(50,000명) 규모에서는 Polling이 더 단순하고 안정적입니다.

---

### 4. Lua 스크립트로 ZPOPMIN + 토큰 발급을 원자적으로 실행

**문제 상황:**

스케줄러가 대기열에서 N명을 꺼내고 각각 토큰을 발급하는 과정을 개별 Redis 명령으로 처리하면 두 가지 문제가 있습니다:
1. ZPOPMIN 1회 + SET N회 = (N+1)회의 네트워크 왕복
2. ZPOPMIN과 SET 사이에 다른 명령이 끼어들 수 있어 원자성 미보장

**해결:**

Lua 스크립트로 ZPOPMIN + SET을 Redis 내부에서 한 번에 실행합니다.

```lua
local members = redis.call('ZPOPMIN', queueKey, batchSize)
for i = 1, #members, 2 do
    local userId = members[i]
    redis.call('SET', tokenPrefix .. userId, 'GRANTED', 'EX', ttl, 'NX')
end
```

Redis는 싱글 스레드라 Lua 스크립트 실행 중에는 다른 명령이 끼어들 수 없습니다. 네트워크 왕복도 1회로 줄어듭니다.

**SET에 NX 옵션을 쓴 이유:**

이전 스케줄러 실행에서 토큰을 받았지만 아직 사용하지 않은 유저가 다시 대기열에 들어올 수 있습니다. 이때 기존 토큰을 덮어쓰면 TTL이 리셋되는데, NX를 쓰면 기존 토큰이 있을 때는 건드리지 않습니다.

**고민한 점:**
- Lua 스크립트가 길어지면 Redis 싱글 스레드를 오래 점유해서 다른 명령이 밀릴 수 있습니다. batch-size가 5명이니까 실행 시간은 μs 단위로, 실무에서 문제가 되지 않을 수준입니다.
- Lua 스크립트는 디버깅이 어렵습니다. 이걸 보완하기 위해 스케줄러 실행 로그에 발급 수, 대기열 크기를 기록해서 스크립트의 동작을 간접적으로 검증할 수 있게 했습니다.

---

### 5. 처리량 기반 스케줄러 배치 크기 산정

**문제 상황:**

대기열에서 유저를 꺼내는 속도가 너무 빠르면 DB가 과부하되고, 너무 느리면 유저 대기 시간이 불필요하게 길어집니다. "감"이 아니라 "데이터"로 산정해야 했습니다.

**산정 과정:**

시나리오 A에서 주문 1건 처리 시간을 실측했습니다.

```
실측 주문 처리 시간: avg=543ms (200 VU 기준)
DB 커넥션 풀: 40개
이론적 최대 TPS: 40 / 0.543 = 73.7
안전 마진 70%: 73.7 × 0.7 = 51.6 TPS
스케줄러 100ms 주기 (1초에 10회): 51.6 / 10 ≈ 5
```

초기에 18로 잡았다가, 실측 후 5로 하향 조정했습니다. 18이면 초당 180명이 DB로 몰리는데 실제로 51 TPS만 감당 가능하니까 과부하가 걸립니다.

**안전 마진을 70%로 잡은 이유:**

주문 외에도 상품 조회, 좋아요 등 다른 API가 같은 커넥션 풀을 공유합니다. 주문이 커넥션을 전부 차지하면 다른 API가 멈추니까, 30% 여유를 남겨뒀습니다.

**스케줄러 주기를 1초가 아닌 100ms로 잡은 이유:**

1초 주기로 50명을 한꺼번에 풀면 50명이 동시에 주문 API를 호출합니다(Thundering Herd). 100ms 주기로 5명씩 나눠서 풀면 DB 입장에서 순간적으로 몰리는 요청이 50건에서 5건으로 줄어듭니다. 초당 처리량은 동일하지만, 한 번에 몰리는 건수가 줄어서 DB 커넥션 경합이 완화됩니다.

**고민한 점:**
- batch-size는 현재 DB 처리 성능에 맞춘 값이라, DB 스케일업이나 주문 로직 최적화로 TPS가 올라가면 비례해서 상향 조정하면 됩니다.
- `QueueProperties`로 외부화해뒀기 때문에 재배포 없이 변경 가능합니다.

---

### 6. 토큰 검증을 인터셉터로 구현한 이유

**문제 상황:**

토큰 검증 로직을 어디에 둘지 네 가지 선택지가 있었습니다.

| 방식 | 장점 | 단점 |
|------|------|------|
| **Servlet Filter** | 가장 앞단에서 차단, Spring 컨텍스트 밖 | Spring Bean 주입이 자연스럽지 않음, `@CurrentUserId` 같은 Spring MVC 기능과 연계 어려움 |
| **HandlerInterceptor** | Spring Bean 주입 가능, 경로 패턴으로 적용 대상 지정 | ArgumentResolver보다 먼저 실행되어 userId를 직접 얻어야 함 |
| **AOP (@Aspect)** | 메서드 레벨 세밀한 제어, `@CurrentUserId`로 이미 주입된 userId 사용 가능 | 이 프로젝트에서 AOP를 인증/인가에 쓰는 패턴이 없음, 설정이 복잡 |
| **UseCase 내부 검증** | 별도 인프라 없이 구현 가능 | 주문 로직에 대기열 의존성이 생김, `CreateOrderUseCase`가 대기열을 알아야 함 |

**인터셉터를 선택한 이유:**

1. **Filter vs Interceptor**: Filter는 Spring 컨텍스트 밖에서 동작하니까 `OrderQueueStore`(Spring Bean) 주입이 깔끔하지 않습니다. 인터셉터는 Spring 컨텍스트 안에서 동작해서 Bean 주입이 자연스럽고, `WebMvcConfig`에서 `addPathPatterns`로 적용할 경로를 지정할 수 있습니다.

2. **UseCase 내부 검증을 피한 이유**: `CreateOrderUseCase`에 토큰 검증을 넣으면 주문 로직이 대기열을 알게 됩니다. 대기열은 주문의 "비즈니스 규칙"이 아니라 "인프라 차원의 트래픽 제어"이기 때문에, 주문 로직과 분리하는 게 맞다고 판단했습니다. `queue.enabled=false`로 끄면 인터셉터만 비활성화되고 주문 로직은 전혀 영향 없습니다.

3. **AOP를 쓰지 않은 이유**: AOP로 하면 `@CurrentUserId`가 이미 주입된 시점에서 userId를 쓸 수 있어서 편하지만, 이 프로젝트에서 인증/인가 목적의 AOP 패턴이 없습니다. 기존에 `WebMvcConfig`에서 ArgumentResolver를 등록하는 패턴이 이미 있으니까, 같은 곳에서 인터셉터를 등록하는 게 일관성 있습니다.

**인터셉터에서의 이중 인증 문제:**

인터셉터는 ArgumentResolver보다 먼저 실행되기 때문에 `userId`를 모릅니다. 인터셉터에서 `AuthenticateUserUseCase`를 호출하면 인증이 두 번 발생합니다:
1. 인터셉터에서 1회 (토큰 확인용)
2. ArgumentResolver에서 1회 (컨트롤러 파라미터 주입용)

이걸 해결하기 위해 인터셉터에서 인증한 `userId`를 request attribute에 저장하고, ArgumentResolver가 이걸 먼저 확인하도록 했습니다.

```kotlin
// EntryTokenInterceptor
request.setAttribute("resolvedUserId", userId)

// CurrentUserIdArgumentResolver
val cachedUserId = (webRequest as? ServletWebRequest)
    ?.request?.getAttribute("resolvedUserId") as? Long
if (cachedUserId != null) return cachedUserId
```

기존 `CurrentUserIdArgumentResolver`에 3줄만 추가했습니다. 대기열이 비활성화되면(`enabled=false`) 인터셉터가 동작하지 않으니까 기존 인증 흐름 그대로 돌아갑니다.

---

### 7. 토큰 삭제 시점: 주문 트랜잭션 커밋 후

**문제 상황:**

토큰을 언제 삭제하느냐에 따라 유저 경험이 달라집니다. 세 가지 시점을 고려했습니다:

| 시점 | 문제 |
|------|------|
| 주문 처리 시작 시 | 주문이 롤백되면 토큰도 없어져서 유저가 다시 줄 서야 함 |
| 주문 트랜잭션 커밋 후 | 커밋 확정 후 삭제하니까 안전 |
| 별도 스케줄러 | 복잡도만 증가 |

**선택: 커밋 후 삭제 (`@TransactionalEventListener(AFTER_COMMIT)`)**

기존 `UserActionEventListener`, `PaymentEventListener`와 동일한 패턴입니다. `OrderCreatedEvent`를 수신하는 `QueueTokenEventListener`를 추가했습니다.

```kotlin
@Async("eventExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun handleOrderCreated(event: OrderCreatedEvent) {
    orderQueueStore.deleteToken(event.userId)
}
```

별도의 이벤트 타입을 만들지 않고 기존 `OrderCreatedEvent`를 그대로 수신합니다. 토큰 삭제에 필요한 `userId`가 이미 이 이벤트에 들어있고, 대기열 전용 이벤트를 추가하면 `CreateOrderUseCase`가 대기열의 존재를 알아야 하는 구조가 되기 때문입니다.

**AFTER_COMMIT에서 토큰 삭제가 실패하면?**

토큰에 TTL(5분)이 있으므로 최악의 경우에도 5분 후에 자동으로 정리됩니다. 이 5분 동안 같은 유저가 다시 주문하는 건 주문 로직 자체의 중복 방지로 막습니다.

**결제 실패 시에는 토큰을 유지합니다.**

대기열에서 기다린 유저가 결제 오류 한 번으로 다시 줄을 서야 한다면 이탈률이 높아집니다. 토큰을 유지해서 TTL(5분) 내에 결제를 재시도할 수 있도록 했습니다.

---

### 8. Graceful Degradation: Redis 장애 시 대응 전략

**문제 상황:**

대기열이 Redis에 전적으로 의존하고 있기 때문에, Redis가 죽으면 진입, 순번 조회, 토큰 검증이 전부 불가능해집니다. 이에 대비해 각 기능별로 Redis 장애 시 동작을 사전에 정의하고 구현해뒀습니다.

**선택한 전략:**

| 기능 | Redis 장애 시 | 이유 |
|------|-------------|------|
| 대기열 진입 | 503 응답 | 대기열 없이 주문이 몰리면 DB 과부하 |
| 순번 조회 | 503 응답 | 순번 정보가 Redis에만 존재 |
| 토큰 검증 | **통과 (fail-open)** | 아래 설명 |

**토큰 검증에서 fail-open을 선택한 이유:**

이 판단이 가장 어려웠습니다.

- **차단하면**: Redis 장애 동안 아무도 주문 불가. 이미 토큰을 받은 정상 유저도 피해를 봄 → 매출 손실
- **통과시키면**: 대기열이 무력화되어 주문이 몰릴 수 있음 → DB 부하

통과시키는 쪽을 선택한 근거는:
1. 대기열은 보안 경계가 아니라 부하 제어 장치. 토큰이 없어도 주문 자체가 위험한 건 아님
2. 대기열이 무력화되어 요청이 몰려도 HikariCP가 DB를 보호함. 커넥션 풀(40개)이 가득 차면 추가 요청은 대기하다가 connection-timeout(3s) 후 빠르게 실패 처리됨. DB가 완전히 무너지지는 않음
3. Redis 장애는 보통 수 분 내에 복구됨. 그 짧은 시간 동안 전면 차단은 과한 대응

`QueueProperties.enabled` 플래그를 두어서, 장애 상황에서 대기열 전체를 즉시 비활성화할 수 있는 킬 스위치도 마련했습니다.

**Redis 재시작 후 대기열 상태는?**

Redis가 재시작되면 Sorted Set 데이터가 사라지니까, 유저가 다시 대기열에 진입해야 합니다. 프론트에서 503 응답 시 재진입 로직으로 대응합니다.

---

### 9. 부하 테스트에서 발견한 한계와 개선 방향

**발견한 사실:**

대기열 API 자체는 Redis만 사용하니까 71ms 수준으로 빠릅니다(시나리오 D, 200 VU). 그런데 VU를 1000으로 올리면 3.76s까지 느려집니다(시나리오 B).

원인은 대기열 API가 아니라 **인증 과정**입니다. `@CurrentUserId`가 매 요청마다 DB에서 유저를 조회하기 때문에, Tomcat 스레드 200개가 전부 DB 응답을 기다리면서 밀리는 겁니다.

```
대기열 API 자체 (Redis): 71ms
인증 포함 (DB SELECT): 3.76s
→ 병목은 Redis가 아니라 인증 DB 조회
```

**개선 선택지 검토:**

| 방법 | 효과 | 판단 |
|------|------|------|
| JWT 전환 | 인증에서 DB 제거 → 대기열 API에서 DB 의존 완전 제거 | 근본 해결이지만 인증 구조 전면 변경 필요 |
| 가상 스레드 | 스레드 블로킹 완화 | 스레드 병목이 커넥션 풀 병목으로 이동할 뿐, 근본 해결 아님 |
| 인증 결과 캐시 | DB 조회를 Redis 조회로 대체 | 비밀번호 변경 시 캐시 무효화 복잡 |

가상 스레드(Java 21)를 검토했지만, 현재 상황에서는 효과가 제한적이라 적용하지 않았습니다. 가상 스레드가 스레드 블로킹을 풀어줘도 DB 커넥션 풀(40개)이 새로운 병목이 되기 때문입니다. 가상 스레드는 JWT 전환 후(인증에서 DB를 안 쓸 때) 적용하면 수만 명 동시 처리가 가능해져서 제대로 효과를 볼 수 있습니다.

현재 인증 구조에서는 대기열의 DB 보호 효과가 **주문 API에 한정**됩니다. 대기열 진입/polling API의 성능 개선은 인증 구조 전환(JWT)이 선행되어야 합니다.

---

### 10. Round 7과의 연결

대기열은 주문 API "앞단의 관문"이고, 주문 이후의 흐름은 Round 7에서 구축한 구조를 그대로 사용합니다.

```
[대기열 통과] → POST /orders (토큰 검증)
  → 주문 로직 (재고, 포인트, 쿠폰, 결제) → @Transactional
  → 토큰 삭제 → @TransactionalEventListener(AFTER_COMMIT)
  → 부가 로직 (로깅, 알림) → ApplicationEvent
  → 시스템 간 전파 → Kafka (Outbox Pattern)
```

Round 7 코드에 대한 변경은 없습니다. 기존 주문 E2E 테스트도 그대로 통과합니다(대기열 비활성화 시).

---

## 부하 테스트 결과

### 시나리오 A: 비교군 (대기열 없이 주문 직접 호출)

| VU | 에러율 | p50 | p95 | p99 | TPS |
|-----|--------|------|------|------|-----|
| 200 | 0% | 499ms | 1.09s | 1.24s | 169/s |
| 500 | 0% | 1.33s | 2.69s | 2.84s | 181/s |
| 1000 | 0% | 2.96s | 5.75s | 5.94s | 176/s |

- TPS는 170~180에서 포화. VU를 올려도 처리량은 안 늘고 응답시간만 길어짐
- 500 VU부터 p50이 1초를 넘기면서 임계값 초과

### 시나리오 B: 대기열 진입 (1000 VU)

| 항목 | 값 |
|------|-----|
| 에러율 | 0% |
| p50 | 3.76s |
| p95 | 4.97s |

- 인증 DB 조회가 병목 (Redis 자체는 빠름)

### 시나리오 C: Polling (1000 VU, 2초 간격)

| 항목 | 값 |
|------|-----|
| 에러율 | 0% |
| p50 | 3.26s |
| p95 | 3.59s |

- 시나리오 B와 같은 원인 (인증 DB 조회)

### 시나리오 D: 통합 스파이크 (200 VU)

| 항목 | 값 |
|------|-----|
| 대기열 진입 p50 | 71ms |
| polling p50 | 71ms |
| 에러율 | 0% |

- VU가 낮을 때 대기열 API 자체는 71ms로 Redis 성능 그대로 나옴
- 대기열의 DB 보호 효과 확인: 주문은 batch-size(5)에 맞춰 순차 처리

### batch-size 재조정

```
초기 추정: 주문 200ms 가정 → batch-size = 18
실측 결과: 주문 543ms 측정 → batch-size = 5
```

---

## 패키지 구조

기존 프로젝트의 4계층 구조(interfaces/application/domain/infrastructure)를 그대로 따랐습니다. `domain/coupon/CouponCounterStore` → `infrastructure/coupon/CouponRedisCounterStore` 패턴과 동일하게 `OrderQueueStore` 인터페이스를 domain에, Redis 구현체를 infrastructure에 배치했습니다.

```
support/error/QueueErrorCode.kt              ← ErrorCode enum
application/queue/QueueProperties.kt          ← 설정값 외부화
application/queue/EnterQueueUseCase.kt        ← 대기열 진입
application/queue/GetQueuePositionUseCase.kt  ← 순번 조회
application/queue/QueueInfo.kt                ← 출력 DTO
application/queue/QueueTokenScheduler.kt      ← 스케줄러
application/queue/QueueTokenEventListener.kt  ← 토큰 삭제 리스너
domain/queue/OrderQueueStore.kt               ← 포트 인터페이스
infrastructure/queue/OrderQueueRedisStore.kt  ← Redis 구현체
interfaces/api/queue/QueueV1Controller.kt     ← 컨트롤러
interfaces/api/queue/QueueEntryResponse.kt    ← 응답 DTO
interfaces/api/queue/QueuePositionResponse.kt ← 응답 DTO
interfaces/api/queue/EntryTokenInterceptor.kt ← 토큰 검증
resources/redis/queue-token-issue.lua         ← Lua 스크립트
```

---

## 수정한 기존 파일

| 파일 | 변경 내용 |
|------|----------|
| `application.yml` | `queue:` 설정 섹션 추가 |
| `ApiPaths.kt` | `Queue` object 추가 |
| `WebMvcConfig.kt` | `EntryTokenInterceptor` 등록 |
| `CurrentUserIdArgumentResolver.kt` | request attribute 캐시 확인 (3줄) |

기존 주문 API, `CreateOrderUseCase`, Round 7 이벤트 파이프라인은 변경 없음.

---

## 테스트

### 단위 테스트 (MockK)
- `EnterQueueUseCaseTest`: 정상 진입, 중복 진입, 토큰 보유 유저, 대기열 초과, Redis 장애
- `GetQueuePositionUseCaseTest`: READY/WAITING/NOT_IN_QUEUE 상태, polling 주기 검증, Redis 장애
- `QueueTokenSchedulerTest`: 정상 발급, 빈 대기열, 예외 처리, 비활성화
- `EntryTokenInterceptorTest`: 토큰 유/무, GET 통과, 인증 실패 통과, fail-open, 비활성화
- `QueueGracefulDegradationTest`: 진입 503, 조회 503, 토큰 검증 fail-open, 스케줄러 예외 처리

### 통합 테스트
- `QueueConcurrencyTest`: 1000명 동시 진입 순서 보장, 동일 유저 중복 방지

### 부하 테스트 (k6)
- 시나리오 A~E: 5개 스크립트 (`k6/queue-scenario-*.js`)
