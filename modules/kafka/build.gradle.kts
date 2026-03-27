plugins {
    `java-test-fixtures`
}

dependencies {
    api("org.springframework.kafka:spring-kafka")
    api("org.apache.avro:avro:1.11.4")
    api("io.confluent:kafka-avro-serializer:${project.properties["confluentVersion"]}")

    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:kafka")

    testFixturesImplementation("org.testcontainers:kafka")
}
