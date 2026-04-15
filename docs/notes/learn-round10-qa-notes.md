# Round 10 학습 노트 — Spring Batch 기반 주간/월간 랭킹 집계

## 학습 개요
- 학습 일자: 2026-04-13
- 라운드 주제: Spring Batch + Materialized View로 주간/월간 랭킹 시스템 구축
- 참조 문서: docs/quests/round-10.md
- 이전 라운드 연결: Round 9 (Redis ZSET 실시간 일간 랭킹)

---

## Q1. 왜 주간/월간은 배치인가? — 실시간 vs 배치의 근본적 차이

**[질문]**
이미 Redis ZSET이라는 도구가 있는데, 주간/월간 랭킹은 왜 배치 + MV 방식으로 설계할까요? 주간 ZSET 키 하나 만들어서 이벤트 들어올 때마다 일간/주간 둘 다 `ZINCRBY` 하면 안 되는 건가요?

**[답변]**
> 데이터의 양이 다를것이다. 그래서 실시간으로 하기에는 조회 성능과 db 부하를 발생시킬 수 있다.
> 그리고 주간/월간 랭킹은 과거 데이터(어제 데이터)와 매우 연관이 깊어 최신 랭킹과는 거리가 멀다

**[멘토 반론: ZSET 성능 특성]**
ZSET은 O(log N) 자료구조라 100만 건이든 1억 건이든 연산 속도는 거의 차이 없음. "데이터 양이 많아서 실시간이 불리하다"는 ZSET의 성능 특성상 완전히 맞는 말은 아님. 두 번째 지적("과거 데이터와 연관이 깊다")을 더 깊게 파고들 필요가 있음.

**[후속 질문: 입력의 차이]**
일간 랭킹의 입력은 raw 이벤트(주문/조회/좋아요) 하나하나. 주간/월간의 입력도 똑같이 raw 이벤트일까요, 아니면 다른 무언가일까요? `product_metrics` 테이블이 왜 존재하는지와 연결해 생각해보세요.

**[답변]**
> 주간/월간 랭킹의 입력은 일간 랭킹과 똑같이 주문/조회/좋아요 가 될것이라 생각된다.

**[후속 질문: 이벤트 양의 구체화]**
주간은 7일치 raw 이벤트 7,000만 건, 월간은 3억 건을 매번 긁어와야 함. 이게 효율적일까? `product_metrics`는 raw 이벤트인가요, 가공된 데이터인가요? 일별 집계가 이미 있다면 주간은 7행, 월간은 30행만 합치면 되지 않을까요? (단, 현재 스키마는 상품별 단일 누적 row라 일별 분리 불가 — 구조 변경 필요)

**[답변]**
> product_metrics 의 구조를 변경해야할거 같아. 일별로 row 를 만들어서 일간/주간/월간 조회가 가능하게 해야할거 같아. 그러면 지금보다 데이터의 양은 많아지겠지

**[후속 질문: 배치의 본질적 장점 3가지]**
(1) 처리 데이터 크기, (2) 실행 타이밍, (3) 가중치 변경 대응 — 세 관점에서 배치 방식의 장점을 설명해보세요.

**[답변]**
> 처리 데이터의 양이나 크기는 배치가 클 수 있다. 배치는 데이터의 양이 많을 때 집계를 계산하기 위해 존재하는 프레임워크이다.
> 주간 랭킹은 실시간이 아니다. 주간 랭킹은 전주 또는 이번주 동안의 랭킹으로 긴 시간 동안 누적되는 데이터 이기때문에 실시간에 대한 영향이 적을것이다.
> 가중치가 변경된다면 대공사를 해야하는것은 주간/월간 배치이다. 해당 요구사항이 변경되면 배치를 사용해야 한다
> 결론 배치는 대용량 데이터를 처리할 때 주로 사용한다.

**[악마의 변호인 + 후속 질문: 가중치 변경, 정말 배치가 불리한가?]**
실시간 ZSET은 이미 "가중치 적용된 점수"만 누적되어 있어 원본 복원 불가. 배치는 `product_metrics`에 raw 카운트(view_count, like_count, sales_count)가 남아있어 가중치만 바꿔서 재실행 가능. 어느 쪽이 재실행에 자유로운가? (Round 9의 "ZSET score를 되돌리기 어렵다" 문제와 연결)

**[답변]**
> 배치의 유연성이 매우 높네요 확실하게 이해했습니다~

**[정리]**
- **계층적 집계 구조**: raw event → 일간 집계(product_metrics) → 주간/월간 집계(MV). 배치는 이미 집계된 소량 데이터를 재료로 삼음 (일반적 "배치=대용량" 일반론과 역설적)
- **실시간성 불필요**: 주간/월간은 새벽 한산한 시간대에 몰아서 처리 가능 → 피크 타임 DB 부하 회피
- **재실행 자유도**: raw 카운트 보존 → 가중치 변경 시 배치 재실행만으로 완전 재계산 가능. Redis ZSET은 누적된 점수만 남아 원본 복원 불가

---

## Q2. "주간 랭킹"의 '주간'이란 무엇인가 — 기간 경계 설계

**[질문]**
"주간 랭킹"이라고 했을 때 본인이 생각하는 한 주의 범위는 어디부터 어디까지인가요?

오늘이 2026-04-13(월)이라고 할 때, 선택지:
- **A. 롤링 7일 (최근 7일)**: 오늘 기준 -6일 ~ 오늘 (4/7 ~ 4/13). 조회 시점마다 범위 슬라이딩
- **B. ISO Week (월~일 단위)**: 이번 주 4/13(월) ~ 4/19(일). 주가 넘어가야 새 랭킹 생성
- **C. 캘린더 주 (일~토)**: B와 유사, 시작 요일 차이
- **D. 다른 방식**

어느 쪽을 선택할지, 그리고 그 선택이 **배치 실행 주기**와 어떻게 연결되는지 설명하기.
(월간 랭킹도 같은 맥락: "이번 달" = 4/1~4/30 vs "최근 30일")

**[답변]**
> 선택을 한다면 B, C 중 하나를 선택할 것이다.
> 이유는 대중적인 선택을 해야 서비스 이용자들이 이해를 할 수 있기 때문이다. 만약 다르게 선택한다면 서비스 이용자들에게 혼란만 줄 수 있다고 생각된다.
> B,C 를 선택한다면 주에 한번 배치를 실행하고, A를 선택하면 하루 한번 배치를 실행해야한다.
> 월간 랭킹도 모든게 같다.

**[악마의 변호인: UX 근거 흔들기]**
"대중적 선택"이라는 논리는 설득력 있지만, 실제 대형 서비스는 양쪽 다 씁니다. 멜론/벅스/빌보드 = B·C(월~일 고정), 유튜브 급상승·넷플릭스 Top10·Spotify Today's Top Hits = A에 가까운 롤링. 주간 "차트" 정서 vs 라이브 "트렌드" 정서로 갈림.

**[후속 질문: 기간 정의 ≠ 배치 주기?]**
"B/C = 주 1회 배치, A = 하루 1회 배치"라는 1:1 연결을 검증해봅시다.

B를 골랐다고 가정. 오늘이 4/13(월)이고 '이번 주 = 4/13 ~ 4/19'입니다. 주 1회 배치(월요일 새벽)만 돌린다면:
- 월요일 오후 2시: '이번 주 랭킹'에 들어갈 데이터는 **0일치** (이번 주 배치는 아직 안 돌았음)
- 수요일/금요일에 다시 조회: 월요일 조회 결과와 **완전히 동일** (배치가 안 돌았으니까)
- 일요일 23시59분: 여전히 월요일에 본 그 결과

이게 자연스러운 '이번 주 랭킹'인가요? 어색하다면, B를 **유지하면서** 이 문제를 해결하려면 어떻게 해야 할까요?
(힌트: 배치 주기와 "조회 대상 주"를 분리해서 생각)

**[답변]**
> 일반 사용자라.... 이거는 누가 좋다, 아니다 라고 딱 잘라 말하기 어렵네
> 기술적으로 크게 다른건 없다고 생각해. 하루 한번 배치 실행이냐? 주 한번 배치 실행이냐의 차이 뿐이라 생각해
> 이거는 비즈니스에 따라 다르다고 생각해. 모든건 어떤 데이터를 서비스 사용자에게 보여주고 싶은가? 에 따라 하루 한번, 주 한번, 월 한번 이 선택된다고 생각해

**[후속 질문: 기술적 차이가 정말 없는지 시나리오 검증]**
실제 프로젝트의 `ProductMetrics.kt`는 `@Id val productId: Long` 하나만으로 식별되는 단일 누적 row 구조. 시간 축이 없으므로 Q1 정리대로 일별 row 분리가 전제됨.

