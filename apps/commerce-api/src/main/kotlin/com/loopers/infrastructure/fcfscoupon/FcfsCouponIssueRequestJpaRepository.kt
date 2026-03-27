package com.loopers.infrastructure.fcfscoupon

import org.springframework.data.jpa.repository.JpaRepository

interface FcfsCouponIssueRequestJpaRepository : JpaRepository<FcfsCouponIssueRequestJpaModel, Long>
