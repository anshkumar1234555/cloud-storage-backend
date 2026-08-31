package com.cloudstorage.controller;

import com.cloudstorage.model.StoredFile;
import com.cloudstorage.model.User;
import com.cloudstorage.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import com.cloudstorage.dto.RenameFileRequest;
import jakarta.validation.Valid;
import com.cloudstorage.dto.MoveFileRequest;
import com.cloudstorage.service.ShareService;
import com.cloudstorage.model.StoredFile;
import com.cloudstorage.model.User;
import com.cloudstorage.dto.FileSearchResponse;
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final ShareService shareService;


    public FileController(
            FileService fileService,
            ShareService shareService) {

        this.fileService = fileService;
        this.shareService = shareService;
    }
    @GetMapping
    public ResponseEntity<List<StoredFile>> getMyFiles(
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                fileService.getMyFiles(user)
        );
    }

    @PostMapping("/upload")
    public ResponseEntity<StoredFile> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false)
            Long folderId,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        StoredFile storedFile =
                fileService.uploadFile(
                        file,
                        folderId,
                        user
                );

        return ResponseEntity.ok(storedFile);
    }
    @GetMapping("/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(
            @PathVariable Long id,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        StoredFile storedFile = fileService.getFileById(id);

        org.springframework.core.io.Resource resource =
                fileService.downloadFile(id, user);

        return ResponseEntity.ok()
                .header(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                storedFile.getOriginalName() + "\""
                )
                .contentType(
                        org.springframework.http.MediaType.parseMediaType(
                                storedFile.getContentType()
                        )
                )
                .body(resource);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<StoredFile> trashFile(
            @PathVariable Long id,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                fileService.trashFile(id, user)
        );
    }
    @GetMapping("/trash")
    public ResponseEntity<List<StoredFile>> getTrash(
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                fileService.getTrash(user)
        );
    }
    @PutMapping("/{id}/restore")
    public ResponseEntity<StoredFile> restoreFile(
            @PathVariable Long id,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                fileService.restoreFile(id, user)
        );
    }
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<String> permanentlyDeleteFile(
            @PathVariable Long id,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        fileService.permanentlyDeleteFile(id, user);

        return ResponseEntity.ok(
                "File permanently deleted");
    }
    @PutMapping("/{id}/rename")
    public ResponseEntity<StoredFile> renameFile(
            @PathVariable Long id,
            @Valid @RequestBody RenameFileRequest request,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                fileService.renameFile(id, request, user)
        );
    }
    @PutMapping("/{id}/move")
    public ResponseEntity<StoredFile> moveFile(
            @PathVariable Long id,
            @RequestBody MoveFileRequest request,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                fileService.moveFile(id, request, user)
        );
    }
    @GetMapping("/{id}/shared-download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadSharedFile(
            @PathVariable Long id,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        // Check that this user has access to the file
        shareService.getShare(id, user);

        StoredFile storedFile = fileService.getFileById(id);

        org.springframework.core.io.Resource resource =
                fileService.downloadFile(
                        id,
                        storedFile.getOwner()
                );

        return ResponseEntity.ok()
                .header(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                storedFile.getOriginalName() + "\""
                )
                .contentType(
                        org.springframework.http.MediaType.parseMediaType(
                                storedFile.getContentType()
                        )
                )
                .body(resource);
    }
    @GetMapping("/{id}")
    public ResponseEntity<StoredFile> getFileById(
            @PathVariable Long id,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        StoredFile file = fileService.getFileById(id);

        if (!file.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You do not have access to this file");
        }

        return ResponseEntity.ok(file);
    }
    @GetMapping("/search")
    public ResponseEntity<List<FileSearchResponse>> searchFiles(
            @RequestParam String name,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        System.out.println("========== SEARCH DEBUG ==========");
        System.out.println("JWT User ID = " + user.getId());
        System.out.println("JWT User Email = " + user.getEmail());
        System.out.println("Search Name = " + name);
        System.out.println("=================================");

        List<StoredFile> files =
                fileService.searchFiles(name, user);

        List<FileSearchResponse> response =
                files.stream()
                        .map(file -> new FileSearchResponse(
                                file.getId(),
                                file.getName(),
                                file.getOriginalName(),
                                file.getContentType(),
                                file.getSize(),
                                file.getFolder() != null
                                        ? file.getFolder().getId()
                                        : null,
                                file.isDeleted()
                        ))
                        .toList();

        return ResponseEntity.ok(response);
    }
}