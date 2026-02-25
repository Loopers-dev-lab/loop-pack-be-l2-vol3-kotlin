package com.loopers.domain.brand

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "brands")
class Brand(
    name: String,
    description: String?,
) : BaseEntity() {

    @Column(name = "name", nullable = false, length = 100)
    var name: String = name
        private set

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = description
        private set

    init {
        validateName(name)
    }

    fun update(name: String, description: String?) {
        validateName(name)
        this.name = name
        this.description = description
    }

    fun isDeleted(): Boolean = deletedAt != null

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "브랜드명은 빈 값일 수 없습니다.")
        }
        if (name.length > MAX_NAME_LENGTH) {
            throw CoreException(ErrorType.BAD_REQUEST, "브랜드명은 ${MAX_NAME_LENGTH}자 이하여야 합니다.")
        }
    }

    companion object {
        private const val MAX_NAME_LENGTH = 100
    }
}
