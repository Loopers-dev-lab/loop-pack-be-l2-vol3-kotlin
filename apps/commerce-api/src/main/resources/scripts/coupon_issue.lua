-- coupon_issue.lua: 원자적 수량 차감 + 중복 방지
local setKey = KEYS[1]            -- coupon-issued:{couponId}
local maxQuantity = tonumber(ARGV[1])
local userId = ARGV[2]

-- 1. 중복 체크
if redis.call('SISMEMBER', setKey, userId) == 1 then
    return -1  -- 이미 요청한 유저
end

-- 2. 수량 체크
if redis.call('SCARD', setKey) >= maxQuantity then
    return 0   -- 수량 소진
end

-- 3. 유저 추가
redis.call('SADD', setKey, userId)
return 1       -- 성공
