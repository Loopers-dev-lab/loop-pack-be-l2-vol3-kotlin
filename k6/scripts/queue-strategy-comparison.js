import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const STRATEGY = __ENV.QUEUE_STRATEGY || 'REDIS_ONLY';
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;
const USER_COUNT = Number(__ENV.USER_COUNT || 40);
const PRODUCT_COUNT = Number(__ENV.PRODUCT_COUNT || 5);
const POLL_INTERVAL_SECONDS = Number(__ENV.POLL_INTERVAL_SECONDS || 0.2);
const MAX_POLLS = Number(__ENV.MAX_POLLS || 25);

const queueEnterDuration = new Trend('queue_enter_duration', true);
const queuePollDuration = new Trend('queue_poll_duration', true);
const orderWithQueueDuration = new Trend('order_with_queue_duration', true);
const queueEndToEndDuration = new Trend('queue_end_to_end_duration', true);
const queueWaitSeconds = new Trend('queue_wait_seconds', true);

const queueEnterFail = new Counter('queue_enter_fail');
const queuePollFail = new Counter('queue_poll_fail');
const queueTokenTimeout = new Counter('queue_token_timeout');
const queueOrderSuccess = new Counter('queue_order_success');
const queueOrderFail = new Counter('queue_order_fail');
const queueOrderForbidden = new Counter('queue_order_forbidden');
const errors = new Rate('errors');

