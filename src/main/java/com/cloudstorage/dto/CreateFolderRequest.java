package com.cloudstorage.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateFolderRequest {

    @NotBlank(message = "Folder name is required")
    private String name;

    private Long parentId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}