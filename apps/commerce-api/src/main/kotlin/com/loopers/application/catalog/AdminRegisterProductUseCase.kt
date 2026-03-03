package com.loopers.application.catalog

import com.loopers.application.UseCase
import com.loopers.domain.catalog.BrandService
import com.loopers.domain.catalog.ProductService
import com.loopers.domain.catalog.RegisterProductCommand
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminRegisterProductUseCase(
    private val brandService: BrandService,
    private val productService: ProductService,
) : UseCase<RegisterProductCriteria, RegisterProductResult> {

    @Transactional
    override fun execute(criteria: RegisterProductCriteria): RegisterProductResult {
        brandService.getBrand(criteria.brandId)

        val info = productService.register(
            RegisterProductCommand(
                brandId = criteria.brandId,
                name = criteria.name,
                quantity = criteria.quantity,
                price = criteria.price,
            ),
        )
        return RegisterProductResult.from(info)
    }
}
