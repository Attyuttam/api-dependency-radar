package com.apidependencyradar.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "api_changes", indexes = {
        @Index(name = "idx_api_changes_dependency_detected", columnList = "dependency_id, detected_at")
})
public class ApiChangeEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependency_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_api_changes_dependency"))
    private ApiDependencyEntity dependency;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "change_type", nullable = false, length = 80)
    private String type;

    @Column(length = 500)
    private String endpoint;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private boolean breaking;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;

    protected ApiChangeEntity() {
    }

    public ApiChangeEntity(String id, ApiDependencyEntity dependency, String sourceType, String type,
            String endpoint, String description, boolean breaking) {
        this.id = id;
        this.dependency = dependency;
        this.sourceType = sourceType;
        this.type = type;
        this.endpoint = endpoint;
        this.description = description;
        this.breaking = breaking;
    }

    @PrePersist
    void onCreate() {
        if (detectedAt == null) detectedAt = Instant.now();
    }

    public String getId() { return id; }
    public ApiDependencyEntity getDependency() { return dependency; }
    public String getSourceType() { return sourceType; }
    public String getType() { return type; }
    public String getEndpoint() { return endpoint; }
    public String getDescription() { return description; }
    public boolean isBreaking() { return breaking; }
    public Instant getDetectedAt() { return detectedAt; }
}
