plugins {
    id("java")
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.springdoc.openapi-gradle-plugin") version "1.8.0"

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
    // 1. Подключаем платформу Spring Boot для управления версиями
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.0"))
    // 2. Подключаем платформу Spring Cloud (для loadbalancer)
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    // Spring Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Spring Cloud LoadBalancer
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")

    // Micrometer Tracing (Zipkin)
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
    implementation("io.zipkin.reporter2:zipkin-sender-okhttp3")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Logstash TCP appender for centralized logging
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Resilience4j Circuit Breaker
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.3.0")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("com.h2database:h2")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.mockito:mockito-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // H2 for tests
    testRuntimeOnly("com.h2database:h2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named("generateOpenApiDocs") {
    mustRunAfter("compileJava")
    mustRunAfter("processResources")
    mustRunAfter("classes")
}

tasks.named("bootJar") {
    mustRunAfter("generateOpenApiDocs")
}

tasks.named("jar") {
    mustRunAfter("generateOpenApiDocs")
}

tasks.named("forkedSpringBootRun") {
    mustRunAfter("compileJava")
    mustRunAfter("processResources")
    mustRunAfter("classes")
    mustRunAfter("compileTestJava")
    mustRunAfter("processTestResources")
    mustRunAfter("testClasses")
    mustRunAfter("test")
}

openApi {
    apiDocsUrl.set("http://localhost:8080/v3/api-docs")
    outputDir.set(file("${project.projectDir}/build"))
    outputFileName.set("openapi.json")
    waitTimeInSeconds.set(30)
}
