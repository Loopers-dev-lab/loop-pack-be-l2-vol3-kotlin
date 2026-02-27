package com.loopers.domain.brand

import com.loopers.domain.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "brands")
class Brand(
    name: String,
    description: String,
) : BaseEntity() {
    var name: String = name
        protected set

    var description: String = description
        protected set

    fun update(name: String, description: String) {
        this.name = name
        this.description = description
    }
}
