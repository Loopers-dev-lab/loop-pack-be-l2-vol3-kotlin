# 멀티 레이어 캐시 전략: Local Cache + Redis Cache

## 배경

상품 조회 API에 Redis 캐시(cache-aside 패턴)가 적용된 상태에서, 로컬 캐시를 추가하여 성능을 한 단계 더 개선하고자 했다.

```
[Before] Controller → Facade → CacheManager → Redis → DB
[After]  Controller → Facade → CacheManager → Local(Caffeine) → Redis → DB
```

로컬 캐시 도입 시 핵심 과제는 **cache stampede 방지**와 **아키텍처 일관성(DIP)** 두 가지였다.

## Redis 캐시 데이터 최신화 전략

로컬 캐시를 추가하기 전에, 기반이 되는 Redis 캐시의 데이터 최신화 전략을 먼저 정리한다. Redis 캐시는 공유 캐시로서 **스케줄러 기반 워밍**으로 데이터를 최신화한다.

### 워밍 스케줄러 (`ProductCacheWarmingScheduler`)

```
@Scheduled(initialDelay = 0, fixedRate = 2분)
fun warmProductListCache()
```

| 설정 | 값 | 의미 |
|------|-----|------|
| initialDelay | 0 | 서버 시작 직후 즉시 실행 (cold start 방지) |
| fixedRate | 2분 | 목록 캐시 TTL(3분)보다 짧은 주기로 갱신 |

### 워밍 대상

```
상위 10개 브랜드 + 전체(brandId=null)
  × 3가지 정렬 (최신순, 가격순, 좋아요순)
  × 3페이지 (0, 1, 2)
= 99건 캐시 항목
```

인기 있는 조회 조건을 사전에 캐싱하여, 대부분의 요청이 Redis에서 즉시 응답할 수 있도록 한다.

### 서버 시작 시 캐시 워밍 흐름

```
서버 시작
  │
  ▼
@Scheduled(initialDelay = 0) 실행
  │
  ▼
productCacheManager.getProducts(brandId, pageQuery) 호출
  │
  ├─ 로컬 캐시: miss → loader 실행
  │     ├─ Redis 캐시: miss (서버 재시작 후 TTL 만료 or 비어있음)
  │     │     ├─ DB 조회
  │     │     └─ Redis에 저장
  │     └─ 로컬에 저장
  │
  └─ 99건 반복 → Redis + 로컬 캐시 모두 워밍 완료
```

- **Redis 캐시**: 공유 캐시이므로 1대의 인스턴스만 워밍하면 모든 인스턴스가 혜택
- **로컬 캐시**: 워밍 스케줄러가 실행되는 인스턴스의 로컬 캐시도 함께 적재 (cold start 방지)
- **다른 인스턴스**: 첫 요청 시 Redis에서 로컬로 로드 (Redis hit이므로 ~1ms)

### 워밍 주기와 TTL의 관계

```
Redis 목록 TTL: 3분
워밍 주기:      2분

[정상 상태] 워밍 주기 < TTL → 만료 전에 항상 갱신
0m        2m        3m        4m        5m
|--워밍----|--워밍----|--워밍----|--워밍----|
     ↑          ↑
  캐시 적재   만료(3m) 전에 갱신 → 끊김 없음

[워밍 실패 시] TTL 만료 후 cache-aside로 자연 복구
0m        2m        3m        4m
|--워밍----|--실패----|--만료----|--워밍----|
                      ↑         ↑
               캐시 만료    다음 워밍에서 복구
               (요청 시 cache-aside로 DB 조회)
```

- 워밍 주기(2분) < TTL(3분) 이므로, 정상 상태에서는 캐시가 만료되기 전에 항상 갱신된다
- 워밍이 일시적으로 실패하더라도 TTL 만료 후 cache-aside가 동작하여 자연 복구된다

## 의사결정 1: 로컬 캐시 Cache Stampede 방지 전략

Redis 캐시 위에 로컬 캐시를 추가할 때, TTL 만료 시점에 동시 요청이 Redis로 몰리는 cache stampede 문제를 해결할 방법을 검토했다.

### A. 스케줄러 기반 (주기적 워밍)

스케줄러가 주기적으로 Redis에서 데이터를 읽어 로컬 캐시를 갱신하는 방식.

```
[스케줄러] --주기적-→ Redis 조회 → 로컬 캐시 갱신
[요청]    --→ 로컬 캐시 hit → 즉시 반환
```

| 장점 | 단점 |
|------|------|
| stampede 완전 차단 | 인스턴스 수 × 워밍 부하 (로컬 캐시는 인스턴스별) |
| 구현 단순 | 접근하지 않는 키도 워밍 (메모리 낭비) |
| 예측 가능한 부하 | 워밍 대상 키를 사전에 알아야 함 |

Redis 캐시 워밍은 1대만 실행하면 모든 인스턴스가 혜택을 받지만, 로컬 캐시는 인스턴스마다 독립적이라 효율이 떨어진다.

### B. Redis 조회 기반 (요청 시 cache-aside)

요청 시 로컬 캐시 미스 → Redis 조회 → 로컬 캐시 저장하는 일반적 cache-aside.

```
[요청] → 로컬 캐시 miss → Redis 조회 → 로컬 캐시 저장
```

| 장점 | 단점 |
|------|------|
| 실제 접근하는 키만 캐싱 | **stampede 문제 그대로** |
| 구현 단순 | 만료 시점에 동시 요청이 Redis로 몰림 |

단독으로는 stampede 방어가 없으므로 별도 보호 메커니즘이 필요하다.

### C. Caffeine Cache.get(key, loader) (선택)

Caffeine의 `Cache.get(key, mappingFunction)` 원자적 로딩을 활용하는 방식.

