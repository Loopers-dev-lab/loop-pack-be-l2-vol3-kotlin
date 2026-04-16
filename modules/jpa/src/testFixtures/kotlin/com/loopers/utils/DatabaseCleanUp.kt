package com.loopers.utils

import jakarta.persistence.Entity
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Table
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DatabaseCleanUp(
    @PersistenceContext private val entityManager: EntityManager,
) : InitializingBean {
    private val tableNames = mutableListOf<String>()

    // MV 테이블은 배치 작업에서 관리되므로 제외
    private val excludedTables = setOf("mv_product_rank_weekly", "mv_product_rank_monthly")

    override fun afterPropertiesSet() {
        entityManager.metamodel.entities
            .filter { entity -> entity.javaType.getAnnotation(Entity::class.java) != null }
            .map { entity -> entity.javaType.getAnnotation(Table::class.java).name }
            .filter { tableName -> !excludedTables.contains(tableName) }
            .forEach { tableNames.add(it) }
    }

    @Transactional
    fun truncateAllTables() {
        entityManager.flush()
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate()
        tableNames.forEach { table ->
            runCatching {
                entityManager.createNativeQuery("TRUNCATE TABLE `$table`").executeUpdate()
            }.onFailure { ex ->
                // 테이블이 없거나 다른 이유로 실패해도 계속 진행
                System.err.println("Warning: Failed to truncate table $table: ${ex.message}")
            }
        }
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate()
    }

    @Transactional
    fun execute() {
        truncateAllTables()
    }
}
