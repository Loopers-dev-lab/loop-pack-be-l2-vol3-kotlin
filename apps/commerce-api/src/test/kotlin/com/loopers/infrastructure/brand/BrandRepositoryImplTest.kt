package com.loopers.infrastructure.brand

import com.loopers.domain.brand.BrandModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class BrandRepositoryImplTest {

    private val brandJpaRepository: BrandJpaRepository = mockk()
    private val brandRepositoryImpl = BrandRepositoryImpl(brandJpaRepository)

    @DisplayName("브랜드를 저장할 때,")
    @Nested
    inner class Save {
        @DisplayName("JpaRepository에 위임하여 저장하고 결과를 반환한다.")
        @Test
        fun delegatesToJpaRepository() {
            // arrange
            val brand = BrandModel(name = "나이키", description = "스포츠 브랜드")
            every { brandJpaRepository.save(brand) } returns brand

            // act
            val result = brandRepositoryImpl.save(brand)

            // assert
            assertThat(result).isEqualTo(brand)
            verify(exactly = 1) { brandJpaRepository.save(brand) }
        }
    }

    @DisplayName("브랜드를 ID로 조회할 때,")
    @Nested
    inner class FindById {
        @DisplayName("삭제되지 않은 브랜드가 존재하면 반환한다.")
        @Test
        fun returnsBrand_whenExists() {
            // arrange
            val brand = BrandModel(name = "아디다스")
            every { brandJpaRepository.findByIdAndDeletedAtIsNull(1L) } returns brand

            // act
            val result = brandRepositoryImpl.findByIdAndDeletedAtIsNull(1L)

            // assert
            assertThat(result).isNotNull
            assertThat(result!!.name).isEqualTo("아디다스")
            verify(exactly = 1) { brandJpaRepository.findByIdAndDeletedAtIsNull(1L) }
        }

        @DisplayName("존재하지 않으면 null을 반환한다.")
        @Test
        fun returnsNull_whenNotExists() {
            // arrange
            every { brandJpaRepository.findByIdAndDeletedAtIsNull(999L) } returns null

            // act
            val result = brandRepositoryImpl.findByIdAndDeletedAtIsNull(999L)

            // assert
            assertThat(result).isNull()
        }
    }

    @DisplayName("브랜드 목록을 조회할 때,")
    @Nested
    inner class FindAll {
        @DisplayName("JpaRepository에 페이징 파라미터를 위임하여 결과를 반환한다.")
        @Test
        fun delegatesPageableToJpaRepository() {
            // arrange
            val pageable = PageRequest.of(0, 10)
            val brands = listOf(
                BrandModel(name = "브랜드1"),
                BrandModel(name = "브랜드2"),
            )
            val page = PageImpl(brands, pageable, 2)
            every { brandJpaRepository.findAllByDeletedAtIsNull(pageable) } returns page

            // act
            val result = brandRepositoryImpl.findAllByDeletedAtIsNull(pageable)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.content.map { it.name }).containsExactly("브랜드1", "브랜드2")
            verify(exactly = 1) { brandJpaRepository.findAllByDeletedAtIsNull(pageable) }
        }
    }

    @DisplayName("브랜드 이름 중복 검사를 할 때,")
    @Nested
    inner class ExistsByName {
        @DisplayName("이름이 존재하면 true를 반환한다.")
        @Test
        fun returnsTrue_whenNameExists() {
            // arrange
            every { brandJpaRepository.existsByNameAndDeletedAtIsNull("나이키") } returns true

            // act
            val result = brandRepositoryImpl.existsByNameAndDeletedAtIsNull("나이키")

            // assert
            assertThat(result).isTrue()
            verify(exactly = 1) { brandJpaRepository.existsByNameAndDeletedAtIsNull("나이키") }
        }

        @DisplayName("이름이 존재하지 않으면 false를 반환한다.")
        @Test
        fun returnsFalse_whenNameDoesNotExist() {
            // arrange
            every { brandJpaRepository.existsByNameAndDeletedAtIsNull("존재하지않는") } returns false

            // act
            val result = brandRepositoryImpl.existsByNameAndDeletedAtIsNull("존재하지않는")

            // assert
            assertThat(result).isFalse()
        }
    }

    @DisplayName("여러 ID로 브랜드를 일괄 조회할 때,")
    @Nested
    inner class FindAllByIds {
        @DisplayName("JpaRepository에 ID 목록을 위임하여 결과를 반환한다.")
        @Test
        fun delegatesIdsToJpaRepository() {
            // arrange
            val ids = listOf(1L, 2L, 3L)
            val brands = listOf(
                BrandModel(name = "브랜드A"),
                BrandModel(name = "브랜드B"),
            )
            every { brandJpaRepository.findAllByIdInAndDeletedAtIsNull(ids) } returns brands

            // act
            val result = brandRepositoryImpl.findAllByIdInAndDeletedAtIsNull(ids)

            // assert
            assertThat(result).hasSize(2)
            verify(exactly = 1) { brandJpaRepository.findAllByIdInAndDeletedAtIsNull(ids) }
        }
    }
}
