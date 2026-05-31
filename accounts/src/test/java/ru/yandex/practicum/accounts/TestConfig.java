package ru.yandex.payments;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(WebConfiguration.class)
public class TestConfig {
}
