local queueKey = KEYS[1]
local tokenPrefix = KEYS[2]
local batchSize = tonumber(ARGV[1])
local ttl = tonumber(ARGV[2])

local members = redis.call('ZPOPMIN', queueKey, batchSize)
local issued = 0

for i = 1, #members, 2 do
    local userId = members[i]
    local token = redis.call('SET', tokenPrefix .. userId, 'GRANTED', 'EX', ttl, 'NX')
    if token then
        issued = issued + 1
    end
end

return issued
