package com.loopers.domain.brand

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "brands")
class Brand(
    name: String,
    description: String?,
) : BaseEntity() {

    var name: String = name
        protected set

    var description: String? = description
        protected set

    init {
        validateName(name)
    }

    fun update(name: String, description: String?) {
        validateName(name)
        this.name = name
        this.description = description
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "브랜드 이름은 비어있을 수 없습니다.")
        }
    }
}
