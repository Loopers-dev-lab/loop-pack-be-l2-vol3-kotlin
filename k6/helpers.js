import http from 'k6/http';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const LOGIN_ID = __ENV.LOGIN_ID || 'testuser';
export const LOGIN_PW = __ENV.LOGIN_PW || 'testpassword1!';

export const headers = {
  'X-Loopers-LoginId': LOGIN_ID,
  'X-Loopers-LoginPw': LOGIN_PW,
};

export const BRAND_IDS = [1, 5, 10, 20, 30, 40, 50];

export function randomBrandId() {
  return BRAND_IDS[Math.floor(Math.random() * BRAND_IDS.length)];
}

export function randomProductId(max = 100000) {
  return Math.floor(Math.random() * max) + 1;
}

export function randomPage(max = 10) {
  return Math.floor(Math.random() * max);
}

export function getProductList(sort, page, size, brandId) {
  let url = `${BASE_URL}/api/v1/products?sort=${sort}&page=${page}&size=${size || 20}`;
  if (brandId) url += `&brandId=${brandId}`;
  return http.get(url, { headers });
}

export function getProductDetail(productId) {
  return http.get(`${BASE_URL}/api/v1/products/${productId}`, { headers });
}

export function flushRedis() {
  // Redis flush는 외부에서 수행 (redis-cli FLUSHALL)
}
