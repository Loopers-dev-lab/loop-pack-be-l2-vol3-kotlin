plugins {
    `java-test-fixtures`
}

dependencies {
    implementation(project(":supports:common"))
    api("org.springframework.boot:spring-boot-starter-data-redis")

    testFixturesImplementation("com.redis:testcontainers-redis")
}
