// 시나리오 05: Layered (Caffeine→Redis→DB) — 전체 계층 적용 시 성능
// CACHE_MODE=layered
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getProductList, getProductDetail, randomPage, randomProductId, randomBrandId } from '../helpers.js';

const popularLatency = new Trend('popular_latency', true);
const detailLatency = new Trend('detail_latency', true);
const errorRate = new Rate('error_rate');

export const options = {
  scenarios: {
    layered: {
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
