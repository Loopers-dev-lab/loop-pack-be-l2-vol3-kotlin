package com.loopers.domain.brand

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class BrandTest {

    @Test
    fun `create로 생성한 Brand의 persistenceId는 null이어야 한다`() {
        val brand = createBrand()

        assertThat(brand.persistenceId).isNull()
    }

    @Test
    fun `create로 생성한 Brand의 상태는 ACTIVE여야 한다`() {
        val brand = createBrand()

        assertThat(brand.status).isEqualTo(BrandStatus.ACTIVE)
    }

    @Test
    fun `create로 생성한 Brand의 deletedAt은 null이어야 한다`() {
        val brand = createBrand()

        assertThat(brand.deletedAt).isNull()
    }

    @Test
    fun `reconstitute로 생성한 Brand는 persistenceId를 가져야 한다`() {
        val brand = Brand.reconstitute(
            persistenceId = 1L,
            name = BrandName(BRAND_NAME),
            description = DESCRIPTION,
            logoUrl = LOGO_URL,
            status = BrandStatus.ACTIVE,
            deletedAt = null,
        )

        assertThat(brand.persistenceId).isEqualTo(1L)
    }

    @Test
    fun `정상적인 경우 update가 새 Brand를 반환해야 한다`() {
        val brand = createBrand()

        val updated = brand.update(
            name = BrandName(UPDATED_NAME),
            description = UPDATED_DESCRIPTION,
            logoUrl = UPDATED_LOGO_URL,
        )

        assertThat(updated).isNotSameAs(brand)
        assertThat(updated.name.value).isEqualTo(UPDATED_NAME)
        assertThat(updated.description).isEqualTo(UPDATED_DESCRIPTION)
        assertThat(updated.logoUrl).isEqualTo(UPDATED_LOGO_URL)
    }

    @Test
    fun `삭제된 브랜드의 경우 update가 실패해야 한다`() {
        val brand = createBrand().delete()

        assertThatThrownBy {
            brand.update(
                name = BrandName(UPDATED_NAME),
                description = UPDATED_DESCRIPTION,
                logoUrl = UPDATED_LOGO_URL,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `activate 호출시 상태가 ACTIVE로 변경되어야 한다`() {
        val brand = createBrand().deactivate()

        val activated = brand.activate()

        assertThat(activated.status).isEqualTo(BrandStatus.ACTIVE)
    }

    @Test
    fun `deactivate 호출시 상태가 INACTIVE로 변경되어야 한다`() {
        val brand = createBrand()

        val deactivated = brand.deactivate()

        assertThat(deactivated.status).isEqualTo(BrandStatus.INACTIVE)
    }

    @Test
    fun `delete 호출시 deletedAt이 설정되어야 한다`() {
        val brand = createBrand()

        val deleted = brand.delete()

        assertThat(deleted.deletedAt).isNotNull()
    }

    @Test
    fun `delete 호출시 isDeleted가 true를 반환해야 한다`() {
        val brand = createBrand()

        val deleted = brand.delete()

        assertThat(deleted.isDeleted()).isTrue()
    }

    @Test
    fun `deletedAt이 null인 경우 isDeleted가 false를 반환해야 한다`() {
        val brand = createBrand()

        assertThat(brand.isDeleted()).isFalse()
    }

    @Test
    fun `delete 호출시 새 Brand 인스턴스를 반환해야 한다`() {
        val brand = createBrand()

        val deleted = brand.delete()

        assertThat(deleted).isNotSameAs(brand)
    }

    private fun createBrand(): Brand = Brand.create(
        name = BrandName(BRAND_NAME),
        description = DESCRIPTION,
        logoUrl = LOGO_URL,
    )

    companion object {
        private const val BRAND_NAME = "나이키"
        private const val DESCRIPTION = "스포츠 브랜드"
        private const val LOGO_URL = "https://example.com/logo.png"
        private const val UPDATED_NAME = "아디다스"
        private const val UPDATED_DESCRIPTION = "수정된 설명"
        private const val UPDATED_LOGO_URL = "https://example.com/new-logo.png"
    }
}
