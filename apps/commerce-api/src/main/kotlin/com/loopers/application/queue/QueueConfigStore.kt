package com.loopers.application.queue

interface QueueConfigStore {

    fun isEnabled(): Boolean

    fun setEnabled(enabled: Boolean)
}
