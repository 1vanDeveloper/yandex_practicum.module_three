package ru.yandex.practicum.notifications.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "notifications", name = "notifications")
public class Notification {

    @Id
    private Long id;

    @Column("login")
    private String login;

    @Column("message")
    private String message;

    @Column("created_at")
    private LocalDateTime createdAt;
}
