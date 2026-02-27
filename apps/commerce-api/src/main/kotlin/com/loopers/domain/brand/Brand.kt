package com.loopers.domain.brand

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

@Entity
@Table(name = "brands")
@Comment("브랜드")
class Brand(
    name: String,
    description: String? = null,
    imageUrl: String? = null,
) : BaseEntity() {

    @Comment("브랜드명")
    @Column(nullable = false)
    var name: String = name
        protected set

    @Comment("브랜드 설명")
    @Column
    var description: String? = description
        protected set

    @Comment("브랜드 이미지")
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
