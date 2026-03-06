package com.loopers.domain.admin

import com.loopers.infrastructure.admin.AdminJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AdminServiceIntegrationTest @Autowired constructor(
    private val adminService: AdminService,
    private val adminJpaRepository: AdminJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("어드민을 LDAP으로 조회할 때, ")
    @Nested
    inner class GetAdminByLdap {
        @DisplayName("해당 LDAP의 어드민이 존재하면, 어드민 정보를 반환한다.")
        @Test
        fun returnsAdmin_whenLdapExists() {
            // arrange
            val admin = adminJpaRepository.save(Admin(ldap = "loopers.admin", name = "관리자"))

            // act
            val result = adminService.getAdminByLdap(admin.ldap)

            // assert
            assertAll(
                { assertThat(result).isNotNull() },
                { assertThat(result?.ldap).isEqualTo(admin.ldap) },
                { assertThat(result?.name).isEqualTo(admin.name) },
            )
        }

        @DisplayName("해당 LDAP의 어드민이 존재하지 않으면, null을 반환한다.")
        @Test
        fun returnsNull_whenLdapNotExists() {
            // arrange
            val ldap = "nonexistent.admin"

            // act
            val result = adminService.getAdminByLdap(ldap)

            // assert
            assertThat(result).isNull()
        }
    }
}
