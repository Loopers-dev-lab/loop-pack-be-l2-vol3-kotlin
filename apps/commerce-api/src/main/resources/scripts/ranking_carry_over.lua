-- ranking_carry_over.lua: ZUNIONSTORE로 전날 점수를 가중치 곱해 복사 + TTL 설정
-- KEYS[1] = fromKey (전날 랭킹)
-- KEYS[2] = toKey (다음날 랭킹)
-- ARGV[1] = weight (가중치)
-- ARGV[2] = TTL (seconds)
redis.call('ZUNIONSTORE', KEYS[2], 1, KEYS[1], 'WEIGHTS', ARGV[1])
redis.call('EXPIRE', KEYS[2], tonumber(ARGV[2]))
return 1
