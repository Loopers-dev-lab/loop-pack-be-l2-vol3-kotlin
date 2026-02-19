package com.loopers.domain.catalog.brand

import com.loopers.domain.BaseEntity
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
        BrandName(name)
    }

    fun update(name: String) {
        this.name = name
        guard()
    }
}
