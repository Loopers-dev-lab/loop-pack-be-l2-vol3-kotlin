package com.loopers.testcontainers

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(RedisTestContainersConfig::class)
class QueueTestContainersConfig
