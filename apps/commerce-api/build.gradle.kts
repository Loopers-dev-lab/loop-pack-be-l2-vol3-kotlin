import org.gradle.api.tasks.SourceSetContainer

plugins {
    id("org.jetbrains.kotlin.plugin.jpa")
}

val commerceStreamerMainOutput =
    rootProject.project(":apps:commerce-streamer")
        .extensions
        .getByType(SourceSetContainer::class.java)
        .named("main")
        .get()
        .output

dependencies {
    // add-ons
    implementation(project(":modules:jpa"))
    implementation(project(":modules:kafka"))
    implementation(project(":modules:redis"))
    implementation(project(":supports:jackson"))
    implementation(project(":supports:logging"))
    implementation(project(":supports:monitoring"))

    // web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.properties["springDocOpenApiVersion"]}")
    implementation("io.github.resilience4j:resilience4j-spring-boot3")

    // security (for BCrypt password encoding)
    implementation("org.springframework.security:spring-security-crypto")

    // querydsl
    kapt("com.querydsl:querydsl-apt::jakarta")

    // test-fixtures
    testImplementation(files(commerceStreamerMainOutput))
    testImplementation(testFixtures(project(":modules:jpa")))
    testImplementation(testFixtures(project(":modules:kafka")))
    testImplementation(testFixtures(project(":modules:redis")))
}
