plugins {
    id("java")
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.yandex.practicum"
version = "1.0-SNAPSHOT"

val springCloudVersion = "2025.1.0"

dependencies {
    // Spring Boot BOM
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))
    // Spring Cloud BOM
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.cloud:spring-cloud-starter-consul-discovery")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
    implementation("org.apache.commons:commons-lang3:3.14.0")
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    testImplementation("org.postgresql:postgresql:42.7.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.register("prepareKotlinBuildScriptModel") {}

tasks.test {
    useJUnitPlatform()
}