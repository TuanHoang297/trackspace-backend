package com.trackspace.github.entity;

import com.trackspace.github.ConnectionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "github_connections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Connection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "connection_id", nullable = false)
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Size(max = 500)
    @NotNull
    @Column(name = "repository_url", nullable = false, length = 500)
    private String repositoryUrl;

    @Size(max = 100)
    @NotNull
    @Column(name = "branch_name", nullable = false, length = 100)
    private String branchName;

    @NotNull
    @Lob
    @Column(name = "access_token_encrypted", nullable = false)
    private String accessTokenEncrypted;

    @ColumnDefault("'DISCONNECTED'")
    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status")
    private ConnectionStatus status;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
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
