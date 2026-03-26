package com.loopers.domain.productlike

import com.loopers.domain.productlike.event.LikeCountEvent
import com.loopers.domain.productlike.event.LikeCountEventType
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean

@TestConfiguration
class ProductLikeConcurrencyTestConfig {
    @Bean
    fun likeCountEventListener(
        productLikeCountRepository: ProductLikeCountRepository,
    ): ApplicationListener<LikeCountEvent> {
        return LikeCountEventListenerImpl(productLikeCountRepository)
    }

    private class LikeCountEventListenerImpl(
        private val productLikeCountRepository: ProductLikeCountRepository,
    ) : ApplicationListener<LikeCountEvent> {
        override fun onApplicationEvent(event: LikeCountEvent) {
            when (event.type) {
                LikeCountEventType.INCREMENT -> productLikeCountRepository.increment(event.productId)
                LikeCountEventType.DECREMENT -> productLikeCountRepository.decrement(event.productId)
            }
        }
    }
}
