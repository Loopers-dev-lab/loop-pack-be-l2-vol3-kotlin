package com.loopers.interfaces.config

import com.loopers.interfaces.api.product.ProductSortRequest
import org.springframework.core.convert.converter.Converter

class StringToProductSortRequestConverter : Converter<String, ProductSortRequest> {
    override fun convert(source: String): ProductSortRequest {
        return ProductSortRequest.valueOf(source.uppercase())
    }
}
