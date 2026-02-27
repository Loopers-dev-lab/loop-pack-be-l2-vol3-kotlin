# Brand-Product 애그리거트 분석

## 배경

Brand와 Product 도메인이 애그리거트로 묶여야 하는지 분석한다.

## 현재 구조

| 항목 | Brand | Product |
|------|-------|---------|
| 엔티티 | name, description | name, description, price, stockQuantity, likes, brandId |
| 참조 방식 | - | `brandId: Long` (ID 참조) |
| 독립 API | O (조회/CRUD) | O (조회/CRUD/재고/좋아요) |
| 생명주기 | 독립 | 독립 (Brand 삭제 시 연쇄 삭제) |

### 연결 지점

- Product는 `brandId: Long`으로 Brand를 참조한다 (생성 시 설정, 변경 불가)
- Brand 삭제 시 `AdminBrandFacade`에서 소속 Product를 연쇄 삭제한다
- Product 상세 응답에 brandName을 포함하며, Facade에서 BrandService를 호출해 조합한다

## 결론: 별도 애그리거트 유지

Brand와 Product를 하나의 애그리거트로 묶지 않는다.

### 근거

**1. 트랜잭션 경계가 다르다**

Product는 재고 차감(`deductStock`), 좋아요(`increaseLikeCount`) 등 Brand와 무관한 상태 변경이 빈번하다. 하나의 애그리거트로 묶으면 Product 수정 시마다 Brand까지 잠금 범위에 들어가 불필요한 동시성 병목이 생긴다.

**2. Brand는 공유 참조 대상이다**

1:N 관계(하나의 Brand에 다수의 Product)이며, Product는 주문(Order) 등 다른 도메인에서도 직접 참조된다. DDD에서 애그리거트 내부 엔티티는 외부에서 직접 참조하면 안 되므로, Product를 Brand 내부 엔티티로 두면 이 원칙에 위배된다.

**3. 독립적인 불변식을 갖는다**

- Brand: "이름이 비어있으면 안 된다"
- Product: "가격 > 0", "재고 >= 0", "브랜드 변경 불가"

서로의 불변식을 보호하기 위해 같은 트랜잭션에 있을 필요가 없다.

**4. 현재 설계가 DDD 권장 패턴을 따른다**

- Product → Brand를 ID 참조(`brandId: Long`)로 연결 — 애그리거트 간 참조의 권장 패턴
- 연쇄 삭제는 Application Layer(`AdminBrandFacade`)에서 조율 — 적절한 책임 배치
- Brand 정보 보강(brandName)은 Facade에서 조합 — 도메인 계층의 독립성 유지

### 만약 하나의 애그리거트로 묶는다면

```
Brand (Aggregate Root)
  └── Product (내부 엔티티)
```

발생하는 문제:

- Product를 직접 조회/수정할 수 없고, 항상 Brand를 통해야 한다
- `GET /api/v1/products/{productId}` 같은 독립 API가 부자연스러워진다
- 주문에서 Product를 참조할 때도 Brand를 거쳐야 한다
- Brand 하나에 수천 개 Product가 있으면 애그리거트 로딩 비용이 과도해진다

## 향후 고려 사항

`AdminBrandFacade.deleteBrand()`에서 `@Transactional`로 Product 연쇄 삭제와 Brand 삭제를 하나의 트랜잭션으로 묶고 있다. 현재는 강한 일관성을 택한 것이며 정합성이 보장된다. 향후 Product 수가 많아지면 도메인 이벤트 기반 비동기 처리(결과적 일관성)로 전환을 고려할 수 있다.
