package com.spimex.user.domain.model.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    private UUID id;

    @Column(unique = true, length = 255)
    private String login;

    @Column(unique = true, length = 320)
    private String email;

    @Column(nullable = false, length = 500)
    private String fio;

    @Column(name = "short_name", nullable = false, length = 255)
    private String shortName;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
