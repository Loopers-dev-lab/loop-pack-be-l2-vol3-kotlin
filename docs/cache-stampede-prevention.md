# 캐시 스탬피드(Cache Stampede) 예방 기법

캐시 스탬피드(= Cache Miss Storm, Thundering Herd)는 캐시가 만료되는 순간 다수의 요청이 동시에 DB로 몰리는 현상입니다.

---

## 1. Locking 기반 기법

### 1-1. Mutex Lock (분산 락)
캐시 미스 시 **하나의 스레드만** DB 조회를 허용, 나머지는 대기
```
Thread A → Cache Miss → Redis SETNX lock → DB 조회 → 캐시 갱신 → lock 해제
Thread B → Cache Miss → lock 획득 실패 → 대기 → 캐시 Hit
```
- **장점**: 구현 단순, DB 요청 최소화
- **단점**: 대기 스레드 발생 → 지연 증가

### 1-2. Probabilistic Early Expiration (PER)
만료 **직전**에 확률적으로 캐시를 미리 갱신
```
남은_TTL < -1/β * log(random()) → 미리 갱신
```
- **장점**: 만료 순간 스탬피드 자체를 방지
- **단점**: 불필요한 갱신 가능성

---

## 2. 만료 시간 분산

### 2-1. TTL Jitter (랜덤 TTL)
```
TTL = baseTTL + random(0, jitterRange)
```
- 동일 키들이 동시에 만료되지 않도록 분산
- 대규모 캐시 무효화(Cache Avalanche) 예방에도 효과적

### 2-2. Staggered Expiration
아이템별로 만료 시간을 의도적으로 다르게 설정

---

## 3. 캐시 갱신 전략

### 3-1. Cache-Aside + Background Refresh
```
요청 → 캐시 Hit → 남은 TTL < 임계값 → 비동기로 백그라운드 갱신 시작
                                      → 현재 요청은 기존 캐시 값 반환
```
- **장점**: 사용자는 항상 응답을 즉시 받음
- **단점**: 갱신 시점에 살짝 오래된 데이터 가능

### 3-2. Read-Through Cache
캐시 레이어가 직접 DB 조회 + 자동 갱신 담당
- 애플리케이션은 캐시만 바라봄
- 캐시 레이어 내부에서 락/갱신 처리

### 3-3. Write-Through / Write-Behind
쓰기 시점에 캐시도 함께 갱신 → 만료 자체가 발생하지 않음

---

## 4. 소프트 만료 (Soft TTL)

캐시에 **두 개의 TTL** 저장:
```json
{
  "data": "...",
  "soft_ttl": 300,
  "hard_ttl": 600
}
```
- Soft TTL 초과 시 비동기 갱신, Hard TTL까지는 응답 제공
- **장점**: 응답 지연 없음

---

## 5. 요청 통합 (Request Coalescing)

동일 키에 대한 다수 요청을 **하나로 묶어** DB를 단 한 번만 조회
```
Request 1, 2, 3 → Deduplication → DB 조회 1회 → 결과 공유
```
- DataLoader 패턴 (GraphQL 등에서 많이 사용)

---

## 비교 요약

| 기법 | 복잡도 | 대기 발생 | 오래된 데이터 | 적합한 상황 |
|------|--------|----------|-------------|------------|
| Mutex Lock | 낮음 | O | X | 단순 구현, 낮은 QPS |
| PER | 중간 | X | X | TTL 기반 단순 캐시 |
| TTL Jitter | 낮음 | X | X | 대규모 동시 만료 방지 |
| Background Refresh | 높음 | X | O (순간) | 고QPS, 낮은 지연 요구 |
| Soft TTL | 중간 | X | O (짧음) | 응답 일관성 중요 |
| Request Coalescing | 높음 | X | X | 동일 키 대량 요청 |

---

> 현재 프로젝트(Spring Cache + Redis + `@Cacheable`)에서는 **TTL Jitter**, **Background Refresh**, **Mutex Lock** 조합이 실용적입니다.
