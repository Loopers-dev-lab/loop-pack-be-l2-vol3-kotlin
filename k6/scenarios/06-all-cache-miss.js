// 시나리오 06: All Cache Miss (Layered) — 모든 캐시 미스 시 추가 홉 비용
// CACHE_MODE=layered + 캐시 flush 후 실행
// 실행 전: redis-cli FLUSHALL && 앱 재시작 (Caffeine 초기화)
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getProductList, getProductDetail, randomBrandId } from '../helpers.js';

const popularLatency = new Trend('popular_latency', true);
const detailLatency = new Trend('detail_latency', true);
const errorRate = new Rate('error_rate');

// 짧은 시간 내에 모든 요청이 cold start
export const options = {
  scenarios: {
    all_cache_miss: {
      executor: 'constant-vus',
      vus: 200,
      duration: '30s',
    },
  },
  thresholds: {
    error_rate: ['rate<0.05'],
  },
};

// 매번 다른 페이지/상품을 요청하여 캐시 미스 유도
let counter = 0;

export default function () {
  counter++;
  const uniquePage = counter % 5000;
  const uniqueProductId = (counter % 100000) + 1;

  const scenario = Math.random();

  if (scenario < 0.5) {
    const res = getProductList('popular', uniquePage, 20);
    popularLatency.add(res.timings.duration);
    check(res, { 'popular 200': (r) => r.status === 200 });
    errorRate.add(res.status !== 200);
  } else {
    const res = getProductDetail(uniqueProductId);
    detailLatency.add(res.timings.duration);
    check(res, { 'detail 2xx': (r) => r.status === 200 || r.status === 404 });
    errorRate.add(res.status !== 200 && res.status !== 404);
  }

  sleep(0.1);
}
