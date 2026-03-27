package com.loopers.infrastructure.useraction

import org.springframework.data.jpa.repository.JpaRepository

interface UserActionLogJpaRepository : JpaRepository<UserActionLogJpaModel, Long>
