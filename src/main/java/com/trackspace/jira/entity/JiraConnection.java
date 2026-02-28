package com.trackspace.jira.entity;

import com.trackspace.jira.JiraConnectionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "jira_connections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JiraConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "connection_id", nullable = false)
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Size(max = 255)
    @NotNull
    @Column(name = "site_url", nullable = false)
    private String siteUrl;

    @Size(max = 255)
    @NotNull
    @Column(name = "email", nullable = false)
    private String email;

    @NotNull
    @Lob
    @Column(name = "api_token_encrypted", nullable = false)
    private String apiTokenEncrypted;

    @Size(max = 50)
    @NotNull
    @Column(name = "project_key", nullable = false, length = 50)
    private String projectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status")
    private JiraConnectionStatus connectionStatus;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
