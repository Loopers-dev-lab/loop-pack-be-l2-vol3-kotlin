package com.loopers.domain.brand

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "brands")
class Brand private constructor(
    name: String,
    description: String,
) : BaseEntity() {

    @Column(nullable = false, unique = true, length = 100)
    var name: String = name
        protected set

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String = description
        protected set

    fun isDeleted(): Boolean = deletedAt != null

    fun changeBrandName(newName: String) {
        this.name = newName
    }

    fun updateInfo(newName: String, newDescription: String) {
        this.name = newName
        this.description = newDescription
        guard()
    }

    override fun guard() {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "브랜드 명은 빈 값일 수 없습니다.")
        }
        if (description.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "설명은 빈 값일 수 없습니다.")
        }
    }

    companion object {
        fun create(name: String, description: String): Brand {
            val brand = Brand(
                name = name,
                description = description,
            )
            brand.guard()
            return brand
        }
    }
}
