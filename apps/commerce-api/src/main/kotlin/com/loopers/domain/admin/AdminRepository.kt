package com.loopers.domain.admin

interface AdminRepository {
    fun save(admin: Admin): Admin
    fun findByLdap(ldap: String): Admin?
}