추가 요구 가정: "2026년 3월 한 달간 주차별 랭킹 변화 추이를 대시보드로" → B vs A 비교 표:
- MV 키: B는 `(yearWeek, productId)` / A는 `(snapshotDate, productId)` 또는 단일 row
- 주차 불변성: B는 주 종료 후 immutable archive / A는 매일 다른 스냅샷
- 과거 주차 조회: B는 `WHERE year_week IN (...)` 단발 쿼리 / A는 "3월 1주차" 개념 자체가 없음
- 저장 row 수: B는 4주 × 100 = 400 / A는 28일 × 100 = 2800 (7배)

퀘스트 문서 MV 예시의 `yearMonthWeek VARCHAR` 컬럼 존재가 무엇을 암시하는지 함께 판단 요청.

**[답변]**
> 1. A (yearMonthWeek 컬럼이 A를 전제)
> 2. 가중치 변경 유연성과 과거를 알고 활용할 수 있냐의 차이
> 3. 주간과 월간은 같게 가는게 좋다고 생각해

**[멘토 교정: 1번과 2번 모순 짚기]**
`yearMonthWeek` 값이 '2026-W15' 같은 고정 구획 식별자 vs '2026-04-13' 같은 시점 표기 중 어느 쪽이 자연스러운지, 그리고 과거 주차 archive가 A와 B 중 어느 쪽에 맞는지 재검증 요청. (1번이 A면 2번의 "과거 활용"과 논리적으로 충돌)

**[답변]**
> 1번 답변은 잘못 선택했다. 변수명 그대로 a(= α, '2026-W15' = ISO Week = B)가 자연스럽다.
> 2번 과거 archive도 B에 자연스럽다.

**[정리]**
- **기간 정의 ≠ 배치 실행 주기**: 서로 분리 가능한 두 축. B(ISO Week 고정 경계)를 선택해도 배치는 하루 1회 돌려 진행 중인 주의 누적 데이터를 매일 갱신할 수 있음. "B = 주 1회 배치"는 1:1 매핑이 아님
- **Fixed-boundary key의 본질**: `yearMonthWeek VARCHAR` 같은 컬럼은 달력상 고정 구획을 식별하는 이름. 롤링 7일은 이런 의미의 "주차 식별자"를 가질 수 없음
- **Archive 가능성 = B의 본질적 장점**: 주가 끝나면 해당 row는 immutable이 되어 영구 archive. `(year_week, product_id)` 복합키로 과거 주차 랭킹을 단일 쿼리로 조회 가능. A는 매일 스냅샷을 저장해야만 과거 접근 가능하고 "3월 1주차"라는 개념 자체가 없음
- **Q1의 "재실행 자유도"와 Q2의 "archive 가능성"이 같은 축에서 만남**: raw 데이터 보존(일별 row) + 고정 경계(yearWeek) 조합이 시너지
- **이 프로젝트 확정 방향**: B-style (ISO Week / YearMonth 고정 경계) + 하루 1회 배치, 주간/월간 동일 패턴
- **퀘스트 문서 스키마의 허점**: PK가 `product_id`만 → 주차 히스토리 못 쌓음. 실제로는 `(product_id, yearMonthWeek)` 복합키 또는 `(yearMonthWeek, rank, product_id)` 필요 → Q4에서 재설계
- **[글감]**: "기간 경계 정의와 배치 실행 주기는 분리 가능한 축이다" — 직관적으로 묶어 생각하기 쉬운 두 결정을 분리해서 다루는 설계 감각

---

## Q3. Spring Batch 구조 선택 — Chunk-Oriented vs Tasklet `[답변 대기 중]`

**[질문]**
이번 주간 랭킹 배치가 구체적으로 무슨 일을 하는지부터 그려봅시다.

**가정한 스키마 (Q1에서 합의)**: `product_metrics_daily(product_id, metric_date, view_count, like_count, sales_count, ...)` — 상품별 일별 row.

**배치의 임무**: 매일 새벽 3시에 실행. 오늘이 2026-04-15(수)라면, 이번 주(2026-W15, 4/13~4/19)의 **현재까지 누적된 데이터(4/13~4/14 이틀치)**를 집계해 `mv_product_rank_weekly`의 '2026-W15' row들을 갱신.

두 가지 구현 후보가 있습니다:

- **옵션 A — Chunk-Oriented**: `ItemReader`가 `product_metrics_daily`에서 chunk(예: 1000건)씩 읽고, `ItemProcessor`에서 상품별 집계 가공, `ItemWriter`가 MV에 upsert
- **옵션 B — Tasklet + 단일 SQL**:
  ```sql
  INSERT INTO mv_product_rank_weekly (year_week, product_id, score, ...)
  SELECT '2026-W15', product_id, SUM(view * w_v + like * w_l + sales * w_s), ...
  FROM product_metrics_daily
  WHERE metric_date BETWEEN '2026-04-13' AND '2026-04-14'
  GROUP BY product_id
  ORDER BY score DESC
  LIMIT 100
  ON DUPLICATE KEY UPDATE score = VALUES(score), ...
  ```
  한 방에 끝.

질문 3개:
1. 어느 쪽을 고를 건가요? 왜?
2. Chunk-Oriented를 고른다면 `chunk size`를 얼마로 잡을 건가요? (1, 100, 1000, 10000 중에서. 그 이유도)
3. Tasklet + 단일 SQL 방식의 **치명적 단점**은 무엇일까요? (힌트: 데이터가 커지면? 중간에 실패하면?)

**[답변]**
> 1. A 선택. 한번에 모든 데이터 작성을 끝내면 좋지만 db 부하와 중간에 잘못된 데이터가 있어. 데이터 오류가 발생하면 모든 데이터가 등록 또는 실패 두가지 경우만 있을것이다. 하지만 A 는 1000개 단위로 하면 중간에 오류가 발생해도, 발생된 구간만 다시 실행하면 되기 때문에 오류발생에도 유연하게 대처가 가능하다.
> 2. 1, 100, 1000, 10000 하나씩 테스트를 해보고 시간을 확인 해봐야 겠지만 처음에는 1000개를 선택할 것이다. 이유는 10000개를 하면 너무 오래 트랜잭션을 소유하게 되고, db 에 락을 오래 잡게 된다. 100개를 선택하면 작은 네트워크 비용이 발생하여 시간이 오래 걸릴 수 있다.
> 3. 많은 양의 데이터가 있으면 db에 부하가 너무 올라가 다른 서비스에 영향을 가게 할 것이다. 그리고 중간 에러가 발생하면 처음부터 다시 실행해야 한다.

**[멘토 평가]**
chunk size = 1000에 대한 trade-off 분석(트랜잭션 길이/락 vs 네트워크 비용)이 실무 권장값과 정확히 일치. Tasklet 단일 SQL의 약점(부하 + 처음부터 재실행)도 정확히 짚음.

**[후속 질문: Chunk 경계의 함정 — 집계 단위 vs chunk 단위 충돌]**
상품 X의 일별 row 7개가 한 chunk에 모두 들어간다는 보장이 없음. chunk 1에 5개 + chunk 2에 2개로 흩어지면, ItemProcessor에서 "상품 X 주간 합계"를 어떻게 계산?

세 후보 제시:
- 후보 1: ItemReader가 SQL 단계에서 `GROUP BY product_id`로 미리 집계, 페이징해서 읽기. 한 건 = 한 상품의 주간 합계
- 후보 2: ItemProcessor에 `Map<productId, accumulator>` 두고 모든 chunk 거치며 누적
- 후보 3: ItemReader가 product_id 정렬 후 boundary 감지하여 그룹 단위로 emit

**[답변]**
> 1. C 선택 (= 후보 3), 성공/실패에 대한 확인이 편안하다.
> 2. 누적을 한다는 것인데 그러면 chunk 로 작업하는게 아니라고 생각된다. 독립 트랜잭션이 아니다.
> 3. GROUP BY 로 한다는게 결국은 sql 에서 계산을 한다는게 아닐까? 그것을 page 로 조회한다고 생각되기 때문에 tasklet과는 다르다고 생각된다.

**[멘토 평가 + 재검증 요청]**
- 2번: 정확. chunk 트랜잭션 독립성과 상태 누적의 충돌이 후보 2의 치명적 약점
- 3번: "페이징해서 조회"를 차이점으로 잡은 건 옳지만, 한 단계 더 깊이 들어갈 여지 있음 (메모리/트랜잭션/Processor 확장)
- 1번: 후보 1과 후보 3은 결과물이 동일 — Reader가 "상품별 집계 1건"을 emit. 차이는 GROUP BY 주체(DB vs Java). "성공/실패 확인이 편안"이라는 이유는 후보 1에도 동일하게 적용됨. 후보 3을 고른 이유 재검증 요청

