package com.cloudstorage.repository;

import com.cloudstorage.model.Folder;
import com.cloudstorage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FolderRepository
        extends JpaRepository<Folder, Long> {

    // =========================
    // ROOT FOLDERS
    // =========================
    List<Folder> findByOwnerAndParentIsNullAndDeletedFalse(
            User owner
    );

    // =========================
    // CHILD FOLDERS
    // =========================
    List<Folder> findByOwnerAndParentAndDeletedFalse(
            User owner,
            Folder parent
    );

    // =========================
    // TRASH
    // =========================
    List<Folder> findByOwnerAndDeletedTrue(
            User owner
    );

    // =========================
    // CHILD FOLDERS INCLUDING TRASH
    // Used for recursive permanent deletion
    // =========================
    List<Folder> findByOwnerAndParent(
            User owner,
            Folder parent
    );

    // =========================
    // CHECK DUPLICATE FOLDER NAME
    // =========================
    boolean existsByOwnerAndParentAndNameAndDeletedFalse(
            User owner,
            Folder parent,
            String name
    );

    // =========================
    // FIND FOLDER BY NAME
    // Used when moving folders
    // =========================
    Folder findByOwnerAndParentAndNameAndDeletedFalse(
            User owner,
            Folder parent,
            String name
    );
}