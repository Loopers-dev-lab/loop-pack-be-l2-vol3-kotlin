# Round 8 리뷰 포인트 메모

> PR 리뷰 포인트 작성을 위한 소재 정리. 목~금 PR 본문에 반영 예정.

## Lua 스크립트 기반 원자적 대기열 진입

### 배경

대기열 진입 시 상한 검증(ZCARD) + 삽입(ZADD)이 별도 명령이면 race condition 발생 가능.
동시에 100명이 진입하면 상한 50,000을 초과할 수 있다.

### 해결

`RedisWaitingQueueRepository.enter()`에서 Lua 스크립트로 원자적 수행:

```lua
-- 1. 이미 대기열에 있으면 기존 순번 반환 (중복 진입 방지)
local existingRank = redis.call('ZRANK', KEYS[1], ARGV[2])
if existingRank then
    return existingRank
end
-- 2. 상한 검증
local currentCount = redis.call('ZCARD', KEYS[1])
if currentCount >= tonumber(ARGV[3]) then
    return -1
end
-- 3. 삽입 + 순번 반환
redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2])
return redis.call('ZRANK', KEYS[1], ARGV[2])
```

### 논의 포인트

- Lua 스크립트는 Redis 싱글 스레드에서 실행 → 원자성 보장
- 기존 유저는 상한 검증을 건너뛰고 기존 순번 반환 (ZADD NX 시맨틱)
- 반환값: 순번(0-based) 또는 -1(상한 초과) → UseCase에서 null/예외 변환
- 대안: WATCH/MULTI 트랜잭션 → 충돌 시 재시도 필요, Lua가 더 단순
