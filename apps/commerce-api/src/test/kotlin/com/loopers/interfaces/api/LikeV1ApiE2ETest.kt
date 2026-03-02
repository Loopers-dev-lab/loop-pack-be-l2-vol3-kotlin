package com.loopers.interfaces.api

import com.loopers.interfaces.api.admin.brand.AdminBrandRegisterRequest
import com.loopers.interfaces.api.admin.brand.AdminBrandResponse
import com.loopers.interfaces.api.admin.product.AdminProductRegisterRequest
import com.loopers.interfaces.api.admin.product.AdminProductResponse
import com.loopers.interfaces.api.like.LikeAddRequest
import com.loopers.interfaces.api.like.LikeProductResponse
import com.loopers.interfaces.api.product.ProductDetailResponse
import com.loopers.interfaces.api.user.UserV1Dto
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
import com.loopers.support.constant.AuthHeaders
import com.loopers.support.error.LikeErrorCode
import com.loopers.support.error.ProductErrorCode
import com.loopers.support.error.UserErrorCode
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestContainersConfig::class)
class LikeV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private val testLoginId = "testuser"
    private val testPassword = "Test123!"

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(AuthHeaders.Admin.LDAP, AuthHeaders.Admin.LDAP_VALUE)
        }
    }

    private fun userHeaders(loginId: String = testLoginId, password: String = testPassword): HttpHeaders {
        return HttpHeaders().apply {
            set(AuthHeaders.User.LOGIN_ID, loginId)
            set(AuthHeaders.User.LOGIN_PW, password)
        }
    }

    private fun registerUser(loginId: String = testLoginId, password: String = testPassword) {
        val request = UserV1Dto.RegisterRequest(
            loginId = loginId,
            password = password,
            name = "홍길동",
            birthDate = "1990-01-01",
            email = "$loginId@example.com",
        )
        testRestTemplate.postForEntity(ApiPaths.Users.REGISTER, request, Any::class.java)
    }

    private fun registerBrandViaAdmin(name: String = "나이키"): AdminBrandResponse {
        val request = AdminBrandRegisterRequest(name = name)
        val responseType = object : ParameterizedTypeReference<ApiResponse<AdminBrandResponse>>() {}
        val response = testRestTemplate.exchange(
            ApiPaths.AdminBrands.BASE,
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        return requireNotNull(response.body?.data) { "브랜드 등록 응답이 비어 있습니다." }
    }

    private fun registerProductViaAdmin(
        brandId: Long,
        name: String = "테스트 상품",
    ): AdminProductResponse {
        val request = AdminProductRegisterRequest(
            brandId = brandId,
            name = name,
            description = "상품 설명",
            price = 10000,
            stock = 100,
            imageUrl = "https://example.com/image.jpg",
        )
        val responseType = object : ParameterizedTypeReference<ApiResponse<AdminProductResponse>>() {}
        val response = testRestTemplate.exchange(
            ApiPaths.AdminProducts.BASE,
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        return requireNotNull(response.body?.data) { "상품 등록 응답이 비어 있습니다." }
    }

    private fun deleteProductViaAdmin(productId: Long) {
        testRestTemplate.exchange(
            "${ApiPaths.AdminProducts.BASE}/$productId",
            HttpMethod.DELETE,
            HttpEntity<Void>(adminHeaders()),
            ApiResponse::class.java,
        )
    }

    @DisplayName("POST /api/v1/likes - 좋아요 등록")
    @Nested
    inner class AddLike {

        @DisplayName("활성 상품에 좋아요하면 200 OK를 반환하고 likeCount가 증가한다")
        @Test
        fun success() {
            // arrange
            registerUser()
            val brand = registerBrandViaAdmin()
            val product = registerProductViaAdmin(brand.id)

            // act
            val request = LikeAddRequest(productId = product.id)
            val response = testRestTemplate.exchange(
                ApiPaths.Likes.BASE,
                HttpMethod.POST,
                HttpEntity(request, userHeaders()),
                ApiResponse::class.java,
            )

            // assert - 좋아요 성공
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

            // likeCount 반영 확인
            val productResponseType = object : ParameterizedTypeReference<ApiResponse<ProductDetailResponse>>() {}
            val productResponse = testRestTemplate.exchange(
                "${ApiPaths.Products.BASE}/${product.id}",
                HttpMethod.GET,
                null,
                productResponseType,
            )
            assertThat(productResponse.body?.data?.likeCount).isEqualTo(1)
        }

        @DisplayName("중복 좋아요하면 409 CONFLICT를 반환한다")
        @Test
        fun failWhenAlreadyLiked() {
            // arrange
            registerUser()
            val brand = registerBrandViaAdmin()
            val product = registerProductViaAdmin(brand.id)
            testRestTemplate.exchange(
                ApiPaths.Likes.BASE,
                HttpMethod.POST,
                HttpEntity(LikeAddRequest(productId = product.id), userHeaders()),
                ApiResponse::class.java,
            )

            // act - 중복 좋아요
            val response = testRestTemplate.exchange(
                ApiPaths.Likes.BASE,
                HttpMethod.POST,
                HttpEntity(LikeAddRequest(productId = product.id), userHeaders()),
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(LikeErrorCode.ALREADY_LIKED.code) },
            )
        }

        @DisplayName("삭제된 상품에 좋아요하면 404 NOT_FOUND를 반환한다")
        @Test
        fun failWhenProductDeleted() {
            // arrange
            registerUser()
            val brand = registerBrandViaAdmin()
            val product = registerProductViaAdmin(brand.id)
            deleteProductViaAdmin(product.id)

            // act
            val response = testRestTemplate.exchange(
                ApiPaths.Likes.BASE,
                HttpMethod.POST,
                HttpEntity(LikeAddRequest(productId = product.id), userHeaders()),
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND.code) },
            )
        }

        @DisplayName("인증 헤더 누락 시 401 UNAUTHORIZED를 반환한다")
        @Test
        fun failWhenNotAuthenticated() {
            // act
            val response = testRestTemplate.exchange(
                ApiPaths.Likes.BASE,
                HttpMethod.POST,
                HttpEntity(LikeAddRequest(productId = 1L)),
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(UserErrorCode.AUTHENTICATION_FAILED.code) },
            )
        }
    }

    @DisplayName("DELETE /api/v1/likes/{productId} - 좋아요 취소")
    @Nested
    inner class CancelLike {

        @DisplayName("좋아요를 취소하면 200 OK를 반환한다")
        @Test
        fun success() {
            // arrange
            registerUser()
            val brand = registerBrandViaAdmin()
            val product = registerProductViaAdmin(brand.id)
            testRestTemplate.exchange(
                ApiPaths.Likes.BASE,
                HttpMethod.POST,
                HttpEntity(LikeAddRequest(productId = product.id), userHeaders()),
                ApiResponse::class.java,
            )

            // act
            val response = testRestTemplate.exchange(
                "${ApiPaths.Likes.BASE}/${product.id}",
                HttpMethod.DELETE,
                HttpEntity<Void>(userHeaders()),
                ApiResponse::class.java,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @DisplayName("좋아요하지 않은 상품을 취소하면 404 NOT_FOUND를 반환한다")
        @Test
        fun failWhenLikeNotFound() {
            // arrange
            registerUser()
            val brand = registerBrandViaAdmin()
            val product = registerProductViaAdmin(brand.id)

            // act
            val response = testRestTemplate.exchange(
                "${ApiPaths.Likes.BASE}/${product.id}",
                HttpMethod.DELETE,
                HttpEntity<Void>(userHeaders()),
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(LikeErrorCode.LIKE_NOT_FOUND.code) },
            )
        }
    }

    @DisplayName("GET /api/v1/likes/me - 내 좋아요 목록 조회")
    @Nested
    inner class GetMyLikes {

        @DisplayName("좋아요한 활성 상품 목록을 반환한다 (삭제 상품 필터링 포함)")
        @Test
        fun success() {
            // arrange
            registerUser()
            val brand = registerBrandViaAdmin()
            val activeProduct = registerProductViaAdmin(brand.id, name = "활성 상품")
            val deletedProduct = registerProductViaAdmin(brand.id, name = "삭제 상품")

            // 두 상품 모두 좋아요
            testRestTemplate.exchange(
                ApiPaths.Likes.BASE,
                HttpMethod.POST,
                HttpEntity(LikeAddRequest(productId = activeProduct.id), userHeaders()),
                ApiResponse::class.java,
            )
            testRestTemplate.exchange(
                ApiPaths.Likes.BASE,
                HttpMethod.POST,
                HttpEntity(LikeAddRequest(productId = deletedProduct.id), userHeaders()),
                ApiResponse::class.java,
            )

            // 하나 삭제
            deleteProductViaAdmin(deletedProduct.id)

            // act
            val responseType =
                object : ParameterizedTypeReference<ApiResponse<PageResult<LikeProductResponse>>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.Likes.ME}?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Void>(userHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(1) },
                { assertThat(response.body?.data?.content?.get(0)?.productName).isEqualTo("활성 상품") },
            )
        }

        @DisplayName("인증 헤더 누락 시 401 UNAUTHORIZED를 반환한다")
        @Test
        fun failWhenNotAuthenticated() {
            // act
            val response = testRestTemplate.getForEntity(
                "${ApiPaths.Likes.ME}?page=0&size=20",
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(UserErrorCode.AUTHENTICATION_FAILED.code) },
            )
        }
    }
}
