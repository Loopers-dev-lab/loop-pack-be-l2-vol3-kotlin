package com.loopers.application.admin.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock

@DisplayName("AdminBrandRegisterUseCase")
class AdminBrandRegisterUseCaseTest {
    private val brandRepository: BrandRepository = mock()
    private val useCase = AdminBrandRegisterUseCase(brandRepository)

    private fun command(name: String = "나이키", admin: String = "loopers.admin"): AdminBrandCommand.Register =
        AdminBrandCommand.Register(name = name, admin = admin)

    @Nested
    @DisplayName("브랜드 등록 시")
    inner class WhenRegister {
        @Test
        @DisplayName("Brand.register()로 생성 후 repository.save()를 호출하고 AdminBrandResult.Register를 반환한다")
        fun register_success() {
            // arrange
            given(brandRepository.save(any(), any())).willAnswer {
                val brand = it.arguments[0] as Brand
                Brand.retrieve(id = 1L, name = brand.name.value, status = brand.status)
            }

            // act
            val result = useCase.register(command())

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(1L) },
                { assertThat(result.name).isEqualTo("나이키") },
                { assertThat(result.status).isEqualTo("INACTIVE") },
            )
        }

        @Test
        @DisplayName("repository.save()가 호출된다")
        fun register_callsSave() {
            // arrange
            given(brandRepository.save(any(), any())).willAnswer {
                val brand = it.arguments[0] as Brand
                Brand.retrieve(id = 1L, name = brand.name.value, status = brand.status)
            }

            // act
            useCase.register(command())

            // assert
            then(brandRepository).should().save(
                check { brand ->
                    assertThat(brand.name.value).isEqualTo("나이키")
                },
                any(),
            )
        }
    }
}
