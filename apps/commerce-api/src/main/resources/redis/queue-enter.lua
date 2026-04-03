local queueKey = KEYS[1]
local counterKey = KEYS[2]
local userId = ARGV[1]
local maxQueueSize = tonumber(ARGV[2])

-- 이미 대기열에 있으면 새로 넣지 않음
local rank = redis.call('ZRANK', queueKey, userId)
if rank then
    return -1
end

-- 대기열 최대 인원 초과 확인
local queueSize = redis.call('ZCARD', queueKey)
if maxQueueSize > 0 and queueSize >= maxQueueSize then
    return -2
end

-- 원자적 카운터로 score 생성 — 절대 겹치지 않아 FIFO 순서 보장
local score = redis.call('INCR', counterKey)

redis.call('ZADD', queueKey, score, userId)

return redis.call('ZRANK', queueKey, userId)
