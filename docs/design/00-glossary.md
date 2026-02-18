# 00. 유비쿼터스 언어 (Ubiquitous Language)

> 설계 문서, 코드, 기획, 커뮤니케이션에서 **동일한 용어**를 사용하기 위한 용어 사전.
> 코드의 영문 네이밍과 문서의 한글 표현을 1:1로 매핑한다.

---

## 도메인 용어

| 한글 | 영문 (코드) | 설명 |
|------|------------|------|
| 브랜드 | `Brand` | 상품을 묶는 상위 카테고리. 자사몰(1P)에서 관리자가 직접 등록 |
| 상품 | `Product` | 판매 가능한 단일 아이템. 하나의 브랜드에 소속 |
| 좋아요 | `Like` | 회원이 특정 상품에 관심을 표시하는 행위 |
| 주문 | `Order` | 회원이 1개 이상의 상품을 구매 요청하는 단위 |
| 주문 상품 | `OrderItem` | 주문에 포함된 개별 상품 항목 (수량 포함) |
| 주문번호 | `orderNumber` | 주문을 식별하는 외부 노출용 번호 (yyMMddxxxxxxxx, 14자리) |
| 회원 | `User` | 서비스에 가입한 사용자 |
| 관리자 | `Admin` | LDAP 인증 기반의 내부 운영자 |

---

## 상태 용어

| 한글 | 영문 (코드) | 소속 | 설명 |
|------|------------|------|------|
| 판매중 | `ACTIVE` | ProductStatus | 구매 가능 상태 |
| 판매중지 | `INACTIVE` | ProductStatus | 구매 불가 상태 (전시 여부와 무관) |
| 삭제됨 | `DELETED` | ProductStatus | Soft Delete된 상태 |
| 전시 여부 | `displayYn` | Product 필드 | 목록 노출 여부 (Boolean → MySQL TINYINT(1) 자동 매핑) |
| 주문완료 | `ORDERED` | OrderStatus | 주문이 생성된 상태 (현재 과제 범위) |

---

## 기술 용어

| 한글 | 영문 (코드) | 설명 |
|------|------------|------|
| 스냅샷 | `Snapshot` | 주문 시점의 상품/가격 정보를 복사하여 보존하는 것 |
| 상품 스냅샷 | `ProductSnapshot` | @Embeddable — 상품명, 브랜드명, 이미지 등 |
| 가격 스냅샷 | `PriceSnapshot` | @Embeddable — 원가, 할인액, 최종가 |
| 좋아요 수 | `likeCount` | Product에 캐싱된 좋아요 카운트 (정렬용) |
| 재고 수량 | `stockQuantity` | 현재 판매 가능한 재고 수 |
| 논리 삭제 | Soft Delete | `deletedAt` 컬럼에 삭제 일시를 기록하여 논리적으로 삭제 |
| 물리 삭제 | Hard Delete | DB에서 실제 레코드를 DELETE |

---

## 레이어 용어

| 한글 | 영문 (코드) | 패키지 | 역할 |
|------|------------|--------|------|
| 컨트롤러 | `Controller` | interfaces | HTTP 요청 수신, DTO 변환, 인증 헤더 검증 |
| 파사드 | `Facade` | application | 여러 도메인 서비스 조합, 트랜잭션 경계 |
| 서비스 | `Service` | domain | 단일 도메인 비즈니스 로직 |
| 리포지토리 | `Repository` | domain (인터페이스) / infrastructure (구현체) | 데이터 접근 추상화 |

---

## 데이터 전달 객체

| 한글 | 영문 (코드) | 위치 | 역할 |
|------|------------|------|------|
| 요청 DTO | `*Dto.CreateRequest` 등 | interfaces | HTTP 요청 역직렬화 + Bean Validation |
| 커맨드 | `*Command` | application | 비즈니스 의도를 담은 명령 객체 (Dto → Command 변환) |
| 인포 | `*Info` | application | Facade → Controller 응답 전달 |
| 응답 DTO | `*Dto.Response` | interfaces | HTTP 응답 직렬화 (Info → Response 변환) |

---

## 사용 금지 용어 (Deprecated)

| 금지 용어 | 대체 용어 | 이유 |
|----------|----------|------|
| Goods | Product | 프로젝트 표준 용어 |
| Buyer / Customer / Member | User | 1주차에 User로 확정 |
| OrderGoods / OrderLine | OrderItem | 프로젝트 표준 용어 |
| Favorite / Wish / Bookmark | Like | 프로젝트 표준 용어 |
| qty | quantity | 축약어 사용 금지 (코드에서도 풀네임) |
| amt | amount | 축약어 사용 금지 |
| cnt | count | 축약어 사용 금지 |
| yn (변수명에서) | 허용 | `displayYn` 등 Boolean 네이밍은 한국 이커머스 관례상 허용. Kotlin `is` prefix는 Jackson 직렬화 충돌 위험이 있어 `Yn` 접미사를 채택 |

---

*이 문서는 설계/구현 전 과정에서 참조하며, 새로운 도메인 용어가 추가되면 여기에 먼저 정의한다.*
