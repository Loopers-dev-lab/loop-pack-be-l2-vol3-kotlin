package com.loopers.batch.orchestrator

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "batch.ranking.orchestrator")
data class OrchestratorProperties(
    val cron: String,
)
