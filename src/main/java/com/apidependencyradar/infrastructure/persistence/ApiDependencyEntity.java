package com.apidependencyradar.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

@Entity
@Table(name = "api_dependencies", indexes = {
        @Index(name = "idx_api_dependencies_created_at", columnList = "created_at")
})
public class ApiDependencyEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUserEntity owner;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "provider_name", length = 120)
    private String providerName;

    @Column(name = "source_url", nullable = false, length = 2048)
    private String sourceUrl;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(length = 1000)
    private String description;

    @Column(name = "baseline_spec", nullable = false, columnDefinition = "text")
    private String baselineSpec;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_check_success")
    private Boolean lastCheckSuccess;

    @Column(name = "last_check_error", columnDefinition = "text")
    private String lastCheckError;

    @Column(name = "next_check_at")
    private Instant nextCheckAt;

    @Version
    private long version;

    protected ApiDependencyEntity() {
    }

    public ApiDependencyEntity(
            String id,
            AppUserEntity owner,
            String name,
            String providerName,
            String sourceUrl,
            String sourceType,
            String description,
            String baselineSpec) {

        this.id = id;
        this.owner = owner;
        this.name = name;
        this.providerName = providerName;
        this.sourceUrl = sourceUrl;
        this.sourceType = sourceType;
        this.description = description;
        this.baselineSpec = baselineSpec;
        this.status = "ACTIVE";
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getDescription() {
        return description;
    }

    public String getBaselineSpec() {
        return baselineSpec;
    }

    public String getStatus() {
        return status;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateBaseline(String baselineSpec) {
        this.baselineSpec = baselineSpec;
    }

    public void markChecked(Instant checkedAt) {
        this.lastCheckedAt = checkedAt;
    }

    public AppUserEntity getOwner() {
        return owner;
    }

    public Boolean getLastCheckSuccess() {
        return lastCheckSuccess;
    }

    public String getLastCheckError() {
        return lastCheckError;
    }

    public Instant getNextCheckAt() {
        return nextCheckAt;
    }

    public void markCheckSucceeded(Instant checkedAt, Instant nextCheck) {
        this.lastCheckedAt = checkedAt;
        this.lastCheckSuccess = true;
        this.lastCheckError = null;
        this.nextCheckAt = nextCheck;
    }

    public void markCheckFailed(String error, Instant nextCheck) {
        this.lastCheckSuccess = false;
        this.lastCheckError = error;
        this.nextCheckAt = nextCheck;
    }
}
