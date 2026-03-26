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
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.properties["springDocOpenApiVersion"]}")

    // querydsl
    kapt("com.querydsl:querydsl-apt::jakarta")

    // test-fixtures
    testImplementation(testFixtures(project(":modules:jpa")))
    testImplementation(testFixtures(project(":modules:redis")))

    implementation("net.datafaker:datafaker:2.2.2")

    // cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // resilience
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${project.properties["resilience4jVersion"]}")
    implementation("org.springframework.boot:spring-boot-starter-aop")
}
