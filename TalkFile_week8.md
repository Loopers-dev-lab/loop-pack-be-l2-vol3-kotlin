## 📌 Summary
API 서비스(`commerce-api`)와 독립된 집계 서비스(`commerce-streamer`)를 추가하고, 두 서비스 간 느슨한 연결을 Kafka 기반 이벤트 파이프라인으로 구현했습니다.

**주요 구현 내용:**
- **Producer (commerce-api)**: Transactional Outbox Pattern을 통한 이벤트 발행 (도메인 트랜잭션과 동일 트랜잭션에서 Outbox 저장, 별도 스케줄러가 Kafka로 발행)
- **Consumer (commerce-streamer)**: 이벤트 수취 및 상품 메트릭 집계 (좋아요 수, 판매량, 조회 수)
- **멱등성 보장**: `event_handled` 테이블(UUID 기반 eventId)과 `version` 필드(aggregateId별 순차적 버전)를 통한 중복 처리 방지
- **순서 보장**: 파티션 키 기반 이벤트 순서 보장 및 `offset.reset: latest` 설정으로 불필요한 과거 메시지 처리 방지

**구현된 이벤트:**
- `like-events`: `LikeAdded`, `LikeRemoved` → 좋아요 수 집계
- `order-events`: `OrderCreated` → 판매량 집계
- `product-events`: `ProductViewed` → 조회 수 집계

## 💬 Review Points

#### 1. Transactional Outbox Pattern 구현 방식의 적절성

**배경 및 문제 상황:**
외부 시스템(Kafka)과의 통신이 필요한 상황에서, 도메인 트랜잭션과 Kafka 발행을 동일 트랜잭션으로 묶을 수 없기 때문에 이벤트 유실 가능성이 있었습니다. 예를 들어, 주문 생성 트랜잭션이 성공했지만 Kafka 발행이 실패하면, 집계 서비스는 해당 주문 이벤트를 받지 못하게 됩니다. 반대로 Kafka 발행은 성공했지만 도메인 트랜잭션이 롤백되면, 실제로는 주문이 생성되지 않았는데 집계 서비스는 주문 이벤트를 받게 되는 문제가 발생합니다.

**해결 방안:**
이러한 문제를 해결하기 위해 Transactional Outbox Pattern을 적용했습니다. 도메인 트랜잭션과 같은 트랜잭션에서 `OutboxEvent`를 DB에 먼저 저장하고, 별도 스케줄러가 주기적으로 PENDING 상태의 이벤트를 읽어 Kafka로 발행하는 구조입니다. 이렇게 하면 도메인 트랜잭션이 성공하면 Outbox에 이벤트가 저장되고, 트랜잭션이 롤백되면 Outbox 저장도 함께 롤백되어 일관성이 보장됩니다.

**구현 세부사항:**
1. **ApplicationEvent → OutboxEvent 변환**: `OutboxBridgeEventListener`가 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`로 설정되어, 도메인 트랜잭션이 커밋된 후에만 Outbox에 저장합니다. 이렇게 하면 도메인 로직이 실패하여 트랜잭션이 롤백되면 Outbox 저장도 롤백되어 불필요한 이벤트가 저장되지 않습니다.

2. **스케줄러 기반 발행**: `OutboxEventPublisher`가 1초마다 실행되어 PENDING 상태의 이벤트를 최대 100개씩 읽어 Kafka로 발행합니다. 발행 성공 시 `PUBLISHED` 상태로 변경하고, 실패 시 `FAILED` 상태로 변경하여 다음 스케줄에서 재시도할 수 있도록 했습니다.

**관련 코드:**
```java
// OutboxBridgeEventListener.java - 도메인 트랜잭션 커밋 후에만 Outbox에 저장
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleLikeAdded(LikeEvent.LikeAdded event) {
    outboxEventService.saveEvent(/* ... */);
}

// OutboxEventPublisher.java - 1초마다 PENDING 이벤트를 읽어 Kafka로 발행
@Scheduled(fixedDelay = 1000)
public void publishPendingEvents() {
    List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(BATCH_SIZE);
    for (OutboxEvent event : pendingEvents) {
        publishEvent(event);
        event.markAsPublished();  // PUBLISHED 상태로 변경
    }
}
```

**고민한 점:**
- `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`를 사용한 이유는, 도메인 트랜잭션이 성공적으로 커밋된 후에만 Outbox에 저장하여 일관성을 보장하기 위함입니다. 만약 `AFTER_COMMIT`이 아닌 다른 시점에 저장하면, 도메인 로직이 실패하여 롤백되었는데도 Outbox에 이벤트가 저장될 수 있습니다. 하지만 `AFTER_COMMIT`은 트랜잭션이 완전히 커밋된 후에 실행되므로, Outbox 저장 실패 시 도메인 트랜잭션을 롤백할 수 없다는 단점이 있습니다. 이 부분에 대한 검토가 필요합니다.

- 스케줄러 주기(1초)와 배치 크기(100)는 현재 트래픽을 고려하여 설정했지만, 실제 운영 환경에서는 이벤트 발생 빈도와 Kafka 처리 속도를 고려하여 조정이 필요할 수 있습니다. 너무 짧은 주기(예: 100ms)는 DB 부하를 증가시킬 수 있고, 너무 긴 주기(예: 10초)는 이벤트 발행 지연이 발생할 수 있습니다.

- 개별 이벤트 발행 실패 시 `FAILED` 상태로 변경하고 계속 진행하도록 했는데, 이렇게 하면 일부 이벤트만 실패해도 다음 스케줄에서 재시도할 수 있습니다. 하지만 `FAILED` 상태의 이벤트를 별도로 모니터링하거나, 재시도 횟수 제한을 두는 등의 추가 로직이 필요할 수 있습니다.

---

#### 2. 멱등성 처리 전략: event_handled 테이블과 version 필드의 조합

**배경 및 문제 상황:**
Kafka는 기본적으로 At Least Once 보장을 제공하므로, 네트워크 오류나 Consumer 재시작 등의 상황에서 동일한 메시지가 여러 번 전달될 수 있습니다. 또한 Producer 측에서도 `acks=all`, `enable.idempotence=true` 설정으로 At Least Once를 보장하므로, 동일한 이벤트가 중복 발행될 수 있습니다. 이러한 중복 메시지를 그대로 처리하면 좋아요 수나 판매량이 중복 집계되어 잘못된 메트릭이 생성됩니다.

**해결 방안:**
중복 처리를 방지하기 위해 두 가지 전략을 조합했습니다:
1. **event_handled 테이블**: 동일 `eventId`의 중복 처리 방지 (동일 이벤트의 완전 중복 방지)
2. **version 필드**: 오래된 이벤트가 최신 상태를 덮어쓰는 것 방지 (순서가 뒤바뀐 이벤트 처리 방지)

**구현 세부사항:**

**1) event_handled 테이블을 통한 중복 처리 방지:**
- 각 이벤트에 UUID 기반의 고유한 `eventId`를 부여합니다.
- Consumer에서 이벤트를 처리하기 전에 `event_handled` 테이블에서 해당 `eventId`가 이미 처리되었는지 확인합니다.
- 이미 처리된 경우 스킵하고, 처리되지 않은 경우에만 비즈니스 로직을 실행한 후 `event_handled` 테이블에 기록을 저장합니다.
- `event_handled` 테이블의 `event_id` 컬럼에 UNIQUE 제약조건을 설정하여, 동시성 상황에서도 중복 처리를 방지합니다. 만약 두 개의 Consumer 인스턴스가 동시에 같은 이벤트를 처리하려고 하면, 하나는 성공하고 다른 하나는 UNIQUE 제약조건 위반 예외가 발생하여 중복 처리가 방지됩니다.

**2) version 필드를 통한 오래된 이벤트 처리 방지:**
- `OutboxEvent`에 `aggregateId`별로 순차적으로 증가하는 `version` 필드를 부여합니다. 예를 들어, `productId=1`에 대한 첫 번째 이벤트는 `version=1`, 두 번째 이벤트는 `version=2`가 됩니다.
- 이 `version`은 Kafka 메시지 헤더에 포함되어 Consumer로 전달됩니다.
- Consumer에서 이벤트를 처리할 때, `ProductMetrics`의 현재 `version`과 이벤트의 `version`을 비교합니다. 이벤트의 `version`이 메트릭의 `version`보다 크면 업데이트하고, 그렇지 않으면 스킵합니다.
- 이렇게 하면 네트워크 지연이나 파티션 순서 문제로 인해 오래된 이벤트가 나중에 도착하더라도, 이미 더 최신 버전의 메트릭이 존재하면 오래된 이벤트는 무시됩니다.

**관련 코드:**
```java
// OutboxEventService.java - eventId(UUID)와 version(aggregateId별 순차 증가) 부여
public void saveEvent(...) {
    String eventId = UUID.randomUUID().toString();
    Long nextVersion = outboxEventRepository.findLatestVersionByAggregateId(...) + 1L;
    // OutboxEvent에 eventId와 version 저장
}

