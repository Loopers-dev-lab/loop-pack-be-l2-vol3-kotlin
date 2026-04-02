package com.loopers.infrastructure.queue

import com.loopers.support.annotation.WaitingQueue
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.util.ReflectionUtils
import org.springframework.web.bind.annotation.RestController

@Component
class WaitingQueueRegistry(
    private val applicationContext: ApplicationContext,
) : SmartInitializingSingleton {

    private val log = LoggerFactory.getLogger(WaitingQueueRegistry::class.java)

    data class QueueConfig(
        val name: String,
        val throughputPerServerPerSecond: Int,
        val activeTokenTTLSeconds: Int,
    )

    private var queueConfigMap: Map<String, QueueConfig> = emptyMap()

    override fun afterSingletonsInstantiated() {
        val result = mutableMapOf<String, QueueConfig>()
        val controllerBeans = applicationContext.getBeansWithAnnotation(RestController::class.java)

        controllerBeans.values.forEach { bean ->
            val targetClass = AopUtils.getTargetClass(bean)
            ReflectionUtils.doWithMethods(targetClass) { method ->
                val annotation = AnnotationUtils.findAnnotation(method, WaitingQueue::class.java)
                if (annotation != null) {
                    validateAnnotation(annotation)
                    val config = QueueConfig(
                        name = annotation.name,
                        throughputPerServerPerSecond = annotation.throughputPerServerPerSecond,
                        activeTokenTTLSeconds = annotation.activeTokenTTLSeconds,
                    )
                    result[annotation.name] = config
                    log.info(
                        "[WaitingQueueRegistry] 큐 등록 완료. name={}, throughput={}, ttl={}",
                        annotation.name,
                        annotation.throughputPerServerPerSecond,
                        annotation.activeTokenTTLSeconds,
                    )
                }
            }
        }

        this.queueConfigMap = result.toMap()
        log.info("[WaitingQueueRegistry] 스캔 완료. 등록된 큐 수={}", queueConfigMap.size)
    }

    private fun validateAnnotation(annotation: WaitingQueue) {
        if (annotation.name.isBlank()) {
            throw IllegalArgumentException("@WaitingQueue name must not be blank")
        }
        if (annotation.throughputPerServerPerSecond <= 0) {
            throw IllegalArgumentException(
                "@WaitingQueue throughputPerServerPerSecond must be > 0, " +
                    "got ${annotation.throughputPerServerPerSecond}",
            )
        }
        if (annotation.activeTokenTTLSeconds <= 0) {
            throw IllegalArgumentException(
                "@WaitingQueue activeTokenTTLSeconds must be > 0, " +
                    "got ${annotation.activeTokenTTLSeconds}",
            )
        }
    }

    fun getQueueConfigs(): Collection<QueueConfig> = queueConfigMap.values

    fun getQueueConfig(queueName: String): QueueConfig? = queueConfigMap[queueName]
}
