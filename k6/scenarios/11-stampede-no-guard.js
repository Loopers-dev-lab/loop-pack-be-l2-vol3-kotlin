// 시나리오 11: 스탬피드 (방어 없음) — 캐시 flush 직후 200 VU 동시 요청
// CACHE_MODE=layered, STAMPEDE_STRATEGY=none
// 실행 전: redis-cli FLUSHALL && 앱 재시작
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { getProductList, getProductDetail } from '../helpers.js';

const latency = new Trend('stampede_latency', true);
const errorRate = new Rate('error_rate');
const dbHits = new Counter('estimated_db_hits');

export const options = {
  scenarios: {
    stampede_burst: {
      executor: 'shared-iterations',
      vus: 200,
      iterations: 1000,
      maxDuration: '30s',
    },
  },
};

export default function () {
  // 동일한 인기순 첫 페이지를 200 VU가 동시에 요청
  const res = getProductList('popular', 0, 20);
  latency.add(res.timings.duration);
  check(res, { '200': (r) => r.status === 200 });
  errorRate.add(res.status !== 200);

  // 응답 시간이 100ms 이상이면 DB 히트로 추정
  if (res.timings.duration > 100) {
    dbHits.add(1);
  }
}
