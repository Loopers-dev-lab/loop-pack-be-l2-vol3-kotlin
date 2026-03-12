// 시나리오 07: 캐시 히트율 70% — 히트율↔latency 상관관계 측정
// CACHE_MODE=layered
// 요청의 70%는 상위 인기 상품(사전 워밍), 30%는 cold ID
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { getProductDetail, getProductList, randomPage } from '../helpers.js';

const latency = new Trend('request_latency', true);
const errorRate = new Rate('error_rate');

export const options = {
  scenarios: {
    hit_rate_70: {
      executor: 'shared-iterations',
      vus: 200,
      iterations: 1000,
      maxDuration: '30s',
    },
  },
};

// 워밍 대상: 상품 ID 1~1000 (인기 상위)
const HOT_IDS_MAX = 1000;
const COLD_IDS_MIN = 50001;
const COLD_IDS_MAX = 100000;
const HIT_RATE = 0.7;

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
}
