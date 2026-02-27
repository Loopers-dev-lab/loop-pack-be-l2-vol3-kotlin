package com.loopers.domain.admin

import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AdminTest {
    @DisplayName("어드민을 생성할 때, ")
    @Nested
    inner class Create {
        @DisplayName("ldap과 이름이 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsAdmin_whenLdapAndNameAreProvided() {
            // arrange
            val ldap = "loopers.admin"
            val name = "관리자"

            // act
            val admin = Admin(ldap = ldap, name = name)

            // assert
            assertAll(
                { assertThat(admin.ldap).isEqualTo(ldap) },
                { assertThat(admin.name).isEqualTo(name) },
            )
        }

        @DisplayName("ldap이 '문자열.문자열' 형식이 아니면, 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = ["admin", "admin.", ".admin", "admin.admin.extra", "admin admin", "", "admin..admin", "관리자.admin"])
        fun throwsException_whenLdapFormatIsInvalid(invalidLdap: String) {
            // arrange & act & assert
            assertThrows<CoreException> {
                Admin(ldap = invalidLdap, name = "관리자")
            }
        }
    }
}