export const options = {
  scenarios: {
    queue_strategy_learning: {
      executor: 'shared-iterations',
      vus: USER_COUNT,
      iterations: USER_COUNT,
      maxDuration: __ENV.MAX_DURATION || '2m',
    },
  },
  thresholds: {
    errors: ['rate<0.2'],
    queue_enter_duration: ['p(95)<1000'],
    queue_poll_duration: ['p(95)<1000'],
    order_with_queue_duration: ['p(95)<2000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

function jsonHeaders(extra = {}) {
  return {
    'Content-Type': 'application/json',
    ...extra,
  };
}

function authHeaders(user) {
  return jsonHeaders({
    'X-Loopers-LoginId': user.loginId,
    'X-Loopers-LoginPw': user.password,
  });
}

function authQueueHeaders(user, token) {
  return jsonHeaders({
    'X-Loopers-LoginId': user.loginId,
    'X-Loopers-LoginPw': user.password,
    'X-Queue-Token': token,
    'X-Queue-Strategy': STRATEGY,
  });
}

function parseResponse(res) {
  try {
    return res.json();
  } catch (_error) {
    return null;
  }
}

function ensureSuccess(res, label) {
  const body = parseResponse(res);
  const success = check(res, {
    [`${label} status 200`]: (response) => response.status === 200,
    [`${label} meta success`]: () => body?.meta?.result === 'SUCCESS',
  });
  errors.add(!success);
  return { success, body };
}

function createUser(index) {
  const normalizedRunId = RUN_ID.replace(/[^a-zA-Z0-9]/g, '');
  return {
    loginId: `queueuser${normalizedRunId}${index}`,
    password: `QueuePass${index}!`,
    email: `queue-user-${normalizedRunId}-${index}@test.com`,
    name: `queue-user-${index}`,
  };
}

function signup(user) {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/signup`,
    JSON.stringify({
      loginId: user.loginId,
      password: user.password,
      name: user.name,
      birthDate: '2000-05-15',
      email: user.email,
    }),
    { headers: jsonHeaders() },
  );
  return ensureSuccess(res, 'signup').success;
}

function createBrand(seedUser) {
  const res = http.post(
    `${BASE_URL}/api/v1/brands`,
    JSON.stringify({ name: `QueueBrand-${RUN_ID}` }),
    { headers: authHeaders(seedUser) },
  );
  const { success, body } = ensureSuccess(res, 'brand_create');
  return success ? body.data.id : null;
}

function createProducts(seedUser, brandId) {
  const productIds = [];
  for (let index = 1; index <= PRODUCT_COUNT; index += 1) {
    const res = http.post(
      `${BASE_URL}/api/v1/products`,
      JSON.stringify({
        brandId,
        name: `QueueProduct-${RUN_ID}-${index}`,
        price: 10000 + index,
        description: `Queue comparison product ${index}`,
        stock: 100000,
      }),
      { headers: authHeaders(seedUser) },
    );
    const { success, body } = ensureSuccess(res, 'product_create');
    if (success) {
      productIds.push(body.data.id);
    }
  }
  return productIds;
}

export function setup() {
  const users = [];
  for (let index = 1; index <= USER_COUNT; index += 1) {
    const user = createUser(index);
    if (!signup(user)) {
      throw new Error(`failed to create user ${user.loginId}`);
    }
    users.push(user);
  }

  const seedUser = users[0];
  const brandId = createBrand(seedUser);
  if (!brandId) {
    throw new Error('failed to create brand');
  }

  const productIds = createProducts(seedUser, brandId);
  if (productIds.length === 0) {
    throw new Error('failed to create products');
  }

  return {
    users,
    productIds,
  };
}

function enterQueue(user) {
  const res = http.post(
    `${BASE_URL}/api/v1/queue/enter`,
    JSON.stringify({ strategy: STRATEGY }),
    { headers: authHeaders(user) },
  );
  queueEnterDuration.add(res.timings.duration, { strategy: STRATEGY });
  const { success, body } = ensureSuccess(res, 'queue_enter');
  if (!success) {
    queueEnterFail.add(1, { strategy: STRATEGY });
  }
  return body?.data;
}

function pollQueue(user) {
  const res = http.get(
    `${BASE_URL}/api/v1/queue/position?strategy=${STRATEGY}`,
    { headers: authHeaders(user) },
  );
  queuePollDuration.add(res.timings.duration, { strategy: STRATEGY });
  const { success, body } = ensureSuccess(res, 'queue_poll');
  if (!success) {
    queuePollFail.add(1, { strategy: STRATEGY });
  }
  return body?.data;
}

function createOrder(user, productId, token) {
  const res = http.post(
    `${BASE_URL}/api/v1/orders`,
    JSON.stringify({ items: [{ productId, quantity: 1 }] }),
    { headers: authQueueHeaders(user, token) },
  );
  orderWithQueueDuration.add(res.timings.duration, { strategy: STRATEGY });

  const body = parseResponse(res);
  const success = check(res, {
    order_status_expected: (response) => response.status === 200 || response.status === 403 || response.status === 400,
  });
  errors.add(!success || body?.meta?.result === 'FAIL');

  if (res.status === 200 && body?.meta?.result === 'SUCCESS') {
    queueOrderSuccess.add(1, { strategy: STRATEGY });
    return true;
  }

  if (res.status === 403) {
    queueOrderForbidden.add(1, { strategy: STRATEGY });
  }
  queueOrderFail.add(1, { strategy: STRATEGY });
  return false;
}

function pickProduct(productIds) {
  return productIds[(__VU - 1) % productIds.length];
}

export default function (data) {
  const user = data.users[__VU - 1];
  const productId = pickProduct(data.productIds);
  const startedAt = Date.now();

  let state = enterQueue(user);
  if (!state) {
    return;
  }

  let polls = 0;
  while (!state.canEnterOrderApi && polls < MAX_POLLS) {
    sleep(POLL_INTERVAL_SECONDS);
    state = pollQueue(user);
    polls += 1;
    if (!state) {
      return;
    }
  }

  if (!state.canEnterOrderApi || !state.token) {
    queueTokenTimeout.add(1, { strategy: STRATEGY });
    errors.add(true);
    return;
  }

  const queueWait = (Date.now() - startedAt) / 1000;
  queueWaitSeconds.add(queueWait, { strategy: STRATEGY });

  const orderSucceeded = createOrder(user, productId, state.token);
  if (orderSucceeded) {
    queueEndToEndDuration.add(Date.now() - startedAt, { strategy: STRATEGY });
  }
}

export function handleSummary(data) {
  return {
    stdout: JSON.stringify(
      {
        strategy: STRATEGY,
        runId: RUN_ID,
        metrics: data.metrics,
      },
      null,
      2,
    ),
  };
}
