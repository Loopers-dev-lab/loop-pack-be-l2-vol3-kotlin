package com.loopers.infrastructure.admin

import com.loopers.domain.admin.Admin
import com.loopers.domain.admin.AdminRepository
import org.springframework.stereotype.Component

@Component
class AdminRepositoryImpl(
    private val adminJpaRepository: AdminJpaRepository,
) : AdminRepository {
    override fun save(admin: Admin): Admin {
        return adminJpaRepository.save(admin)
    }

    override fun findByLdap(ldap: String): Admin? {
        return adminJpaRepository.findByLdap(ldap)
    }
}
