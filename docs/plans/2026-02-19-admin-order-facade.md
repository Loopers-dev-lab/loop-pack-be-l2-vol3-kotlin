# AdminOrderFacade 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**목표:** 시스템 관리자가 모든 주문을 조회할 수 있는 AdminOrderFacade를 구현하고 테스트한다.

**아키텍처:** AdminOrderFacade는 OrderService의 기존 메서드를 활용하여 모든 주문(사용자 제약 없이)을 조회한다. OrderService에 관리자용 조회 메서드를 추가하고, AdminOrderDto를 새로 정의하여 고객 아이디를 포함한 응답을 제공한다.

**기술 스택:** Kotlin, Spring Data JPA, Spring Data Pageable, JUnit 5, MockK

---

## Task 1: AdminOrderDto 생성

**파일:**
- 생성: `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/dto/AdminOrderDto.kt`

**Step 1: AdminOrderDto 파일 생성**

AdminOrderDto를 생성합니다. OrderedDto의 OrderedItemDto를 재사용합니다.

```kotlin
package com.loopers.domain.order.dto

import com.loopers.domain.order.Order
import java.math.BigDecimal
import java.time.ZonedDateTime

data class AdminOrderDto(
    val orderId: Long,
    val userId: Long,
    val orderDate: ZonedDateTime,
    val totalPrice: BigDecimal,
    val orderItems: List<OrderedDto.OrderedItemDto>,
) {
    companion object {
        fun from(order: Order): AdminOrderDto = AdminOrderDto(
            orderId = order.id,
            userId = order.userId,
            orderDate = order.createdAt,
            totalPrice = order.getTotalPrice(),
            orderItems = order.orderItems.map { OrderedDto.OrderedItemDto.from(it) },
        )
    }
}
```

**Step 2: 파일 저장 및 IDE 검증**

파일을 저장하고, IDE에서 빨간 줄이 없는지 확인합니다.

**Step 3: Commit**

```bash
git add apps/commerce-api/src/main/kotlin/com/loopers/domain/order/dto/AdminOrderDto.kt
git commit -m "feat: AdminOrderDto 생성

- 고객 아이디를 포함한 관리자용 주문 DTO
- OrderedDto.OrderedItemDto 재사용"
```

---

## Task 2: OrderService에 관리자용 조회 메서드 추가

**파일:**
- 수정: `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderService.kt`

**Step 1: OrderService에 getAllOrders 메서드 추가**

기존 OrderService에 관리자용 메서드를 추가합니다.

파일 내용을 보고 `getOrdersByUserId` 메서드 아래에 다음 메서드를 추가합니다:

```kotlin
fun getAllOrders(pageable: Pageable): Page<Order> {
    return orderRepository.findAll(pageable)
}

fun getOrderByIdForAdmin(orderId: Long): Order =
    orderRepository.findById(orderId)
        ?: throw CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다")
```

**Step 2: 파일 저장 및 빌드**

```bash
./gradlew :apps:commerce-api:compileKotlin
```

성공하는지 확인합니다.

**Step 3: Commit**

```bash
git add apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderService.kt
git commit -m "feat: OrderService에 관리자용 조회 메서드 추가

- getAllOrders(): 모든 주문 페이지네이션 조회
- getOrderByIdForAdmin(): 특정 주문 상세 조회 (사용자 제약 없음)"
```

---

## Task 3: AdminOrderFacade 생성

**파일:**
- 생성: `apps/commerce-api/src/main/kotlin/com/loopers/application/api/order/AdminOrderFacade.kt`

**Step 1: AdminOrderFacade 파일 생성**

```kotlin
package com.loopers.application.api.order

import com.loopers.domain.order.OrderService
import com.loopers.domain.order.dto.AdminOrderDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminOrderFacade(
    private val orderService: OrderService,
) {

    fun getOrders(pageable: Pageable): Page<AdminOrderDto> =
        orderService.getAllOrders(pageable).map { AdminOrderDto.from(it) }

    fun getOrderById(orderId: Long): AdminOrderDto =
        orderService.getOrderByIdForAdmin(orderId).let { AdminOrderDto.from(it) }
}
```

**Step 2: 파일 저장 및 빌드**

```bash
./gradlew :apps:commerce-api:compileKotlin
```

성공하는지 확인합니다.

**Step 3: Commit**

```bash
git add apps/commerce-api/src/main/kotlin/com/loopers/application/api/order/AdminOrderFacade.kt
git commit -m "feat: AdminOrderFacade 구현

- getOrders(): 모든 주문 목록 조회 (페이지네이션)
- getOrderById(): 주문 상세 조회 (고객 아이디 포함)"
```

