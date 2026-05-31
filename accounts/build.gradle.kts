plugins {
    id("java")
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.springframework.cloud.contract") version "5.0.0"
    id("org.springdoc.openapi-gradle-plugin") version "1.8.0"
}

group = "ru.yandex.practicum"
version = "1.0-SNAPSHOT"

val springCloudVersion = "2025.1.0"

dependencies {
    // 1. Подключаем платформу Spring Boot для управления версиями (замените 3.4.4 на вашу версию Boot)
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))
    // 2. Подключаем платформу Spring Cloud (для consul-discovery и consul-config)
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.6.0")
    
    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-consul-discovery")
    implementation("org.springframework.cloud:spring-cloud-starter-consul-config")
    
    // Database
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    
    // Spring Cloud Contract
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-verifier")
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")
    testImplementation("io.rest-assured:rest-assured:5.5.0")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Contract tests configuration - we use manual WebTestClient-based tests for WebFlux
contracts {
    packageWithBaseClasses = "ru.yandex.practicum.accounts"
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

// Use only manual contract tests from src/contractTest/java
sourceSets.named("contractTest") {
    java {
        srcDir("src/contractTest/java")
        // Exclude auto-generated tests (they use MockMvc which doesn't work with WebFlux)
        exclude("**/build/generated-test-sources/**")
    }
}

// Clean generated contract tests before compiling
tasks.named("generateContractTests") {
    doLast {
        delete(fileTree("build/generated-test-sources/contractTest/java"))
    }
}
