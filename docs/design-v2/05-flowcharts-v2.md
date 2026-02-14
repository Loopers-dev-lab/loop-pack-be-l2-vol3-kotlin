# 플로우차트

시스템의 핵심 흐름을 의사결정 관점에서 시각화한다.
시퀀스 다이어그램이 "누가 누구를 호출하는가"를 보여준다면, 플로우차트는 "어떤 조건에서 어떤 경로로 분기하는가"를 보여준다.

> 단순 CRUD는 시퀀스 다이어그램으로 충분하므로, **분기가 복잡하거나 cross-domain 흐름**만 플로우차트로 작성한다.

---

## 1. 서비스 전체 흐름

사용자가 시스템을 이용하는 핵심 여정을 조감도로 표현한다.

```mermaid
flowchart TB
    A([사용자 진입]) --> B[회원가입 / 로그인]
    B --> C[브랜드 & 상품 탐색]

    C --> D{마음에 드는 상품?}
    D -->|예| E[좋아요 등록]
    D -->|아니오| C

    E --> F[상품 선택 & 수량 결정]
    C --> F

    F --> G[주문 요청]
    G --> H{상품 유효성 검증}
    H -->|실패| I[주문 실패]:::error
    I --> C

    H -->|통과| J{재고 충분?}
    J -->|부족| I
    J -->|충분| K[재고 차감 + 스냅샷 저장]
    K --> L[주문 완료]:::success

    L --> M[주문 내역 조회]
    M --> C

    classDef success fill:#c8e6c9,stroke:#2e7d32
    classDef error fill:#ffcdd2,stroke:#c62828
```

### 참고

- 쿠폰 발급/적용 단계는 추후 개발 시 F→G 사이에 삽입 예정
- 결제 단계는 K→L 사이에 삽입 예정
- 어드민 흐름(브랜드/상품 관리)은 별도 경로이므로 이 조감도에서 생략

---

## 2. 주문 생성 프로세스

주문 생성 시 내부에서 일어나는 검증 및 처리 로직의 의사결정 흐름이다.
시퀀스 다이어그램 4.1과 대응된다.

```mermaid
flowchart TB
    Start([주문 요청 수신]) --> Validate{요청 데이터 검증}
    Validate -->|items 비어있음 / 중복 productId / quantity ≤ 0| Err400[400 Bad Request]:::error

    Validate -->|유효| FetchProducts[주문 대상 상품 일괄 조회]
    FetchProducts --> CheckExist{모든 상품 존재?}
    CheckExist -->|일부 없음 / 삭제됨| Err400_2[400 존재하지 않는 상품]:::error

    CheckExist -->|전부 존재| CheckStatus{모두 ON_SALE?}
    CheckStatus -->|HIDDEN / SOLD_OUT 포함| Err400_3[400 판매 불가 상품]:::error

    CheckStatus -->|모두 판매중| CheckStock{모든 상품 재고 충분?}
    CheckStock -->|1개라도 부족| Err400_4[400 재고 부족]:::error

    CheckStock -->|충분| Deduct[상품별 재고 차감]
    Deduct --> AutoStatus{차감 후 재고 == 0?}
    AutoStatus -->|예| StatusChange[status → SOLD_OUT 자동 전환]
    AutoStatus -->|아니오| Snapshot
    StatusChange --> Snapshot[상품 이름/가격 스냅샷 복사]
    Snapshot --> CreateOrder[Order + OrderItem 생성]
    CreateOrder --> CalcTotal[totalPrice 계산]
    CalcTotal --> Save[DB 저장]
    Save --> Response([200 OK + orderId]):::success

    classDef error fill:#ffcdd2,stroke:#c62828
    classDef success fill:#c8e6c9,stroke:#2e7d32
```

### 참고

- 전체 과정이 **하나의 트랜잭션** 내에서 원자적으로 실행 (all-or-nothing)
- 어느 단계에서든 실패하면 트랜잭션 롤백으로 재고 차감도 원복
- 인증 인터셉터 검증은 전제 조건으로 생략 (시퀀스 다이어그램 공통 규칙 참고)

---

## 3. 좋아요 토글 (등록 / 취소)

좋아요의 멱등성 보장과 삭제된 상품에 대한 분기 처리를 시각화한다.
시퀀스 다이어그램 3.1, 3.2와 대응된다.

### 3.1 좋아요 등록

```mermaid
flowchart TB
    Start([좋아요 등록 요청]) --> CheckProduct{상품 존재 & 미삭제?}
    CheckProduct -->|없음 / 삭제됨| Err404[404 Not Found]:::error

    CheckProduct -->|유효| CheckLike{이미 좋아요 존재?}
    CheckLike -->|존재| Idempotent([200 OK — 변경 없음]):::idempotent

    CheckLike -->|없음| SaveLike[Like 엔티티 저장]
    SaveLike --> IncCount[Product.likeCount + 1]
    IncCount --> Success([200 OK]):::success

    classDef error fill:#ffcdd2,stroke:#c62828
    classDef success fill:#c8e6c9,stroke:#2e7d32
    classDef idempotent fill:#fff9c4,stroke:#f9a825
```

### 3.2 좋아요 취소

```mermaid
flowchart TB
    Start([좋아요 취소 요청]) --> CheckProduct{상품 존재?}
    CheckProduct -->|없음| Err404[404 Not Found]:::error

    CheckProduct -->|존재| CheckLike{좋아요 존재?}
    CheckLike -->|없음| Idempotent([200 OK — 변경 없음]):::idempotent

    CheckLike -->|존재| DeleteLike[Like 엔티티 물리 삭제]
    DeleteLike --> CheckDeleted{상품이 삭제 상태?}
    CheckDeleted -->|삭제됨| SkipCount([200 OK — likeCount 미갱신]):::idempotent
    CheckDeleted -->|활성| DecCount[Product.likeCount - 1]
    DecCount --> Success([200 OK]):::success

    classDef error fill:#ffcdd2,stroke:#c62828
    classDef success fill:#c8e6c9,stroke:#2e7d32
    classDef idempotent fill:#fff9c4,stroke:#f9a825
```

### 참고

- 등록: 삭제된 상품에는 좋아요 등록 불가 (404)
- 취소: 삭제된 상품이라도 Like 레코드가 있으면 물리 삭제 수행, 단 likeCount는 갱신하지 않음
- 노란색 경로는 멱등성에 의한 무변경 응답
