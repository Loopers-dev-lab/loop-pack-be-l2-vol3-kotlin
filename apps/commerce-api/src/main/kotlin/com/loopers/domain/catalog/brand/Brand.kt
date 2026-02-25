package com.loopers.domain.catalog.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

/**
 * 브랜드 도메인 모델 (JPA 비의존)
 *
 * @property id 식별자 (영속화 전에는 0L)
 * @property name 브랜드명
 * @property description 브랜드 설명
 */
class Brand(
    name: String,
    description: String,
    val id: Long = 0L,
) {
    var name: String = name
        private set

    var description: String = description
        private set

    init {
        if (name.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "브랜드명은 비어있을 수 없습니다.")
    }

    fun update(name: String, description: String) {
        if (name.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "브랜드명은 비어있을 수 없습니다.")
        this.name = name
        this.description = description
    }
}