// OutboxEventPublisher.java - Kafka 헤더에 eventId와 version 포함
private void publishEvent(OutboxEvent event) {
    messageBuilder
        .setHeader("eventId", event.getEventId())
        .setHeader("version", event.getVersion());
}

// ProductMetricsConsumer.java - 멱등성 체크 및 버전 비교
public void consumeLikeEvents(...) {
    String eventId = extractEventId(record);
    if (eventHandledService.isAlreadyHandled(eventId)) continue;  // 중복 체크
    
    Long eventVersion = extractVersion(record);
    productMetricsService.incrementLikeCount(productId, eventVersion);  // version 비교 포함
    eventHandledService.markAsHandled(eventId, ...);
}

// ProductMetricsService.java - version 비교로 최신 이벤트만 반영
public void incrementLikeCount(Long productId, Long eventVersion) {
    if (!metrics.shouldUpdate(eventVersion)) return;  // 오래된 이벤트 스킵
    metrics.incrementLikeCount();
}
```

**고민한 점:**
- `event_handled` 테이블의 UNIQUE 제약조건으로 동시성 상황에서도 중복 처리를 방지했지만, 이 테이블이 계속 증가하는 문제가 있습니다. 시간이 지나면서 이 테이블의 데이터가 무한정 증가하게 되는데, 이는 스토리지 비용과 조회 성능에 영향을 줄 수 있습니다. TTL(Time To Live)을 설정하여 일정 기간이 지난 레코드를 자동으로 삭제하거나, 아카이빙 전략을 수립하여 오래된 데이터를 별도 테이블로 이동시키는 등의 방안이 필요할 수 있습니다.

- `version` 필드는 `aggregateId`별로 자동 증가하도록 구현했는데, 이 방식의 장점은 간단하고 순차적인 버전 관리가 가능하다는 것입니다. 하지만 `updatedAt` 기반 방식과 비교했을 때, `updatedAt`은 시간 기반이므로 네트워크 지연이나 시스템 시간 불일치 문제가 발생할 수 있습니다. 반면 `version`은 순차적으로 증가하므로 이러한 문제가 없습니다. 다만, `aggregateId`별로 별도의 버전을 관리해야 하므로 복잡도가 증가합니다. 이 방식이 적절한지, 또는 다른 방식(예: `updatedAt` 기반, 또는 이벤트 발생 시점의 타임스탬프 기반)이 더 나은지 검토가 필요합니다.

- `event_handled` 테이블과 `version` 필드를 모두 사용하는 것이 중복일 수 있다는 의문도 있습니다. 하지만 두 가지는 서로 다른 목적을 가지고 있습니다. `event_handled`는 동일한 이벤트의 완전 중복을 방지하고, `version`은 순서가 뒤바뀐 이벤트를 처리하지 않도록 합니다. 예를 들어, 네트워크 문제로 인해 `version=3` 이벤트가 먼저 도착하고 `version=2` 이벤트가 나중에 도착하는 경우, `event_handled`로는 중복을 감지할 수 없지만 `version`으로는 오래된 이벤트임을 감지할 수 있습니다.

---

#### 3. 파티션 키 기반 순서 보장과 offset.reset: latest 설정의 조합

**배경 및 문제 상황:**
Kafka는 기본적으로 파티션 내에서만 순서를 보장하고, 서로 다른 파티션 간의 순서는 보장하지 않습니다. 또한 Consumer가 재시작되거나 새로운 Consumer Group이 시작될 때, 과거의 모든 메시지를 다시 처리하게 되면 이미 처리된 오래된 메시지를 중복 처리하거나, 테스트 환경에서 이전 테스트의 메시지가 다음 테스트에 영향을 줄 수 있습니다.

**해결 방안:**

**1) 파티션 키 기반 순서 보장:**
- 같은 도메인에서 발생한 이벤트는 동일한 파티션에서 처리되도록 파티션 키를 설정했습니다.
- `like-events`와 `product-events`는 `productId`를 파티션 키로 사용하여, 같은 상품에 대한 이벤트는 항상 같은 파티션에서 순서대로 처리됩니다.
- `order-events`는 `orderId`를 파티션 키로 사용하여, 같은 주문에 대한 이벤트는 항상 같은 파티션에서 순서대로 처리됩니다.
- 이렇게 하면 같은 aggregate root에 대한 이벤트는 순서가 보장되어, 예를 들어 `LikeAdded` → `LikeRemoved` 순서로 이벤트가 발생했을 때 Consumer도 같은 순서로 처리할 수 있습니다.

**2) offset.reset: latest 설정:**
- Consumer가 새로운 Consumer Group으로 시작할 때(offset이 없을 때), 최신 메시지부터 읽기 시작하도록 `offset.reset: latest`를 설정했습니다.
- 이렇게 하면 Consumer가 재시작되거나 새로운 Consumer Group이 시작될 때, 과거의 오래된 메시지를 처리하지 않고 최신 메시지부터 처리할 수 있습니다.
- 특히 테스트 환경에서는 이전 테스트에서 발행한 메시지가 다음 테스트에 영향을 주지 않도록 하는 데 유용합니다.

**다른 옵션과의 비교:**
- `offset.reset: latest` 설정은 새로운 Consumer Group이 시작할 때 최신 메시지부터 읽기 시작하므로, `earliest`를 사용할 경우 발생하는 문제(이전 테스트의 메시지가 다음 테스트에 영향을 주어 테스트 격리 실패)를 방지하고, `manual` 방식처럼 복잡한 offset 관리 로직 없이도 테스트 격리를 보장할 수 있습니다.
- 그러나 `latest` 설정은 새로운 Consumer Group이 시작할 때만 적용되므로, 같은 Consumer Group을 계속 사용하는 경우 이미 커밋된 offset이 있으면 `latest`는 적용되지 않고 기존 offset부터 계속 읽게 됩니다.
- 따라서 통합 테스트에서는 `KafkaCleanUp.resetAllTestTopics()`로 토픽을 삭제하고 재생성하여 Consumer Group을 초기화함으로써, 매 테스트마다 `offset.reset: latest` 설정이 적용되도록 하고, 이전 테스트의 메시지가 완전히 제거되어 테스트 간 격리를 보장합니다.

**구현 세부사항:**

**파티션 키 설정:**
```java
// OutboxBridgeEventListener.java
// productId를 파티션 키로 사용
public void handleLikeAdded(LikeEvent.LikeAdded event) {
    outboxEventService.saveEvent(
        "LikeAdded",
        event.productId().toString(),  // aggregateId
        "Product",
        event,
        "like-events",
        event.productId().toString()  // partitionKey
    );
}

