package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@DisplayName("BrandService")
class BrandServiceTest {

    private val brandRepository: BrandRepository = mockk()
    private val brandService = BrandService(brandRepository)

    companion object {
        private const val VALID_NAME = "루프팩"
        private const val VALID_DESCRIPTION = "감성 이커머스 브랜드"
        private const val VALID_LOGO_URL = "https://example.com/logo.png"
    }

    @DisplayName("create")
    @Nested
    inner class Create {
        @DisplayName("정상적인 브랜드를 생성하면 저장 후 반환한다")
        @Test
        fun savesBrand_whenValidDataProvided() {
            // arrange
            every { brandRepository.existsByNameAndDeletedAtIsNull(VALID_NAME) } returns false
            every { brandRepository.save(any()) } answers { firstArg() }

            // act
            val result = brandService.create(
                name = VALID_NAME,
                description = VALID_DESCRIPTION,
                logoUrl = VALID_LOGO_URL,
            )

            // assert
            assertThat(result.name).isEqualTo(VALID_NAME)
            assertThat(result.description).isEqualTo(VALID_DESCRIPTION)
            assertThat(result.logoUrl).isEqualTo(VALID_LOGO_URL)
            assertThat(result.status).isEqualTo(BrandStatus.ACTIVE)

            verify(exactly = 1) { brandRepository.existsByNameAndDeletedAtIsNull(VALID_NAME) }
            verify(exactly = 1) { brandRepository.save(any()) }
        }

        @DisplayName("중복된 이름으로 생성하면 CONFLICT 예외가 발생한다")
        @Test
        fun throwsConflictException_whenNameAlreadyExists() {
            // arrange
            every { brandRepository.existsByNameAndDeletedAtIsNull(VALID_NAME) } returns true

            // act & assert
            assertThatThrownBy {
                brandService.create(
                    name = VALID_NAME,
                    description = VALID_DESCRIPTION,
                    logoUrl = VALID_LOGO_URL,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CONFLICT)

            verify(exactly = 1) { brandRepository.existsByNameAndDeletedAtIsNull(VALID_NAME) }
            verify(exactly = 0) { brandRepository.save(any()) }
        }
    }

    @DisplayName("findById")
    @Nested
    inner class FindById {
        @DisplayName("존재하는 브랜드 ID면 브랜드가 반환된다")
        @Test
        fun returnsBrand_whenBrandExists() {
            // arrange
            val brand = BrandModel(
                name = VALID_NAME,
                description = VALID_DESCRIPTION,
                logoUrl = VALID_LOGO_URL,
            )
            every { brandRepository.findByIdAndDeletedAtIsNull(1L) } returns brand

            // act
            val result = brandService.findById(1L)

            // assert
            assertThat(result.name).isEqualTo(VALID_NAME)
            assertThat(result.description).isEqualTo(VALID_DESCRIPTION)
            verify(exactly = 1) { brandRepository.findByIdAndDeletedAtIsNull(1L) }
        }

        @DisplayName("존재하지 않는 브랜드 ID면 NOT_FOUND 예외가 발생한다")
        @Test
        fun throwsNotFoundException_whenBrandDoesNotExist() {
            // arrange
            every { brandRepository.findByIdAndDeletedAtIsNull(999L) } returns null

            // act & assert
            assertThatThrownBy { brandService.findById(999L) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)

            verify(exactly = 1) { brandRepository.findByIdAndDeletedAtIsNull(999L) }
        }
    }

    @DisplayName("findAll")
    @Nested
    inner class FindAll {
        @DisplayName("브랜드 목록을 페이징으로 조회한다")
        @Test
        fun returnsBrandPage_whenCalled() {
            // arrange
            val brands = listOf(
                BrandModel(name = "브랜드A"),
                BrandModel(name = "브랜드B"),
            )
            val pageable = PageRequest.of(0, 10)
            every { brandRepository.findAllByDeletedAtIsNull(pageable) } returns PageImpl(brands, pageable, 2)

            // act
            val result = brandService.findAll(pageable)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].name).isEqualTo("브랜드A")
            assertThat(result.content[1].name).isEqualTo("브랜드B")
            verify(exactly = 1) { brandRepository.findAllByDeletedAtIsNull(pageable) }
        }
    }

    @DisplayName("findAllByIds")
    @Nested
    inner class FindAllByIds {
        @DisplayName("여러 브랜드 ID로 일괄 조회한다")
        @Test
        fun returnsBrands_whenIdsProvided() {
            // arrange
            val brands = listOf(
                BrandModel(name = "브랜드A"),
                BrandModel(name = "브랜드B"),
            )
            every {
                brandRepository.findAllByIdInAndDeletedAtIsNull(listOf(1L, 2L))
            } returns brands

            // act
            val result = brandService.findAllByIds(listOf(1L, 2L))

            // assert
            assertThat(result).hasSize(2)
            assertThat(result[0].name).isEqualTo("브랜드A")
            assertThat(result[1].name).isEqualTo("브랜드B")
            verify(exactly = 1) {
                brandRepository.findAllByIdInAndDeletedAtIsNull(listOf(1L, 2L))
            }
        }
    }

    @DisplayName("update")
    @Nested
    inner class Update {
        @DisplayName("존재하는 브랜드를 수정하면 변경된 브랜드가 반환된다")
        @Test
        fun updatesBrand_whenBrandExists() {
            // arrange
            val brand = BrandModel(
                name = VALID_NAME,
                description = VALID_DESCRIPTION,
                logoUrl = VALID_LOGO_URL,
            )
            val newName = "새로운브랜드"
            val newDescription = "새로운 설명"
            val newLogoUrl = "https://example.com/new-logo.png"

            every { brandRepository.findByIdAndDeletedAtIsNull(1L) } returns brand
            every { brandRepository.existsByNameAndDeletedAtIsNull(newName) } returns false
            every { brandRepository.save(any()) } answers { firstArg() }

            // act
            val result = brandService.update(
                id = 1L,
                name = newName,
                description = newDescription,
                logoUrl = newLogoUrl,
                status = BrandStatus.INACTIVE,
            )

            // assert
            assertThat(result.name).isEqualTo(newName)
            assertThat(result.description).isEqualTo(newDescription)
            assertThat(result.logoUrl).isEqualTo(newLogoUrl)
            assertThat(result.status).isEqualTo(BrandStatus.INACTIVE)

            verify(exactly = 1) { brandRepository.findByIdAndDeletedAtIsNull(1L) }
            verify(exactly = 1) { brandRepository.save(any()) }
        }

        @DisplayName("동일한 이름으로 수정하면 중복 검사를 건너뛴다")
        @Test
        fun skipsNameDuplicateCheck_whenNameIsUnchanged() {
            // arrange
            val brand = BrandModel(
                name = VALID_NAME,
                description = VALID_DESCRIPTION,
                logoUrl = VALID_LOGO_URL,
            )

            every { brandRepository.findByIdAndDeletedAtIsNull(1L) } returns brand
            every { brandRepository.save(any()) } answers { firstArg() }

            // act
            val result = brandService.update(
                id = 1L,
                name = VALID_NAME,
                description = "수정된 설명",
                logoUrl = null,
                status = BrandStatus.ACTIVE,
            )

            // assert
            assertThat(result.name).isEqualTo(VALID_NAME)
            assertThat(result.description).isEqualTo("수정된 설명")

            verify(exactly = 0) { brandRepository.existsByNameAndDeletedAtIsNull(any()) }
            verify(exactly = 1) { brandRepository.save(any()) }
        }

        @DisplayName("다른 브랜드와 중복된 이름으로 수정하면 CONFLICT 예외가 발생한다")
        @Test
        fun throwsConflictException_whenNewNameAlreadyExists() {
            // arrange
            val brand = BrandModel(
                name = VALID_NAME,
                description = VALID_DESCRIPTION,
                logoUrl = VALID_LOGO_URL,
            )
            val duplicateName = "이미있는브랜드"

            every { brandRepository.findByIdAndDeletedAtIsNull(1L) } returns brand
            every { brandRepository.existsByNameAndDeletedAtIsNull(duplicateName) } returns true

            // act & assert
            assertThatThrownBy {
                brandService.update(
                    id = 1L,
                    name = duplicateName,
                    description = null,
                    logoUrl = null,
                    status = BrandStatus.ACTIVE,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.CONFLICT)

            verify(exactly = 0) { brandRepository.save(any()) }
        }
    }

    @DisplayName("delete")
    @Nested
    inner class Delete {
        @DisplayName("존재하는 브랜드를 삭제하면 소프트 삭제된다")
        @Test
        fun softDeletesBrand_whenBrandExists() {
            // arrange
            val brand = BrandModel(name = VALID_NAME)
            every { brandRepository.findByIdAndDeletedAtIsNull(1L) } returns brand
            every { brandRepository.save(any()) } answers { firstArg() }

            // act
            brandService.delete(1L)

            // assert
            assertThat(brand.deletedAt).isNotNull()
            verify(exactly = 1) { brandRepository.findByIdAndDeletedAtIsNull(1L) }
            verify(exactly = 1) { brandRepository.save(any()) }
        }

        @DisplayName("존재하지 않는 브랜드를 삭제하면 NOT_FOUND 예외가 발생한다")
        @Test
        fun throwsNotFoundException_whenBrandDoesNotExist() {
            // arrange
            every { brandRepository.findByIdAndDeletedAtIsNull(999L) } returns null

            // act & assert
            assertThatThrownBy { brandService.delete(999L) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)

            verify(exactly = 1) { brandRepository.findByIdAndDeletedAtIsNull(999L) }
            verify(exactly = 0) { brandRepository.save(any()) }
        }
    }
}
