package com.loopers.infrastructure.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserEntity, Long> {
    /**
 * Finds a user with the given login identifier.
 *
 * @param loginId The user's login identifier.
 * @return The matching `UserEntity` if present, `null` otherwise.
 */
fun findByLoginId(loginId: String): UserEntity?

    /**
 * Determines whether a user with the specified login identifier exists.
 *
 * @param loginId The login identifier to check for existence.
 * @return `true` if a user with the given `loginId` exists, `false` otherwise.
 */
fun existsByLoginId(loginId: String): Boolean
}