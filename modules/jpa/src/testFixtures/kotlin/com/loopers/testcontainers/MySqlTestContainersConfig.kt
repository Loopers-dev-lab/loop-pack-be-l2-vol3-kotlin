package com.loopers.testcontainers

import org.springframework.context.annotation.Configuration
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

@Configuration
class MySqlTestContainersConfig {
    companion object {
        private val mySqlContainer: MySQLContainer<*> = MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .apply {
                withDatabaseName("loopers")
                withUsername("test")
                withPassword("test")
                withExposedPorts(3306)
                withCommand(
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_general_ci",
                    "--skip-character-set-client-handshake",
                )
                start()
            }

        /** 컨테이너 JDBC URL. `@DynamicPropertySource`에서 직접 참조해 사용한다. */
        val jdbcUrl: String
            get() = "jdbc:mysql://${mySqlContainer.host}:${mySqlContainer.firstMappedPort}/${mySqlContainer.databaseName}"

        val username: String
            get() = mySqlContainer.username

        val password: String
            get() = mySqlContainer.password

        init {
            System.setProperty("datasource.mysql-jpa.main.jdbc-url", jdbcUrl)
            System.setProperty("datasource.mysql-jpa.main.username", username)
            System.setProperty("datasource.mysql-jpa.main.password", password)
        }
    }
}
