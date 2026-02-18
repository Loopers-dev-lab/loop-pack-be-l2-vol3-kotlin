package com.loopers.infrastructure.brand

import com.loopers.domain.BaseEntity
import com.loopers.domain.brand.BrandStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "brand")
class BrandEntity(
    id: Long?,

    @Column(name = "name", nullable = false, unique = true, length = 100)
    val name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    val description: String?,

    @Column(name = "logo_url", length = 500)
    val logoUrl: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: BrandStatus,

    deletedAt: ZonedDateTime?,
) : BaseEntity() {
    init {
        this.id = id
        if (deletedAt != null) {
            this.deletedAt = deletedAt
        }
    }
}