**[답변]**
> 개인적으로 3번을 선호합니다. db에 계산 로직이 있으면 디버깅이 힘들고, db에 비즈니스 로직이 있다는게 좋지 않다고 생각된다.
> 지금은 rdb를 사용하지만 만약 다른 저장 장소라면? 유연성을 확보 할 수 있다.

**[멘토 평가]**
"DB에 비즈니스 로직 두지 마라"는 DDD 원칙은 valid. 다만 GROUP BY/SUM 같은 단순 데이터 집계와 가중치 계산 같은 비즈니스 로직을 동일선상에서 봐야 하는지가 의문.

**[후속 질문: "DB에 비즈니스 로직"을 한 단계 더 분해 + 하이브리드 제안]**
두 종류로 분해:
- (a) `SUM(view_count) GROUP BY product_id` = 데이터 집계 (DB 주특기, 거의 안 변함)
- (b) `view*0.1 + like*0.3 + sales*0.6` = 비즈니스 로직 (가중치 정책, 자주 변함)

하이브리드 옵션(후보 1'): Reader는 DB GROUP BY로 raw 합계만 페이징, Processor는 Java로 가중치 적용해서 score 산출, Writer는 TOP 100 upsert.

**[답변]**
> yes, 하이브리드 좋다고 생각해

**[정리]**
- **Spring Batch 기본 구조**: Chunk-Oriented가 대용량 집계 배치의 표준. Reader → Processor → Writer, chunk별 독립 트랜잭션
- **Chunk size = 1000 sweet spot**: 트랜잭션 길이/락 시간(↑이면 ↑) vs 네트워크 commit 비용(↓이면 ↑)의 균형
- **Tasklet + 단일 SQL 약점**: ① DB 부하 집중으로 다른 서비스 영향 ② 부분 실패 시 처음부터 재실행
- **Chunk 경계의 함정**: 같은 상품의 일별 row가 여러 chunk에 흩어질 때 집계 처리법
  - 후보 1 (DB GROUP BY): 단순, DB 최적화 활용
  - 후보 2 (Processor 누적): chunk 트랜잭션 독립성과 충돌 → ✗
  - 후보 3 (Reader boundary 감지): 코드 복잡, RDB 외 소스(CSV/API stream)에서 유효
- **확정 방향: 하이브리드 (후보 1')**:
  - Reader: `SELECT product_id, SUM(view), SUM(like), SUM(sales) FROM product_metrics_daily WHERE metric_date BETWEEN ? AND ? GROUP BY product_id`을 `JdbcPagingItemReader`로 페이징
  - Processor: 가중치를 Java로 계산해 score 산출 (가중치는 `@ConfigurationProperties`로 외부화 가능 → application.yml 수정만으로 가중치 변경 + 배치 재실행)
  - Writer: TOP 100 추려서 MV에 upsert (`INSERT ... ON DUPLICATE KEY UPDATE`)
- **하이브리드의 장점**: "DB에 비즈니스 로직" 우려 해결 + DB GROUP BY 최적화 활용 + 가중치 변경 유연성 확보 (Q1의 재실행 자유도와 연결)
- **[글감]**: "GROUP BY는 DB의 비즈니스 로직 침해인가?" — 단순 데이터 집계와 가중치 정책의 경계 분해, ItemReader/Processor 책임 분리의 실무 패턴

---

## Q4. Materialized View 스키마 설계 `[답변 대기 중]`

**[질문]**
이제 MV 테이블을 직접 설계해봅시다.

**확정된 전제:**
- B-style 고정 경계 (yearWeek/yearMonth)
- 과거 주차/월별 archive 가능 (기획팀 "월별 주차 추이" 요구)
- 하이브리드 배치로 채워짐 (Reader → Processor → Writer)

**API 패턴 (가정):**
```
GET /api/v1/rankings?date=20260415&period=WEEKLY&size=20&page=1
```
주된 쿼리: `WHERE year_week = '2026-W15' ORDER BY rank LIMIT 20 OFFSET 0`

**질문 — 직접 CREATE TABLE 문으로 설계해주세요:**

`mv_product_rank_weekly` 테이블에 대해:
1. 어떤 **컬럼**이 들어가야 할까요? (필수 + 선택)
2. **PK**는 무엇으로 정할 건가요? (퀘스트 예시의 `product_id` 단독 PK는 Q2에서 허점으로 짚었음)
3. **인덱스**는 어떻게 추가할 건가요? 위 API 쿼리 패턴을 효율적으로 처리하려면?
4. **기간 식별자(year_week)** 형식: `VARCHAR('2026-W15')`? `INT(202615)`? `(year INT, week INT)` 분리? 어느 쪽을 고르고 왜?
5. **rank 컬럼**을 미리 계산해서 저장할까요, 매 조회 때 `ORDER BY score`로 계산할까요? trade-off는?

CREATE TABLE 문으로 답하시고, 위 5개 질문에 대한 본인의 결정 근거를 짧게 곁들여주세요.

**[답변]**
> 1. view_count / like_count / sales_count 모두 추가할게. 모든 데이터는 필요할 거 같아
> 2. id BIGINT AUTO_INCREMENT PK + unique key, 이유는 다른 db 테이블과 일관성을 맞추기 위해 선택할게
> 3. year_week, rank 로 인덱스를 만들게, year_week 의 범위로 먼저 정렬하고, rank 로 해야 정렬이 올바르게 될거야.
> 4. VARCHAR(10) 로 할게, b 방식은 가독성이 떨어지고, c 는 함수이기 때문에 계산이 들어가서 안좋아
> 5. 가) 는 빠른 조회가 가능하다., 나)는 매 조회마다 rank 계산이 수행되어 성능이 떨어진다

**[멘토 평가]**
- 4번 C 기각 이유(쿼리 조건 2개로 분해됨)와 5번 pre-compute 선택(주간 갱신 주기 특성)은 합리적
- 2번 + 3번이 서로 물려있는 InnoDB 특성 함정을 품고 있음 → 후속 흔들기 필요
- 1번 컬럼 구성(raw 메트릭 중복 저장)은 `product_metrics_daily`와의 관계에서 재검토 여지 있음
- 4번 VARCHAR 선택은 정렬 측면에서 미묘한 함정 있음 (zero-padding 이슈)

**[악마의 변호인: PK 선택과 InnoDB Clustered Index]**
"다른 테이블과 일관성을 맞추기 위해 `id AUTO_INCREMENT` PK"라는 논리는 일반 Entity에는 타당하나, MV 테이블에는 성능 함정이 있을 수 있음.

**시나리오:** 2년치(104주) 데이터 축적. 주차마다 TOP 100 row → 총 10,400 row. 인덱스는 선택대로 `(year_week, rank)`. 다음 쿼리 실행:
```sql
SELECT * FROM mv_product_rank_weekly
WHERE year_week = '2026-W15'
ORDER BY rank LIMIT 20;
```

**핵심 개념 질문:**
1. InnoDB의 **clustered index**가 무엇인지? PK가 실제 데이터 row를 어떻게 저장하는지?
2. `id AUTO_INCREMENT` PK 선택 시, 2026-W15에 속하는 100개 row가 디스크에 **물리적으로** 어떻게 흩어져 있을까요? (힌트: row 삽입 순서 = 배치 실행 시점 순서, 같은 주차 row가 연속 삽입되긴 함)
3. 보조 인덱스 `(year_week, rank)`로 20건을 찾아낸 뒤, 실제 전체 row 데이터를 읽으려면 어떤 동작이 일어나나요? (힌트: "index lookup" vs "table lookup", `SELECT *`)

**비교 케이스:** PK를 `(year_week, product_id)` 복합키로 잡았다면?
- 2026-W15의 100개 row가 디스크에 **물리적으로 연속** 저장됨 (clustered index가 year_week 순)
- `WHERE year_week = ?` 쿼리는 range scan 한 번으로 100개 row가 다 읽힘
- 단, `ORDER BY rank`는 filesort 필요 → 이것만 해결하려면 보조 인덱스 `(year_week, rank)` 하나 더

**판단 질문:**
- 두 PK 전략의 I/O 비용 차이를 설명해보세요
- "다른 테이블과 일관성"이라는 논리가 **조회 전용 derived table**인 MV에도 그대로 적용되어야 할까요? (CLAUDE.md의 `BaseEntity` 규칙은 **도메인 Entity**의 일관성 원칙인데, MV는 domain인가 infrastructure인가?)
- Q1에서 정리한 "MV = 조회 전용 구조 (Materialized View)" 성격을 생각하면, 어느 쪽이 더 자연스러운가?

