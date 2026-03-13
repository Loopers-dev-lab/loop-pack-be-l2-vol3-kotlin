// 시나리오 13: 스탬피드 (Warmup 방어) — 사전 워밍으로 콜드 스타트 방어
// CACHE_MODE=layered, STAMPEDE_STRATEGY=warmup
// 앱 시작 시 ProductCacheWarmer가 인기 상품 사전 캐시
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { getProductList } from '../helpers.js';

const latency = new Trend('stampede_warmup_latency', true);
const errorRate = new Rate('error_rate');
const dbHits = new Counter('estimated_db_hits');

export const options = {
  scenarios: {
    stampede_warmup: {
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
