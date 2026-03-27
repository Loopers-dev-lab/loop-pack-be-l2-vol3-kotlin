package com.loopers.infrastructure.fcfscoupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface FcfsCouponTemplateJpaRepository : JpaRepository<FcfsCouponTemplateJpaModel, Long> {
    fun findAllByOrderByIdDesc(pageable: Pageable): Page<FcfsCouponTemplateJpaModel>
}