// OutboxEventPublisher.java
// Kafka 메시지에 파티션 키 설정
private void publishEvent(OutboxEvent event) {
    Object payload = objectMapper.readValue(event.getPayload(), Object.class);
    
    var messageBuilder = MessageBuilder
        .withPayload(payload)
        .setHeader(KafkaHeaders.KEY, event.getPartitionKey())  // 파티션 키 설정
        .setHeader("eventId", event.getEventId())
        .setHeader("version", event.getVersion());
    
    kafkaTemplate.send(event.getTopic(), message);
}
```

**offset.reset: latest 설정:**
```yaml
# modules/kafka/src/main/resources/kafka.yml
spring:
  kafka:
    properties:
      auto.offset.reset: latest  # 새로운 Consumer Group 시작 시 최신 메시지부터
    producer:
      properties:
        acks: all                    # 모든 리플리카에 쓰기 확인
        enable.idempotence: true     # 중복 방지
    consumer:
      properties:
        enable-auto-commit: false    # 수동 커밋 사용
    listener:
      ack-mode: manual               # 수동 커밋 모드
```

**테스트 환경에서의 토픽 및 Consumer Group 초기화:**
```java
// KafkaCleanUp.java
// 테스트 실행 전에 토픽과 Consumer Group을 초기화하여 offset.reset: latest가 적용되도록 함
public void resetAllTestTopics() {
    deleteAllTestTopics();
    recreateTestTopics();
}

public void resetAllConsumerGroups() {
    try (AdminClient adminClient = createAdminClient()) {
        Set<String> consumerGroups = adminClient.listConsumerGroups()
            .all()
            .get(5, TimeUnit.SECONDS)
            .stream()
            .map(group -> group.groupId())
            .collect(java.util.stream.Collectors.toSet());
        
        if (!consumerGroups.isEmpty()) {
            DeleteConsumerGroupsResult deleteResult = adminClient.deleteConsumerGroups(consumerGroups);
            deleteResult.all().get(5, TimeUnit.SECONDS);
        }
    } catch (Exception e) {
        // Consumer Group이 없거나 이미 삭제된 경우 무시
    }
}
```

**테스트 환경에서의 격리 보장:**
- `offset.reset: latest`는 새로운 Consumer Group이 시작할 때만 적용되므로, 이미 offset이 커밋된 Consumer Group에서는 적용되지 않습니다. 따라서 테스트 환경에서는 각 테스트 실행 전에 `KafkaCleanUp.resetAllTestTopics()`와 `resetAllConsumerGroups()`를 호출하여 토픽과 Consumer Group을 초기화했습니다.
- 이렇게 하면 매 테스트마다 `offset.reset: latest` 설정이 적용되어, 이전 테스트의 메시지가 다음 테스트에 영향을 주지 않습니다.
- 또한 테스트 프로파일에서 Consumer Group ID를 동적으로 생성(`${spring.application.name}-test-${random.uuid}`)하여, 각 테스트마다 다른 Consumer Group을 사용하도록 했습니다. 이렇게 하면 이전 테스트의 offset이 다음 테스트에 영향을 주지 않습니다.

**관련 코드:**
```java
// KafkaCleanUp.java - 테스트 격리를 위한 초기화
public void resetAllTestTopics() {
    deleteAllTestTopics();  // 모든 메시지 제거
    recreateTestTopics();   // 깨끗한 상태로 재생성
}

