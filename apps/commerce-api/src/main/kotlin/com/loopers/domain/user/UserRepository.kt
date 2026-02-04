package com.loopers.domain.user

interface UserRepository {
    /**
 * Persist the given user and return its generated identifier.
 *
 * @param user The user to persist.
 * @return The generated identifier of the saved user.
 */
fun save(user: User): Long

    /**
 * Retrieve a user by its unique identifier.
 *
 * @param id The user's primary identifier.
 * @return The matching User if found, `null` otherwise.
 */
fun findById(id: Long): User?

    /**
 * Retrieve a User by their login identifier.
 *
 * @param loginId The user's login identifier.
 * @return `User` if a user with the given login identifier exists, `null` otherwise.
 */
fun findByLoginId(loginId: LoginId): User?

    /**
 * Checks whether a user with the given login identifier exists.
 *
 * @param loginId The login identifier to check for existence.
 * @return `true` if a user with `loginId` exists, `false` otherwise.
 */
fun existsByLoginId(loginId: LoginId): Boolean
}