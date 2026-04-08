plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":modules:redis"))

    testImplementation(testFixtures(project(":modules:redis")))

    testFixturesImplementation(testFixtures(project(":modules:redis")))
    testFixturesImplementation("com.redis:testcontainers-redis")
}
