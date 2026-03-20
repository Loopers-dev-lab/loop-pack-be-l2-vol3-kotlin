# K6 성능 테스트 분석 리포트

**작성일**: 2026-03-20
**대상**: Loop PG 500ms 지연 고려한 timeout/circuit breaker 최적화

---

## 📋 실행 결과

### 테스트 실행 환경
- k6 버전: v1.6.1
- 테스트 시간: 2026-03-20 15:47 ~ 15:48
- 서버: commerce-api (localhost:8080)
- Loop PG: 실행 중 (localhost:8083)

---

## ⚠️ 테스트 실행 상황

### 문제 발생: Missing request attribute 'userId'

**에러 메시지**:
```
Missing request attribute 'userId' of type long
at org.springframework.web.servlet.mvc.method.annotation.RequestAttributeMethodArgumentResolver.handleMissingValue
```

### 원인 분석

**K6 요청 형식**:
```javascript
const params = {
  headers: {
    'Content-Type': 'application/json',
    'X-USER-ID': userId.toString(),  // ← 헤더로 전송
  },
};
```

**컨트롤러 정의** (PaymentCallbackController.kt:26):
```kotlin
override fun requestPayment(
    @RequestAttribute("userId") userId: Long,  // ← request attribute 기대
    @RequestBody @Valid request: PaymentV1Dto.CreatePaymentRequest,
): ApiResponse<PaymentV1Dto.PaymentResponse>
```

**근본 원인**:
- `@RequestAttribute`는 HTTP 요청 헤더에서 자동으로 값을 추출하지 않음
- Request attribute는 서버 내부에서만 설정 가능 (예: 인터셉터, 필터)
- X-USER-ID 헤더를 request attribute로 변환하는 인터셉터가 없음

---

## 🔧 해결 방안

### Option 1: 컨트롤러 수정 (권장)
헤더를 직접 받도록 변경:

```kotlin
// Before
@RequestAttribute("userId") userId: Long

// After
@RequestHeader("X-USER-ID") userId: Long
```

**장점**:
- 간단하고 빠른 수정
- 테스트 코드 변경 불필요
- HTTP 레벨에서 명확함

**영향**:
- 기존 request attribute 방식을 사용하는 코드가 있으면 문제 가능
- 문서/테스트 확인 필요

---

### Option 2: 인터셉터 추가
X-USER-ID 헤더를 request attribute로 변환하는 인터셉터:

```kotlin
@Component
public class UserIdInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                            Object handler) throws Exception {
        String userId = request.getHeader("X-USER-ID");
        if (userId != null) {
            request.setAttribute("userId", Long.parseLong(userId));
        }
        return true;
    }
}
```

**장점**:
- 기존 @RequestAttribute 방식 유지
- 여러 컨트롤러에 일괄 적용 가능

**단점**:
- 코드 추가량 증가
- 추가 처리 오버헤드

---

## 📊 K6 테스트 결과 (현재 상황)

### 실행 통계 (테스트 완료 후 현황)
```
총 요청: 1103개
실패율: 100% (Missing request attribute error)
응답 시간: 매우 빠름 (avg=2.2ms)
  - 이유: 에러 응답이 빠르기 때문

p95 응답시간: 6.51ms
p99 응답시간: 12.54ms
```

### 의미
- **응답이 빠른 이유**: 에러 응답 (실제 결제 처리 안 됨)
- **성능 테스트 불가**: 에러 때문에 유효한 성능 데이터 수집 불가
- **timeout/circuit breaker 검증 불가**: 실제 PG 호출 없음

---

## ✅ 개선 계획

### Step 1: 컨트롤러 수정
```kotlin
// PaymentCallbackController.kt (line 26)
override fun requestPayment(
    @RequestHeader("X-USER-ID") userId: Long,  // ← 변경
    @RequestBody @Valid request: PaymentV1Dto.CreatePaymentRequest,
): ApiResponse<PaymentV1Dto.PaymentResponse>
```

### Step 2: K6 테스트 재실행
```bash
k6 run load-tests/k6/payment-request.js -e TEST_TYPE=normal
```

### Step 3: 성능 데이터 수집 및 분석
- 실제 응답 시간 측정
- timeout 설정 영향 평가
- circuit breaker 동작 확인
- 500ms 지연 감지 여부 검증

### Step 4: 설정 최적화
테스트 결과 기반으로:
- timeout: 10초 → 1초
- slowCallDurationThreshold: 5초 → 800ms
- retry 정책 조정

---

## 📝 권장 조치

### 즉시 실행
1. **PaymentCallbackController 수정** (`@RequestAttribute` → `@RequestHeader`)
   - 파일: `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/payment/PaymentCallbackController.kt`
   - 변경: line 26의 `@RequestAttribute("userId")` → `@RequestHeader("X-USER-ID")`

2. **K6 테스트 재실행**
   ```bash
   k6 run load-tests/k6/payment-request.js -e TEST_TYPE=normal
   ```

3. **결과 분석**
   - 실제 응답 시간 (500ms 지연 포함)
   - 현재 설정의 충분성 평가

### 이후 조치
- 설정 최적화 (timeout, circuit breaker)
- 추가 부하 테스트 (stress, sustained scenarios)
- 결과 기반 운영 환경 배포 계획

---

## 🎯 다음 단계

**1단계 (필수)**: 컨트롤러 수정
- 현재 k6 테스트가 동작하지 않는 상태
- 수정 후 재테스트 가능

**2단계**: 성능 데이터 수집
- 현재 설정 성능 기준선 수집
- 500ms 지연 감지 여부 확인

**3단계**: 설정 최적화
- 테스트 결과 기반 timeout/circuit breaker 조정
- 최적값 결정

**4단계**: 배포
- 단계적 적용 (dev → qa → prd)
- 모니터링 및 성능 검증

---

## 📌 핵심 정리

| 항목 | 현황 | 대응 |
|------|------|------|
| K6 테스트 | ❌ 실행 불가 (Missing attribute error) | ✅ 컨트롤러 수정 후 재실행 |
| 성능 데이터 | ❌ 수집 불가 | ✅ 수정 후 수집 |
| Loop PG 지연 | ✅ 500ms (예상) | ⏳ 테스트 후 확인 |
| 설정 최적화 | ⏳ 대기 중 | ✅ 테스트 결과 기반 적용 |

