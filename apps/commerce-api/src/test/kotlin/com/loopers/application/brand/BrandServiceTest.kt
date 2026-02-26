package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.brand.CreateBrandCommand
import com.loopers.domain.brand.UpdateBrandCommand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class BrandServiceTest {

    @Mock
    private lateinit var brandRepository: BrandRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    private lateinit var brandService: BrandService

    @BeforeEach
    fun setUp() {
        brandService = BrandService(brandRepository, productRepository)
    }

    @DisplayName("브랜드를 조회할 때,")
    @Nested
    inner class GetBrand {

        @DisplayName("존재하는 브랜드 ID로 조회하면, 브랜드 정보가 반환된다.")
        @Test
        fun returnsBrand_whenBrandExists() {
            // arrange
            val brandId = 1L
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")

            whenever(brandRepository.findById(brandId)).thenReturn(brand)

            // act
            val result = brandService.getBrand(brandId)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("나이키") },
                { assertThat(result.description).isEqualTo("스포츠 브랜드") },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsException_whenBrandNotFound() {
            // arrange
            val brandId = 999L

            whenever(brandRepository.findById(brandId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                brandService.getBrand(brandId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("브랜드 목록을 조회할 때,")
    @Nested
    inner class GetAllBrands {

        @DisplayName("브랜드가 존재하면, 페이징된 목록이 반환된다.")
        @Test
        fun returnsBrandList_whenBrandsExist() {
            // arrange
            val pageable = PageRequest.of(0, 20)
            val brands = listOf(
                Brand(name = "나이키", description = "스포츠 브랜드"),
                Brand(name = "아디다스", description = "독일 스포츠 브랜드"),
            )
            val brandPage = PageImpl(brands, pageable, brands.size.toLong())

            whenever(brandRepository.findAll(pageable)).thenReturn(brandPage)

            // act
            val result = brandService.getAllBrands(pageable)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.content[0].name).isEqualTo("나이키") },
                { assertThat(result.content[1].name).isEqualTo("아디다스") },
            )
        }
    }

    @DisplayName("브랜드를 등록할 때,")
    @Nested
    inner class CreateBrand {

        @DisplayName("정상적인 정보가 주어지면, 브랜드가 생성된다.")
        @Test
        fun createsBrand_whenValidInfoProvided() {
            // arrange
            val command = CreateBrandCommand(name = "나이키", description = "스포츠 브랜드")

            whenever(brandRepository.existsByName(command.name)).thenReturn(false)
            whenever(brandRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            val result = brandService.createBrand(command)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("나이키") },
                { assertThat(result.description).isEqualTo("스포츠 브랜드") },
            )
        }

        @DisplayName("이미 존재하는 브랜드명으로 등록하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsException_whenBrandNameAlreadyExists() {
            // arrange
            val command = CreateBrandCommand(name = "나이키", description = "스포츠 브랜드")

            whenever(brandRepository.existsByName(command.name)).thenReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                brandService.createBrand(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @DisplayName("브랜드를 수정할 때,")
    @Nested
    inner class UpdateBrand {

        @DisplayName("정상적인 정보가 주어지면, 브랜드가 수정된다.")
        @Test
        fun updatesBrand_whenValidInfoProvided() {
            // arrange
            val brandId = 1L
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")
            val command = UpdateBrandCommand(name = "아디다스", description = "독일 스포츠 브랜드")

            whenever(brandRepository.findById(brandId)).thenReturn(brand)
            whenever(brandRepository.existsByNameAndIdNot(command.name, brandId)).thenReturn(false)
            whenever(brandRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            val result = brandService.updateBrand(brandId, command)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("아디다스") },
                { assertThat(result.description).isEqualTo("독일 스포츠 브랜드") },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID로 수정하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsException_whenBrandNotFound() {
            // arrange
            val brandId = 999L
            val command = UpdateBrandCommand(name = "아디다스", description = "독일 스포츠 브랜드")

            whenever(brandRepository.findById(brandId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                brandService.updateBrand(brandId, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("다른 브랜드와 같은 이름으로 수정하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsException_whenNameAlreadyExists() {
            // arrange
            val brandId = 1L
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")
            val command = UpdateBrandCommand(name = "아디다스", description = "설명")

            whenever(brandRepository.findById(brandId)).thenReturn(brand)
            whenever(brandRepository.existsByNameAndIdNot(command.name, brandId)).thenReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                brandService.updateBrand(brandId, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("자기 자신의 이름으로 수정하면, 정상 수정된다.")
        @Test
        fun updatesBrand_whenSameNameAsSelf() {
            // arrange
            val brandId = 1L
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")
            val command = UpdateBrandCommand(name = "나이키", description = "설명 변경")

            whenever(brandRepository.findById(brandId)).thenReturn(brand)
            whenever(brandRepository.existsByNameAndIdNot(command.name, brandId)).thenReturn(false)
            whenever(brandRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            val result = brandService.updateBrand(brandId, command)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("나이키") },
                { assertThat(result.description).isEqualTo("설명 변경") },
            )
        }
    }

    @DisplayName("브랜드를 삭제할 때,")
    @Nested
    inner class DeleteBrand {

        @DisplayName("존재하는 브랜드를 삭제하면, 브랜드와 상품이 soft delete 된다.")
        @Test
        fun deletesBrand_whenBrandExists() {
            // arrange
            val brandId = 1L
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")
            val product = Product(brandId = brandId, name = "에어맥스 90", price = BigDecimal("129000"), stock = 100, description = null, imageUrl = null)

            whenever(brandRepository.findById(brandId)).thenReturn(brand)
            whenever(productRepository.findAllByBrandId(brandId)).thenReturn(listOf(product))
            whenever(brandRepository.save(any())).thenAnswer { it.arguments[0] }
            whenever(productRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            brandService.deleteBrand(brandId)

            // assert
            assertAll(
                { assertThat(brand.isDeleted()).isTrue() },
                { assertThat(product.isDeleted()).isTrue() },
            )
            verify(brandRepository).save(brand)
            verify(productRepository).save(product)
        }

        @DisplayName("존재하지 않는 브랜드를 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsException_whenBrandNotFound() {
            // arrange
            val brandId = 999L

            whenever(brandRepository.findById(brandId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                brandService.deleteBrand(brandId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