// ProductMetricsConsumerIntegrationTest.java
@BeforeEach
void setUp() {
    kafkaCleanUp.resetAllTestTopics();      // 토픽 초기화
    kafkaCleanUp.resetAllConsumerGroups();  // Consumer Group 초기화
}
```

**고민한 점:**
- `offset.reset: latest`는 새로운 Consumer Group이 시작할 때만 적용되므로, 이미 offset이 커밋된 Consumer Group에서는 적용되지 않습니다. 따라서 테스트 환경에서는 각 테스트 실행 전에 `KafkaCleanUp.resetAllTestTopics()`와 `resetAllConsumerGroups()`를 호출하여 토픽과 Consumer Group을 초기화했습니다. 이렇게 하면 매 테스트마다 `offset.reset: latest` 설정이 적용되어, 이전 테스트의 메시지가 다음 테스트에 영향을 주지 않습니다. 하지만 이 방식은 테스트 실행 시간을 증가시킬 수 있고, 프로덕션 환경에서는 사용할 수 없습니다. 다른 테스트 격리 전략(예: 각 테스트마다 고유한 Consumer Group ID 사용, 또는 테스트용 별도 토픽 사용)이 더 나은지 검토가 필요합니다.

- 파티션 키를 `productId` 또는 `orderId`로 설정했는데, 이로 인한 파티션 불균형 문제가 발생할 수 있습니다. 예를 들어, 특정 상품에 대한 이벤트가 매우 많으면 해당 상품의 파티션에만 메시지가 집중되어 다른 파티션은 비어있을 수 있습니다. 하지만 현재 구현에서는 파티션 키를 사용하여 순서를 보장하는 것이 더 중요하다고 판단했습니다. 만약 파티션 불균형이 심각한 문제가 된다면, 파티션 키를 해시 함수로 변환하거나, 복합 키를 사용하는 등의 방안을 고려할 수 있습니다.

- 파티션 키를 사용하지 않고 랜덤 키를 사용하면 어떤 문제가 발생할까요? 예를 들어, 같은 상품에 대한 `LikeAdded`와 `LikeRemoved` 이벤트가 서로 다른 파티션에 발행되면, Consumer가 `LikeRemoved`를 먼저 처리하고 `LikeAdded`를 나중에 처리할 수 있습니다. 이 경우 좋아요 수가 음수가 되거나 잘못된 메트릭이 생성될 수 있습니다. 따라서 파티션 키를 사용하여 같은 aggregate root에 대한 이벤트는 항상 같은 파티션에서 순서대로 처리되도록 하는 것이 중요합니다.

---

### 구현 세부사항

#### 1. 내부 이벤트와 외부 이벤트의 구분

**배경:**
기존에는 JVM 내에서 처리 가능한 느슨한 연결을 위해 Spring Application Event를 사용했습니다. 예를 들어, 주문 생성 시 재고 차감이나 포인트 적립 등의 로직은 같은 애플리케이션 내에서 처리되므로 Application Event로 충분했습니다. 하지만 이번에는 외부 시스템(집계 서비스)과의 통신이 필요하므로, JVM을 벗어나 네트워크를 통해 메시지를 전달할 수 있는 Kafka를 사용하도록 구성했습니다.

**내부 이벤트 (Application Event):**
- 같은 JVM 내에서 처리되는 이벤트
- 예: 주문 생성 → 재고 차감, 포인트 적립 등
- Spring의 `ApplicationEventPublisher`를 통해 발행
- 동기 또는 비동기로 처리 가능
- 트랜잭션 내에서 처리되므로 일관성 보장이 상대적으로 쉬움

**외부 이벤트 (Kafka Event):**
- 다른 서비스(집계 서비스)로 전달되어야 하는 이벤트
- 예: 주문 생성 → 판매량 집계, 좋아요 추가 → 좋아요 수 집계 등
- Kafka를 통해 네트워크로 전달
- 비동기로 처리되며, 네트워크 오류나 서비스 다운 등의 상황을 고려해야 함
- 트랜잭션 경계를 넘어서므로 일관성 보장이 복잡함 (Outbox 패턴 필요)

**구조:**
```
도메인 로직 (예: 주문 생성)
    ↓
ApplicationEvent 발행 (JVM 내부)
    ↓
OutboxBridgeEventListener (ApplicationEvent → OutboxEvent 변환)
    - @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    - 도메인 트랜잭션과 같은 트랜잭션에서 OutboxEvent 저장
    ↓
OutboxEvent (DB 저장, PENDING 상태)
    ↓
OutboxEventPublisher (스케줄러가 주기적으로 PENDING 이벤트 읽기)
    - @Scheduled(fixedDelay = 1000)
    - Kafka로 발행 후 PUBLISHED 상태로 변경
    ↓
Kafka Topic (like-events, order-events, product-events)
    ↓
ProductMetricsConsumer (집계 서비스, 다른 JVM)
    - 이벤트 수취 및 메트릭 집계
    - event_handled 테이블로 멱등성 보장
    - version 필드로 최신 이벤트만 반영
```

**관련 코드:**
```java
// 내부 이벤트: ApplicationEvent (JVM 내부)
applicationEventPublisher.publishEvent(new OrderEvent.OrderCreated(...));

// 외부 이벤트: ApplicationEvent → OutboxEvent → Kafka
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleOrderCreated(OrderEvent.OrderCreated event) {
    outboxEventService.saveEvent(/* OutboxEvent로 변환하여 DB 저장 */);
}

@Scheduled(fixedDelay = 1000)
public void publishPendingEvents() {
    // PENDING 이벤트를 읽어 Kafka로 발행
}
```

#### 2. Producer 설정: At Least Once 보장 및 이벤트 생성 실패 처리

**배경:**
외부 시스템과의 통신이기 때문에 DB에서 데이터를 직접 조회하거나, 기존의 서비스 로직을 사용하여 검증처리하기는 어려운 상황입니다. 따라서 이벤트 publisher 자체의 설정을 통해 메시지 유실을 방지하고, Consumer 측에서 멱등 처리를 통해 중복을 방지하는 구조로 구성했습니다.

**설정 내용:**
- `acks=all`: Producer가 메시지를 발행할 때, 모든 리플리카에 쓰기가 완료될 때까지 대기합니다. 이렇게 하면 리플리카 중 하나가 실패하더라도 다른 리플리카에 메시지가 저장되어 유실을 방지할 수 있습니다.
- `enable.idempotence=true`: Producer가 동일한 메시지를 여러 번 발행하더라도 Kafka 브로커가 중복을 제거하여 한 번만 저장하도록 합니다. 이를 통해 네트워크 오류로 인한 재시도 시 중복 메시지가 저장되는 것을 방지합니다.
- `max.in.flight.requests.per.connection=5`: `idempotence=true`일 때 필수 설정입니다. 동시에 전송할 수 있는 미확인 요청의 최대 개수를 제한합니다.

**관련 코드:**
```yaml
# modules/kafka/src/main/resources/kafka.yml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      retries: 3
      properties:
        acks: all                    # 모든 리플리카에 쓰기 확인 (At Least Once 보장)
        enable.idempotence: true     # 중복 방지 (At Least Once 보장)
        max.in.flight.requests.per.connection: 5  # idempotence=true일 때 필수
