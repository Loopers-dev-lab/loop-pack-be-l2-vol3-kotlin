package com.loopers.application.queue

class FakeQueueConfigStore : QueueConfigStore {

    private var enabled = false

    override fun isEnabled(): Boolean = enabled

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
}
