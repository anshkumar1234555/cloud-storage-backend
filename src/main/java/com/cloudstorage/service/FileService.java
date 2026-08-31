package com.cloudstorage.service;

import com.cloudstorage.dto.MoveFileRequest;
import com.cloudstorage.dto.RenameFileRequest;
import com.cloudstorage.model.Folder;
import com.cloudstorage.model.StoredFile;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.FolderRepository;
import com.cloudstorage.repository.ShareRepository;
import com.cloudstorage.repository.StoredFileRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private final StoredFileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final ShareService shareService;
    private final ShareRepository shareRepository;

    private final Path storageLocation =
            Paths.get("uploads").toAbsolutePath().normalize();

    public FileService(
            StoredFileRepository fileRepository,
            FolderRepository folderRepository,
            ShareService shareService,
            ShareRepository shareRepository) {

        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.shareService = shareService;
        this.shareRepository = shareRepository;

        try {
            Files.createDirectories(storageLocation);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not create upload directory",
                    e
            );
        }
    }

    // =========================================================
    // UPLOAD FILE
    // =========================================================
    public StoredFile uploadFile(
            MultipartFile file,
            Long folderId,
            User owner) {

        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        Folder folder = null;

        if (folderId != null) {

            folder = folderRepository.findById(folderId)
                    .orElseThrow(() ->
                            new RuntimeException("Folder not found"));

            if (!folder.getOwner().getId()
                    .equals(owner.getId())) {

                throw new RuntimeException(
                        "You do not have access to this folder"
                );
            }

            if (folder.isDeleted()) {
                throw new RuntimeException(
                        "Cannot upload to a folder in Trash"
                );
            }
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            originalName = "unknown";
        }

        originalName = Paths.get(originalName)
                .getFileName()
                .toString();

        if (fileRepository
                .existsByOwnerAndFolderAndNameAndDeletedFalse(
                        owner,
                        folder,
                        originalName)) {

            throw new RuntimeException(
                    "A file with this name already exists in this folder"
            );
        }

        String storageKey =
                UUID.randomUUID() + "_" + originalName;

        Path targetLocation =
                storageLocation
                        .resolve(storageKey)
                        .normalize();

        if (!targetLocation.startsWith(storageLocation)) {
            throw new RuntimeException("Invalid file path");
        }

        try {
            Files.copy(
                    file.getInputStream(),
                    targetLocation
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not save file",
                    e
            );
        }

        StoredFile storedFile = StoredFile.builder()
                .name(originalName)
                .originalName(originalName)
                .contentType(
                        file.getContentType() != null
                                ? file.getContentType()
                                : "application/octet-stream"
                )
                .size(file.getSize())
                .storageKey(storageKey)
                .owner(owner)
                .folder(folder)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return fileRepository.save(storedFile);
    }

    // =========================================================
    // GET MY FILES
    // =========================================================
    public List<StoredFile> getMyFiles(User owner) {

        return fileRepository
                .findByOwnerAndDeletedFalse(owner);
    }

    // =========================================================
    // DOWNLOAD FILE
    // =========================================================
    public Resource downloadFile(
            Long fileId,
            User owner) {

        StoredFile storedFile =
                fileRepository.findById(fileId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "File not found"
                                ));

        if (!storedFile.getOwner().getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "You do not have access to this file"
            );
        }

        if (storedFile.isDeleted()) {
            throw new RuntimeException(
                    "File is in Trash"
            );
        }

        Path filePath = storageLocation
                .resolve(storedFile.getStorageKey())
                .normalize();

        if (!Files.exists(filePath)) {
            throw new RuntimeException(
                    "Physical file not found"
            );
        }

        return new FileSystemResource(filePath);
    }

    // =========================================================
    // GET FILE BY ID
    // =========================================================
    public StoredFile getFileById(Long fileId) {

        return fileRepository.findById(fileId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "File not found"
                        ));
    }

    // =========================================================
    // MOVE FILE TO TRASH
    // =========================================================
    public StoredFile trashFile(
            Long fileId,
            User owner) {

        StoredFile file =
                fileRepository.findById(fileId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "File not found"
                                ));

        if (!file.getOwner().getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "You do not have access to this file"
            );
        }

        if (file.isDeleted()) {
            throw new RuntimeException(
                    "File is already in Trash"
            );
        }

        file.setDeleted(true);
        file.setUpdatedAt(LocalDateTime.now());

        return fileRepository.save(file);
    }

    // =========================================================
    // GET TRASH
    // =========================================================
    public List<StoredFile> getTrash(User owner) {

        return fileRepository
                .findByOwnerAndDeletedTrue(owner);
    }

    // =========================================================
    // RESTORE FILE
    // =========================================================
    public StoredFile restoreFile(
            Long fileId,
            User owner) {

        StoredFile file =
                fileRepository.findById(fileId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "File not found"
                                ));

        if (!file.getOwner().getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "You do not have access to this file"
            );
        }

        if (!file.isDeleted()) {
            throw new RuntimeException(
                    "File is not in Trash"
            );
        }

        file.setDeleted(false);
        file.setUpdatedAt(LocalDateTime.now());

        return fileRepository.save(file);
    }

    // =========================================================
    // PERMANENT DELETE FILE
    // =========================================================
    @Transactional
    public void permanentlyDeleteFile(
            Long fileId,
            User owner) {

        StoredFile file =
                fileRepository.findById(fileId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "File not found"
                                ));

        if (!file.getOwner().getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "You do not have access to this file"
            );
        }

        if (!file.isDeleted()) {
            throw new RuntimeException(
                    "File must be in Trash first"
            );
        }

        System.out.println(
                "========== PERMANENT FILE DELETE =========="
        );

        System.out.println(
                "File ID = " + file.getId()
        );

        System.out.println(
                "File Name = " + file.getName()
        );

        System.out.println(
                "Storage Key = " + file.getStorageKey()
        );

        // =====================================================
        // DELETE SHARES FIRST
        // =====================================================
        System.out.println(
                "Deleting shares for file ID = " + file.getId()
        );

        shareRepository.deleteByFile(file);
        shareRepository.flush();

        System.out.println(
                "Shares deleted successfully"
        );

        // =====================================================
        // DELETE PHYSICAL FILE
        // =====================================================
        Path filePath = storageLocation
                .resolve(file.getStorageKey())
                .normalize();

        try {

            Files.deleteIfExists(filePath);

            System.out.println(
                    "Deleted physical file: "
                            + file.getStorageKey()
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not delete physical file: "
                            + file.getStorageKey(),
                    e
            );
        }

        // =====================================================
        // DELETE FILE FROM DATABASE
        // =====================================================
        fileRepository.delete(file);
        fileRepository.flush();

        System.out.println(
                "Deleted file from database: "
                        + file.getId()
        );

        System.out.println(
                "============================================"
        );
    }

    // =========================================================
    // RENAME FILE
    // =========================================================
    public StoredFile renameFile(
            Long fileId,
            RenameFileRequest request,
            User owner) {

        StoredFile file =
                fileRepository.findById(fileId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "File not found"
                                ));

        boolean isOwner =
                file.getOwner().getId()
                        .equals(owner.getId());

        boolean isEditor = false;

        if (!isOwner) {

            try {

                isEditor =
                        shareService.canEdit(
                                fileId,
                                owner
                        );

            } catch (RuntimeException e) {

                isEditor = false;
            }
        }

        if (!isOwner && !isEditor) {

            throw new RuntimeException(
                    "You do not have permission to rename this file"
            );
        }

        if (file.isDeleted()) {

            throw new RuntimeException(
                    "Cannot rename a file in Trash"
            );
        }

        String newName = Paths.get(request.getName())
                .getFileName()
                .toString();

        if (fileRepository
                .existsByOwnerAndFolderAndNameAndDeletedFalse(
                        file.getOwner(),
                        file.getFolder(),
                        newName)) {

            throw new RuntimeException(
                    "A file with this name already exists in this folder"
            );
        }

        file.setName(newName);
        file.setUpdatedAt(LocalDateTime.now());

        return fileRepository.save(file);
    }

    // =========================================================
    // MOVE FILE
    // =========================================================
    public StoredFile moveFile(
            Long fileId,
            MoveFileRequest request,
            User owner) {

        StoredFile file =
                fileRepository.findById(fileId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "File not found"
                                ));

        boolean isOwner =
                file.getOwner().getId()
                        .equals(owner.getId());

        boolean isEditor = false;

        if (!isOwner) {

            try {

                isEditor =
                        shareService.canEdit(
                                fileId,
                                owner
                        );

            } catch (RuntimeException e) {

                isEditor = false;
            }
        }

        if (!isOwner && !isEditor) {

            throw new RuntimeException(
                    "You do not have permission to move this file"
            );
        }

        if (file.isDeleted()) {

            throw new RuntimeException(
                    "Cannot move a file in Trash"
            );
        }

        Folder newFolder = null;

        if (request.getFolderId() != null) {

            newFolder =
                    folderRepository.findById(
                                    request.getFolderId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Folder not found"
                                    ));

            if (!newFolder.getOwner().getId()
                    .equals(file.getOwner().getId())) {

                throw new RuntimeException(
                        "You do not have access to this folder"
                );
            }

            if (newFolder.isDeleted()) {

                throw new RuntimeException(
                        "Cannot move file into Trash folder"
                );
            }
        }

        file.setFolder(newFolder);
        file.setUpdatedAt(LocalDateTime.now());

        return fileRepository.save(file);
    }

    // =========================================================
    // SEARCH FILES
    // =========================================================
    public List<StoredFile> searchFiles(
            String name,
            User owner) {

        return fileRepository
                .findByOwnerAndNameContainingIgnoreCaseAndDeletedFalse(
                        owner,
                        name
                );
    }
}