```

**고민한 점:**
- `acks=all`은 메시지 유실을 방지하지만, 모든 리플리카에 쓰기가 완료될 때까지 대기하므로 지연 시간이 증가할 수 있습니다. 하지만 메시지 유실을 방지하는 것이 더 중요하다고 판단하여 `acks=all`을 선택했습니다.
- `enable.idempotence=true`는 Producer 측에서 중복을 제거하지만, 네트워크 오류나 Consumer 재시작 등의 상황에서 동일한 메시지가 여러 번 전달될 수 있습니다. 따라서 Consumer 측에서도 멱등 처리가 필요합니다.

#### 3. Consumer 설정: Manual Ack 처리

**배경:**
Consumer가 이벤트를 처리하는 과정에서 오류가 발생할 수 있습니다. 만약 자동 커밋을 사용하면, 이벤트를 처리하기 전에 offset이 커밋되어 이벤트가 유실될 수 있습니다. 반대로 이벤트 처리 실패 시에도 offset이 커밋되지 않도록 하여 재처리가 가능하도록 해야 합니다.

**설정 내용:**
- `enable-auto-commit: false`: 자동 커밋을 비활성화하여 수동 커밋을 사용합니다.
- `ack-mode: manual`: 수동 커밋 모드를 사용하여, 이벤트 처리 성공 후에만 `Acknowledgment.acknowledge()`를 호출하여 offset을 커밋합니다.

**처리 흐름:**
1. Consumer가 메시지를 수신합니다.
2. 각 메시지를 처리합니다 (멱등성 체크, 비즈니스 로직 실행, event_handled 테이블에 기록).
3. 모든 메시지 처리 완료 후 `acknowledgment.acknowledge()`를 호출하여 offset을 커밋합니다.
4. 만약 처리 중 오류가 발생하면 `acknowledgment.acknowledge()`를 호출하지 않아 offset이 커밋되지 않으므로, Consumer가 재시작되거나 다음 poll 시 동일한 메시지를 다시 받아 재처리할 수 있습니다.

**관련 코드:**
```yaml
# modules/kafka/src/main/resources/kafka.yml
spring:
  kafka:
    consumer:
      group-id: loopers-default-consumer
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.ByteArrayDeserializer
      properties:
        enable-auto-commit: false    # 자동 커밋 비활성화
    listener:
      ack-mode: manual                # 수동 커밋 모드
```

```java
// ProductMetricsConsumer.java - 처리 성공 후에만 수동 커밋
@KafkaListener(topics = "like-events")
public void consumeLikeEvents(..., Acknowledgment acknowledgment) {
    // 이벤트 처리 로직
    acknowledgment.acknowledge();  // 성공 시에만 커밋
}
```

**고민한 점:**
- Manual Ack를 사용하면 이벤트 처리 성공 후에만 offset이 커밋되므로, 처리 실패 시 재처리가 가능합니다. 하지만 Consumer가 재시작되거나 장애가 발생하면 처리 중이던 메시지들이 다시 처리될 수 있습니다. 따라서 멱등 처리가 필수적입니다.
- 배치 처리 시 모든 메시지 처리 완료 후 한 번에 커밋하는 방식과, 각 메시지 처리 후 개별적으로 커밋하는 방식 중 선택할 수 있습니다. 현재는 배치 처리 후 한 번에 커밋하는 방식을 사용했는데, 이는 성능상 유리하지만 일부 메시지 처리 실패 시 전체 배치가 재처리됩니다. 개별 커밋 방식은 성능이 떨어지지만 실패한 메시지만 재처리할 수 있습니다.

#### 4. 멱등성 처리: event_handled 테이블

**배경:**
Kafka의 At Least Once 보장과 Manual Ack 처리로 인해, 동일한 이벤트가 여러 번 전달될 수 있습니다. 또한 Consumer가 여러 인스턴스로 실행되는 경우, 동시에 같은 이벤트를 처리하려고 할 수 있습니다. 이러한 상황에서 중복 처리를 방지하기 위해 `event_handled` 테이블을 사용합니다.

**구현 방식:**
- 각 이벤트에 고유한 UUID 기반 `eventId`를 부여합니다.
- Consumer에서 이벤트를 처리하기 전에 `event_handled` 테이블에서 해당 `eventId`가 이미 처리되었는지 확인합니다.
- 이미 처리된 경우 스킵하고, 처리되지 않은 경우에만 비즈니스 로직을 실행한 후 `event_handled` 테이블에 기록을 저장합니다.
- `event_handled` 테이블의 `event_id` 컬럼에 UNIQUE 제약조건을 설정하여, 동시성 상황에서도 중복 처리를 방지합니다.

**관련 코드:**
```java
// EventHandled.java - eventId에 UNIQUE 제약조건
@Entity
public class EventHandled {
    @Id
    @Column(unique = true)
    private String eventId;  // UNIQUE 제약조건으로 동시성 보장
}

// ProductMetricsConsumer.java - 중복 체크
if (eventHandledService.isAlreadyHandled(eventId)) continue;
productMetricsService.incrementLikeCount(...);
eventHandledService.markAsHandled(eventId, ...);  // UNIQUE 제약조건으로 중복 방지
```

**고민한 점:**
- `event_handled` 테이블을 DB로 구현했지만, Redis를 사용하는 것도 고려할 수 있습니다. Redis는 TTL을 쉽게 설정할 수 있고 조회 성능이 빠르지만, 영속성이 보장되지 않습니다. DB는 영속성이 보장되지만 TTL 설정이 복잡하고 조회 성능이 상대적으로 느릴 수 있습니다. 현재는 DB를 선택했는데, 이는 영속성이 중요하고 TTL은 별도 아카이빙 전략으로 해결할 수 있다고 판단했기 때문입니다.
- `event_handled` 테이블이 계속 증가하는 문제가 있습니다. 시간이 지나면서 이 테이블의 데이터가 무한정 증가하게 되는데, 이는 스토리지 비용과 조회 성능에 영향을 줄 수 있습니다. TTL을 설정하여 일정 기간이 지난 레코드를 자동으로 삭제하거나, 아카이빙 전략을 수립하여 오래된 데이터를 별도 테이블로 이동시키는 등의 방안이 필요할 수 있습니다.

#### 5. 버전 기반 최신 이벤트만 반영

**배경:**
네트워크 지연이나 파티션 순서 문제로 인해, 이벤트가 발생한 순서와 Consumer가 받는 순서가 다를 수 있습니다. 예를 들어, `version=3` 이벤트가 먼저 도착하고 `version=2` 이벤트가 나중에 도착하는 경우, `version=2` 이벤트를 처리하면 이미 `version=3`으로 업데이트된 메트릭을 덮어쓰게 되어 잘못된 상태가 됩니다.

**해결 방안:**
- `OutboxEvent`에 `aggregateId`별로 순차적으로 증가하는 `version` 필드를 부여합니다.
- 이 `version`은 Kafka 메시지 헤더에 포함되어 Consumer로 전달됩니다.
- Consumer에서 이벤트를 처리할 때, `ProductMetrics`의 현재 `version`과 이벤트의 `version`을 비교합니다.
- 이벤트의 `version`이 메트릭의 `version`보다 크면 업데이트하고, 그렇지 않으면 스킵합니다.

**구현 방식:**
- `OutboxEventService.saveEvent()`에서 `aggregateId`별 최신 버전을 조회한 후 +1하여 새로운 버전을 부여합니다.
- `ProductMetrics`에도 `version` 필드를 두고, 업데이트할 때마다 증가시킵니다.
- `ProductMetrics.shouldUpdate()` 메서드로 이벤트 버전과 메트릭 버전을 비교하여, 이벤트가 최신인 경우에만 업데이트합니다.

**전체 플로우 다이어그램:**
```
[도메인 이벤트 발생]
    ↓
