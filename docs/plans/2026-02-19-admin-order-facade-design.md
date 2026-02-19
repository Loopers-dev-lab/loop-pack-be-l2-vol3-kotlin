# AdminOrderFacade 설계 문서

**작성일**: 2026-02-19
**목적**: 시스템 관리자가 모든 주문을 조회할 수 있는 관리자용 Facade 구현

## 개요

AdminOrderFacade는 시스템 관리자 전용 주문 조회 기능을 제공합니다. 기존 OrderFacade(일반 사용자용)와 분리하여 역할별로 명확하게 관리합니다.

## 아키텍처 구조

### 레이어별 배치
```
application/api/order/
├── OrderFacade.kt (기존, 일반 사용자용)
└── AdminOrderFacade.kt (신규, 관리자용)

domain/order/dto/
├── OrderedDto.kt (기존)
└── AdminOrderDto.kt (신규)
```

### 의존성
```
AdminOrderFacade
└── OrderService (읽기 전용)

OrderService
└── OrderRepository (읽기 전용)
```

## 기능 명세

### AdminOrderFacade 메서드

#### 1. 주문 목록 조회
```kotlin
fun getOrders(pageable: Pageable): Page<AdminOrderDto>
```
- **설명**: 모든 주문을 페이지네이션으로 조회
- **파라미터**: `pageable` - Spring Data Pageable (페이지번호, 크기)
- **반환**: `Page<AdminOrderDto>`
- **정렬**: 생성일 기준 최신순 (내림차순)
- **접근 제어**: 관리자 전용 (컨트롤러 단에서 처리)

#### 2. 주문 상세 조회
```kotlin
fun getOrderById(orderId: Long): AdminOrderDto
```
- **설명**: 특정 주문의 상세 정보 조회
- **파라미터**: `orderId` - 주문 ID
- **반환**: `AdminOrderDto`
- **예외 처리**: 주문이 없으면 `ErrorType.NOT_FOUND` 에러 반환
- **접근 제어**: 관리자 전용

## DTO 설계

### AdminOrderDto
OrderedDto에 고객 아이디(userId)를 추가한 관리자용 DTO:

```kotlin
data class AdminOrderDto(
    val orderId: Long,              // 주문 ID
    val userId: Long,               // 고객 ID (관리자 필요정보)
    val orderDate: ZonedDateTime,   // 주문일시
    val totalPrice: BigDecimal,     // 총액
    val orderItems: List<OrderedItemDto>, // 주문 항목 목록
) {
    companion object {
        fun from(order: Order): AdminOrderDto = AdminOrderDto(
            orderId = order.id,
            userId = order.userId,
            orderDate = order.createdAt,
            totalPrice = order.getTotalPrice(),
            orderItems = order.orderItems.map { OrderedItemDto.from(it) },
        )
    }

    // OrderedItemDto는 기존 OrderedDto.OrderedItemDto 재사용
    data class OrderedItemDto(...)
}
```

## 구현 전략

### 1. 신규 파일 생성
- `apps/commerce-api/src/main/kotlin/com/loopers/application/api/order/AdminOrderFacade.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/dto/AdminOrderDto.kt`

### 2. 기존 코드 수정
- OrderedDto의 OrderedItemDto를 AdminOrderDto에서도 재사용 (import)
- 기존 OrderFacade 수정 없음

### 3. 스프링 설정
- `@Service` 애노테이션으로 빈 등록
- `@Transactional(readOnly = true)` 읽기 전용 트랜잭션 적용

## 테스트 전략

### 단위 테스트 (AdminOrderFacadeTest.kt)
- getOrders(): 페이지네이션 검증, 최신순 정렬 검증
- getOrderById(): 정상 조회, 존재하지 않는 주문 예외 처리

### E2E 테스트 (AdminOrderFacadeE2ETest.kt)
- API 엔드포인트를 통한 전체 흐름 검증
- 관리자 권한 검증 (필요시)

## 향후 확장성
현재는 기본 조회만 제공하지만, 향후 다음 기능들을 추가할 수 있습니다:
- 주문 상태 필터링
- 기간 범위 검색
- 주문 상태 변경 (COMPLETED, CANCELLED 등)
- 환불/반품 처리
