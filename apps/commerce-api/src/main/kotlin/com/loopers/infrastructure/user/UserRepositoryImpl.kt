package com.loopers.infrastructure.user

import com.loopers.domain.user.LoginId
import com.loopers.domain.user.User
import com.loopers.domain.user.UserRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository,
) : UserRepository {

    /**
     * Persists the given domain User and returns the generated database identifier.
     *
     * @param user The domain User to persist.
     * @return The generated id of the saved user.
     */
    override fun save(user: User): Long {
        val entity = UserMapper.toEntity(user)
        return jpaRepository.save(entity).id!!
    }

    /**
     * Retrieve a user by its database id.
     *
     * @param id The user's database identifier.
     * @return The matching User if found, `null` otherwise.
     */
    override fun findById(id: Long): User? {
        return jpaRepository.findById(id).orElse(null)?.let { UserMapper.toDomain(it) }
    }

    /**
     * Finds a user by their login identifier.
     *
     * @param loginId The domain login identifier to look up.
     * @return The corresponding User if found, `null` otherwise.
     */
    override fun findByLoginId(loginId: LoginId): User? {
        return jpaRepository.findByLoginId(loginId.value)?.let { UserMapper.toDomain(it) }
    }

    /**
     * Checks whether a user exists with the given login ID.
     *
     * @param loginId The login identifier to check.
     * @return `true` if a user with the given login ID exists, `false` otherwise.
     */
    override fun existsByLoginId(loginId: LoginId): Boolean {
        return jpaRepository.existsByLoginId(loginId.value)
    }
}