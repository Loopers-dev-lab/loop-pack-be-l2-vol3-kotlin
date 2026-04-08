# Queue experiment comparison

## Implemented strategies

- `REDIS_ONLY`: Redis Sorted Set keeps FIFO order and Redis TTL token gates order creation.
- `REDIS_KAFKA`: Redis keeps the waiting room and Kafka delivers asynchronous admission events before Redis token issuance.
- `KAFKA_ONLY`: Kafka offset becomes queue order, while MySQL stores the queryable projection and issued tokens.
- `PESSIMISTIC_LOCK`: MySQL queue rows are admitted with `FOR UPDATE SKIP LOCKED`.
- `DISTRIBUTED_LOCK`: MySQL queue rows are admitted under a Redis `SET NX PX` lock.

## Shared API shape

- `POST /api/v1/queue/enter`
- `GET /api/v1/queue/position`
- `POST /api/v1/orders` with `X-Queue-Token` and optional `X-Queue-Strategy`

## Batch size formula

The scheduler batch size is derived from the DB connection budget and average order processing time.

`floor(dbConnectionPoolSize * dbUtilizationRatio * schedulerInterval / avgOrderProcessingTime)`

With the current defaults:

- DB pool size: `40`
- Safe utilization: `0.7`
- Scheduler interval: `1s`
- Average order processing time: `2s`

The computed admission batch size is `14` per scheduler tick.

## What this harness is good for

- Fairness checks under concurrent entry
- Token TTL expiration checks
- Scheduler batch-size behavior under backlog
- Relative implementation complexity across Redis, Kafka, and DB-lock based approaches

## Practical comparison guidance

- Prefer `REDIS_ONLY` when low latency and simple FIFO admission matter most.
- Prefer `REDIS_KAFKA` when admission side effects must be decoupled from scheduler execution.
- Treat `KAFKA_ONLY` as an experiment baseline, because Kafka still needs a projection store for position queries.
- Use `PESSIMISTIC_LOCK` when the queue must stay in MySQL and scheduler coordination should stay transactional.
- Use `DISTRIBUTED_LOCK` when multiple scheduler instances may race and Redis is already part of the operational stack.
