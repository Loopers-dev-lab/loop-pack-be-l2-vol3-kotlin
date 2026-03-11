// 시나리오 10: 캐시 히트율 0% — 모든 요청이 cold
// CACHE_MODE=layered
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getProductDetail } from '../helpers.js';

const latency = new Trend('request_latency', true);
const errorRate = new Rate('error_rate');

export const options = {
  scenarios: {
    hit_rate_0: {
      executor: 'constant-vus',
      vus: 200,
      duration: '60s',
    },
  },
};

// 모든 요청이 다른 상품 → 캐시 히트 불가
let counter = 0;

export default function () {
  counter++;
  const productId = (counter % 100000) + 1;

  const res = getProductDetail(productId);
  latency.add(res.timings.duration);
  check(res, { '2xx': (r) => r.status === 200 || r.status === 404 });
  errorRate.add(res.status !== 200 && res.status !== 404);

  sleep(0.1);
}
