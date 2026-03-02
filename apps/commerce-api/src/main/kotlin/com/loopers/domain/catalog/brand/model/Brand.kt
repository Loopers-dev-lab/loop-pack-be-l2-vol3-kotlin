package com.loopers.domain.catalog.brand.model

import com.loopers.domain.catalog.brand.vo.BrandName
import com.loopers.domain.common.vo.BrandId
import java.time.ZonedDateTime

class Brand(
    val id: BrandId = BrandId(0),
    name: BrandName,
    deletedAt: ZonedDateTime? = null,
) {

    var name: BrandName = name
        private set

    var deletedAt: ZonedDateTime? = deletedAt
        private set

    fun update(name: BrandName) {
        this.name = name
    }

    fun delete() {
        deletedAt ?: run { deletedAt = ZonedDateTime.now() }
    }

    fun restore() {
        deletedAt?.let { deletedAt = null }
    }

    fun isDeleted(): Boolean = deletedAt != null
}
