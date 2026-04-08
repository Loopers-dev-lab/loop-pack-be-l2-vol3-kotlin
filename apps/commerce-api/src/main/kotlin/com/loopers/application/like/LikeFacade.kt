package com.loopers.application.like

import com.loopers.application.brand.BrandInfo
import com.loopers.application.product.ProductInfo
import com.loopers.domain.brand.BrandService
import com.loopers.domain.event.EventTopics
import com.loopers.domain.event.OutboxEventService
import com.loopers.domain.event.ProductLikedEvent
import com.loopers.domain.event.ProductUnlikedEvent
import com.loopers.domain.like.LikeService
import com.loopers.domain.product.ProductService
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Component

@Component
class LikeFacade(
    private val likeService: LikeService,
    private val productService: ProductService,
    private val brandService: BrandService,
    private val userService: UserService,
    private val eventPublisher: ApplicationEventPublisher,
    private val outboxEventService: OutboxEventService,
) {
    @Transactional
    fun like(loginId: String, password: String, productId: Long) {
        val user = getAuthenticatedUser(loginId, password)
        productService.getProduct(productId)

        val existing = likeService.findLike(user.id, productId)
        if (existing != null) return

        likeService.createLike(user.id, productId)

        val event = ProductLikedEvent(userId = user.id, productId = productId)
        eventPublisher.publishEvent(event)

        outboxEventService.saveOutboxEvent(
            aggregateType = "Product",
            aggregateId = productId.toString(),
            eventType = "ProductLiked",
            topic = EventTopics.CATALOG_EVENTS,
            event = event,
        )
    }

    @Transactional
    fun unlike(loginId: String, password: String, productId: Long) {
        val user = getAuthenticatedUser(loginId, password)

        val existing = likeService.findLike(user.id, productId)
            ?: return

        likeService.deleteLike(existing)

        val event = ProductUnlikedEvent(userId = user.id, productId = productId)
        eventPublisher.publishEvent(event)

        outboxEventService.saveOutboxEvent(
            aggregateType = "Product",
            aggregateId = productId.toString(),
            eventType = "ProductUnliked",
            topic = EventTopics.CATALOG_EVENTS,
            event = event,
        )
    }

    fun getUserLikes(loginId: String, password: String): List<LikeInfo> {
        val user = getAuthenticatedUser(loginId, password)
        val likes = likeService.getUserLikes(user.id)
        return likes.map { like ->
            val product = productService.getProduct(like.productId)
            val brandInfo = BrandInfo.from(brandService.getBrand(product.brandId))
            LikeInfo(productInfo = ProductInfo.from(product, brandInfo))
        }
    }

    private fun getAuthenticatedUser(loginId: String, password: String) =
        userService.getUserByLoginIdAndPassword(loginId, password)
            ?: throw CoreException(ErrorType.NOT_FOUND, "User not found")
}
