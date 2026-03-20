import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = 'http://localhost:8080';

export const options = {
  vus: 1,
  iterations: 3,
};

export default function () {
  const userId = 1;
  const orderId = 100 + Math.random() * 1000;
  const amount = 10000;

  const payload = JSON.stringify({
    orderId: orderId,
    cardType: 'SAMSUNG',
    cardNo: '1234-5678-9814-1451',
    amount: amount.toString(),
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-USER-ID': userId.toString(),
    },
  };

  console.log('=== Request Details ===');
  console.log('URL: ' + BASE_URL + '/api/v1/payments');
  console.log('Headers: ' + JSON.stringify(params.headers));
  console.log('Body: ' + payload);

  const res = http.post(`${BASE_URL}/api/v1/payments`, payload, params);

  console.log('=== Response Details ===');
  console.log('Status: ' + res.status);
  console.log('Body: ' + res.body);
  console.log('Headers: ' + JSON.stringify(res.headers));

  check(res, {
    'response status': (r) => {
      console.log('Detailed: Status=' + r.status);
      return r.status > 0;
    },
  });
}
