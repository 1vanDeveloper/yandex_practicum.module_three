plugins {
    id("java")
    id("org.springframework.boot") version "3.4.4"
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
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))
    // Spring Cloud BOM
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion"))
    // Spring Cloud Gateway (WebFlux-based)
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    
    // Service Discovery
    implementation("org.springframework.cloud:spring-cloud-starter-consul-discovery")
    
    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    
    // Actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    
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
