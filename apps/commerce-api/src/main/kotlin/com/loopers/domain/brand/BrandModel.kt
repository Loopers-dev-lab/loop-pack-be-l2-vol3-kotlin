package com.loopers.domain.brand

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "brands")
class BrandModel(
    name: String,
    description: String? = null,
    logoUrl: String? = null,
) : BaseEntity() {
    var name: String = name
        protected set

    var description: String? = description
        protected set

    var logoUrl: String? = logoUrl
        protected set

    init {
        validateName(name)
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
        }
    }
}
