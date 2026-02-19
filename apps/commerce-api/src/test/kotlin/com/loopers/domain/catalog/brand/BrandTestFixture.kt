package com.loopers.domain.catalog.brand

import com.loopers.domain.catalog.brand.entity.Brand

object BrandTestFixture {

    const val DEFAULT_NAME = "나이키"

    fun createBrand(name: String = DEFAULT_NAME): Brand = Brand(name = name)
}