---

## Task 4: AdminOrderFacade 단위 테스트 작성

**파일:**
- 생성: `apps/commerce-api/src/test/kotlin/com/loopers/application/api/order/AdminOrderFacadeTest.kt`

**Step 1: 실패하는 테스트 작성**

```kotlin
package com.loopers.application.api.order

import com.loopers.domain.order.OrderService
import com.loopers.domain.order.dto.AdminOrderDto
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import kotlin.test.assertEquals

@DisplayName("AdminOrderFacade 테스트")
class AdminOrderFacadeTest {

    private val orderService: OrderService = mockk()
    private val adminOrderFacade = AdminOrderFacade(orderService)

    @Test
    @DisplayName("getOrders는 모든 주문을 페이지네이션으로 반환한다")
    fun testGetOrders() {
        // Arrange
        val pageable = PageRequest.of(0, 10)
        val mockOrder = io.mockk.mockk<com.loopers.domain.order.Order>(relaxed = true)
        every { mockOrder.id } returns 1L
        every { mockOrder.userId } returns 100L
        every { mockOrder.getTotalPrice() } returns java.math.BigDecimal.valueOf(50000)
        every { mockOrder.orderItems } returns emptyList()
        every { mockOrder.createdAt } returns java.time.ZonedDateTime.now()

        val mockPage = PageImpl(listOf(mockOrder), pageable, 1)
        every { orderService.getAllOrders(pageable) } returns mockPage

        // Act
        val result = adminOrderFacade.getOrders(pageable)

        // Assert
        assertEquals(1, result.totalElements)
        assertEquals(1, result.content.size)
        assertEquals(100L, result.content[0].userId)
    }

    @Test
    @DisplayName("getOrderById는 특정 주문을 반환한다")
    fun testGetOrderById() {
        // Arrange
        val orderId = 1L
        val mockOrder = io.mockk.mockk<com.loopers.domain.order.Order>(relaxed = true)
        every { mockOrder.id } returns orderId
        every { mockOrder.userId } returns 100L
        every { mockOrder.getTotalPrice() } returns java.math.BigDecimal.valueOf(50000)
        every { mockOrder.orderItems } returns emptyList()
        every { mockOrder.createdAt } returns java.time.ZonedDateTime.now()

        every { orderService.getOrderByIdForAdmin(orderId) } returns mockOrder

        // Act
        val result = adminOrderFacade.getOrderById(orderId)

        // Assert
        assertEquals(orderId, result.orderId)
        assertEquals(100L, result.userId)
    }
}
```

**Step 2: 테스트 실행 (모두 통과해야 함)**

```bash
./gradlew :apps:commerce-api:test --tests "AdminOrderFacadeTest" -v
```

**Step 3: Commit**

```bash
git add apps/commerce-api/src/test/kotlin/com/loopers/application/api/order/AdminOrderFacadeTest.kt
git commit -m "test: AdminOrderFacade 단위 테스트

- getOrders() 페이지네이션 테스트
- getOrderById() 상세 조회 테스트"
```

---

## Task 5: 전체 테스트 실행 및 최종 검증

**Step 1: 전체 테스트 실행**

```bash
./gradlew :apps:commerce-api:test -v
```

모든 테스트가 통과해야 합니다.

**Step 2: ktlint 검사**

```bash
./gradlew :apps:commerce-api:ktlintCheck
```

코드 스타일 위반이 없어야 합니다.

**Step 3: 빌드 검증**

```bash
./gradlew :apps:commerce-api:build
```

성공적으로 빌드되어야 합니다.

**Step 4: Commit**

```bash
git add .
git commit -m "test: 전체 테스트 및 검증 완료

- 모든 단위 테스트 통과
- ktlint 검사 통과
- 빌드 성공"
```

---

## 체크리스트

- [ ] AdminOrderDto 생성 및 커밋
- [ ] OrderService에 관리자용 메서드 추가 및 커밋
- [ ] AdminOrderFacade 생성 및 커밋
- [ ] AdminOrderFacade 테스트 작성 및 커밋
- [ ] 전체 테스트 실행 및 검증
- [ ] ktlint 검사 통과
- [ ] 빌드 성공

---

## 다음 단계

AdminOrderFacade 구현이 완료되면 다음을 진행할 수 있습니다:

1. **AdminOrderController 생성** - REST API 엔드포인트 제공
2. **AdminOrderV1Dto 생성** - API 응답 DTO
3. **E2E 테스트** - MockMvc를 활용한 API 통합 테스트
4. **권한 검증** - 관리자 권한 확인 로직 (Spring Security)
