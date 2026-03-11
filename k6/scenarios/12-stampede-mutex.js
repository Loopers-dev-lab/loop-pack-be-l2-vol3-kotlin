// 시나리오 12: 스탬피드 (Mutex 방어) — 분산 락으로 DB 부하 억제
// CACHE_MODE=layered, STAMPEDE_STRATEGY=mutex
// 실행 전: redis-cli FLUSHALL && 앱 재시작
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { getProductList } from '../helpers.js';

const latency = new Trend('stampede_mutex_latency', true);
const errorRate = new Rate('error_rate');
const dbHits = new Counter('estimated_db_hits');

export const options = {
  scenarios: {
    stampede_mutex: {
      executor: 'shared-iterations',
      vus: 200,
      iterations: 1000,
      maxDuration: '30s',
    },
  },
};

export default function () {
  const res = getProductList('popular', 0, 20);
  latency.add(res.timings.duration);
  check(res, { '200': (r) => r.status === 200 });
  errorRate.add(res.status !== 200);

  if (res.timings.duration > 100) {
    dbHits.add(1);
  }
}
