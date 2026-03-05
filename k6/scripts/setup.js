import http from 'k6/http';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const headers = {
  'Content-Type': 'application/json',
};

export default function () {
  console.log('=== Setting up test data ===');

  // 1. Admin 계정 생성
  const adminRes = http.post(
    `${BASE_URL}/api/v1/auth/signup`,
    JSON.stringify({
      loginId: 'admin',
      password: 'Admin1234!',
      name: 'admin',
      birthDate: '2000-05-15',
      email: 'admin@test.com',
    }),
    { headers },
  );
  console.log(`Admin created: ${adminRes.status}`);

  const authHeaders = {
    ...headers,
    'X-Loopers-LoginId': 'admin',
    'X-Loopers-LoginPw': 'Admin1234!',
  };

  // 2. 회원 가입 (테스트용 유저 100명)
  for (let i = 1; i <= 100; i++) {
    http.post(
      `${BASE_URL}/api/v1/auth/signup`,
      JSON.stringify({
        loginId: `testuser${i}`,
        password: `Password${i}!`,
        name: `testuser${i}`,
        birthDate: '2000-05-15',
        email: `test${i}@test.com`,
      }),
      { headers },
    );
  }
  console.log('Users created');

  // 3. 브랜드 등록
  const brandRes = http.post(
    `${BASE_URL}/api/v1/brands`,
    JSON.stringify({ name: 'LoadTestBrand' }),
    { headers: authHeaders },
  );
  const brandId = brandRes.json('data.id');
  console.log(`Brand created: ${brandId}`);

  // 4. 상품 등록 (10개, 재고 넉넉하게)
  for (let i = 1; i <= 10; i++) {
    http.post(
      `${BASE_URL}/api/v1/products`,
      JSON.stringify({
        brandId: brandId,
        name: `LoadTestProduct${i}`,
        price: 10000 * i,
        description: `Load test product ${i}`,
        stock: 100000,
      }),
      { headers: authHeaders },
    );
  }
  console.log('Products created');

  console.log('=== Setup complete ===');
}
