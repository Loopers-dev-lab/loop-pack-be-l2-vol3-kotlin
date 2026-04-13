# Round 9 리뷰 포인트

## N-13 Cross-App 시스템 테스트 관련 질문

제가 이해한 N-13의 요구사항은, 단순히 API 하나를 검증하는 것이 아니라 아래 흐름 전체를 확인하는 것입니다.

1. Kafka topic에 실제 이벤트 발행
2. 실제 `commerce-streamer`의 `@KafkaListener`가 이벤트 소비
3. Redis ZSET에 점수 반영
4. `commerce-api` HTTP API에서 최종 랭킹 응답 확인

즉, `commerce-streamer`와 `commerce-api`를 함께 검증하는 cross-app 시스템 테스트가 필요하다고 이해했습니다.

이 이해가 맞는지 먼저 확인받고 싶습니다.

## 제가 처음 시도했던 방식

처음에는 `commerce-api` 테스트 안에서 Kafka 이벤트를 발행하고, 테스트용 consumer를 `@Import`로 추가하여 Redis를 갱신한 뒤 API를 조회하는 방식을 시도했습니다.

그런데 이 방식은

- Kafka publish 자체는 검증할 수 있고
- API가 Redis 상태를 읽어 응답하는지도 검증할 수 있지만
- 실제 `commerce-streamer`의 consumer, use case, scheduler 경로가 동작했는지는 증명하지 못해서

결국 "Kafka 계약 + API 조회" 수준의 테스트이지, "실제 streamer가 동작하는가"를 보여주는 시스템 테스트는 아니라고 판단했습니다.

이 판단이 맞는지도 확인받고 싶습니다.

## 현재 구조에서 왜 어렵다고 느꼈는지

이번 PR 범위에서 in-process 방식으로 두 앱을 한 테스트에 함께 올리려다 보니, 구조적으로 부담이 크다고 느꼈습니다.

제가 확인한 이유는 다음과 같습니다.

- 두 앱이 동일한 루트 패키지 `com.loopers`를 사용합니다.
- 두 앱에 동일한 클래스명/FQCN을 가진 파일이 실제로 중복 존재합니다.
- 두 앱에 `application.yml` 같은 리소스도 각각 존재합니다.

예를 들면:

| 구분 | commerce-api | commerce-streamer |
| --- | --- | --- |
| `com.loopers.config.TimeConfig` | 있음 | 있음 |
| `com.loopers.support.error.CoreException` | 있음 | 있음 |
| `com.loopers.support.error.ErrorType` | 있음 | 있음 |
| `application.yml` | 있음 | 있음 |

이 상태에서 한 앱의 테스트 classpath에 다른 앱 모듈을 추가하면

- 컴포넌트 스캔 범위가 `com.loopers` 전체로 겹칠 수 있고
- bean 충돌 이전에 class/resource shadowing이 생길 수 있고
- 우연히 테스트가 통과하더라도 classpath 순서나 설정 변화에 취약할 수 있다고 판단했습니다.

제가 문제를 이렇게 이해한 것이 맞는지 궁금합니다.

## 제가 검토한 대안

### 대안 A. `@ConditionalOnProperty` 등으로 streamer bean 조건 분기

consumer, scheduler, use case에 조건을 달아 `commerce-api` 컨텍스트에서는 streamer bean을 끄는 방법을 생각해봤습니다.

다만 저는 이 접근이

- bean 활성/비활성에는 도움이 될 수 있어도
- 동일 FQCN 클래스/리소스 중복 문제는 그대로 남고
- 테스트 편의를 위해 프로덕션 코드에 조건이 퍼질 수 있어서

근본 해결책으로는 애매하다고 느꼈습니다.

### 대안 B. `apps/system-test` 모듈 추가

cross-app 테스트 전용 모듈을 두는 방향도 생각해봤습니다.

그런데 이 경우에도 두 앱을 같은 classpath로 올리면 동일 문제가 반복될 수 있어서, `system-test` 모듈 자체가 해법이라기보다는 별도 process/classpath 전략과 함께 가야 의미가 있겠다고 생각했습니다.

## 현재 제 판단

현재 구조를 크게 바꾸지 않는다면, N-13을 신뢰도 있게 구현하려면 결국

- 공용 Kafka/Redis/MySQL 테스트 인프라를 준비하고
- `commerce-streamer`를 자기 classpath로 별도 기동하고
- `commerce-api`를 자기 classpath로 별도 기동한 뒤
- Kafka publish -> streamer 처리 -> Redis 반영 -> api HTTP 조회까지 검증하는

분리된 process/classpath 기반의 system-test harness가 필요해 보입니다.

그래서 이번 PR에서는 이 harness까지 확장하지 않고, N-13은 보류하고 다음 두 종류의 하위 증거만 남겼습니다.

- `CatalogEventIntegrationTest`: Kafka -> 실제 streamer consumer -> Redis
- `RankingApiE2ETest`: Redis 상태 -> API 응답

이 두 테스트는 각각 의미는 있다고 생각하지만, 둘을 합쳐도 아직 N-13 전체를 완결한 것은 아니라고 이해하고 있습니다.

이 판단이 적절한지도 여쭙고 싶습니다.

## 멘토님께 여쭙고 싶은 점

1. 제가 이해한 것처럼, 이번 N-13 요구사항은 사실상 `api -> Kafka -> streamer -> Redis -> api`를 가로지르는 시스템 검증으로 보는 것이 맞을까요?
2. 이런 구조에서는 보통 어느 수준까지 테스트를 작성하시나요?
   - 하위 흐름을 나눠서 각각 검증
   - 별도 process/classpath harness를 만들어 cross-app system test 작성
   - 또는 스테이징/통합 환경에서만 전체 흐름 확인
3. 이번 PR에서는 N-13을 blocker로 분리하고, streamer/API 하위 흐름 검증만 남기는 판단이 적절할까요?
4. 장기적으로는 앱별 패키지 분리 또는 공유 모듈 추출이 필요하다고 보시는지 궁금합니다.

## 제 개인적인 이해

저는 이 이슈를 "현재 설계가 완전히 잘못되었다"기보다는,

- 앱 경계는 분리되어 있는데 패키지/클래스/리소스 경계는 충분히 분리되지 않았고
- 공유되어야 할 코드와 앱 전용 코드가 모듈 구조상 명확히 정리되지 않아서

cross-app 테스트와 유지보수 관점에서 구조적 냄새가 드러난 상황이라고 이해하고 있습니다.

이 이해가 맞는지도 함께 확인 부탁드립니다.
