---
name: erd
description:
  영속성 구조를 Mermaid ERD로 작성한다.
  테이블명, 컬럼, FK, soft delete를 포함하며 docs/design/에 저장한다.
---

대상 도메인: $ARGUMENTS

## 절차

1. 클래스 다이어그램이 있으면 참고 (`docs/design/03-class-diagram.md`)
2. 다이어그램을 그리기 전에 설명한다:
    - 영속성 구조에서 **무엇을 확인**하려는지
    - 클래스 다이어그램과의 차이점 (VO → 컬럼, 관계 → FK)
3. ERD를 Mermaid 문법으로 작성한다:
    - 테이블명, 컬럼명, 타입, 제약조건 (PK, FK, NOT NULL, UNIQUE)
    - BaseEntity 공통 컬럼 포함 (id, created_at, updated_at, deleted_at)
    - 관계 표현: `||--o{` (1:N), `}o--o{` (N:M → 조인 테이블)
4. 해석 제공:
    - 관계의 주인은 누구인가
    - 인덱스가 필요한 컬럼
    - 정규화 vs 비정규화 판단 근거
5. `docs/design/04-erd.md`에 추가한다
6. 잠재 리스크 언급

## Mermaid 형식 예시

```mermaid
erDiagram
    products {
        bigint id PK
        varchar name
        bigint brand_id FK
    }
    brands {
        bigint id PK
        varchar name
    }
    likes {
        bigint member_id PK, FK
        bigint product_id PK, FK
        timestamp created_at
    }
    members {
        bigint id PK
        varchar name
    }

    products ||--o{ likes: ""
    members ||--o{ likes: ""
    brands ||--o{ products: ""
  ```

## 규칙

- 테이블명은 복수형 (products, orders, users)
- soft delete: `deleted_at` nullable datetime
- enum은 VARCHAR로 저장
- N:M은 반드시 조인 테이블로 풀어서 표현
- VO는 테이블로 분리하지 않음 — Entity의 컬럼으로 표현