```
스레드 A: Cache.get(key, loader)
  → 캐시 미스 → loader 실행 (이 스레드만)
  → Redis 조회 → 값 반환

스레드 B, C (동시 접근, 같은 key):
  → 스레드 A의 로딩 완료를 대기 → 같은 값 반환
```

| 장점 | 단점 |
|------|------|
| **stampede 완전 차단** (키별 원자적 로딩) | 로딩 중 다른 스레드가 짧게 대기 |
| 접근하는 키만 캐싱 (lazy) | |
| Caffeine이 내부적으로 처리 (코드 단순) | |

### 선택 근거

| 기준 | A. 스케줄러 | B. cache-aside | C. Caffeine |
|------|------------|---------------|-------------|
| stampede 방지 | O | **X** | **O** |
| 메모리 효율 | X (전체 워밍) | O (lazy) | **O (lazy)** |
| 멀티 인스턴스 효율 | X (N배 부하) | O | **O** |
| 구현 복잡도 | 중 | 낮 | **낮** |

- 스케줄러 방식은 Redis 워밍(공유 캐시)에는 적합하지만, 로컬 캐시(인스턴스별)에는 비효율적
- Caffeine의 원자적 로딩이 가장 적은 코드로 stampede를 방지하면서 lazy하게 동작
- 대기 시간은 Redis 조회 시간(~1ms) 수준으로 실질적 영향 없음

## 의사결정 2: 로컬 캐시의 아키텍처 배치 (DIP)

Caffeine 도입 시 기술 구현(Caffeine)을 어느 계층에 배치할지 검토했다.

### A. 정책만 추출 (가벼운 분리)

로컬 캐시 정책 상수를 별도 객체로 추출하되, Caffeine 캐시 생성과 사용은 `ProductCacheManager`(application)에 유지.

```
ProductLocalCachePolicy  (application - 상수만)
ProductCacheManager      (application - Caffeine 직접 사용)
```

| 장점 | 단점 |
|------|------|
| 변경 최소 | Caffeine 의존이 application 계층에 잔존 |
| Redis쪽 ProductCachePolicy와 대칭 | 기술 선택이 오케스트레이션 계층에 노출 |

### B. 인터페이스로 분리 (DIP 적용, 선택)

Redis 캐시와 동일하게 인터페이스/구현체로 분리. `ProductCacheManager`는 오케스트레이션만 담당.

```
ProductLocalCacheRepository      (application - 인터페이스)
ProductLocalCacheRepositoryImpl  (infrastructure - Caffeine 구현)
ProductCacheManager              (application - 오케스트레이션만)
```

| 장점 | 단점 |
|------|------|
| DIP 일관성 (Redis 캐시와 동일 패턴) | 파일 수 증가 (인터페이스 + 구현체) |
| application 계층에서 Caffeine 의존 제거 | |
| 테스트 시 로컬 캐시 Mock 가능 | |
| 캐시 기술 교체 시 구현체만 변경 | |

### C. 정책도 추출하지 않음 (현상 유지)

모든 로컬 캐시 관련 코드를 `ProductCacheManager`에 유지.

```
ProductCacheManager  (application - 정책 + Caffeine + 오케스트레이션)
```

| 장점 | 단점 |
|------|------|
| 파일 수 최소 | 단일 클래스에 3개 책임 혼재 |
| | Redis 캐시와 패턴 불일치 |

### 선택 근거

| 기준 | A. 정책 추출 | B. DIP 적용 | C. 현상 유지 |
|------|------------|------------|------------|
| DIP 일관성 | △ | **O** | X |
| Redis 패턴 대칭 | △ | **O** | X |
| 테스트 용이성 | X | **O** | X |
| 변경 범위 | 중 | 대 | 없음 |

- 기존 Redis 캐시가 `ProductCacheRepository`(interface) + `ProductCacheRepositoryImpl`(infrastructure) 패턴을 이미 사용 중
- 동일 패턴을 로컬 캐시에도 적용하여 아키텍처 일관성 확보
- `ProductCacheManager`가 "어떤 순서로 캐시를 조회할 것인가"(오케스트레이션)에만 집중

## 최종 구조

```
Controller → ProductFacade → ProductCacheManager (오케스트레이션)
                                  │
                  ┌───────────────┼───────────────┐
                  ▼               ▼               ▼
         ProductLocal       ProductCache      ProductService
         CacheRepository    Repository        (domain)
         (interface)        (interface)
                  │               │
                  ▼               ▼
         ...Impl            ...Impl
         (Caffeine)         (Redis)

ProductCacheWarmingScheduler ──→ ProductCacheManager
  (@Scheduled: 서버 시작 즉시 + 2분 주기)
```

### 캐시 정책

| 구분 | 로컬 캐시 (Caffeine) | Redis 캐시 |
|------|---------------------|-----------|
| 상품 상세 | expire 60초, 최대 1,000건 | TTL 30초 |
| 상품 목록 | expire 5분, 최대 200건 | TTL 3분 |
| stampede 방지 | Cache.get(key, loader) 원자적 로딩 | 워밍 스케줄러 (2분 주기) |

### 조회 흐름

```
1. 로컬 캐시 hit  → 즉시 반환 (0ms)
2. 로컬 캐시 miss → Redis 조회 hit → 로컬에 저장 → 반환 (~1ms)
3. 둘 다 miss     → DB 조회 → Redis 저장 → 로컬에 저장 → 반환 (~5-50ms)
```

### 무효화 흐름

```
evictProduct(id)    → 로컬 캐시 invalidate + Redis 캐시 delete
evictAllProductLists()  → 로컬 목록 캐시 전체 invalidate + Redis 목록 캐시 전체 delete
evictAllLocalCaches() → 로컬 캐시 전체 invalidate (테스트용)
```
