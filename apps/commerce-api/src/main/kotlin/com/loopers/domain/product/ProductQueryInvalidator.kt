package com.loopers.domain.product

interface ProductQueryInvalidator {
    fun invalidateDetails(productIds: Collection<Long>)

    fun invalidateListsByBrandId(brandId: Long)
}
