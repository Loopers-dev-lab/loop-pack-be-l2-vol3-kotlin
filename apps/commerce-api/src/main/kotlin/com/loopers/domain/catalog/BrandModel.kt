package com.loopers.domain.catalog

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version

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

    @Version
    var version: Long = 0
        protected set

    init {
        validateName(name)
    }

    fun update(
        newName: String? = this.name,
        newDescription: String? = this.description,
        newLogoUrl: String? = this.logoUrl,
    ) {
        newName?.let {
            validateName(it)
            this.name = it
        }
        this.description = newDescription
        this.logoUrl = newLogoUrl
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
        }
    }
}