**[답변]**
> 1. 이건 잘 모르겠어. 알려줘
> 2. 생각해보니 entity와 다른 성격이네, pk 는 다르게 선택해도 좋을 거 같아.
> 3. 이거도 알려줘

**[멘토 답변: I/O 비용 차이 상세 설명]**

**Case A — `id AUTO_INCREMENT` PK + 보조 인덱스 `(year_week, rank)`:**
1. 보조 인덱스 B-Tree에서 `year_week='2026-W15'` 진입, rank 순서로 20개의 **PK 값(id)** 획득 → 인덱스 I/O 2~3회 (sequential)
2. 획득한 `id` 20개로 **PK 클러스터드 인덱스를 다시 타고 실제 row 읽기 (Bookmark Lookup)**
3. `id`는 **삽입 순서**로 저장되므로, rank 순으로 얻은 id들은 디스크상 위치가 랜덤 → 최악의 경우 **random I/O 20회**
4. 총: 인덱스 3 + random lookup 20 = **~23회 I/O**

**Case B — `(year_week, product_id)` 복합 PK + 보조 인덱스 `(year_week, rank)`:**
1. 보조 인덱스에서 `year_week='2026-W15' ORDER BY rank LIMIT 20` → (year_week, product_id) 20개 획득 → 인덱스 I/O 2~3회
2. PK 클러스터드 인덱스에서 실제 row 읽기 — 여기가 핵심:
   - PK가 `(year_week, product_id)` 순서로 저장되므로 2026-W15 주차 100 row가 **물리적으로 연속(contiguous)**
   - 20 row가 같은/인접한 2~3개 페이지에 밀집 → **sequential I/O 2~3회**
3. 총: 인덱스 3 + sequential 2~3 = **~5회 I/O**

**차이**: 최악 시 **random 20회 vs sequential 2~3회**. HDD에선 수십 배 차이, SSD에서도 의미 있는 차이.

**더 강력한 최적화 — Covering Index:**
보조 인덱스에 자주 읽는 컬럼까지 포함하면(`(year_week, rank) INCLUDE (product_id, score, ...)`), PK 트리를 타지 않고 인덱스만으로 응답 완료 → **I/O 2~3회로 끝**. 이걸 **Covering Index**라 부름.

**[멘토 답변: 복합 PK로 바꿀 때 잃는 것]**

1. **`BaseEntity` 상속 불가**: BaseEntity는 `@Id @GeneratedValue val id: Long` 전제. 복합키로 가려면 `@EmbeddedId` 또는 `@IdClass`를 써야 해서 상속 구조 깨짐
2. **JPA Entity 매핑 복잡도 증가**: `@EmbeddedId val id: MvProductRankWeeklyId` + 별도 ID 클래스 (`@Embeddable data class`) 정의 필요 → 보일러플레이트
3. **단건 조회 API의 어색함**: `findById(1L)` → `findById(MvProductRankWeeklyId("2026-W15", 100L))` 로 파라미터 구조 변화
4. **JPA 연관관계 매핑의 불편**: 만약 다른 Entity가 이걸 참조한다면 FK도 복합이 됨 (MV는 보통 참조되지 않으므로 실질 문제 적음)
5. **일관성 규약의 예외 발생**: CLAUDE.md "기존 코드 패턴 분석 후 일관성 유지" 원칙과 겉보기엔 충돌. 실제로는 "MV는 도메인 Entity가 아닌 derived data"라는 근거로 예외 인정 가능

**얻는 것 (복기):**
- InnoDB Clustered Index의 range scan 효율 (위 I/O 비교 결과)
- 의미론적 명확성: PK 자체가 "어느 주차의 어느 상품 랭킹"이라는 정체성을 담음
- 자연스러운 natural key 활용 (surrogate key 불필요)

**실용적 타협안 (Q5 예고):**
사실 MV는 **JPA Entity로 매핑할 필요가 없을 수도 있음**. 만약 Q5에서 "배치는 JDBC로 쓰고 조회도 JdbcTemplate으로 한다"고 결정하면, 위 1~4번 단점이 **자동 해소**됨. 즉 PK 선택 문제가 도구 선택(JPA vs JDBC)과 맞물려 있음.

**[후속 질문: VARCHAR 정렬의 숨은 함정]**
4번에서 `VARCHAR(10) '2026-W15'` 선택. 이제 과거 주차 리스트 쿼리를 생각해봅시다:
```sql
SELECT DISTINCT year_week FROM mv_product_rank_weekly ORDER BY year_week;
```

실제 결과가 이렇게 나올 수 있음:
```
2026-W1
2026-W10
2026-W11
2026-W12
...
2026-W2
2026-W3
...
```

왜 이런 일이 생기고, 어떻게 해결할까요? (힌트: 문자열 정렬 규칙)

**[답변]**
> 그러네 너가 말한 것처럼 정렬에 문제가 생기겠네. 그렇다고 int도 문제가 생기고, 그러면 결국 (year SMALLINT, week TINYINT) 으로 넣는게 좋을거 같아

**[악마의 변호인: VARCHAR 해결책 & C안의 숨은 함정]**

너무 빠른 선회. 세 가지를 점검해보자.

**(1) VARCHAR의 진짜 해결책 — Zero-padding**

문자열 정렬 문제의 원인은 `'W1' vs 'W10'`에서 **자릿수가 다르기 때문**. 자릿수를 고정하면 해결됨:
```
'2026-W01', '2026-W02', ..., '2026-W09', '2026-W10', '2026-W11'
```
→ 문자열 사전순 정렬로도 완벽히 자연 정렬됨. ISO 8601이 `2026-01-15`, `2026-W01`처럼 **모든 자릿수에 zero-padding을 강제**하는 이유가 정확히 이것.

애플리케이션 레벨 생성도 간단:
```kotlin
val yearWeek = "%d-W%02d".format(year, week)  // "2026-W05"
```

그리고 zero-padded된 값은 항상 **8자 고정** → `CHAR(8)` 이 `VARCHAR(10)`보다 더 적절.

**(2) C안의 숨은 함정 — 연도 경계 범위 쿼리**

기획팀 요청: "2026년 52주차부터 2027년 3주차까지 최근 4주 랭킹 추이 보여줘"

- C안 `(year SMALLINT, week TINYINT)` 에서의 쿼리:
  ```sql
  WHERE (year = 2026 AND week >= 52)
     OR (year = 2027 AND week <= 3)
  ```
  연도 경계마다 조건이 분기됨. 최근 N주 쿼리 작성이 상당히 번거로움.

- Zero-padded CHAR(8) 에서의 쿼리:
  ```sql
  WHERE year_week BETWEEN '2026-W52' AND '2027-W03'
  ```
  한 줄. 문자열 사전순 정렬로도 자연 범위 지정.

- `ORDER BY year_week DESC LIMIT 10` (최근 10주) — CHAR(8)은 단일 컬럼 정렬. C안은 `(year DESC, week DESC)` 복합 정렬.

**(3) 기각 이유 재검증**

원래 C를 기각한 이유는 "**계산이 들어가서 안좋다**". 지금 다시 고르는 근거는 "VARCHAR 정렬 문제". 하지만:
- VARCHAR 정렬 문제는 zero-padding으로 해결됨
- C안의 "계산이 들어간다"는 문제는 여전히 유효함 (범위 쿼리 복잡, 조건 분기)

→ 기각 이유가 여전히 valid한데, 해결 가능한 다른 문제 때문에 돌아가는 건 **본말전도** 아닌가?

**재선택 질문:**
1. Zero-padded `CHAR(8) '2026-W15'` vs `(year SMALLINT, week TINYINT)` — 다시 고른다면?
2. 고른 이유는? (정렬/범위 쿼리/저장 공간/쿼리 간결성 중 어느 것을 우선?)

**[답변]**
> 원래 선택한 방법으로 갈게, 정렬이 해결된다면, 가독성도 좋고, 쿼리 간결성도 좋고, 저장 공간도 c안 보다 덜 차지 한다고 생각된다

**[멘토 팩트 교정: 저장 공간]**
저장 공간은 실제로는 C안이 유리하다:
- `CHAR(8)` = 8 bytes (ASCII 고정)
- `SMALLINT(2 bytes) + TINYINT(1 byte)` = 3 bytes
- row당 5 bytes 차이, 10,000 row × 2 보조 인덱스에서 ~100KB. **실무에선 무시 가능**.

판단의 진짜 근거는 "가독성 + 쿼리 간결성"이고, 저장 공간은 tiebreaker가 되지 못함. **결정은 그대로 유지하되 근거를 정리**할 것.

