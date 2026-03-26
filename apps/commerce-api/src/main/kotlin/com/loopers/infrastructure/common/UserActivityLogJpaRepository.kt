package com.loopers.infrastructure.common

import com.loopers.domain.common.UserActivityLogModel
import org.springframework.data.jpa.repository.JpaRepository

interface UserActivityLogJpaRepository : JpaRepository<UserActivityLogModel, Long>
