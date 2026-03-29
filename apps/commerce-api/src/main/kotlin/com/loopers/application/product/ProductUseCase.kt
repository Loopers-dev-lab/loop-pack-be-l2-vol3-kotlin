package com.loopers.application.product

import com.loopers.application.event.OutboxEventWriter
import com.loopers.application.event.UserActionLogEvent
import com.loopers.application.event.UserActionType
import com.loopers.domain.brand.BrandReader
import com.loopers.domain.product.ProductChanger
import com.loopers.domain.product.ProductReader
import com.loopers.domain.product.ProductRegister
import com.loopers.domain.product.ProductRemover
import com.loopers.domain.product.ProductSortType
import com.loopers.kafka.IntegrationEvent
import com.loopers.kafka.KafkaTopics
import com.loopers.kafka.ProductViewedPayload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.context.ApplicationEventPublisher

@Component
class ProductUseCase(
    private val productRegister: ProductRegister,
    private val productReader: ProductReader,
    private val productChanger: ProductChanger,
    private val productRemover: ProductRemover,
    private val brandReader: BrandReader,
    private val productCacheStore: ProductCacheStore,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val outboxEventWriter: OutboxEventWriter,
) {

    @Transactional
    fun register(command: RegisterCommand): ProductInfo.Detail {
        val brand = brandReader.getActiveById(command.brandId)
        val product = productRegister.register(
            brandId = command.brandId,
            name = command.name,
            price = command.price,
            description = command.description,
            stock = command.stock,
        )
        productCacheStore.evictList()
        productCacheStore.evictList(command.brandId)
        return ProductInfo.Detail.from(product, brand)
    }

    @Transactional
    fun getById(id: Long): ProductInfo.Detail {
        val detail = productCacheStore.getDetail(id) {
            val product = productReader.getById(id)
            val brand = brandReader.getById(product.brandId)
            ProductInfo.Detail.from(product, brand)
        }
        val occurredAt = ZonedDateTime.now()
        applicationEventPublisher.publishEvent(
            UserActionLogEvent(
                actionType = UserActionType.PRODUCT_VIEWED,
                memberId = null,
                targetType = "product",
                targetId = id.toString(),
            ),
        )
        outboxEventWriter.append(
            topic = KafkaTopics.CATALOG_EVENTS,
            event = IntegrationEvent(
                eventId = "catalog-product-viewed:${UUID.randomUUID()}",
                eventType = "ProductViewed",
                aggregateType = "product",
                aggregateId = id.toString(),
                key = id.toString(),
                version = occurredAt.toInstant().toEpochMilli(),
                occurredAt = occurredAt,
                payload = ProductViewedPayload(
                    productId = id,
                    memberId = null,
                ),
            ),
        )
        return detail
    }

    @Transactional(readOnly = true)
    fun getAll(sortType: ProductSortType, brandId: Long?): List<ProductInfo.Main> {
        return productCacheStore.getList(sortType, brandId) {
            val products = productReader.getAll(sortType, brandId)
            if (products.isEmpty()) {
                return@getList emptyList()
            }
            val brandIds = products.map { it.brandId }.distinct()
            val brandMap = brandReader.getAllByIds(brandIds).associateBy { it.id }

            products.map { product ->
                val brand = brandMap[product.brandId]
                ProductInfo.Main.from(product, brand)
            }
        }
    }

    @Transactional
    fun changeInfo(id: Long, command: ChangeInfoCommand): ProductInfo.Detail {
        val product = productChanger.changeInfo(
            id = id,
            name = command.name,
            price = command.price,
            description = command.description,
        )
        val brand = brandReader.getById(product.brandId)
        productCacheStore.evictDetail(requireNotNull(product.id))
        productCacheStore.evictList()
        productCacheStore.evictList(product.brandId)
        return ProductInfo.Detail.from(product, brand)
    }

    @Transactional
    fun remove(id: Long) {
        val product = productReader.getById(id)
        productRemover.remove(id)
        productCacheStore.evictDetail(id)
        productCacheStore.evictList()
        productCacheStore.evictList(product.brandId)
    }

    data class RegisterCommand(
        val brandId: Long,
        val name: String,
        val price: Long,
        val description: String,
        val stock: Int,
    )

    data class ChangeInfoCommand(
        val name: String,
        val price: Long,
        val description: String,
    )
}
