-- ranking_increment.lua: ZINCRBY + 조건부 TTL 설정
-- KEYS[1] = ranking key
-- ARGV[1] = member (productId)
-- ARGV[2] = increment (score)
-- ARGV[3] = TTL (seconds)
local score = redis.call('ZINCRBY', KEYS[1], ARGV[2], ARGV[1])
if redis.call('TTL', KEYS[1]) == -1 then
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]))
end
return score