**[정리]**
- **확정: Zero-padded CHAR(8) `'2026-W15'` 형식**
- 근거 우선순위:
  1. **쿼리 간결성** (`BETWEEN`, 단일 컬럼 `ORDER BY`, 연도 경계 범위 쿼리가 한 줄)
  2. **가독성** (코드에서 `yearWeek` 하나로 표현, API 응답에서도 그대로 전달 가능)
  3. **정렬 자연성** (zero-padding + 사전순 정렬이 시간순과 정확히 일치)
- 저장 공간은 판단 근거가 아님 (미미한 차이)
- Zero-padding 규칙은 **application 레이어에서 강제** (`"%d-W%02d".format(...)`)
- 월간도 동일 패턴: `CHAR(7) '2026-04'` (이미 자연 zero-padded)
- **[글감]**: "문자열 식별자의 정렬 함정 — Zero-padding이 해결하는 것과 남기는 것"

---

### Q4 최종 CREATE TABLE (확정)

**주간:**
```sql
CREATE TABLE mv_product_rank_weekly (
    year_week    CHAR(8)     NOT NULL COMMENT 'ISO Week 형식: 2026-W15 (zero-padded)',
    product_id   BIGINT      NOT NULL,
    `rank`       INT         NOT NULL COMMENT '주차 내 순위 (1~100)',
    score        BIGINT      NOT NULL COMMENT '가중치 적용 최종 점수',
    view_count   BIGINT      NOT NULL DEFAULT 0 COMMENT '주간 누적 조회 수',
    like_count   BIGINT      NOT NULL DEFAULT 0 COMMENT '주간 누적 좋아요 수',
    sales_count  BIGINT      NOT NULL DEFAULT 0 COMMENT '주간 누적 판매 수',
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (year_week, product_id),
    KEY idx_year_week_rank (year_week, `rank`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='주간 상품 랭킹 MV (TOP 100/주차)';
```

**월간:**
```sql
CREATE TABLE mv_product_rank_monthly (
    `year_month` CHAR(7)     NOT NULL COMMENT 'YYYY-MM 형식: 2026-04',
    product_id   BIGINT      NOT NULL,
    `rank`       INT         NOT NULL,
    score        BIGINT      NOT NULL,
    view_count   BIGINT      NOT NULL DEFAULT 0,
    like_count   BIGINT      NOT NULL DEFAULT 0,
    sales_count  BIGINT      NOT NULL DEFAULT 0,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`year_month`, product_id),
    KEY idx_year_month_rank (`year_month`, `rank`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='월간 상품 랭킹 MV (TOP 100/월)';
```

**DDL 포인트:**
- `rank`, `year_month`는 MySQL 예약어 → 백틱 이스케이프 필수
- `PRIMARY KEY (year_week, product_id)` 복합키가 clustered index → 주차별 row 물리 연속 저장
- `KEY idx_year_week_rank` 보조 인덱스로 `WHERE year_week=? ORDER BY rank` filesort 제거
- `created_at/updated_at DATETIME(6)`: BaseEntity 관례와 정밀도 맞춤 (배치 실행 시각 추적)
- `utf8mb4`: 랭킹 조회가 다국어 상품명과 JOIN될 가능성 고려

---

## Q5. 배치에서의 JPA vs JDBC — Reader/Writer 도구 선택

**[질문]**
Q3에서 하이브리드 Chunk-Oriented 구조 확정 (Reader: DB `GROUP BY` 페이징 → Processor: 가중치 Java 계산 → Writer: TOP 100 upsert). 이제 **실제 구현 도구**를 선택할 차례.

**Spring Batch 제공 구현체:**
| 구분 | 후보 |
|---|---|
| Reader | `JpaPagingItemReader` / `JdbcPagingItemReader` / `JdbcCursorItemReader` / `RepositoryItemReader` |
| Writer | `JpaItemWriter` / `JdbcBatchItemWriter` |

**이 프로젝트 맥락:**
- Kotlin + Spring Boot 3.4.4, 모든 Entity는 `BaseEntity` 상속
- 기존 Repository는 **전부** `JpaRepository` 기반
- `product_metrics`는 Entity로 관리 중

**세부 질문 5개 (순차 답변):**

**Q5-1. Reader 선택 — 영속성 컨텍스트의 함정**
JPA 기반 Reader를 쓰면 "1차 캐시(영속성 컨텍스트)"라는 단어가 왜 배치에서 **문제**가 될까요? 100만 건 읽는 배치(chunk size = 1000, 총 1000 chunks)에서 구체적으로 어떤 일이 벌어지나요?

**Q5-2. Writer 선택 — Upsert 구현**
Q3에서 `ON DUPLICATE KEY UPDATE` 방식의 upsert로 결정. 이걸 `JpaItemWriter`(= `EntityManager.merge()`)로 구현하면 뭐가 문제인가요? `JdbcBatchItemWriter` + native SQL이 어떤 이득을 주나요?

**Q5-3. Reader 쿼리 형태 불일치**
Q3의 `SELECT product_id, SUM(view_count), SUM(like_count), SUM(sales_count) FROM product_metrics_daily WHERE metric_date BETWEEN ? AND ? GROUP BY product_id`는 Entity 매핑이 없는 **집계 투영(projection)** 결과. `JpaPagingItemReader`로 자연스럽게 표현되나요?

**Q5-4. "일관성" vs "최적화" 트레이드오프**
*"이 프로젝트는 JPA 기반이니 일관성을 위해 배치도 JPA로 가야 한다"* 는 팀원 주장에 동의하시나요? 반대한다면 근거는?

**Q5-5. 혼합 선택의 정당화**
Reader/Writer 모두 JDBC로 결정 시, *"어드민/API는 JPA 쓰면서 배치만 JDBC 쓰는 건 DIP 위반 아닌가?"* 에 어떻게 답변할 건가요?

---

### Q5-1. Reader 선택 — 영속성 컨텍스트의 함정

**[질문]**
200만 row × chunk 1000 = 2000개 chunk 시나리오. JPA 기반 Reader를 쓰면 1차 캐시가 배치에서 왜 독이 되는가?
1. 1/500/1000/2000번째 chunk 시점의 영속성 컨텍스트 상태
2. JVM heap / GC 관점
3. `EntityManager.clear()`로 해결되는지, 그래도 JPA가 JDBC 대비 손해 보는 부분

**[답변]**
> jpa를 사용하면 chunk 단위인 1000개가 1차 캐시에 있게 된다. 그러면 jpa 의 장점이 독이 되게 된다.
> 우선 n+1 발생할 수 있기 때문에 문제가 되고, 대용량 insert 에서 메모리 부족이나 느린 insert가 발생한다.
> chunk 기반에서 1000개 가 reader -> processor -> writer 를 하고 커밋하기 때문에 .... 라고 생각하기 때문에

**[멘토 평가 — 맞은 것 / 비켜간 것]**
- 맞음: "chunk 단위 1000개가 1차 캐시에 있게 된다"
- 비켜감: N+1은 연관관계 순회 시 발생 (이번 시나리오는 단일 테이블 GROUP BY 집계라 N+1 무관)
- 비켜감: "대용량 insert" 언급은 Writer(Q5-2) 관점. Q5-1은 Reader만의 관점에서 JPA의 손해를 보는 것
- 문장이 중간에 끊김 — 뒷부분이 핵심

**[악마의 변호인: chunk 커밋되면 캐시 비워지지 않나?]**
팀원 가상 반론: "Spring Batch는 chunk 경계에서 트랜잭션 커밋 → 영속성 컨텍스트도 닫힘 → 매 chunk마다 1000개만 있다가 비워지고 다시 1000개. 메모리 누적 문제는 사실상 없다."

**[후속 질문: a~d]**
- a. 위 팀원 주장은 어디가 맞고 어디가 틀린가? (JpaPagingItemReader의 transacted 옵션, page size vs chunk size)
- b. 100% 메모리 누적이 없다 가정해도, JPA는 Entity를 영속성 컨텍스트에 넣는 순간 "추가로 뭔가를 한다". 그게 뭔가? (변경 감지 → 스냅샷)
- c. 이 배치 Reader는 Entity 상태를 바꿀 일이 없는데, JPA는 b를 무조건 한다. 배치 관점의 비용은?
- d. Q3의 Reader 쿼리 결과물 한 row는 `ProductMetricsDaily` Entity인가? (Q5-3 예고편)

**[답변]**
> 전부다 알려줘

