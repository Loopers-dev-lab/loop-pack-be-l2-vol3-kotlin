package com.loopers.domain.catalog.brand.entity

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
) : BaseEntity() {

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    init {
        guard()
    }

    override fun guard() {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "브랜드명은 필수입니다.")
        }
    }

    fun update(name: String) {
        this.name = name
        guard()
    }

    fun isDeleted(): Boolean = deletedAt != null
}