[OutboxBridgeEventListener]
    ↓
[OutboxEventService.saveEvent()]
    ├─ aggregateId별 최신 버전 조회 (DB)
    ├─ 버전 +1 계산
    └─ OutboxEvent에 version 저장 (DB)
    ↓
[OutboxEventPublisher (스케줄러, 1초마다)]
    ├─ PENDING 이벤트 조회
    ├─ Kafka 메시지 헤더에 version 추가
    └─ Kafka로 발행
    ↓
[ProductMetricsConsumer]
    ├─ Kafka 헤더에서 version 추출
    ├─ ProductMetricsService에 version 전달
    └─ version 비교로 최신 이벤트만 반영
```

**핵심 코드 위치:**

**1) 버전 생성: OutboxEventService.saveEvent() (59-60줄)**
```java
// 집계 ID별 최신 버전 조회 후 +1
Long latestVersion = outboxEventRepository.findLatestVersionByAggregateId(aggregateId, aggregateType);
Long nextVersion = latestVersion + 1L;
```

**2) Kafka 헤더에 추가: OutboxEventPublisher.publishEvent() (99-102줄)**
```java
// version이 있으면 헤더에 추가
if (event.getVersion() != null) {
    messageBuilder.setHeader("version", event.getVersion());
}
```

**3) Consumer에서 추출: ProductMetricsConsumer.extractVersion() (374-387줄)**
```java
// Kafka 헤더에서 version 추출
Header header = record.headers().lastHeader(VERSION_HEADER);
return Long.parseLong(new String(header.value(), StandardCharsets.UTF_8));
```

**4) 버전 비교: ProductMetricsService에서 eventVersion과 metrics.version 비교하여 최신 이벤트만 반영**

이렇게 `aggregateId`별로 순차적인 버전이 생성되어 Kafka 헤더로 전달되고, Consumer에서 최신 이벤트만 반영하는 데 사용됩니다.

**관련 코드:**
```java
// OutboxEventService.java - aggregateId별 순차적 version 부여
Long nextVersion = findLatestVersionByAggregateId(...) + 1L;

// ProductMetrics.java - version 비교로 최신 이벤트만 반영
public boolean shouldUpdate(Long eventVersion) {
    return eventVersion > this.version;  // 이벤트 버전이 더 크면 업데이트
}

// ProductMetricsService.java
if (!metrics.shouldUpdate(eventVersion)) return;  // 오래된 이벤트 스킵
metrics.incrementLikeCount();
```

**고민한 점:**
- `version` 필드는 `aggregateId`별로 자동 증가하도록 구현했는데, 이 방식의 장점은 간단하고 순차적인 버전 관리가 가능하다는 것입니다. 하지만 `updatedAt` 기반 방식과 비교했을 때, `updatedAt`은 시간 기반이므로 네트워크 지연이나 시스템 시간 불일치 문제가 발생할 수 있습니다. 반면 `version`은 순차적으로 증가하므로 이러한 문제가 없습니다. 다만, `aggregateId`별로 별도의 버전을 관리해야 하므로 복잡도가 증가합니다.
- `version` 필드가 `aggregateId`별로 관리되므로, 같은 `aggregateId`에 대한 이벤트는 순차적으로 처리되어야 합니다. 하지만 파티션 키를 `aggregateId`로 설정했으므로, 같은 `aggregateId`에 대한 이벤트는 같은 파티션에서 순서대로 처리되므로 이 문제는 해결됩니다.

#### 6. 파티션 키 설정

**배경:**
Kafka는 기본적으로 파티션 내에서만 순서를 보장하고, 서로 다른 파티션 간의 순서는 보장하지 않습니다. 따라서 같은 aggregate root에 대한 이벤트는 같은 파티션에서 처리되어야 순서가 보장됩니다.

#### 7. 퍼블리셔와 컨슈머가 동일 이벤트를 판단하는 기준 설정

**배경 및 문제 상황:**
aggregate root id(예: `productId`, `orderId`)만으로는 해당 event가 동일한 항목을 지정하는지 보장하기 어렵습니다. 예를 들어, 같은 `productId`에 대해 여러 번 좋아요가 추가되면, 각각은 서로 다른 이벤트이지만 `productId`만으로는 구분할 수 없습니다. 또한 네트워크 오류나 Consumer 재시작으로 인해 동일한 이벤트가 여러 번 전달될 수 있는데, `productId`만으로는 이것이 중복인지 새로운 이벤트인지 판단할 수 없습니다.

**해결 방안:**
이러한 문제를 해결하기 위해 두 가지 식별자를 조합하여 사용합니다:
1. **eventId (UUID)**: 각 이벤트에 고유한 UUID를 부여하여, 동일한 이벤트는 한 번만 처리될 수 있도록 합니다.
2. **version**: 같은 aggregate root에 대한 이벤트의 순서를 보장하고, 오래된 이벤트가 최신 상태를 덮어쓰는 것을 방지합니다.

**구현 세부사항:**

**1) eventId를 통한 동일 이벤트 판단:**
- `OutboxEventService.saveEvent()`에서 각 이벤트에 UUID 기반의 고유한 `eventId`를 부여합니다.
- 이 `eventId`는 Kafka 메시지 헤더에 포함되어 Consumer로 전달됩니다.
- Consumer에서 이벤트를 처리하기 전에 `event_handled` 테이블에서 해당 `eventId`가 이미 처리되었는지 확인합니다.
- 이미 처리된 경우 스킵하고, 처리되지 않은 경우에만 비즈니스 로직을 실행한 후 `event_handled` 테이블에 기록을 저장합니다.
- 이렇게 하면 동일한 이벤트가 여러 번 전달되더라도 한 번만 처리됩니다.

**2) version을 통한 불필요한 이벤트 처리 방지:**
- `OutboxEventService.saveEvent()`에서 `aggregateId`별로 순차적으로 증가하는 `version`을 부여합니다.
- 이 `version`은 Kafka 메시지 헤더에 포함되어 Consumer로 전달됩니다.
- Consumer에서 이벤트를 처리할 때, `ProductMetrics`의 현재 `version`과 이벤트의 `version`을 비교합니다.
- 이벤트의 `version`이 메트릭의 `version`보다 크면 업데이트하고, 그렇지 않으면 스킵합니다.
- 이렇게 하면 네트워크 지연이나 파티션 순서 문제로 인해 오래된 이벤트가 나중에 도착하더라도, 이미 더 최신 버전의 메트릭이 존재하면 오래된 이벤트는 무시됩니다.

**관련 코드:**
```java
// OutboxEventService.java - eventId(UUID)와 version(aggregateId별 순차 증가) 부여
String eventId = UUID.randomUUID().toString();
Long nextVersion = findLatestVersionByAggregateId(...) + 1L;