**[멘토 해설 — a: transacted 옵션과 page/chunk size]**
- 맞는 절반: chunk 경계 커밋 → chunk 트랜잭션 범위 영속성 컨텍스트는 닫힘 → 200만 개가 단순 누적되진 않음
- 틀린/미묘한 절반:
  - `JpaPagingItemReader.transacted = true` (기본값): Reader가 **자체 트랜잭션**으로 페이지를 읽고 즉시 커밋 → Entity가 chunk 트랜잭션에는 **detached 상태**로 전달됨. chunk 트랜잭션과 다른 트랜잭션
  - `transacted = false`: Reader/Writer가 chunk 트랜잭션 하나를 공유. Entity가 영속 상태로 chunk 내내 체류
  - `pageSize > chunkSize`면 다음 chunk용 Entity가 영속성 컨텍스트에 미리 올라와 누적됨
  - 실무 함정: 설정이 교과서대로일 때만 메모리 문제가 없음. 트랜잭션 경계가 복잡해져 디버깅·모니터링 비용 증가

**[멘토 해설 — b: 영속성 컨텍스트가 Entity를 받아들일 때 "추가로 하는 일"]**
- **스냅샷(Snapshot) 저장**: Entity 필드 값을 그대로 복사해 별도 보관
- flush/commit 시점에 현재 Entity vs 스냅샷을 필드 단위 비교 → 변경된 필드에 대해 UPDATE 자동 생성 = **Dirty Checking(변경 감지)**
- 결과: 영속성 컨텍스트 메모리 ≈ Entity × 2 (원본 + 스냅샷), CPU는 chunk당 (1000 × 필드 수)회 비교 루프

**[멘토 해설 — c: 읽기 전용 배치에서 Dirty Checking이 큰 낭비인 이유]**
- 이 배치 Reader는 Entity 상태를 **0% 수정**, 그러나 JPA는 스냅샷을 **100% 저장**하고 **변경 감지 루프를 무조건** 돎
- 200만 스냅샷 × 2000 chunks 분의 메모리 부하·CPU 낭비·Young GC 압박
- 완화책: `@QueryHints(HINT_READONLY=true)` 또는 `Session.setReadOnly(true)` → 스냅샷 생략
- **핵심 깨달음**: 영속성 컨텍스트의 모든 장점을 끄고 JPA를 쓴다는 것 자체가 도구 선택 오류 신호

**[멘토 해설 — d: Reader 쿼리 결과는 Entity가 아니다]**
- Q3 Reader 쿼리 결과 row = `(product_id, SUM(view), SUM(like), SUM(sales))` 4개 컬럼
- `ProductMetricsDaily` Entity 구조 = `(product_id, metric_date, view_count, like_count, sales_count, ...)` 7+개 컬럼, metric_date 포함
- GROUP BY로 metric_date가 소거되고, 집계 합계가 들어감 → **Entity 1:1 매핑 원천적으로 불가능**
- JPA 우회: `SELECT new ProductAggregateDto(...)` 생성자 투영. 작동은 하지만 JPA를 쓰는 의미가 거의 없음(Entity 매핑 0, 영속성 컨텍스트 불필요, 연관관계 없음)
- `JdbcPagingItemReader<ProductAggregateDto>` + `RowMapper`가 투영 쿼리의 **네이티브 표현 방식**

**[정리: Q5-1]**
JPA Reader의 3대 손해:
1. **스냅샷 메모리 2배 + Dirty Checking CPU** — 읽기 전용인데도 무조건 지불
2. **트랜잭션 경계 복잡성** — `transacted` 옵션·page/chunk 조합이 영속성 컨텍스트 수명 좌우
3. **투영 쿼리 표현 부자연스러움** — 집계 결과는 Entity가 아님, JPA의 장점을 쓸 자리 없음

**확정:** Reader는 `JdbcPagingItemReader` + `RowMapper`로 DTO(ProductAggregateDto) 직접 매핑.

**[글감]**: "JPA의 장점을 전부 꺼야 성능이 나온다면 도구 선택이 잘못된 것 — 배치에서 JPA Reader를 쓰지 않는 실질적 이유"

---

### Q5-2. Writer 선택 — Upsert 구현

**[상황]**
- Writer 동작: Processor가 계산한 주간 집계 결과 중 TOP 100을 `mv_product_rank_weekly`에 저장
- 같은 `(year_week, product_id)` 존재 시 갱신, 없으면 삽입 = **Upsert**
- MySQL 표준 패턴:
  ```sql
  INSERT INTO mv_product_rank_weekly (year_week, product_id, rank, score, view_count, like_count, sales_count)
  VALUES (?, ?, ?, ?, ?, ?, ?)
  ON DUPLICATE KEY UPDATE
      rank = VALUES(rank),
      score = VALUES(score),
      view_count = VALUES(view_count),
      like_count = VALUES(like_count),
      sales_count = VALUES(sales_count);
  ```

**[후보]**
- `JpaItemWriter` — 내부적으로 `EntityManager.merge(entity)` 호출
- `JdbcBatchItemWriter` — 위 native SQL을 JDBC batch로 실행

**[질문 a/b/c]**
- **a.** `EntityManager.merge()`의 동작 순서를 그리기 (신규 vs 기존 판단 로직 포함)
- **b.** a의 동작이 이 배치에서 문제가 되는 포인트 3가지 이상 (SELECT 횟수, 쓸모없는 SELECT, `rank` 컬럼 업데이트 인지 방식)
- **c.** `JdbcBatchItemWriter` + `INSERT ... ON DUPLICATE KEY UPDATE`의 이득 — 쿼리 횟수·SELECT 필요 여부 비교

**[답변]**
> a. 식별자로 1차 캐시에 존재여부 조회, 없으면 db 에서 식별자로 조회 한다. 식별자 마다 이런 판단을 하기 때문에 총 100개의 row이면 100번 반복하게 된다.
> b. top 100에서 이미 존재하는 데이터가 누구인지 알지 못 한다. 일단 top 100 데이터가 있는지 전부 가져오고, 있으면 rank 를 수정하고, 없으면 새로 만든다. 수정된 데이터는 더티체킹으로 update 가 될것이다.
> c. 100개의 row 이니 100번의 네트워크가 발생한다. mysql 에서는 insert 해보고 중복 key가 있다고 에러가 발생하면 바로 업데이트로 하게된다.

**[멘토 평가 — a, b: 정확]**
- a: merge = "1차 캐시 → DB SELECT → 판단 → persist/update 상태 전이"를 식별자마다 반복. 100건 upsert 시 **100번의 SELECT**가 선행
- b: JPA는 chunk 단위로 "누가 존재하는지"를 **미리 알 수 없음** → 매 배치마다 존재 여부 재확인. 존재 시 Dirty Checking으로 UPDATE 자동 생성

**[악마의 변호인 1: ON DUPLICATE KEY UPDATE는 에러 기반인가?]**
사용자 답 c의 "insert 해보고 중복 key가 있다고 에러가 발생하면 업데이트"는 부정확. 실제로는:
- MySQL이 **unique index(PK)를 lookup하여 충돌을 사전 예측**
- 충돌 감지 시 애플리케이션 레벨 예외 없이 **UPDATE 경로로 내부 분기**
- 에러 기반이라면 99건이 존재할 때 99번의 예외 복구 비용이 발생해 무의미해짐

**[악마의 변호인 2: 100건 = 100 왕복인가?]**
`JdbcBatchItemWriter`의 실제 동작:
- `addBatch()`는 클라이언트 메모리에 쌓기만 함 (네트워크 없음)
- `executeBatch()`에서 **1번의 왕복**으로 묶어 전송
- `rewriteBatchedStatements=true` 옵션 시 100건이 **단일 멀티-밸류 INSERT 한 줄**로 재작성 → 서버 파싱 비용까지 절감

**[답변]**
> 1. 지금 상황에서는 select 1번, write 1번 이 되겠네.
> 2. 네트워크 왕복은 없을거야. mysql 내부에서 한번에 할거야.
> 3. 네트워크 i/o 대기시간이 발생하는 만큼 배치 시간도 오래걸린다

**[멘토 평가 — 2, 3: 정확]**
- 2: `JdbcBatchItemWriter` + `ON DUPLICATE KEY UPDATE` + `rewriteBatchedStatements=true` = **왕복 1번**
- 3: latency 1ms 가정 시 JpaItemWriter ~200ms vs JdbcBatchItemWriter ~1ms. chunk 2000개 환산 시 수 분 vs 수 초 차이

**[1번 교정: Writer vs Reader 혼선]**
1번 답은 Reader 시나리오로 잘못 연결된 것. 실제 `JpaItemWriter`의 왕복 수:
- SELECT 100번 (batching 불가 — 다음 결과를 보고 INSERT/UPDATE 분기 결정 필요)
- INSERT/UPDATE 100번 (Hibernate batch 옵션으로 묶임 가능)
- **합계 최소 ~101회 ~ 최대 200회 왕복**

