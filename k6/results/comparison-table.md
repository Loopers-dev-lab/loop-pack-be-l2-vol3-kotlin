| strategy | order_success | order_fail | token_timeout | errors | http_req_duration_p95 | http_req_duration_p99 | queue_enter_p95 | queue_poll_p95 | order_p95 | end_to_end_p95 | wait_avg | wait_p95 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DISTRIBUTED_LOCK | 40 | 0 | 0 | 0 | 243.51 | 277.53 | 278.91 | 169.74 | 130.64 | 4317.25 | 2.70 | 4.23 |
| KAFKA_ONLY | 40 | 0 | 0 | 0 | 811.53 | 816.97 | 817.97 | 259.11 | 154.72 | 4581 | 2.97 | 4.50 |
| PESSIMISTIC_LOCK | 0 | 0 | 40 | 0.04 | 192.37 | 253.71 | 259.39 | 181.64 | 0 | 0.00 | 0.00 | 0.00 |
| REDIS_KAFKA | 40 | 0 | 0 | 0 | 235.81 | 252.97 | 249.93 | 172.47 | 126.61 | 4642.10 | 3.00 | 4.57 |
| REDIS_ONLY | 40 | 0 | 0 | 0 | 228.04 | 256.60 | 256.83 | 208.00 | 204.09 | 3606.05 | 1.93 | 3.52 |
