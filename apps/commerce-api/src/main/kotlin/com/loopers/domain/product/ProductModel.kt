package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "tb_product")
@SQLRestriction("deleted_at IS NULL")
class ProductModel(
    brandId: Long,
    name: Name,
    imageUrl: ImageUrl,
    description: Description,
    price: Price,
) : BaseEntity() {

    @Column(nullable = false)
    var brandId: Long = brandId
        protected set

    @Column(nullable = false)
    var name: Name = name
        protected set

    @Column(nullable = false)
    var imageUrl: ImageUrl = imageUrl
        protected set

    @Column
    var description: Description = description
        protected set

    @Column
    var price: Price = price
        protected set

    @Column
    var likeCount: LikeCount = LikeCount(0L)
        protected set

    fun update(name: Name, imageUrl: ImageUrl, description: Description, price: Price) {
        this.name = name
        this.imageUrl = imageUrl
        this.description = description
        this.price = price
    }

    fun increaseLikeCount() {
        likeCount = LikeCount(likeCount.value + 1)
    }

    fun decreaseLikeCount() {
        if (likeCount.value > 0) {
            likeCount = LikeCount(likeCount.value - 1)
        }
    }
}
