package ru.yandex.practicum.transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class TransferApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransferApplication.class, args);
    }
}
