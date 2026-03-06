package com.loopers.domain.brand

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "brand")
class BrandModel(
    name: String,
    description: String? = null,
    logoUrl: String? = null,
    status: BrandStatus = BrandStatus.ACTIVE,
) : BaseEntity() {

    @Column(nullable = false, unique = true, length = 100)
    var name: String = name
        protected set

    @Column(length = 500)
    var description: String? = description
        protected set

    @Column(name = "logo_url", length = 500)
    var logoUrl: String? = logoUrl
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: BrandStatus = status
        protected set

    init {
        validateName(name)
    }

    fun update(name: String, description: String?, logoUrl: String?, status: BrandStatus) {
        validateName(name)
        this.name = name
        this.description = description
        this.logoUrl = logoUrl
        this.status = status
    }

    fun isActive(): Boolean = status == BrandStatus.ACTIVE

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "브랜드명은 필수입니다.")
        }
        if (name.length > 100) {
            throw CoreException(ErrorType.BAD_REQUEST, "브랜드명은 100자 이하여야 합니다.")
        }
    }
}
