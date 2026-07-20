plugins {
    id("java")
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.flywaydb.flyway") version "10.17.0"

    groovy
}

group = "ru.yandex.practicum"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val springCloudVersion = "2024.0.0"

dependencies {
    // Spring Boot platform
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))

    // Spring Boot - минимальные зависимости для Kafka consumer с actuator
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka")

    // Micrometer Tracing (Zipkin)
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
    implementation("io.zipkin.reporter2:zipkin-sender-okhttp3")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Logstash TCP appender for centralized logging
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-core:11.10.4")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:11.10.4")
    testImplementation("com.h2database:h2")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mustRunAfter("compileJava")
    mustRunAfter("processResources")
    mustRunAfter("classes")
}

tasks.named("jar") {
    mustRunAfter("compileJava")
    mustRunAfter("processResources")
    mustRunAfter("classes")
}

// Copy Flyway migrations to resources
tasks.named<ProcessResources>("processResources") {
    from("src/main/resources/db/migration")
    into("build/resources/main/db/migration")
}
