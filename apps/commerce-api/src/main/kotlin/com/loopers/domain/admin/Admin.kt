package com.loopers.domain.admin

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "admins")
class Admin(
    ldap: String,
    name: String,
) : BaseEntity() {
    @Column(unique = true, nullable = false)
    var ldap: String = ldap
        protected set

    var name: String = name
        protected set

    init {
        if (!validateLdap(ldap)) throw CoreException(ErrorType.BAD_REQUEST, "invalid ldap")
    }

    companion object {
        private val LDAP_REGEX = Regex("^[A-Za-z0-9]+\\.[A-Za-z0-9]+$")

        private fun validateLdap(ldap: String): Boolean {
            return LDAP_REGEX.matches(ldap)
        }
    }
}
