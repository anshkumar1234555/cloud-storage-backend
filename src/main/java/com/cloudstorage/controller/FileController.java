package com.cloudstorage.controller;

import com.cloudstorage.dto.FileSearchResponse;
import com.cloudstorage.dto.MoveFileRequest;
import com.cloudstorage.dto.RenameFileRequest;
import com.cloudstorage.model.StoredFile;
import com.cloudstorage.model.User;
import com.cloudstorage.service.FileService;
import com.cloudstorage.service.ShareService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    // =========================================================
    // GET MY FILES
    // GET /api/files
    // GET /api/files?folderId=19
    // =========================================================

    @GetMapping
    public ResponseEntity<List<StoredFile>> getMyFiles(
            @RequestParam(
                    value = "folderId",
                    required = false
            )
            Long folderId,

            Authentication authentication) {

        User user =
                (User) authentication.getPrincipal();

        // If folderId is provided
        if (folderId != null) {

            return ResponseEntity.ok(
                    fileService.getFilesInFolder(
                            folderId,
                            user
                    )
            );
        }

        // Otherwise get root files
        return ResponseEntity.ok(
                fileService.getMyFiles(user)
        );
    }

    // =========================================================
    // UPLOAD FILE
    // POST /api/files/upload
    // =========================================================

    @PostMapping("/upload")
    public ResponseEntity<StoredFile> uploadFile(
            @RequestParam("file")
            MultipartFile file,

            @RequestParam(
                    value = "folderId",
                    required = false
            )
            Long folderId,

            Authentication authentication) {

        User user =
                (User) authentication.getPrincipal();

        StoredFile storedFile =
                fileService.uploadFile(
                        file,
                        folderId,
                        user
                );

        return ResponseEntity.ok(storedFile);
    }

    // =========================================================
    // DOWNLOAD MY FILE
    // GET /api/files/{id}/download
    // =========================================================

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long id,
            Authentication authentication) {

        User user =
                (User) authentication.getPrincipal();

        StoredFile storedFile =
                fileService.getFileById(id);

        Resource resource =
                fileService.downloadFile(
                        id,
                        user
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                storedFile.getOriginalName() +
                                "\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                storedFile.getContentType()
                        )
                )
                .body(resource);
    }

    // =========================================================
    // MOVE FILE TO TRASH
    // DELETE /api/files/{id}
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<StoredFile> trashFile(
            @PathVariable Long id,
            Authentication authentication) {

        User user =
                (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                fileService.trashFile(
                        id,
                        user
                )
        );
    }

    // =========================================================
    // GET TRASH
    // GET /api/files/trash
    // =========================================================

    @GetMapping("/trash")
    public ResponseEntity<List<StoredFile>> getTrash(
            Authentication authentication) {

        User user =
                (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                fileService.getTrash(user)
        );
    }

    // =========================================================
    // RESTORE FILE
    // PUT /api/files/{id}/restore
    // =========================================================

    @PutMapping("/{id}/restore")
    public ResponseEntity<StoredFile> restoreFile(
            @PathVariable Long id,
            Authentication authentication) {

        User user =
                (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                fileService.restoreFile(
                        id,
                        user
                )
        );
    }

    // =========================================================
    // PERMANENT DELETE
    // DELETE /api/files/{id}/permanent
    // =========================================================

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<String> permanentlyDeleteFile(
            @PathVariable Long id,
            Authentication authentication) {

        User user =
                (User) authentication.getPrincipal();

        fileService.permanentlyDeleteFile(
                id,
                user
        );

        return ResponseEntity.ok(
                "File permanently deleted"
        );
    }

    // =========================================================
    // RENAME FILE
    // PUT /api/files/{id}/rename
    // =========================================================

    @PutMapping("/{id}/rename")
    public ResponseEntity<StoredFile> renameFile(
            @PathVariable Long id,

            @Valid
            @RequestBody
            RenameFileRequest request,

            Authentication authentication) {

        User user =
                (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                fileService.renameFile(
                        id,
                        request,
                        user
                )
        );
    }

    // =========================================================
    // RENAME FILE BY ID
    // PUT /api/files/{id}
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<StoredFile> renameFileById(
            @PathVariable Long id,

            @Valid
            @RequestBody
            RenameFileRequest request,

            Authentication authentication) {

        User user =
                (User) authentication.getPrincipal();

        StoredFile file =
                fileService.renameFile(
                        id,
                        request,
                        user
                );

        return ResponseEntity.ok(file);
    }

    // =========================================================
    // MOVE FILE
    // PUT /api/files/{id}/move
    // =========================================================

    @PutMapping("/{id}/move")
    public ResponseEntity<StoredFile> moveFile(
            @PathVariable Long id,

            @RequestBody
            MoveFileRequest request,

            Authentication authentication) {

        User user =
                (User) authentication.getPrincipal();

        StoredFile file =
                fileService.moveFile(
                        id,
                        request,
                        user
                );

        return ResponseEntity.ok(file);
    }

    // =========================================================
    // DOWNLOAD SHARED FILE
    // GET /api/files/{id}/shared-download
    // =========================================================

    @GetMapping("/{id}/shared-download")
    public ResponseEntity<Resource> downloadSharedFile(
            @PathVariable Long id,
            Authentication authentication) {

        User user =
                (User) authentication.getPrincipal();

        // Verify that user has access
        shareService.getShare(
                id,
                user
        );

        StoredFile storedFile =
                fileService.getFileById(id);

        Resource resource =
                fileService.downloadFile(
                        id,
                        storedFile.getOwner()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                storedFile.getOriginalName() +
                                "\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                storedFile.getContentType()
                        )
                )
                .body(resource);
    }

    // =========================================================
    // GET FILE BY ID
    // GET /api/files/{id}
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<StoredFile> getFileById(
            @PathVariable Long id,
            Authentication authentication) {

        User user =
                (User) authentication.getPrincipal();

        StoredFile file =
                fileService.getFileById(id);

        if (!file.getOwner().getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You do not have access to this file"
            );
        }

        return ResponseEntity.ok(file);
    }

    // =========================================================
    // SEARCH FILES
    // GET /api/files/search?name=test
    // =========================================================

    @GetMapping("/search")
    public ResponseEntity<List<FileSearchResponse>> searchFiles(
            @RequestParam String name,
            Authentication authentication) {

        User user =
                (User) authentication.getPrincipal();

        System.out.println(
                "========== SEARCH DEBUG =========="
        );

        System.out.println(
                "JWT User ID = " +
                        user.getId()
        );

        System.out.println(
                "JWT User Email = " +
                        user.getEmail()
        );

        System.out.println(
                "Search Name = " +
                        name
        );

        System.out.println(
                "================================="
        );

        List<StoredFile> files =
                fileService.searchFiles(
                        name,
                        user
                );

        List<FileSearchResponse> response =
                files.stream()
                        .map(file ->
                                new FileSearchResponse(
                                        file.getId(),
                                        file.getName(),
                                        file.getOriginalName(),
                                        file.getContentType(),
                                        file.getSize(),
                                        file.getFolder() != null
                                                ? file.getFolder().getId()
                                                : null,
                                        file.isDeleted()
                                )
                        )
                        .toList();

        return ResponseEntity.ok(response);
    }
}