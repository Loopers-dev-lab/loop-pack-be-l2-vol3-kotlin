# 랭킹 조회수 어뷰징 방지 k6 테스트

## 사전 조건

1. Docker 인프라 실행
```bash
docker-compose -f docker/infra-compose.yml up -d
```

2. commerce-api 실행
```bash
./gradlew :apps:commerce-api:bootRun
```

3. commerce-streamer 실행
```bash
./gradlew :apps:commerce-streamer:bootRun
```

4. 테스트 상품 존재 확인 (상품 ID 1, 2, 3이 필요)

## 실행

```bash
# 기본 실행 (상품 ID 1, 2, 3)
k6 run k6/ranking-abuse-test.js

# 상품 ID 지정
k6 run -e NORMAL_PRODUCT_ID=10 -e BOT_SINGLE_PRODUCT_ID=20 -e BOT_MULTI_PRODUCT_ID=30 k6/ranking-abuse-test.js

# 다른 서버 주소
k6 run -e BASE_URL=http://localhost:9090 k6/ranking-abuse-test.js
```

## 시나리오 설명

| 시나리오 | VU | 반복 | 설명 |
|---------|-----|------|------|
| normal_users | 100 | 3회/VU | 정상 로그인 유저가 다양한 상품 조회 |
| bot_single_ip | 1 | 100회 | 봇 1대가 같은 상품 반복 조회 |
| bot_multi_ip | 100 | 1회/VU | 봇 100개 IP에서 같은 상품 각 1회 조회 (UA/Referer 없음) |
| mixed_traffic | 50 | 2회/VU | 정상 유저 + 봇 혼합 |
| check_rankings | 1 | 1회 | 최종 랭킹 결과 확인 |

## 기대 결과

```
========== 랭킹 결과 ==========
  1위: 상품1 (score: 10.0000) ← 정상 유저 타겟
  2위: 상품3 (score: 0.5000)  ← 다중IP 봇 타겟 (Trust Score로 감쇠)
  3위: 상품2 (score: 0.0300)  ← 단일IP 봇 타겟 (Layer 1에서 1회만 통과, 비로그인 Trust)

기대: 정상 유저 타겟 >> 다중IP 봇 타겟 >> 단일IP 봇 타겟
```

## 방어 계층 동작 확인

| 시나리오 | Layer 1 (중복 제거) | Layer 2 (Trust Score) | 결과 |
|---------|-------------------|---------------------|------|
| 정상 유저 100명 | 유저별 1회 통과 | ×1.0 (로그인+UA+Referer) | 높은 점수 |
| 봇 단일 IP 100회 | **1회만 통과** | ×0.05~0.75 | 매우 낮은 점수 |
| 봇 다중 IP 100개 | 100개 모두 통과 | **×0.05** (비로그인+UA없음) | 낮은 점수 |
