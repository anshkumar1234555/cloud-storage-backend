package com.cloudstorage.dto;

import jakarta.validation.constraints.NotBlank;

public class RenameFileRequest {

    @NotBlank(message = "File name is required")
    private String name;

    public RenameFileRequest() {
    }

    public RenameFileRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}