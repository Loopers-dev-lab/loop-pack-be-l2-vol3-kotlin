# Round 9 리뷰 포인트

## N-13 Cross-App 시스템 테스트 보류 사유

### 1. 검증하려던 범위

Round 9의 N-13은 아래 흐름을 하나의 시나리오로 증명하는 것이 목적이었다.

1. Kafka topic에 실제 이벤트 발행
2. 실제 `commerce-streamer`의 `@KafkaListener`가 이벤트 소비
3. Redis ZSET에 점수 반영
4. `commerce-api` HTTP API에서 최종 랭킹 응답 확인

즉, 단일 모듈 내부 테스트가 아니라 `commerce-streamer`와 `commerce-api`를 함께 검증하는 cross-app 시스템 테스트가 필요했다.

### 2. 기존 시도와 한계

초기에는 `commerce-api` 테스트 안에서 Kafka 이벤트를 발행하고, 테스트용 consumer를 `@Import`로 추가하여 Redis를 갱신한 뒤 API를 조회하는 접근을 시도했다.

이 방식은 다음 이유로 N-13의 증거로는 부족하다고 판단했다.

- Kafka publish 자체는 검증할 수 있다.
- API가 Redis 상태를 읽어 응답하는지도 검증할 수 있다.
- 그러나 실제 `commerce-streamer`의 consumer, use case, scheduler 경로가 동작했는지는 증명하지 못한다.

정리하면, 해당 방식은 "Kafka 계약 + API 조회" 테스트로는 의미가 있지만, "실제 streamer가 동작하는가"를 보여주는 시스템 테스트는 아니다.

### 3. 현재 구조에서 어려웠던 이유

이번 PR 범위에서 in-process 방식으로 두 앱을 한 테스트에 함께 올리는 것은 신뢰도가 낮다고 판단했다.

주요 이유는 다음과 같다.

- 두 앱이 동일한 루트 패키지 `com.loopers`를 사용한다.
- 두 앱에 동일한 클래스명/FQCN을 가진 파일이 실제로 중복 존재한다.
- 두 앱에 `application.yml` 같은 리소스도 각각 존재한다.

대표 사례:

| 구분 | commerce-api | commerce-streamer |
| --- | --- | --- |
| `com.loopers.config.TimeConfig` | 있음 | 있음 |
| `com.loopers.support.error.CoreException` | 있음 | 있음 |
| `com.loopers.support.error.ErrorType` | 있음 | 있음 |
| `application.yml` | 있음 | 있음 |

이 상태에서 한 앱의 테스트 classpath에 다른 앱 모듈을 추가하면 다음 문제가 생길 수 있다.

- 컴포넌트 스캔 범위가 `com.loopers` 전체로 겹친다.
- bean 충돌 이전에 class/resource shadowing이 생길 수 있다.
- 우연히 테스트가 통과해도 classpath 순서나 설정 변화에 취약하다.

따라서 이번 PR 범위에서 same-classpath dual-context 방식은 유지보수 가능한 해법으로 보기 어려웠다.

### 4. 검토했던 대안

#### 대안 A. `@ConditionalOnProperty` 등으로 streamer bean 조건 분기

consumer, scheduler, use case에 조건을 달아 `commerce-api` 컨텍스트에서는 streamer bean을 끄는 방식이다.

이 접근은 bean 활성/비활성에는 도움이 될 수 있지만, 아래 이유로 충분하지 않다고 봤다.

- 동일 FQCN 클래스/리소스 중복 문제는 그대로 남는다.
- 테스트 편의를 위해 프로덕션 코드에 조건이 퍼진다.
- 구조적 문제를 가리는 우회책이 될 가능성이 크다.

#### 대안 B. `apps/system-test` 모듈 추가

cross-app 테스트 전용 모듈을 두는 방향은 관심사 분리에는 의미가 있다.
다만, 그 모듈 안에서도 두 앱을 같은 classpath로 올리면 동일 문제가 반복된다.

즉, `system-test` 모듈은 단독 해법이 아니라, 별도 process/classpath 전략과 함께 갈 때만 의미가 있다고 판단했다.

### 5. 현재 판단

현재 구조를 크게 바꾸지 않는다면, N-13을 신뢰도 있게 구현하는 현실적인 방향은 다음과 같다고 본다.

- 공용 Kafka/Redis/MySQL 테스트 인프라 준비
- `commerce-streamer`를 자기 classpath로 별도 기동
- `commerce-api`를 자기 classpath로 별도 기동
- Kafka publish -> streamer 처리 -> Redis 반영 -> api HTTP 조회까지 검증

즉, 분리된 process/classpath 기반의 system-test harness가 필요하다.

이번 PR에서는 이 harness까지 확장하지 않고, N-13은 보류하고 다음 두 종류의 하위 증거만 남겼다.

- `CatalogEventIntegrationTest`: Kafka -> 실제 streamer consumer -> Redis
- `RankingApiE2ETest`: Redis 상태 -> API 응답

이 두 테스트는 각각 의미가 있지만, 둘을 합쳐도 아직 N-13 전체를 완결한 것은 아니다.

### 6. 이번 PR에서의 결정

- N-13은 이번 PR에서 구현 완료로 주장하지 않음
- N-13은 별도 구조 작업 또는 별도 task로 분리 필요

## 멘토에게 확인받고 싶은 점

1. N-13을 이번 PR에서는 blocker로 분리하고, streamer/API 하위 흐름 검증만 남긴 판단이 적절한지
2. cross-app 시스템 테스트는 분리된 process/classpath harness로 가는 것이 맞는지
3. 장기적으로 앱별 패키지 분리 또는 공유 모듈 추출이 필요한지

## 개인 판단

이번 이슈는 "현재 설계가 완전히 잘못되었다"기보다는, 아래 두 문제가 겹치며 테스트 가능성이 크게 떨어진 사례에 가깝다고 생각한다.

- 앱 경계는 분리되어 있는데 패키지/클래스/리소스 경계는 충분히 분리되지 않음
- 공유되어야 할 코드와 앱 전용 코드가 모듈 구조상 명확히 분리되지 않음

즉, 운영 배포 자체가 당장 잘못되었다기보다는, cross-app 테스트와 유지보수 관점에서 구조적 냄새가 드러난 상황으로 이해하고 있다.
