package ru.yandex.practicum.accounts.service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.accounts.client.NotificationsClient;
import ru.yandex.practicum.accounts.dto.NotificationRequest;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@TestConfiguration
public class TestOutboxConfig {

    @Bean
    public DiscoveryClient testDiscoveryClient() {
        return new DiscoveryClient() {
            @Override
            public String description() {
                return "Test Discovery Client";
            }

            @Override
            public List<ServiceInstance> getInstances(String serviceId) {
                if ("notifications-service".equals(serviceId)) {
                    return Collections.singletonList(new ServiceInstance() {
                        @Override
                        public String getServiceId() {
                            return "notifications-service";
                        }

                        @Override
                        public String getHost() {
                            return "localhost";
                        }

                        @Override
                        public int getPort() {
                            return 8083;
                        }

                        @Override
                        public boolean isSecure() {
                            return false;
                        }

                        @Override
                        public URI getUri() {
                            return URI.create("http://localhost:8083");
                        }

                        @Override
                        public Map<String, String> getMetadata() {
                            return Collections.emptyMap();
                        }
                    });
                }
                return Collections.emptyList();
            }

            @Override
            public List<String> getServices() {
                return Collections.singletonList("notifications-service");
            }
        };
    }

    @Bean
    @Primary
    public NotificationsClient testNotificationsClient() {
        return new NotificationsClient(RestClient.create()) {
            @Override
            public CompletableFuture<Void> sendNotification(String notificationsUrl, NotificationRequest request) {
                // Mock successful notification send
                return CompletableFuture.completedFuture(null);
            }
        };
    }
}
