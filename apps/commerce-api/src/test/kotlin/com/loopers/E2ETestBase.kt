package com.loopers

import com.loopers.utils.DatabaseCleanUp
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("database")
abstract class E2ETestBase {
    @Autowired
    protected lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    protected lateinit var databaseCleanUp: DatabaseCleanUp

    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }
}
