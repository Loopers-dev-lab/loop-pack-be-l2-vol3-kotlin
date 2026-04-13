# Round 9 — 기술 의사결정 기록

실시간 랭킹 시스템 구현 및 테스트 안정화 과정에서 만난 이슈와 해결 방식을 기록한다.

---

## 1. Spring Test Context Caching과 @Scheduled 간섭

### 문제

`QueueConcurrencyTest`에서 100명 동시 진입 후 `findPosition()`이 일부 유저에 대해 null을 반환한다. `hasSize(100)`, `count() == 100`까지는 통과하지만, 직후 `findPosition()` 호출 시 일부가 null.

### 원인

`@EnableScheduling`이 `CommerceApiApplication`에 선언되어 있어, `@SpringBootTest`로 생성되는 **모든 test context에서 real scheduler가 활성화**된다.

- `QueueScheduler`(100ms 주기)가 `IssueEntryTokensUseCase.execute()` → `ZPOPMIN`으로 대기열 소비
- `QueueConcurrencyTest`에서 `@MockitoBean`으로 scheduler를 mock해도, **다른 캐시된 context의 real scheduler는 여전히 동작**
- 공유 Redis(TestContainers)를 통해 다른 context의 scheduler가 대기열에서 유저를 제거
- `count() == 100` 확인 직후 ~ `findPosition()` 호출 사이(100ms 이내)에 일부 유저가 빠짐

### 해결방안

| 방안 | 설명 | 장점 | 단점 |
|------|------|------|------|
| A. `queue.scheduler-delay-ms` 증대 | test profile에서 delay를 매우 큰 값으로 설정 | 간단 | 의도가 코드에 드러나지 않는 workaround |
| B. `@DirtiesContext` 추가 | 테스트 전후로 context 재생성 | 확실한 격리 | context 재생성 비용이 큼 |
| C. scheduling 조건부 활성화 | `@ConditionalOnProperty`로 test에서 비활성화 | 근본 해결, 명시적 | Application 클래스 수정 필요 |

### 최종 선택

**C. scheduling 조건부 활성화**

```kotlin
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    prefix = "app.scheduling",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class SchedulingConfig
```

- `CommerceApiApplication`에서 `@EnableScheduling` 제거
- test profile에서 `app.scheduling.enabled: false`

### 근거

- A는 "999999ms로 사실상 비활성화"라는 의도가 코드에 드러나지 않음
- B는 24개 `@SpringBootTest` 클래스가 있는 상황에서 context 재생성 비용이 큼
- C는 `matchIfMissing = true`로 기존 환경(local/dev/prod)에 영향 없이, test에서만 명시적으로 비활성화
- scheduler 단위 테스트(`QueueSchedulerTest` 등)는 `@SpringBootTest` 없이 직접 생성하므로 영향 없음
- `app.scheduling.enabled`라는 커스텀 property 이름을 사용하여 Spring 표준 속성과 혼동 방지

---

## 2. Kafka Consumer payload 역직렬화 mismatch

### 문제

`CatalogEventIntegrationTest`에서 Kafka 메시지 발행 후 Redis ZSET 점수가 30초 내에 반영되지 않는다. `awaitRedisScore()`에서 actual이 항상 null.

### 원인

consumer 처리 중 예외 발생 → `DefaultErrorHandler` 재시도 소진 → DLT 전송 시도 → DLT 토픽 미존재 → 메시지 유실.

근본 원인은 **listener 시그니처와 deserializer의 mismatch**:

1. `kafka.yml`에서 consumer `value-deserializer = ByteArrayDeserializer`
2. `KafkaConfig.RECORD_LISTENER` factory에 `setRecordMessageConverter(ByteArrayJsonMessageConverter)` 설정
3. listener 파라미터가 `ConsumerRecord<Any, Any>`이면 Spring Kafka는 **raw record를 그대로 전달** (converter 미적용)
4. `message.value()` = `byte[]` → `as? Map<*, *>` = null → `CoreException("페이로드 파싱 실패")`
5. 재시도 소진 → DLT → Redis 미반영

### 해결방안

| 방안 | 설명 | 장점 | 단점 |
|------|------|------|------|
| A. listener 시그니처를 `Map<String, Any?>` 변경 | converter가 자동 적용 | 간단 | 타입 안전성 부족, 수동 필드 추출 유지 |
| B. listener 시그니처를 DTO 기반 변경 | `CatalogEventPayload` 등 전용 DTO | 타입 안전, 가독성, Jackson 검증 | DTO 클래스 추가 필요 |
| C. consumer에서 byte[] 수동 역직렬화 | `ConsumerRecord` 유지 + ObjectMapper 직접 사용 | 기존 구조 유지 | boilerplate 증가, 프레임워크 활용 못함 |

### 최종 선택

**B. listener 시그니처를 DTO 기반으로 변경**

```kotlin
// Before
fun consume(message: ConsumerRecord<Any, Any>) {
    val payload = message.value() as? Map<*, *> ?: throw ...
    val eventId = payload["eventId"] as? String ?: throw ...
}

// After
data class CatalogEventPayload(
    val eventId: String,
    val eventType: String,
    val productId: Long,
)

fun consume(payload: CatalogEventPayload) {
    updateProductMetricsUseCase.handleCatalogEvent(
        eventId = payload.eventId, ...
    )
}
```

### 근거

- A는 `Map` 기반이라 런타임 cast 오류 가능성이 여전히 존재
- C는 `ByteArrayJsonMessageConverter`를 factory에 설정해놓고 활용하지 않는 모순
- B는 `ByteArrayJsonMessageConverter`가 method parameter의 target type으로 Jackson 역직렬화하므로, `spring.json.add.type.headers: false`여도 정상 동작
- null/누락 필드는 Jackson이 역직렬화 시점에 거부 → 비즈니스 로직과 역직렬화 오류가 명확히 분리
- `CatalogEventConsumer`, `OrderEventConsumer`, `CouponIssueConsumer` 3개 모두 동일 패턴 적용

---

## 3. EmbeddedKafka 테스트 안정화

### 문제

`CatalogEventIntegrationTest`에서 consumer가 partition assignment를 받기 전에 메시지가 발행되면, `auto.offset.reset` 설정에 따라 메시지를 놓칠 수 있다.

### 원인

두 가지 요인의 복합:

1. **listener assignment 대기 누락**: `KafkaListenerEndpointRegistry` + `ContainerTestUtils.waitForAssignment()` 없이 바로 메시지 발행
2. **consumer offset 설정 경로**: `kafka.yml`에서 `spring.kafka.properties.auto.offset.reset: latest`가 nested YAML 구조로 정의되어 있어, `@TestPropertySource`의 flat property override가 의도대로 적용되지 않을 가능성

### 해결방안

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| assignment 대기 | 없음 | `ContainerTestUtils.waitForAssignment(container, 1)` |
| offset reset | `spring.kafka.properties.auto.offset.reset=earliest` | `spring.kafka.consumer.auto-offset-reset=earliest` (typed property) |
| consumer group | 공유 group-id | `catalog-event-integration-test` 전용 group-id |

### 근거

- `waitForAssignment()`로 partition assignment 완료를 보장한 후 메시지 발행
- `spring.kafka.consumer.auto-offset-reset`은 Spring Boot의 typed property 경로로, raw Kafka properties map의 YAML 바인딩 문제를 우회
- 전용 group-id로 다른 테스트의 committed offset 영향을 차단
