package com.loopers.application.catalog

import com.loopers.application.UseCase
import com.loopers.domain.catalog.BrandService
import com.loopers.domain.catalog.ProductService
import com.loopers.domain.common.event.ProductViewedEvent
import com.loopers.domain.user.UserService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserGetProductUseCase(
    private val productService: ProductService,
    private val brandService: BrandService,
    private val userService: UserService,
    private val eventPublisher: ApplicationEventPublisher,
) : UseCase<ViewProductCriteria, UserGetProductResult> {

    @Transactional(readOnly = true)
    override fun execute(criteria: ViewProductCriteria): UserGetProductResult {
        val user = userService.getUser(criteria.loginId)
        val productInfo = productService.getProduct(criteria.productId)
        val brandInfo = brandService.findBrand(productInfo.brandId)

        eventPublisher.publishEvent(
            ProductViewedEvent(
                userId = user.id,
                loginId = criteria.loginId,
                productId = criteria.productId,
            ),
        )

        return UserGetProductResult.from(productInfo, brandName = brandInfo?.name ?: "")
    }
}
