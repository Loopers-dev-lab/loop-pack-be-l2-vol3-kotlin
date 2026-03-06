package com.loopers.domain.admin

import org.springframework.stereotype.Component

@Component
class AdminService(
    private val adminRepository: AdminRepository,
) {
    fun getAdminByLdap(ldap: String): Admin? {
        return adminRepository.findByLdap(ldap)
    }
}
