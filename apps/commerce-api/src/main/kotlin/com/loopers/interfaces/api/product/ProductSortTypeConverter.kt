package com.loopers.interfaces.api.product

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class ProductSortTypeConverter : Converter<String, ProductSortType> {
    override fun convert(source: String): ProductSortType =
        ProductSortType.valueOf(source.uppercase())
}
