package com.cloudstorage.dto;

import java.time.LocalDateTime;

public class FolderResponse {

    private Long id;
    private String name;
    private Long ownerId;
    private Long parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;

    public FolderResponse(
            Long id,
            String name,
            Long ownerId,
            Long parentId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            boolean deleted) {

        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.parentId = parentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Long getParentId() {
        return parentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }
}