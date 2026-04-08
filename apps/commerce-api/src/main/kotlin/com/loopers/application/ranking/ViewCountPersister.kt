package com.loopers.application.ranking

interface ViewCountPersister {
    fun incrementViewCounts(viewCounts: Map<Long, Long>)
}