// OutboxEventPublisher.java - Kafka 헤더에 eventId와 version 포함
messageBuilder.setHeader("eventId", event.getEventId())
              .setHeader("version", event.getVersion());

// ProductMetricsConsumer.java - eventId로 중복 체크, version으로 최신 이벤트만 반영
String eventId = extractEventId(record);
if (eventHandledService.isAlreadyHandled(eventId)) continue;
Long eventVersion = extractVersion(record);
productMetricsService.incrementLikeCount(productId, eventVersion);  // version 비교 포함
```

**고민한 점:**
- `eventId`와 `version`을 모두 사용하는 것이 중복일 수 있다는 의문이 있습니다. 하지만 두 가지는 서로 다른 목적을 가지고 있습니다. `eventId`는 동일한 이벤트의 완전 중복을 방지하고, `version`은 순서가 뒤바뀐 이벤트를 처리하지 않도록 합니다. 예를 들어, 네트워크 문제로 인해 `version=3` 이벤트가 먼저 도착하고 `version=2` 이벤트가 나중에 도착하는 경우, `eventId`로는 중복을 감지할 수 없지만 `version`으로는 오래된 이벤트임을 감지할 수 있습니다. 반대로, 동일한 이벤트가 네트워크 오류로 인해 여러 번 전달되는 경우, `version`으로는 중복을 감지할 수 없지만 `eventId`로는 중복을 감지할 수 있습니다.

- `aggregateId`만으로는 동일 이벤트를 판단할 수 없는 이유는, 같은 `aggregateId`에 대해 여러 번 이벤트가 발생할 수 있기 때문입니다. 예를 들어, 같은 상품에 대해 좋아요가 여러 번 추가되면, 각각은 서로 다른 이벤트이지만 `productId`만으로는 구분할 수 없습니다. 따라서 각 이벤트에 고유한 `eventId`를 부여하여 구분해야 합니다.

- `version`은 `aggregateId`별로 관리되므로, 같은 `aggregateId`에 대한 이벤트는 순차적으로 처리되어야 합니다. 하지만 파티션 키를 `aggregateId`로 설정했으므로, 같은 `aggregateId`에 대한 이벤트는 같은 파티션에서 순서대로 처리되므로 이 문제는 해결됩니다.

#### 6. 파티션 키 설정

**배경:**
Kafka는 기본적으로 파티션 내에서만 순서를 보장하고, 서로 다른 파티션 간의 순서는 보장하지 않습니다. 따라서 같은 aggregate root에 대한 이벤트는 같은 파티션에서 처리되어야 순서가 보장됩니다.

**설정 내용:**
- `like-events`, `product-events`: `productId`를 파티션 키로 사용하여, 같은 상품에 대한 이벤트는 항상 같은 파티션에서 순서대로 처리됩니다.
- `order-events`: `orderId`를 파티션 키로 사용하여, 같은 주문에 대한 이벤트는 항상 같은 파티션에서 순서대로 처리됩니다.

**관련 코드:**
```java
// OutboxBridgeEventListener.java - 파티션 키 설정
outboxEventService.saveEvent(..., partitionKey: productId.toString());  // like-events, product-events
outboxEventService.saveEvent(..., partitionKey: orderId.toString());     // order-events

