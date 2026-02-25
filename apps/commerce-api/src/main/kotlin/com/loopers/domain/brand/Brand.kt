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
    description: String? = null,
    imageUrl: String? = null,
) : BaseEntity() {

    @Column(nullable = false)
    var name: String = name
        protected set

    @Column
    var description: String? = description
        protected set

    @Column(name = "image_url")
    var imageUrl: String? = imageUrl
        protected set

    init {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "브랜드명은 필수입니다.")
        }
    }

    fun update(name: String, description: String?, imageUrl: String?) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "브랜드명은 필수입니다.")
        }
        this.name = name
        this.description = description
        this.imageUrl = imageUrl
    }
}
