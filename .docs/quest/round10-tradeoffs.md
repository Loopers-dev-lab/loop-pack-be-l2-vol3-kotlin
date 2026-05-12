# Round10 트레이드오프와 선택 정리

## 1. daily 의미를 어떻게 가져갈 것인가
### 후보
- `daily`를 전날 확정 일간으로 본다
- `daily`를 최근 24시간 rolling으로 유지한다

### 선택
`daily`는 최근 24시간 rolling으로 유지한다.

### 이유
- 사용자 입장에서 더 직관적이다.
- 기존 API 의미를 최대한 유지할 수 있다.
- fixed 랭킹 의미를 운영형 조회 API에 섞지 않아도 된다.

## 2. weekly/monthly를 rolling으로 유지할지 fixed로 바꿀지
### 후보
- `weekly/monthly`를 fixed 랭킹으로 바꾼다
- `weekly/monthly`는 rolling으로 두고 fixed type을 별도 추가한다

### 선택
`weekly/monthly`는 rolling으로 유지하고,
`week-fixed`, `month-fixed`를 별도 추가한다.

### 이유
- rolling window와 마감형 통계의 의미 충돌을 피할 수 있다.
- 기존 Redis 랭킹 구조를 그대로 활용할 수 있다.
- batch 결과의 목적이 더 명확해진다.

## 3. `product_metrics` 원천 데이터를 어떻게 가져갈 것인가
### 후보
- 기존 상품별 누적 1행 구조 유지
- fixed 전용 집계 테이블을 새로 만든다
- `product_metrics`를 일별 집계 테이블로 재설계한다

### 선택
`product_metrics`를 일별 집계 테이블로 재설계한다.

### 이유
- batch는 일자 단위 원천 데이터가 필요하다.
- 주간/월간 fixed 랭킹 입력 원천으로 자연스럽다.
- 별도 집계 소스를 추가하는 것보다 구조가 단순하다.

## 4. ranking score를 batch에서 계산할지, 적재 시점에 저장할지
### 후보
- batch에서 count만 보고 score를 다시 계산한다
- 이벤트 적재 시점에 `ranking_score`를 같이 저장한다
- batch가 raw event를 다시 읽어 계산한다

### 선택
이벤트 적재 시점에 `ranking_score`를 함께 저장한다.

### 이유
- 주문 점수는 `unitPrice * quantity`를 사용하므로 `sales_count`만으로 복원할 수 없다.
- batch 입력이 단순해진다.
- Redis 점수 의미와 정합성을 맞출 수 있다.

## 5. metrics upsert 방식
### 후보
- JPA find-mutate-save
- JPA + lock
- MySQL native upsert

### 선택
MySQL native upsert를 사용한다.

### 이유
- 집계 hot path에서 atomic update가 가능하다.
- lost update 위험이 낮다.
- `(product_id, metrics_date)` unique key와 잘 맞는다.

## 6. batch 처리 방식을 tasklet로 할지 chunk로 할지
### 후보
- Tasklet만 사용
- raw metrics를 batch 메모리에서 직접 합산하는 chunk
- DB 선집계 + chunk read

### 선택
DB 선집계 + chunk read 방식으로 간다.

### 이유
- `product_metrics`는 이미 집계 테이블이기 때문에 DB의 `GROUP BY product_id`가 자연스럽다.
- batch 내부 상태 관리가 단순해진다.
- 과제의 chunk-oriented 요구사항도 만족한다.

## 7. fixed 랭킹 데이터 보관 정책
### 후보
- 최신 결과만 보관
- 기간별 스냅샷을 계속 누적 보관
- 최근 N개만 보관

### 선택
기간별 스냅샷을 계속 누적 보관한다.

### 이유
- 과거 기간 조회가 가능하다.
- 디버깅과 재현성이 좋아진다.
- 리포트/통계 성격에 더 적합하다.

## 8. 주간/월간 기간 경계
### 주간 후보
- 일~토
- 월~일
- ISO 주차 기준

### 주간 선택
월~일 기준으로 간다.

### 월간 후보
- 최근 30일
- 달력 월

### 월간 선택
fixed 월간은 달력 월 기준으로 간다.

### 이유
- fixed 랭킹은 리포트성 결과물이므로 닫힌 기간이 명확해야 한다.
- 최근 7일/30일 rolling은 이미 Redis 랭킹이 담당하고 있다.

## 9. fixed 조회 파라미터 형태
### 후보
- `date` 하나로 모두 처리
- 기간별 식별자를 분리한다
- 최신 조회를 기본으로 하고 기간 지정은 별도 파라미터로 받는다

### 선택
기본은 최신 조회,
명시적인 기간 조회는 아래 형태로 받는다.

- `weekStartDate=yyyyMMdd`
- `yearMonth=yyyyMM`

### 이유
- 기본 사용성은 좋게 유지할 수 있다.
- 과거 기간 조회 시 의미가 분명하다.
- 주간/월간을 같은 `date`로 억지로 맞추는 것보다 명확하다.
