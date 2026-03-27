package com.loopers.interfaces.api.user.coupon

import com.loopers.interfaces.api.admin.coupon.AdminCouponV1Response
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.user.auth.UserAuthV1Response
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

@DisplayName("First-Come 쿠폰 발급 black-box E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserCouponIssueE2ETest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val PASSWORD = "Password1!"
        private const val SIGN_UP_ENDPOINT = "/api/v1/users"
        private const val COUPON_REGISTER_ENDPOINT = "/api-admin/v1/coupons"
        private const val ISSUE_ENDPOINT_TEMPLATE = "/api/v1/coupons/%d/issue"
        private const val ISSUE_STATUS_ENDPOINT_TEMPLATE = "/api/v1/users/me/coupon-issue-requests/%d"
        private const val COUPONS_ENDPOINT = "/api/v1/users/me/coupons"
        private const val ADMIN_LDAP = "loopers.admin"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("공개 HTTP 인터페이스만으로 쿠폰 발급 요청, 상태 조회, 중복 요청 재사용을 검증한다")
    fun issue_coupon_through_public_http_flow() {
        val streamerProcess = startStreamerProcess()
        try {
            val loginId = signUpUser()
            val couponId = registerCoupon()

            val firstIssueResponse = issueCoupon(loginId, couponId)
            assertAll(
                { assertThat(firstIssueResponse.requestId).isGreaterThan(0L) },
                { assertThat(firstIssueResponse.couponId).isEqualTo(couponId) },
                { assertThat(firstIssueResponse.status).isEqualTo("REQUESTED") },
            )

            val issuedStatus = waitForIssuedStatus(loginId, firstIssueResponse.requestId)
            assertAll(
                { assertThat(issuedStatus.requestId).isEqualTo(firstIssueResponse.requestId) },
                { assertThat(issuedStatus.couponId).isEqualTo(couponId) },
                { assertThat(issuedStatus.status).isEqualTo("ISSUED") },
                { assertThat(issuedStatus.failureReasonCode).isNull() },
                { assertThat(issuedStatus.issuedCouponId).isNotNull() },
            )

            val duplicateIssueResponse = issueCoupon(loginId, couponId)
            assertAll(
                { assertThat(duplicateIssueResponse.requestId).isEqualTo(firstIssueResponse.requestId) },
                { assertThat(duplicateIssueResponse.couponId).isEqualTo(couponId) },
                { assertThat(duplicateIssueResponse.status).isEqualTo("ISSUED") },
            )

            val couponListResponse = testRestTemplate.exchange(
                COUPONS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders(loginId)),
                object : ParameterizedTypeReference<ApiResponse<List<UserCouponV1Response.ListItem>>>() {},
            )

            assertAll(
                { assertThat(couponListResponse.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(couponListResponse.body?.data).hasSize(1) },
                { assertThat(couponListResponse.body?.data?.first()?.couponId).isEqualTo(couponId) },
                { assertThat(couponListResponse.body?.data?.first()?.displayStatus).isEqualTo("AVAILABLE") },
            )
        } finally {
            stopProcess(streamerProcess)
        }
    }

    private fun signUpUser(): String {
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        val loginId = "u$suffix"
        val email = "u$suffix@example.com"
        val body = """
            {
              "loginId": "$loginId",
              "password": "$PASSWORD",
              "name": "홍길동",
              "birthDate": "1990-01-01",
              "email": "$email"
            }
        """.trimIndent()

        val response = testRestTemplate.exchange(
            SIGN_UP_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(body, jsonHeaders()),
            object : ParameterizedTypeReference<ApiResponse<UserAuthV1Response.SignUp>>() {},
        )

        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
            { assertThat(response.body?.data?.loginId).isEqualTo(loginId) },
        )
        return loginId
    }

    private fun registerCoupon(): Long {
        val body = """
            {
              "name": "E2E 쿠폰",
              "type": "FIXED",
              "discountValue": 1000,
              "expiredAt": "2099-12-31T23:59:59+09:00",
              "issueLimit": 1
            }
        """.trimIndent()

        val response = testRestTemplate.exchange(
            COUPON_REGISTER_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(body, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Response.Register>>() {},
        )

        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
            { assertThat(response.body?.data?.name).isEqualTo("E2E 쿠폰") },
            { assertThat(response.body?.data?.issueLimit).isEqualTo(1L) },
        )
        return response.body?.data?.id ?: error("coupon id is missing")
    }

    private fun issueCoupon(
        loginId: String,
        couponId: Long,
    ): UserCouponV1Response.IssueRequest {
        val response = testRestTemplate.exchange(
            ISSUE_ENDPOINT_TEMPLATE.format(couponId),
            HttpMethod.POST,
            HttpEntity<Void>(authHeaders(loginId)),
            object : ParameterizedTypeReference<ApiResponse<UserCouponV1Response.IssueRequest>>() {},
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
        return response.body?.data ?: error("issue response data is missing")
    }

    private fun waitForIssuedStatus(
        loginId: String,
        requestId: Long,
    ): UserCouponV1Response.IssueRequestStatus {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        var latestStatus: UserCouponV1Response.IssueRequestStatus? = null

        while (System.nanoTime() < deadline) {
            val response = testRestTemplate.exchange(
                ISSUE_STATUS_ENDPOINT_TEMPLATE.format(requestId),
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders(loginId)),
                object : ParameterizedTypeReference<ApiResponse<UserCouponV1Response.IssueRequestStatus>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val status = response.body?.data ?: error("issue status response data is missing")
            latestStatus = status
            if (status.status == "ISSUED") {
                return status
            }
            Thread.sleep(200)
        }

        error("issue request did not reach ISSUED within timeout, last status=${latestStatus?.status}")
    }

    private fun jsonHeaders(): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
    }

    private fun adminHeaders(): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        set("X-Loopers-Ldap", ADMIN_LDAP)
    }

    private fun authHeaders(loginId: String): HttpHeaders = HttpHeaders().apply {
        set("X-Loopers-LoginId", loginId)
        set("X-Loopers-LoginPw", PASSWORD)
    }

    private fun startStreamerProcess(): Process {
        val rootDir = repoRoot()
        val processBuilder = ProcessBuilder(
            rootDir.resolve("gradlew").toString(),
            ":apps:commerce-streamer:bootRun",
            "--args=--spring.profiles.active=test --server.port=0",
        )
            .directory(rootDir.toFile())
            .redirectErrorStream(true)

        propagateJdbcEnvironment(processBuilder)

        val process = processBuilder.start()
        val started = AtomicBoolean(false)
        val output = StringBuilder()
        thread(
            name = "streamer-bootrun-log",
            isDaemon = true,
        ) {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    synchronized(output) {
                        output.appendLine(line)
                    }
                    if (line.contains("Started CommerceStreamerApplication")) {
                        started.set(true)
                    }
                }
            }
        }

        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90)
        while (System.nanoTime() < deadline) {
            if (started.get()) {
                return process
            }
            if (!process.isAlive) {
                error("streamer bootRun exited before startup:\n${synchronized(output) { output.toString() }}")
            }
            Thread.sleep(250)
        }

        process.destroyForcibly()
        error("streamer bootRun did not start within timeout:\n${synchronized(output) { output.toString() }}")
    }

    private fun stopProcess(process: Process) {
        if (!process.isAlive) {
            return
        }
        process.destroy()
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            process.waitFor(10, TimeUnit.SECONDS)
        }
    }

    private fun propagateJdbcEnvironment(processBuilder: ProcessBuilder) {
        val environment = processBuilder.environment()
        environment["DATASOURCE_MYSQL_JPA_MAIN_JDBC_URL"] = requireSystemProperty("datasource.mysql-jpa.main.jdbc-url")
        environment["DATASOURCE_MYSQL_JPA_MAIN_USERNAME"] = requireSystemProperty("datasource.mysql-jpa.main.username")
        environment["DATASOURCE_MYSQL_JPA_MAIN_PASSWORD"] = requireSystemProperty("datasource.mysql-jpa.main.password")
        environment["DATASOURCE_REDIS_DATABASE"] = requireSystemProperty("datasource.redis.database")
        environment["DATASOURCE_REDIS_MASTER_HOST"] = requireSystemProperty("datasource.redis.master.host")
        environment["DATASOURCE_REDIS_MASTER_PORT"] = requireSystemProperty("datasource.redis.master.port")
        environment["DATASOURCE_REDIS_REPLICAS_0_HOST"] = requireSystemProperty("datasource.redis.replicas[0].host")
        environment["DATASOURCE_REDIS_REPLICAS_0_PORT"] = requireSystemProperty("datasource.redis.replicas[0].port")
    }

    private fun requireSystemProperty(name: String): String =
        System.getProperty(name) ?: error("missing required system property: $name")

    private fun repoRoot(): Path = Paths.get(System.getProperty("user.dir"))
        .toAbsolutePath()
        .normalize()
        .parent
        .parent
}