**[정리: Q5-2]**
JpaItemWriter의 3대 손해:
1. **merge의 선행 SELECT** — row당 1회, 100 row = 100회의 쓸모없는 왕복 (batching 불가)
2. **배치 관점의 무지(無知)** — 100건 중 누가 존재하는지 chunk 수준에서 알 수 없음 → 매 실행마다 전수 확인
3. **Dirty Checking 기반 UPDATE** — rank/score 재계산 결과가 모든 필드에 반영됐는지 필드별 비교 루프

JdbcBatchItemWriter + ON DUPLICATE KEY UPDATE의 이득:
1. **SELECT 제거** — MySQL 엔진이 인덱스 lookup으로 존재 여부를 **예측**하여 INSERT/UPDATE 내부 분기
2. **왕복 1회** — `rewriteBatchedStatements=true` 시 멀티-밸류 INSERT로 묶임
3. **예외 처리 비용 0** — 충돌 발생해도 예외 없음, 서버 내부에서 조용히 UPDATE로 전환

**확정:** Writer는 `JdbcBatchItemWriter` + `ON DUPLICATE KEY UPDATE` native SQL. JDBC URL에 `rewriteBatchedStatements=true` 옵션 필수.

**[글감]**: "`ON DUPLICATE KEY UPDATE`는 에러 기반이 아니다 — MySQL 인덱스 레벨의 충돌 예측과 내부 분기 메커니즘"
**[글감]**: "JPA merge 기반 upsert가 배치에서 200배 느린 이유 — SELECT batching이 원리적으로 불가능한 지점"

---

### Q5-3. Reader 쿼리 형태 불일치 — 요약 (Q5-1 d에서 이미 다룸)

- Reader 쿼리 결과는 `(product_id, SUM(view), SUM(like), SUM(sales))` **4컬럼 집계 투영**
- `ProductMetricsDaily` Entity(7+컬럼, metric_date 포함)와 1:1 매핑 원천 불가
- `JdbcPagingItemReader<ProductAggregateDto>` + `RowMapper`가 투영 쿼리의 네이티브 표현

---

## Q5-4 + Q5-5. "일관성" vs "최적화" — 팀원 반론 방어

**[상황]**
PR 리뷰에서 두 팀원이 JDBC 기반 배치에 반대:
- 팀원 A (시니어, 일관성 파): "프로젝트는 JPA로 통일돼 있음. 학습 비용 > 배치 성능 이득. 새벽 배치 200ms 차이는 과잉 최적화"
- 팀원 B (주니어, 아키텍처 파): "domain/Repository ← infrastructure/RepositoryImpl 구조로 DIP를 지키는데 배치는 JdbcTemplate 직접 사용 → DIP 위반 아닌가? Repository 인터페이스에 배치용 메서드 추가하는 게 맞지 않나?"

### Q5-4. 팀원 A에게 답변

**[답변]**
> 일관성 유지는 너무 좋다. 근데 일관성이라는게 기술에 대한 일관성을 꼭 말하는것 일까? 우리가 하고자하는 문제해결을 위해 적절한 기술을 선택하는 것이 좋지 않을까? 생각된다. 일관성은 아키텍쳐나 운영/유지보수를 위한 일관성을 봐야한다고 생각된다. 그리고 배치는 대용량 일괄처리로 1개의 데이터를 비교하면 별차이 없어보이지만 쌓이다 보면 엄청난 차이가 발생한다.

**[멘토 평가]**
"일관성의 대상을 재정의"하는 프레이밍이 핵심 무기. 도구(JPA/JDBC) 일관성이 아니라 **아키텍처 일관성**(레이어드 구조, BaseEntity, CoreException, ApiResponse, 테스트 패턴, ArchUnit)이 진짜 지켜야 할 축.

**[멘토 보완: 숫자로 설득력 강화]**
- 시나리오 1 (주간 MV, 100만 상품): JPA ~200초 vs JDBC ~1초 (Writer만). 전체 10분 vs 30초 수준
- 시나리오 2 (장애 복구): 30분 배치 vs 2~3분 배치 → 업무시간 재실행 시 DB 락 경합·운영 부담 차이
- 시나리오 3 (가중치 변경 재실행): Q1 "재실행 자유도" 가치가 JPA 버전에선 **무력화**됨. 10초에 끝나야 "일단 돌려보자" 가능
- "과잉 최적화" 판단 기준: SLA 안에 들어오는가 / 실패 시 재실행이 현실적인가 / 데이터 규모가 자연 증가하는가

### Q5-5. 팀원 B에게 답변

**[답변]**
> 우리가 dip를 사용하는 이유는 domain에서 infrastructure에서 jpa를 사용하던 jdbc를 사용하던 domain은 모르겠고, select 든 save 든 우리가 요청하는 작업만 해줘 라는 뜻이다. 그리고 하나의 repository에 배치용까지 만드는게 아니라 따로 repository를 만들어 관리하는게 유지보수 에 유용하다

**[후속 질문: "배치용 Repository"의 실체]**
1. 어느 모듈·패키지에 둘 건가? (후보 1: apps/commerce-batch 내부 자체 Reader/Writer, 후보 2: domain/Repository 인터페이스 + infrastructure 구현, 후보 3: Spring Batch 프레임워크 인터페이스 활용)
2. 배치 Reader의 **호출자**는 누구인가? (domain Service? Spring Batch 프레임워크?)
3. Spring Batch의 `ItemReader<T>` / `ItemWriter<T>` 위에 추가로 domain Repository를 얹으면 **2중 추상화**가 되지 않나?

**[답변]**
> 이미 배치에서 제공하는 인터페이스가 있기 때문에 따로 만들필요 없이 가져다 사용하면 된다. 그리고 우리가 만드는 것보다 제공하는 기능이 많을 것이고 최적화도 가능할 것이다.

**[멘토 평가: 3번 정답. 1, 2번 보완]**

팀원 B 제안을 거절하는 이유 3가지:

1. **호출자가 다르면 인터페이스의 존재 이유가 다르다**
   - `domain/Repository`: domain Service가 호출 (도메인이 인프라 세부를 모르게)
   - `ItemReader<T>`: Spring Batch 프레임워크가 호출 (StepBuilder chunk 루프)
   - 호출자 다른 두 인터페이스 묶으면 역할 충돌

2. **Reader 쿼리는 도메인 의미 없는 ETL 집계 투영**
   - `SUM + GROUP BY` 결과는 Entity가 아닌 일시적 DTO
   - domain Repository에 주간 집계 메서드 넣으면 "영속성 관리"에서 "ETL 창구"로 책임 오염

3. **Spring Batch 프레임워크가 이미 제공하는 추상화 활용** (사용자 답변)
   - `JdbcPagingItemReader<T>`, `JdbcBatchItemWriter<T>` 직접 활용
   - chunk 제어, restart, skip, retry, 트랜잭션 경계 프레임워크 관리
   - 자체 Repository로 래핑하면 프레임워크 기본 기능 상실

**[최종 구조]**
```
apps/commerce-batch/
  batch/weekly-ranking/
    WeeklyRankingJobConfig.kt      # Job/Step 정의
    ProductAggregateReader.kt      # JdbcPagingItemReader Bean
    WeightScoreProcessor.kt        # Java 가중치 계산
    ProductRankWeeklyWriter.kt     # JdbcBatchItemWriter Bean
  dto/
    ProductAggregateDto.kt         # Reader projection 결과
    ProductRankWeeklyRow.kt        # Writer upsert row
```
- domain/Repository와 완전 분리
- Spring Batch 네이티브 인터페이스 활용 → DIP 위반 아님

**[정리: Q5-4/Q5-5]**
- **일관성의 대상 재정의**: 도구 일관성 ≠ 아키텍처 일관성. 레이어드 구조/에러 처리/테스트 패턴은 JDBC 배치로도 그대로 유지됨
- **"과잉 최적화" 반박**: 100만 row × chunk 2000 환산 시 10분 vs 30초 차이. 장애 복구/가중치 재계산 시나리오에서 JPA는 배치의 본래 가치를 훼손
- **DIP의 본질**: abstraction 개수 경쟁이 아니라 "domain이 infrastructure 세부에 의존하지 않는가". 배치는 domain이 호출하지 않으므로 DIP 적용 대상 외
- **2중 추상화 금지**: `ItemReader<T>`는 이미 Spring Batch의 프레임워크 추상화. 그 위에 domain Repository를 얹으면 프레임워크 기능 상실 + 책임 오염
- **[글감]**: "일관성의 대상 재정의 — 도구 일관성 vs 아키텍처 일관성, 배치 모듈의 JDBC 선택 정당화"
- **[글감]**: "DIP는 abstraction 만들기 대회가 아니다 — Spring Batch ItemReader/Writer 위에 domain Repository를 얹지 않는 이유"

