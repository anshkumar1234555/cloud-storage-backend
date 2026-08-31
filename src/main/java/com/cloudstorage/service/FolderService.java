package com.cloudstorage.service;

import com.cloudstorage.dto.CreateFolderRequest;
import com.cloudstorage.dto.MoveFolderRequest;
import com.cloudstorage.dto.RenameFolderRequest;
import com.cloudstorage.model.Folder;
import com.cloudstorage.model.StoredFile;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.FolderRepository;
import com.cloudstorage.repository.ShareRepository;
import com.cloudstorage.repository.StoredFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FolderService {

    private final FolderRepository folderRepository;
    private final StoredFileRepository fileRepository;
    private final ShareRepository shareRepository;

    private final Path storageLocation =
            Paths.get("uploads").toAbsolutePath().normalize();

    public FolderService(
            FolderRepository folderRepository,
            StoredFileRepository fileRepository,
            ShareRepository shareRepository) {

        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
        this.shareRepository = shareRepository;
    }

    // =========================================================
    // CREATE FOLDER
    // =========================================================
    public Folder createFolder(
            CreateFolderRequest request,
            User owner) {

        Folder parent = null;

        if (request.getParentId() != null) {

            parent = folderRepository.findById(request.getParentId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Parent folder not found"));

            if (!parent.getOwner().getId()
                    .equals(owner.getId())) {

                throw new RuntimeException(
                        "You do not have access to this folder");
            }

            if (parent.isDeleted()) {

                throw new RuntimeException(
                        "Cannot create folder inside Trash");
            }
        }

        // Prevent duplicate folder names in same location
        if (folderRepository
                .existsByOwnerAndParentAndNameAndDeletedFalse(
                        owner,
                        parent,
                        request.getName())) {

            throw new RuntimeException(
                    "A folder with this name already exists");
        }

        LocalDateTime now = LocalDateTime.now();

        Folder folder = Folder.builder()
                .name(request.getName())
                .owner(owner)
                .parent(parent)
                .createdAt(now)
                .updatedAt(now)
                .deleted(false)
                .build();

        return folderRepository.save(folder);
    }

    // =========================================================
    // GET ROOT FOLDERS
    // =========================================================
    public List<Folder> getRootFolders(User owner) {

        return folderRepository
                .findByOwnerAndParentIsNullAndDeletedFalse(owner);
    }

    // =========================================================
    // GET CHILD FOLDERS
    // =========================================================
    public List<Folder> getChildFolders(
            Long parentId,
            User owner) {

        Folder parent = folderRepository.findById(parentId)
                .orElseThrow(() ->
                        new RuntimeException("Folder not found"));

        if (!parent.getOwner().getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "You do not have access to this folder");
        }

        if (parent.isDeleted()) {

            throw new RuntimeException(
                    "Folder is in Trash");
        }

        return folderRepository
                .findByOwnerAndParentAndDeletedFalse(
                        owner,
                        parent);
    }

    // =========================================================
    // RENAME FOLDER
    // =========================================================
    public Folder renameFolder(
            Long folderId,
            RenameFolderRequest request,
            User owner) {

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() ->
                        new RuntimeException("Folder not found"));

        if (!folder.getOwner().getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "You do not have access to this folder");
        }

        if (folder.isDeleted()) {

            throw new RuntimeException(
                    "Cannot rename a folder in Trash");
        }

        String newName = request.getName();

        // Prevent duplicate name in same parent
        if (!newName.equals(folder.getName())
                && folderRepository
                .existsByOwnerAndParentAndNameAndDeletedFalse(
                        owner,
                        folder.getParent(),
                        newName)) {

            throw new RuntimeException(
                    "A folder with this name already exists");
        }

        folder.setName(newName);
        folder.setUpdatedAt(LocalDateTime.now());

        return folderRepository.save(folder);
    }

    // =========================================================
    // MOVE FOLDER
    // =========================================================
    public Folder moveFolder(
            Long folderId,
            MoveFolderRequest request,
            User owner) {

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() ->
                        new RuntimeException("Folder not found"));

        if (!folder.getOwner().getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "You do not have access to this folder");
        }

        if (folder.isDeleted()) {

            throw new RuntimeException(
                    "Cannot move a folder in Trash");
        }

        Folder newParent = null;

        if (request.getParentId() != null) {

            newParent = folderRepository
                    .findById(request.getParentId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Destination folder not found"));

            // Destination must belong to current user
            if (!newParent.getOwner().getId()
                    .equals(owner.getId())) {

                throw new RuntimeException(
                        "You do not have access to destination folder");
            }

            if (newParent.isDeleted()) {

                throw new RuntimeException(
                        "Cannot move folder into Trash");
            }

            // Cannot move folder into itself
            if (newParent.getId()
                    .equals(folder.getId())) {

                throw new RuntimeException(
                        "A folder cannot be its own parent");
            }

            // Prevent circular folder structure
            if (isDescendant(newParent, folder)) {

                throw new RuntimeException(
                        "Cannot move a folder into its own child folder");
            }
        }

        // Prevent duplicate folder name in destination
        if (folderRepository
                .existsByOwnerAndParentAndNameAndDeletedFalse(
                        owner,
                        newParent,
                        folder.getName())) {

            Folder existingFolder =
                    folderRepository
                            .findByOwnerAndParentAndNameAndDeletedFalse(
                                    owner,
                                    newParent,
                                    folder.getName());

            if (existingFolder != null
                    && !existingFolder.getId()
                    .equals(folder.getId())) {

                throw new RuntimeException(
                        "A folder with this name already exists in destination");
            }
        }

        folder.setParent(newParent);
        folder.setUpdatedAt(LocalDateTime.now());

        return folderRepository.save(folder);
    }

    // =========================================================
    // CHECK DESCENDANT
    // =========================================================
    private boolean isDescendant(
            Folder possibleChild,
            Folder folder) {

        Folder current = possibleChild;

        while (current != null) {

            if (current.getId()
                    .equals(folder.getId())) {

                return true;
            }

            current = current.getParent();
        }

        return false;
    }

    // =========================================================
    // MOVE FOLDER TO TRASH
    // =========================================================
    @Transactional
    public Folder trashFolder(
            Long folderId,
            User owner) {

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() ->
                        new RuntimeException("Folder not found"));

        if (!folder.getOwner().getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "You do not have access to this folder");
        }

        if (folder.isDeleted()) {

            throw new RuntimeException(
                    "Folder is already in Trash");
        }

        LocalDateTime now = LocalDateTime.now();

        // =====================================================
        // MARK CURRENT FOLDER AS DELETED
        // =====================================================
        folder.setDeleted(true);
        folder.setUpdatedAt(now);

        // =====================================================
        // MARK FILES INSIDE CURRENT FOLDER AS DELETED
        // =====================================================
        List<StoredFile> files =
                fileRepository.findByFolder(folder);

        for (StoredFile file : files) {

            if (!file.isDeleted()) {

                file.setDeleted(true);
                file.setUpdatedAt(now);

                fileRepository.save(file);
            }
        }

        // =====================================================
        // MARK CHILD FOLDERS AS DELETED RECURSIVELY
        // =====================================================
        trashChildFolders(folder, now);

        return folderRepository.save(folder);
    }

    // =========================================================
    // TRASH CHILD FOLDERS
    // =========================================================
    private void trashChildFolders(
            Folder parent,
            LocalDateTime now) {

        List<Folder> children =
                folderRepository.findByOwnerAndParent(
                        parent.getOwner(),
                        parent);

        for (Folder child : children) {

            // Mark child folder as deleted
            child.setDeleted(true);
            child.setUpdatedAt(now);

            // Mark files inside child folder as deleted
            List<StoredFile> files =
                    fileRepository.findByFolder(child);

            for (StoredFile file : files) {

                if (!file.isDeleted()) {

                    file.setDeleted(true);
                    file.setUpdatedAt(now);

                    fileRepository.save(file);
                }
            }

            // Recursively process deeper child folders
            trashChildFolders(child, now);

            folderRepository.save(child);
        }
    }

    // =========================================================
    // GET TRASH
    // =========================================================
    public List<Folder> getTrash(User owner) {

        return folderRepository
                .findByOwnerAndDeletedTrue(owner);
    }

    // =========================================================
    // RESTORE FOLDER
    // =========================================================
    @Transactional
    public Folder restoreFolder(
            Long folderId,
            User owner) {

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() ->
                        new RuntimeException("Folder not found"));

        if (!folder.getOwner().getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "You do not have access to this folder");
        }

        if (!folder.isDeleted()) {

            throw new RuntimeException(
                    "Folder is not in Trash");
        }

        LocalDateTime now = LocalDateTime.now();

        // =====================================================
        // RESTORE CURRENT FOLDER
        // =====================================================
        folder.setDeleted(false);
        folder.setUpdatedAt(now);

        // =====================================================
        // RESTORE FILES INSIDE CURRENT FOLDER
        // =====================================================
        List<StoredFile> files =
                fileRepository.findByFolder(folder);

        for (StoredFile file : files) {

            file.setDeleted(false);
            file.setUpdatedAt(now);

            fileRepository.save(file);
        }

        // =====================================================
        // RESTORE CHILD FOLDERS RECURSIVELY
        // =====================================================
        restoreChildFolders(folder, now);

        return folderRepository.save(folder);
    }

    // =========================================================
    // RESTORE CHILD FOLDERS
    // =========================================================
    private void restoreChildFolders(
            Folder parent,
            LocalDateTime now) {

        List<Folder> children =
                folderRepository.findByOwnerAndParent(
                        parent.getOwner(),
                        parent);

        for (Folder child : children) {

            child.setDeleted(false);
            child.setUpdatedAt(now);

            // Restore files inside child folder
            List<StoredFile> files =
                    fileRepository.findByFolder(child);

            for (StoredFile file : files) {

                file.setDeleted(false);
                file.setUpdatedAt(now);

                fileRepository.save(file);
            }

            // Recursively restore deeper child folders
            restoreChildFolders(child, now);

            folderRepository.save(child);
        }
    }

    // =========================================================
    // PERMANENT DELETE FOLDER
    // =========================================================
    @Transactional
    public void permanentlyDeleteFolder(
            Long folderId,
            User owner) {

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() ->
                        new RuntimeException("Folder not found"));

        if (!folder.getOwner().getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "You do not have access to this folder");
        }

        if (!folder.isDeleted()) {

            throw new RuntimeException(
                    "Folder must be in Trash first");
        }

        deleteFolderRecursively(folder);
    }

    // =========================================================
    // RECURSIVE PERMANENT DELETE
    // =========================================================
    private void deleteFolderRecursively(
            Folder folder) {

        // =====================================================
        // DELETE FILES INSIDE THIS FOLDER
        // =====================================================
        List<StoredFile> files =
                fileRepository.findByFolder(folder);

        for (StoredFile file : files) {

            // Delete shares associated with file
            shareRepository.deleteByFile(file);

            // Delete physical file
            deletePhysicalFile(file);

            // Delete file from database
            fileRepository.delete(file);
        }

        fileRepository.flush();

        // =====================================================
        // FIND CHILD FOLDERS
        // =====================================================
        List<Folder> children =
                folderRepository.findByOwnerAndParent(
                        folder.getOwner(),
                        folder);

        // =====================================================
        // DELETE CHILD FOLDERS RECURSIVELY
        // =====================================================
        for (Folder child : children) {

            deleteFolderRecursively(child);
        }

        // =====================================================
        // DELETE CURRENT FOLDER
        // =====================================================
        folderRepository.delete(folder);
        folderRepository.flush();
    }

    // =========================================================
    // DELETE PHYSICAL FILE
    // =========================================================
    private void deletePhysicalFile(
            StoredFile file) {

        Path filePath = storageLocation
                .resolve(file.getStorageKey())
                .normalize();

        try {

            Files.deleteIfExists(filePath);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not delete physical file: "
                            + file.getStorageKey(),
                    e);
        }
    }
}