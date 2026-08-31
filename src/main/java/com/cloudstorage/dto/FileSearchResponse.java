package com.cloudstorage.dto;

public record FileSearchResponse(
        Long id,
        String name,
        String originalName,
        String contentType,
        Long size,
        Long folderId,
        boolean deleted
) {
}