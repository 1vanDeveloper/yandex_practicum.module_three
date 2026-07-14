plugins {
    id("java")
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.yandex.practicum"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val springCloudVersion = "2025.0.0"

dependencies {
    // Spring Boot BOM
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.0"))
    // Spring Cloud BOM - 2025.0.0 с улучшенной поддержкой tracing
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion"))
    // Spring Cloud Gateway (WebFlux-based)
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")

    // Spring Cloud LoadBalancer
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")

    // Resilience4j Circuit Breaker
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // Actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Micrometer Tracing (Zipkin) - для WebFlux
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
    implementation("io.zipkin.reporter2:zipkin-sender-okhttp3")
    implementation("io.micrometer:micrometer-observation")
    implementation("io.micrometer:context-propagation")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    // Spring Cloud Contract for stubs
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
}
