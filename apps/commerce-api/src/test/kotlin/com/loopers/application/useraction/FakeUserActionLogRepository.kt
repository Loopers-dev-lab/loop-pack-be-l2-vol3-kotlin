package com.loopers.application.useraction

import com.loopers.domain.useraction.UserActionLogModel
import com.loopers.domain.useraction.UserActionLogRepository
import java.time.ZonedDateTime

class FakeUserActionLogRepository : UserActionLogRepository {
    private val store = mutableListOf<UserActionLogModel>()
    private var idSequence = 1L

    override fun save(log: UserActionLogModel): UserActionLogModel {
        val saved = log.copy(id = idSequence++, createdAt = ZonedDateTime.now())
        store.add(saved)
        return saved
    }

    fun findAll(): List<UserActionLogModel> = store.toList()

    fun clear() {
        store.clear()
        idSequence = 1L
    }
}
