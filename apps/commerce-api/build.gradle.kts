plugins {
    id("org.jetbrains.kotlin.plugin.jpa")
}

dependencies {
    // add-ons
    implementation(project(":modules:jpa"))
    implementation(project(":modules:redis"))
    implementation(project(":supports:jackson"))
    implementation(project(":supports:logging"))
    implementation(project(":supports:monitoring"))

    // web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.properties["springDocOpenApiVersion"]}")
    implementation("org.springframework.security:spring-security-crypto")

    // querydsl
    kapt("com.querydsl:querydsl-apt::jakarta")

    // resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${project.properties["resilience4jVersion"]}")
    implementation("io.github.resilience4j:resilience4j-reactor:${project.properties["resilience4jVersion"]}")
    implementation("io.github.resilience4j:resilience4j-retry:${project.properties["resilience4jVersion"]}")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:${project.properties["resilience4jVersion"]}")
    implementation("io.github.resilience4j:resilience4j-timelimiter:${project.properties["resilience4jVersion"]}")

    // test-fixtures
    testImplementation(testFixtures(project(":modules:jpa")))
    testImplementation(testFixtures(project(":modules:redis")))
}