// OutboxEventPublisher.java - Kafka 메시지에 파티션 키 설정
messageBuilder.setHeader(KafkaHeaders.KEY, event.getPartitionKey());
```

**고민한 점:**
- 파티션 키를 `productId` 또는 `orderId`로 설정했는데, 이로 인한 파티션 불균형 문제가 발생할 수 있습니다. 예를 들어, 특정 상품에 대한 이벤트가 매우 많으면 해당 상품의 파티션에만 메시지가 집중되어 다른 파티션은 비어있을 수 있습니다. 하지만 현재 구현에서는 파티션 키를 사용하여 순서를 보장하는 것이 더 중요하다고 판단했습니다. 만약 파티션 불균형이 심각한 문제가 된다면, 파티션 키를 해시 함수로 변환하거나, 복합 키를 사용하는 등의 방안을 고려할 수 있습니다.
- 파티션 키를 사용하지 않고 랜덤 키를 사용하면 어떤 문제가 발생할까요? 예를 들어, 같은 상품에 대한 `LikeAdded`와 `LikeRemoved` 이벤트가 서로 다른 파티션에 발행되면, Consumer가 `LikeRemoved`를 먼저 처리하고 `LikeAdded`를 나중에 처리할 수 있습니다. 이 경우 좋아요 수가 음수가 되거나 잘못된 메트릭이 생성될 수 있습니다. 따라서 파티션 키를 사용하여 같은 aggregate root에 대한 이벤트는 항상 같은 파티션에서 순서대로 처리되도록 하는 것이 중요합니다.


## ✅ Checklist
- [x] **도메인(애플리케이션) 이벤트 설계**
  - `apps/commerce-api/src/main/java/com/loopers/domain/like/LikeEvent.java`
  - `apps/commerce-api/src/main/java/com/loopers/domain/order/OrderEvent.java`
  - `apps/commerce-api/src/main/java/com/loopers/domain/product/ProductEvent.java`
  - 이벤트 타입: `LikeAdded`, `LikeRemoved`, `OrderCreated`, `ProductViewed`

- [x] **Producer 앱에서 도메인 이벤트 발행**
  - `apps/commerce-api/src/main/java/com/loopers/application/outbox/OutboxBridgeEventListener.java`
  - `apps/commerce-api/src/main/java/com/loopers/infrastructure/outbox/OutboxEventPublisher.java`
  - 토픽: `like-events`, `order-events`, `product-events`

- [x] **PartitionKey 기반의 이벤트 순서 보장**
  - `apps/commerce-api/src/main/java/com/loopers/domain/outbox/OutboxEvent.java` (partitionKey 필드)
  - `apps/commerce-api/src/main/java/com/loopers/infrastructure/outbox/OutboxEventPublisher.java` (KafkaHeaders.KEY 설정)
  - 파티션 키: `like-events`, `product-events` → `productId`, `order-events` → `orderId`

- [x] **At Least Once 보장 (acks=all, idempotence=true)**
  - `modules/kafka/src/main/resources/kafka.yml` (19-21줄)
  - 설정: `acks: all`, `enable.idempotence: true`, `max.in.flight.requests.per.connection: 5`

- [x] **Transactional Outbox Pattern 구현**
  - `apps/commerce-api/src/main/java/com/loopers/domain/outbox/OutboxEvent.java`
  - `apps/commerce-api/src/main/java/com/loopers/application/outbox/OutboxEventService.java`
  - `apps/commerce-api/src/main/java/com/loopers/infrastructure/outbox/OutboxEventPublisher.java`
  - `apps/commerce-api/src/main/java/com/loopers/application/outbox/OutboxBridgeEventListener.java`

- [x] **메시지 발행 실패 처리**
  - `apps/commerce-api/src/main/java/com/loopers/infrastructure/outbox/OutboxEventPublisher.java` (63-69줄)
  - 개별 이벤트 발행 실패 시 `FAILED` 상태로 변경하고 다음 스케줄에서 재시도

### ⚾ Consumer (7/7)

- [x] **Consumer가 Metrics 집계 처리**
  - `apps/commerce-streamer/src/main/java/com/loopers/interfaces/consumer/ProductMetricsConsumer.java`
  - `apps/commerce-streamer/src/main/java/com/loopers/application/metrics/ProductMetricsService.java`
  - 집계 대상: 좋아요 수, 판매량, 조회 수

- [x] **Manual Ack 처리**
  - `modules/kafka/src/main/resources/kafka.yml` (27, 29줄)
  - `apps/commerce-streamer/src/main/java/com/loopers/interfaces/consumer/ProductMetricsConsumer.java` (141, 217줄)
  - 설정: `enable-auto-commit: false`, `ack-mode: manual`

- [x] **`event_handled` 테이블 기반 멱등 처리**
  - `apps/commerce-streamer/src/main/java/com/loopers/domain/eventhandled/EventHandled.java`
  - `apps/commerce-streamer/src/main/java/com/loopers/application/eventhandled/EventHandledService.java`
  - `apps/commerce-streamer/src/main/java/com/loopers/interfaces/consumer/ProductMetricsConsumer.java` (91, 275줄)
  - UNIQUE 제약조건으로 동시성 상황에서도 중복 방지

- [x] **`version` 기준 최신 이벤트만 반영**
  - `apps/commerce-api/src/main/java/com/loopers/domain/outbox/OutboxEvent.java` (56줄, version 필드)
  - `apps/commerce-api/src/main/java/com/loopers/application/outbox/OutboxEventService.java` (59-60줄, 버전 생성)
  - `apps/commerce-api/src/main/java/com/loopers/infrastructure/outbox/OutboxEventPublisher.java` (99-102줄, 헤더에 추가)
  - `apps/commerce-streamer/src/main/java/com/loopers/interfaces/consumer/ProductMetricsConsumer.java` (100, 284줄, 헤더에서 추출)
  - `apps/commerce-streamer/src/main/java/com/loopers/application/metrics/ProductMetricsService.java` (100, 126줄, 버전 비교)
  - `apps/commerce-streamer/src/main/java/com/loopers/domain/metrics/ProductMetrics.java` (shouldUpdate 메서드)

- [x] **`product_metrics` 테이블에 upsert**
  - `apps/commerce-streamer/src/main/java/com/loopers/domain/metrics/ProductMetrics.java`
  - `apps/commerce-streamer/src/main/java/com/loopers/application/metrics/ProductMetricsService.java`
  - `apps/commerce-streamer/src/main/java/com/loopers/infrastructure/metrics/ProductMetricsRepositoryImpl.java`

- [x] **중복 메시지 재전송 테스트**
  - `apps/commerce-streamer/src/test/java/com/loopers/interfaces/consumer/ProductMetricsConsumerTest.java` (352줄, `handlesDuplicateMessagesIdempotently()`)
  - 동일한 `eventId`를 가진 메시지가 한 번만 처리되는지 검증

- [x] **재고 소진 시 상품 캐시 갱신**
  - `apps/commerce-api/src/main/java/com/loopers/application/product/ProductEventHandler.java` (147-151줄)
  - `apps/commerce-api/src/main/java/com/loopers/application/product/ProductCacheService.java` (evictProductDetailCache 메서드)
  - 재고 차감 후 `stock == 0` 체크하여 캐시 무효화

## 📎 References
<!--
  (Optional: 참고 자료가 없는 작업 - 단순 버그 픽스 등 의 경우엔 해당 란을 제거해주세요 !)
  리뷰어가 참고할 수 있는 추가적인 정보나 문서, 링크 등을 작성해주세요.
  예시:
  - 관련 문서 링크
  - 관련 정책 링크
-->

<!-- This is an auto-generated comment: release notes by coderabbit.ai -->

## Summary by CodeRabbit

## 릴리스 노트

* **새로운 기능**
  * 상품 조회, 좋아요, 주문 이벤트에 대한 실시간 메트릭 추적 시스템 추가
  * 신뢰할 수 있는 이벤트 기반 아키텍처로 데이터 처리 안정성 향상
  * 중복 이벤트 처리 방지를 위한 멱등성 보장 메커니즘 구현

* **테스트**
  * 이벤트 처리 및 메트릭 추적 관련 종합 테스트 스위트 추가

<sub>✏️ Tip: You can customize this high-level summary in your review settings.</sub>

<!-- end of auto-generated comment: release notes by coderabbit.ai -->
