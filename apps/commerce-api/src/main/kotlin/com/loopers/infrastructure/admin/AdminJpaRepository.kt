package com.loopers.infrastructure.admin

import com.loopers.domain.admin.Admin
import org.springframework.data.jpa.repository.JpaRepository

interface AdminJpaRepository : JpaRepository<Admin, Long> {
    fun findByLdap(ldap: String): Admin?
}
