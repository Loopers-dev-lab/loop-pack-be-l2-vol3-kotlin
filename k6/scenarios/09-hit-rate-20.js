// 시나리오 09: 캐시 히트율 20%
// CACHE_MODE=layered
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getProductDetail } from '../helpers.js';

const latency = new Trend('request_latency', true);
const errorRate = new Rate('error_rate');

export const options = {
  scenarios: {
    hit_rate_20: {
      executor: 'constant-vus',
      vus: 200,
      duration: '60s',
    },
  },
};

const HOT_IDS_MAX = 1000;
const COLD_IDS_MIN = 50001;
const COLD_IDS_MAX = 100000;
const HIT_RATE = 0.2;

function hotProductId() {
  return Math.floor(Math.random() * HOT_IDS_MAX) + 1;
}

function coldProductId() {
  return Math.floor(Math.random() * (COLD_IDS_MAX - COLD_IDS_MIN)) + COLD_IDS_MIN;
}

export default function () {
  const isHot = Math.random() < HIT_RATE;
  const productId = isHot ? hotProductId() : coldProductId();

  const res = getProductDetail(productId);
  latency.add(res.timings.duration);
  check(res, { '2xx': (r) => r.status === 200 || r.status === 404 });
  errorRate.add(res.status !== 200 && res.status !== 404);

  sleep(0.1);
}
