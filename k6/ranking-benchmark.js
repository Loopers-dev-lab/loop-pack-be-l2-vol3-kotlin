import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// ── Custom Metrics ──
const dbLatency = new Trend('a_db_direct', true);
const redisLatency = new Trend('b_redis_zset', true);
const cache5Latency = new Trend('c5_cache_5s', true);
const cache60Latency = new Trend('c60_cache_60s', true);
const sfLatency = new Trend('d_singleflight', true);
const hybridLatency = new Trend('h_hybrid_l1_sf', true);

const dbErrors = new Rate('a_db_errors');
const redisErrors = new Rate('b_redis_errors');
const cache5Errors = new Rate('c5_cache_5s_errors');
const cache60Errors = new Rate('c60_cache_60s_errors');
const sfErrors = new Rate('d_sf_errors');
const hybridErrors = new Rate('h_hybrid_errors');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const DATE = __ENV.DATE || new Date().toISOString().slice(0, 10).replace(/-/g, '');

// ── 90초 per scenario, 순차 실행 ──
const STAGE_DURATION = '1m30s';

export const options = {
  scenarios: {
    // 그룹 1: DB vs Redis
    a_db_direct: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 50 },
        { duration: '30s', target: 200 },
        { duration: '30s', target: 500 },
        { duration: '15s', target: 0 },
      ],
      startTime: '0s',
      exec: 'scenarioDbDirect',
    },
    b_redis_zset: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 50 },
        { duration: '30s', target: 200 },
        { duration: '30s', target: 500 },
        { duration: '15s', target: 0 },
      ],
      startTime: '95s',
      exec: 'scenarioRedisZset',
    },

    // 그룹 2: freshness 예산별
    c5_cache_5s: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 100 },
        { duration: '30s', target: 500 },
        { duration: '30s', target: 1000 },
        { duration: '15s', target: 0 },
      ],
      startTime: '190s',
      exec: 'scenarioCache5s',
    },
    c60_cache_60s: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 100 },
        { duration: '30s', target: 500 },
        { duration: '30s', target: 1000 },
        { duration: '15s', target: 0 },
      ],
      startTime: '285s',
      exec: 'scenarioCache60s',
    },
    d_singleflight: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 100 },
        { duration: '30s', target: 500 },
        { duration: '30s', target: 1000 },
        { duration: '15s', target: 0 },
      ],
      startTime: '380s',
      exec: 'scenarioSingleflight',
    },

    // 그룹 3: 하이브리드
    h_hybrid: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 100 },
        { duration: '30s', target: 500 },
        { duration: '30s', target: 1000 },
        { duration: '15s', target: 0 },
      ],
      startTime: '475s',
      exec: 'scenarioHybrid',
    },
  },

  thresholds: {
    'a_db_direct': ['p(95)<5000'],
    'b_redis_zset': ['p(95)<2000'],
    'c5_cache_5s': ['p(95)<500'],
    'c60_cache_60s': ['p(95)<200'],
    'd_singleflight': ['p(95)<1000'],
    'h_hybrid_l1_sf': ['p(95)<500'],
  },
};

// ── Helper ──
function rankingGet(endpoint, latencyMetric, errorMetric) {
  const page = Math.floor(Math.random() * 3);
  const url = `${BASE_URL}${endpoint}${endpoint.includes('?') ? '&' : '?'}date=${DATE}&page=${page}&size=20`;

  const res = http.get(url, { timeout: '10s' });
  latencyMetric.add(res.timings.duration);

  const success = check(res, {
    'status 200': (r) => r.status === 200,
    'success response': (r) => {
      try { return JSON.parse(r.body).meta.result === 'SUCCESS'; }
      catch { return false; }
    },
  });

  errorMetric.add(!success);
  sleep(0.1);
}

// ── Scenario Functions ──
export function scenarioDbDirect() {
  rankingGet('/api/v1/rankings/benchmark/db', dbLatency, dbErrors);
}

export function scenarioRedisZset() {
  rankingGet('/api/v1/rankings', redisLatency, redisErrors);
}

export function scenarioCache5s() {
  rankingGet('/api/v1/rankings/benchmark/cached?ttl=5', cache5Latency, cache5Errors);
}

export function scenarioCache60s() {
  rankingGet('/api/v1/rankings/benchmark/cached?ttl=60', cache60Latency, cache60Errors);
}

export function scenarioSingleflight() {
  rankingGet('/api/v1/rankings/benchmark/singleflight', sfLatency, sfErrors);
}

export function scenarioHybrid() {
  rankingGet('/api/v1/rankings/benchmark/hybrid', hybridLatency, hybridErrors);
}

// ── Summary ──
export function handleSummary(data) {
  const scenarios = [
    { name: 'A: DB Direct', fresh: '-', metric: 'a_db_direct', error: 'a_db_errors' },
    { name: 'B: Redis ZSET', fresh: '0s', metric: 'b_redis_zset', error: 'b_redis_errors' },
    { name: 'C-5: Cache 5s', fresh: '5s', metric: 'c5_cache_5s', error: 'c5_cache_5s_errors' },
    { name: 'C-60: Cache 60s', fresh: '60s', metric: 'c60_cache_60s', error: 'c60_cache_60s_errors' },
    { name: 'D: Singleflight', fresh: '~ms', metric: 'd_singleflight', error: 'd_sf_errors' },
    { name: 'H: L1(5s)+SF', fresh: '5s+SF', metric: 'h_hybrid_l1_sf', error: 'h_hybrid_errors' },
  ];

  const W = 72;
  let s = '\n' + '='.repeat(W) + '\n';
  s += '  랭킹 벤치마크 결과 — freshness 예산별 비교\n';
  s += '='.repeat(W) + '\n';
  s += '  Scenario            │ fresh  │   p50  │   p95  │   max  │  Err% \n';
  s += '──────────────────────┼────────┼────────┼────────┼────────┼───────\n';

  for (const sc of scenarios) {
    const m = data.metrics[sc.metric];
    const e = data.metrics[sc.error];
    if (m && m.values) {
      const p50 = (m.values['med'] || 0).toFixed(0).padStart(5);
      const p95 = (m.values['p(95)'] || 0).toFixed(0).padStart(5);
      const max = (m.values['max'] || 0).toFixed(0).padStart(5);
      const errRate = e && e.values ? (e.values.rate * 100).toFixed(1).padStart(4) : ' 0.0';
      const name = sc.name.padEnd(20);
      const fresh = sc.fresh.padEnd(6);
      s += `  ${name} │ ${fresh} │ ${p50}ms │ ${p95}ms │ ${max}ms │ ${errRate}%\n`;
    }
  }
  s += '='.repeat(W) + '\n';

  return {
    stdout: s,
    'k6/ranking-benchmark-result.json': JSON.stringify(data, null, 2),
  };
}
