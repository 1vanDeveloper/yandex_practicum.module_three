plugins {
    id("java")
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"

    groovy
}

group = "ru.yandex.practicum"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val springCloudVersion = "2025.1.0"

dependencies {
    // Spring Boot platform
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))

    // Spring Boot - минимальные зависимости для Kafka consumer с actuator
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka")

    // Micrometer Tracing (Zipkin)
    implementation("io.micrometer:micrometer-tracing")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
    implementation("io.zipkin.reporter2:zipkin-sender-okhttp3")
    implementation("io.zipkin.brave:brave")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("org.aspectj:aspectjweaver:1.9.22.1")

    // Database
    runtimeOnly("org.postgresql:postgresql")

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

tasks.named("bootJar") {
    mustRunAfter("compileJava")
    mustRunAfter("processResources")
    mustRunAfter("classes")
}

tasks.named("jar") {
    mustRunAfter("compileJava")
    mustRunAfter("processResources")
    mustRunAfter("classes")
}
