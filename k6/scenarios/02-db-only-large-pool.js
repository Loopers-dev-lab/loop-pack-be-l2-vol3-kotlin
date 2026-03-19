// 시나리오 02: DB Only (대형 풀 200) — 커넥션 병목 격리
// CACHE_MODE=db_only, maximum-pool-size=200
// 01과 비교하여 커넥션 풀 병목 여부 확인
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getProductList, getProductDetail, randomPage, randomProductId, randomBrandId } from '../helpers.js';

const popularLatency = new Trend('popular_latency', true);
const detailLatency = new Trend('detail_latency', true);
const errorRate = new Rate('error_rate');

export const options = {
  scenarios: {
    db_only_large_pool: {
      executor: 'constant-vus',
      vus: 200,
      duration: '60s',
    },
  },
  thresholds: {
    error_rate: ['rate<0.05'],
  },
};

export default function () {
  const scenario = Math.random();

  if (scenario < 0.4) {
    const res = getProductList('popular', randomPage(), 20);
    popularLatency.add(res.timings.duration);
    check(res, { 'popular 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
  } else if (scenario < 0.6) {
    const res = getProductList('popular', randomPage(), 20, randomBrandId());
    popularLatency.add(res.timings.duration);
    check(res, { 'popular+brand 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
  } else if (scenario < 0.8) {
    const res = getProductList('latest', randomPage(), 20);
    check(res, { 'latest 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
  } else {
    const res = getProductDetail(randomProductId());
    detailLatency.add(res.timings.duration);
    check(res, { 'detail 2xx': (r) => r.status === 200 || r.status === 404 });
    errorRate.add(res.status !== 200 && res.status !== 404);
  }

  sleep(0.1);
}
