-- stock_reserve.lua: 원자적 재고 선점 (DECR + 음수 방지)
local stockKey = KEYS[1]
local quantity = tonumber(ARGV[1])

local current = redis.call('DECRBY', stockKey, quantity)
if current < 0 then
    redis.call('INCRBY', stockKey, quantity)
    return 0
end
return 1
