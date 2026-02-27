package com.loopers.infrastructure.user

import com.loopers.domain.user.User
import com.loopers.domain.user.vo.LoginId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserJpaRepository : JpaRepository<User, Long> {
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.loginId = :loginId")
    fun existsByLoginId(loginId: LoginId): Boolean

    @Query("SELECT u FROM User u WHERE u.loginId = :loginId")
    fun findByLoginId(loginId: LoginId): User?
}
