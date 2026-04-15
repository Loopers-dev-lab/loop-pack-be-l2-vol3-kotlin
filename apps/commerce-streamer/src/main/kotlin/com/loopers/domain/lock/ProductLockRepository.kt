package com.loopers.domain.lock

interface ProductLockRepository {
    /** 상품 행에 PESSIMISTIC_WRITE 락을 걸고 상품 ID를 반환한다. 상품이 없으면 null. */
    fun findByIdForUpdate(id: Long): Long?
}