---

### Q5 전체 확정 요약

| 항목 | 선택 | 근거 |
|---|---|---|
| Reader | `JdbcPagingItemReader<ProductAggregateDto>` | 영속성 컨텍스트 낭비, 집계 투영은 Entity 아님 |
| Processor | `ItemProcessor<ProductAggregateDto, ProductRankRow>` | 가중치 Java 계산, `@ConfigurationProperties` 외부화 |
| Writer | `JdbcBatchItemWriter` + `ON DUPLICATE KEY UPDATE` | merge SELECT 제거, 왕복 1회, 예외 처리 비용 0 |
| JDBC URL | `rewriteBatchedStatements=true` | 멀티-밸류 INSERT rewrite |
| 아키텍처 | `apps/commerce-batch/` 내부 자체 Reader/Writer, domain Repository 불간섭 | 호출자·책임·추상화 계층이 domain과 다름 |

---

## Q6. 멱등성과 재실행 전략

**[상황]**
주간 배치가 매일 새벽 3시 실행. `targetDate=2026-04-15` 파라미터로 `2026-W15` 누적분 집계. 같은 targetDate로 2번 실행해도 데이터 망가지면 안 됨.

**[3가지 시나리오]**
- A: 장애 복구 재실행 (70% 완료 후 실패 → 오전 재실행)
- B: 실수 재실행 (정상 완료된 배치를 담당자가 다시 실행)
- C: 가중치 변경 후 과거 재계산 (같은 targetDate로 다시 돌리고 싶음)

### Q6-a. Spring Batch JobParameters 중복 방지

**[답변]**
> job을 실행하면 메타테이블에 저장을 하는데 여기에 존재하면 재실행을 막는다.

**[멘토 평가: 정확]**
`BATCH_JOB_INSTANCE`에 (Job이름 + JobParameters) 유니크 존재 시 `JobInstanceAlreadyCompleteException`.
- `BATCH_JOB_INSTANCE`: 중복 방지 주체 (COMPLETED 상태의 동일 파라미터 재실행 차단)
- `BATCH_JOB_EXECUTION`: 실행 시도 기록 (FAILED는 재시도 가능)
- 시나리오 A(실패 후 재실행)는 허용, B(완료 후 재실행)는 거부가 기본 동작

### Q6-b. JobParametersIncrementer 선택

**[답변]**
> 옵션 3 선택, 재실행을 하더라도 몇 번 실행했는지 관리하는 것 또한 유지보수하는데 필요하다.

**[멘토 평가: 정답]**
`RunIdIncrementer` → 매 실행마다 `run.id` 자동 +1, JobInstance 분리.

**[악마의 변호인: Incrementer는 중복 방지를 무력화하는가?]**
- 반론: "run.id로 매번 다른 JobInstance가 되면 Q6-a 중복 방지 의미 없어지는 것 아닌가?"
- 반박: 실행 히스토리(`BATCH_JOB_EXECUTION`)는 전부 남음 → 언제·몇 번째 실행인지 추적 가능
- 본질: Incrementer는 "이건 **새 실행**이다"라고 명시적으로 선언하는 장치. 의도적 재실행과 실수 재실행을 **파라미터로 구분**하는 메커니즘
- 역할 분리: `targetDate`는 비즈니스 파라미터, `run.id`는 실행 시퀀스 파라미터

### Q6-c. 실패 후 재실행 시 MV 정합성

**[답변]**
> 방법 3를 선택할 것이다. 랭킹 시스템이라는 비즈니스를 생각하면 방법1번은 조회 리스크가 있을것이고, 방법2는 성공했는데 플래그 업데이트가 실패하면? 이라는 문제가 있을 수 있다.

**[멘토 평가: 선택 타당, 진짜 이유는 더 근본적]**

**[악마의 변호인: upsert인데 왜 staging이 필요한가?]**
- 함정 시나리오: 1차 실행 TOP 100 = `[1, 2, ..., 100]`, 가중치 변경 후 2차 TOP 100 = `[1, 2, ..., 99, 200]` (상품 100 탈락, 상품 200 신규 진입)
- 2차 upsert는 `[1~99, 200]`만 건드림 → **상품 100의 이전 row 그대로 잔존** → MV 최종 101개 row → TOP 100 깨짐
- **staging swap의 진짜 이유: 탈락한 row 정리**

**[실전 구현 패턴]**
- 방법 1 변형 (Step 분리): Step 1 = DELETE, Step 2 = INSERT. Step 2 실패 시 **빈 MV 노출** 위험
- 방법 3 staging: Step 1 = staging 테이블에 집계, Step 2 = `DELETE mv WHERE year_week=?` + `INSERT mv SELECT FROM staging`을 **단일 트랜잭션**. 원자성 보장 ✓
- 간소화: staging 없이 최종 Step에서 `DELETE + INSERT SELECT ... GROUP BY`를 한 트랜잭션에 묶음 (중간 빈 MV 시점 발생 → 조회 캐시로 보호)

### Q6-d. 의도적 재실행 vs 실수 재실행 구분

**[답변]**
> 없다고 생각한다. 그래서 재실행되더라도 같은 값이 반환되도록 멱등성을 유지하게 해야한다.

**[멘토 평가: 진짜 정답]**
이 답이 Q6 전체를 푸는 열쇠.
- 의도적/실수 재실행을 **구분하려는 설계**가 아니라 **구분이 무의미한 설계**가 robust
- upsert + 결정론적 가중치 계산 + staging swap → 몇 번 돌려도 최종 상태 동일
- 실수 재실행이 발생해도 데이터 망가지지 않으므로 허용 가능

**[Spring Batch 중복 방지의 역할 재정의]**
- 본래 의도: 실수 재실행 방지
- 진짜 가치: **동시 실행으로부터 MV 보호** (멱등이어도 두 배치가 동시 실행되면 락 경합)
- Incrementer로 수동 재실행 허용 + **동시 실행 방지**는 별도 락(K8s CronJob `concurrencyPolicy: Forbid`, DB advisory lock)으로 보장

### Q6 전체 확정

1. **JobParameters**: `targetDate=YYYYMMDD` (비즈니스) + `run.id` (Incrementer 자동 주입)
2. **`RunIdIncrementer`** 적용 → 같은 targetDate 재실행 허용
3. **Staging + Swap**: year_week 단위 `DELETE + INSERT SELECT FROM staging`을 단일 트랜잭션 (탈락 row 정리)
4. **멱등성**: upsert + 결정론적 계산 + staging swap 3중 보장
5. **동시 실행 방지**: K8s `concurrencyPolicy: Forbid` 또는 DB advisory lock (멱등과 별개 축)

**[글감]**: "멱등성은 구분하는 설계가 아니라 구분이 무의미한 설계 — 배치 재실행 안전성의 본질"
**[글감]**: "upsert가 멱등을 완성하지 못하는 순간 — TOP 100 탈락 row 처리와 Staging+Swap 패턴"

---

## 다음 세션 이어가기

**중단 지점:** Q7 (API 확장 — period 파라미터 + Redis/MV 분기) 진행 예정
**재개 방법:** `/learn-round @docs/notes/learn-round10-qa-notes.md , @docs/quests/round-10.md 이어서`

**남은 주요 주제:**
- Q7. API 확장 (`period` 파라미터, 일간=Redis / 주간·월간=MV 분기)
- Q8. 실패 복구, 모니터링, 운영 관점
- Q9. `product_metrics` 일별 스키마 재설계 (Q1에서 합의된 전제)
- 최종 백지 설계 테스트
**다음 세션 시작 방법:** 이 파일(`docs/notes/learn-round10-qa-notes.md`)을 참조로 지정해서 `/learn-round @docs/quests/round-10.md` 실행
**남은 주요 주제 (내부 설계):**
- Q5. 배치에서의 JPA vs JDBC — Reader/Writer 도구 선택 **[진행 중]**
- Q6. 멱등성과 재실행 전략 (같은 날짜 두 번 돌리면? JobParameter, 중복 실행 방지)
- Q7. API 확장 (`period` 파라미터) 및 일간(Redis) / 주간·월간(MV) 분기 처리
- Q8. 실패 복구, 모니터링, 운영 관점
- Q9. product_metrics 일별 스키마 재설계 (현재 상품별 누적 → 일별 row)
- 최종 백지 설계 테스트


