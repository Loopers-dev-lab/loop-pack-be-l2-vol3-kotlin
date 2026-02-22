package com.loopers.application.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BrandServiceTest @Autowired constructor(
    private val brandService: BrandService,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(
        name: String = "Nike",
        email: String = "nike@google.com",
        phoneNumber: String = "02-3783-4401",
        businessNumber: String = "123-45-67890",
    ) = brandService.createBrand(
        name = name,
        logoImageUrl = "test.png",
        description = "테스트 브랜드",
        zipCode = "12345",
        roadAddress = "서울특별시 중구 테스트길 1",
        detailAddress = "1층",
        email = email,
        phoneNumber = phoneNumber,
        businessNumber = businessNumber,
    )

    @Nested
    inner class CreateBrand {

        @Test
        fun `브랜드 생성`() {
            val name = "Nike"
            val logoImageUrl = "test.png"
            val description = "Nike는 신발가게"
            val zipCode = "12345"
            val roadAddress = "서울특별시 중구 명동길 14"
            val detailAddress = "1층"
            val email = "nike@google.com"
            val phoneNumber = "02-3783-4401"
            val businessNumber = "123-45-67890"

            val brand = brandService.createBrand(name, logoImageUrl, description, zipCode, roadAddress, detailAddress, email, phoneNumber, businessNumber)

            assertAll(
                { assertThat(brand.id).isGreaterThan(0) },
                { assertThat(brand.logoImageUrl.value).isEqualTo(logoImageUrl) },
                { assertThat(brand.description.value).isEqualTo(description) },
                { assertThat(brand.zipCode).isEqualTo(zipCode) },
                { assertThat(brand.roadAddress).isEqualTo(roadAddress) },
                { assertThat(brand.detailAddress).isEqualTo(detailAddress) },
                { assertThat(brand.email.value).isEqualTo(email) },
                { assertThat(brand.phoneNumber.value).isEqualTo(phoneNumber) },
                { assertThat(brand.businessNumber.value).isEqualTo(businessNumber) },
            )
        }

        @Test
        fun `브랜드명이 이미 존재하면 409 에러를 반환한다`() {
            createBrand(name = "Nike", businessNumber = "123-45-00001")

            val exception = assertThrows<CoreException> {
                createBrand(name = "Nike", businessNumber = "123-45-00002")
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @Test
        fun `사업자등록번호가 이미 존재하면 409 에러를 반환한다`() {
            createBrand(name = "Nike", businessNumber = "123-45-00001")

            val exception = assertThrows<CoreException> {
                createBrand(name = "Adidas", businessNumber = "123-45-00001")
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @Nested
    inner class ReadBrand {

        @Test
        fun `브랜드 목록 조회`() {
            createBrand(name = "Nike", businessNumber = "123-45-00001")
            createBrand(name = "Addidas", businessNumber = "123-45-00002")
            createBrand(name = "NewBalance", businessNumber = "123-45-00003")

            val brandList = brandService.getBrands()

            assertThat(brandList.content).hasSize(3)
        }

        @Test
        fun `특정 브랜드 상세 조회`() {
            val brand = createBrand(name = "Nike", businessNumber = "123-45-00001")

            val result = brandService.getBrandById(brand.id)

            assertThat(result).isNotNull
        }

        @Test
        fun `특정 브랜드가 존재하지 않으면 404 에러 반환`() {
            val exception = assertThrows<CoreException> {
                brandService.getBrandById(1)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class UpdateBrand {

        @Test
        fun `브랜드 수정`() {
            val brand = createBrand(name = "Nike", businessNumber = "123-45-00001")

            val description = "Nike는 스포츠 용품 가게"

            val result = brandService.updateBrand(
                brand.id,
                brand.name.value,
                brand.logoImageUrl.value,
                description,
                brand.zipCode,
                brand.roadAddress,
                brand.detailAddress,
                brand.email.value,
                brand.phoneNumber.value,
                brand.businessNumber.value,
            )

            assertThat(result.id).isEqualTo(brand.id)
            assertThat(result.description.value).isEqualTo(description)
        }
    }

    @Nested
    inner class DeleteBrand {

        @Test
        fun `브랜드 삭제 (soft delete)`() {
            val brand = createBrand(name = "Nike", businessNumber = "123-45-00001")

            brandService.deleteBrand(brand.id)

            val deletedBrand = brandService.getBrandById(brand.id)
            assertThat(deletedBrand.deletedAt).isNotNull()
        }
    }
}
