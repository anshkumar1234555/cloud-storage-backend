package com.cloudstorage.controller;

import com.cloudstorage.dto.CreateFolderRequest;
import com.cloudstorage.dto.FolderResponse;
import com.cloudstorage.dto.MoveFolderRequest;
import com.cloudstorage.dto.RenameFolderRequest;
import com.cloudstorage.model.Folder;
import com.cloudstorage.model.User;
import com.cloudstorage.service.FolderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    // =========================
    // CREATE FOLDER
    // =========================
    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(
            @Valid @RequestBody CreateFolderRequest request,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        Folder folder = folderService.createFolder(
                request,
                user
        );

        FolderResponse response = new FolderResponse(
                folder.getId(),
                folder.getName(),
                folder.getOwner().getId(),
                folder.getParent() != null
                        ? folder.getParent().getId()
                        : null,
                folder.getCreatedAt(),
                folder.getUpdatedAt(),
                folder.isDeleted()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================
    // GET ROOT FOLDERS
    // =========================
    @GetMapping
    public ResponseEntity<List<Folder>> getRootFolders(
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                folderService.getRootFolders(user)
        );
    }

    // =========================
    // GET TRASH
    // =========================
    @GetMapping("/trash")
    public ResponseEntity<List<Folder>> getTrash(
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                folderService.getTrash(user)
        );
    }

    // =========================
    // GET CHILD FOLDERS
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<List<Folder>> getChildFolders(
            @PathVariable Long id,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                folderService.getChildFolders(id, user)
        );
    }

    // =========================
    // RENAME FOLDER
    // =========================
    @PutMapping("/{id}")
    public ResponseEntity<Folder> renameFolder(
            @PathVariable Long id,
            @Valid @RequestBody RenameFolderRequest request,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        Folder folder = folderService.renameFolder(
                id,
                request,
                user
        );

        return ResponseEntity.ok(folder);
    }

    // =========================
    // MOVE FOLDER
    // =========================
    @PutMapping("/{id}/move")
    public ResponseEntity<Folder> moveFolder(
            @PathVariable Long id,
            @RequestBody MoveFolderRequest request,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        Folder folder = folderService.moveFolder(
                id,
                request,
                user
        );

        return ResponseEntity.ok(folder);
    }

    // =========================
    // MOVE FOLDER TO TRASH
    // =========================
    @DeleteMapping("/{id}/trash")
    public ResponseEntity<Folder> trashFolder(
            @PathVariable Long id,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        Folder folder = folderService.trashFolder(
                id,
                user
        );

        return ResponseEntity.ok(folder);
    }

    // =========================
    // RESTORE FOLDER
    // =========================
    @PutMapping("/{id}/restore")
    public ResponseEntity<Folder> restoreFolder(
            @PathVariable Long id,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        Folder folder = folderService.restoreFolder(
                id,
                user
        );

        return ResponseEntity.ok(folder);
    }

    // =========================
    // PERMANENTLY DELETE FOLDER
    // =========================
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanentlyDeleteFolder(
            @PathVariable Long id,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        folderService.permanentlyDeleteFolder(
                id,
                user
        );

        return ResponseEntity.noContent().build();
    }
}