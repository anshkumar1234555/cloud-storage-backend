package com.cloudstorage.dto;

public class MoveFileRequest {

    private Long folderId;

    public MoveFileRequest() {
    }

    public MoveFileRequest(Long folderId) {
        this.folderId = folderId;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }
}