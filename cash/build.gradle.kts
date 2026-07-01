plugins {
    id("java")
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.springframework.cloud.contract") version "5.0.1"
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

val springCloudVersion = "2025.1.0"

dependencies {
    // 1. Подключаем платформу Spring Boot для управления версиями
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))
    // 2. Подключаем платформу Spring Cloud
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")

    // Spring Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Spring Cloud LoadBalancer
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")

    // Resilience4j Circuit Breaker
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.3.0")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("com.h2database:h2")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    // Spring Cloud Contract
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.mockito:mockito-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")
    contractTestImplementation("org.springframework.cloud:spring-cloud-starter-contract-verifier")
    contractTestImplementation("org.springframework.boot:spring-boot-starter-test")
    contractTestImplementation("org.springframework.boot:spring-boot-test")
    contractTestImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    contractTestImplementation("org.springframework.security:spring-security-test")
    contractTestImplementation("io.rest-assured:rest-assured:5.5.0")
    contractTestImplementation("org.apache.groovy:groovy:4.0.22")
    contractTestImplementation("org.apache.groovy:groovy-json:4.0.22")
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

contracts {
    testMode.set(org.springframework.cloud.contract.verifier.config.TestMode.EXPLICIT)
    contractsDslDir.set(file("src/contractTest/resources/contracts"))
    basePackageForTests.set("ru.yandex.practicum.cash")
    baseClassForTests.set("ru.yandex.practicum.cash.ContractVerifierBase")
}

// Настраиваем sourceSets, чтобы добавить Groovy-файлы в область видимости контрактных тестов
sourceSets.named("contractTest") {
    java {
        srcDir("src/contractTest/java")

        // ДОБАВЛЯЕМ (не заменяя) путь к сгенерированным плагином тестам
        srcDir("build/generated-test-sources/contractTest/java")
    }
}

tasks.named<Test>("contractTest") {
    // 1. Указываем Gradle использовать движок JUnit 5 для запуска сгенерированных тестов
    useJUnitPlatform()

    // 2. (Опционально) Показывает лог запуска каждого теста прямо в консоли
    testLogging {
        events("passed", "skipped", "failed")
    }
}
