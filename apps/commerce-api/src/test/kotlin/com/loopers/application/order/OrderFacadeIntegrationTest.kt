package com.loopers.application.order

import com.loopers.application.product.ProductFacade
import com.loopers.domain.brand.BrandService
import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Email
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.Name
import com.loopers.domain.user.PasswordEncryptor
import com.loopers.domain.user.UserModel
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import java.time.LocalDate

@SpringBootTest
class OrderFacadeIntegrationTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val brandService: BrandService,
    private val productFacade: ProductFacade,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private val testPassword = "Password123!"

    private fun createUser(loginId: String = "testuser") = userJpaRepository.save(
        UserModel(
            loginId = LoginId(loginId),
            encryptedPassword = passwordEncryptor.encrypt(testPassword),
            name = Name("홍길동"),
            birthDate = BirthDate("1990-01-01"),
            email = Email("$loginId@example.com"),
        )
    )

    private fun createBrand() = brandService.createBrand(
        name = "Nike",
        logoImageUrl = "test.png",
        description = "테스트 브랜드",
        zipCode = "12345",
        roadAddress = "서울특별시 중구 테스트길 1",
        detailAddress = "1층",
        email = "nike@google.com",
        phoneNumber = "02-3783-4401",
        businessNumber = "123-45-67890",
    )

    private fun createProduct(brandId: Long, quantity: Long = 10L) = productFacade.createProduct(
        brandId = brandId,
        name = "Air Max",
        imageUrl = "image.png",
        description = "설명",
        price = 50_000L,
        quantity = quantity,
    )

    @Nested
    inner class CreateOrder {

        @Test
        fun `사용자 상품 재고 생성 후 주문 시 OrderInfo와 items를 반환한다`() {
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brand.id, quantity = 10L)

            val result = orderFacade.createOrder(
                loginId = "testuser",
                items = listOf(OrderFacade.OrderItemRequest(productId = product.id, quantity = 2L)),
            )

            assertThat(result.userId).isEqualTo(user.id)
            assertThat(result.totalAmount).isEqualTo(100_000L)
            assertThat(result.items).hasSize(1)
            assertThat(result.items[0].productId).isEqualTo(product.id)
            assertThat(result.items[0].quantity).isEqualTo(2L)
            assertThat(result.items[0].subTotal).isEqualTo(100_000L)
        }

        @Test
        fun `재고 부족 시 BAD_REQUEST 예외가 발생한다`() {
            createUser()
            val brand = createBrand()
            createProduct(brand.id, quantity = 1L)
            val productInfo = productFacade.getProducts(brand.id, PageRequest.of(0, 1)).content.first()

            val exception = assertThrows<CoreException> {
                orderFacade.createOrder(
                    loginId = "testuser",
                    items = listOf(OrderFacade.OrderItemRequest(productId = productInfo.id, quantity = 5L)),
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class GetOrders {

        @Test
        fun `날짜 범위 주문 목록 조회 시 해당 사용자의 주문만 반환한다`() {
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brand.id, quantity = 100L)

            orderFacade.createOrder(
                loginId = "testuser",
                items = listOf(OrderFacade.OrderItemRequest(productId = product.id, quantity = 1L)),
            )
            orderFacade.createOrder(
                loginId = "testuser",
                items = listOf(OrderFacade.OrderItemRequest(productId = product.id, quantity = 1L)),
            )

            val result = orderFacade.getOrders(
                loginId = "testuser",
                startAt = LocalDate.now().minusDays(1),
                endAt = LocalDate.now().plusDays(1),
                pageable = PageRequest.of(0, 10),
            )

            assertThat(result.content).hasSize(2)
            assertThat(result.content.map { it.userId }).containsOnly(user.id)
        }
    }

    @Nested
    inner class GetOrderById {

        @Test
        fun `본인 주문 조회 시 OrderInfo를 반환한다`() {
            createUser()
            val brand = createBrand()
            val product = createProduct(brand.id)

            val created = orderFacade.createOrder(
                loginId = "testuser",
                items = listOf(OrderFacade.OrderItemRequest(productId = product.id, quantity = 1L)),
            )

            val result = orderFacade.getOrderById("testuser", created.id)

            assertThat(result.id).isEqualTo(created.id)
            assertThat(result.items).hasSize(1)
        }

        @Test
        fun `타인 주문 조회 시 FORBIDDEN 예외가 발생한다`() {
            createUser("testuser")
            createUser("otheruser")
            val brand = createBrand()
            val product = createProduct(brand.id)

            val otherOrder = orderFacade.createOrder(
                loginId = "otheruser",
                items = listOf(OrderFacade.OrderItemRequest(productId = product.id, quantity = 1L)),
            )

            val exception = assertThrows<CoreException> {
                orderFacade.getOrderById("testuser", otherOrder.id)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }
    }

    @Nested
    inner class GetAllOrders {

        @Test
        fun `관리자 전체 주문 목록 조회 시 페이징으로 반환한다`() {
            createUser("user1")
            createUser("user2")
            val brand = createBrand()
            val product = createProduct(brand.id, quantity = 100L)

            orderFacade.createOrder(
                loginId = "user1",
                items = listOf(OrderFacade.OrderItemRequest(productId = product.id, quantity = 1L)),
            )
            orderFacade.createOrder(
                loginId = "user2",
                items = listOf(OrderFacade.OrderItemRequest(productId = product.id, quantity = 1L)),
            )

            val result = orderFacade.getAllOrders(PageRequest.of(0, 10))

            assertThat(result.content).hasSize(2)
            assertThat(result.totalElements).isEqualTo(2L)
        }
    }
}
