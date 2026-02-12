package com.loopers.infrastructure.user

import com.loopers.domain.user.GenderType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UserMapperTest {

    @Test
    fun `entity id가 null이면 IllegalArgumentException이 발생한다`() {
        val entity = UserEntity(
            id = null,
            loginId = "testuser",
            password = "encodedPassword",
            name = "테스트",
            birthDate = LocalDate.of(1993, 4, 1),
            email = "test@test.com",
            gender = GenderType.MALE,
        )

        assertThatThrownBy { UserMapper.toDomain(entity) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("UserEntity.id가 null입니다")
    }
}
