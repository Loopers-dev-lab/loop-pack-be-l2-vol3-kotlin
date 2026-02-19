package com.loopers.domain.catalog.brand

object BrandTestFixture {

    const val DEFAULT_NAME = "나이키"

    fun createBrand(name: String = DEFAULT_NAME): Brand = Brand(name = name)
}
