package com.cloudstorage.repository;

import com.cloudstorage.model.Folder;
import com.cloudstorage.model.StoredFile;
import com.cloudstorage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoredFileRepository
        extends JpaRepository<StoredFile, Long> {

    List<StoredFile> findByOwnerAndDeletedFalse(User owner);

    List<StoredFile> findByOwnerAndFolderAndDeletedFalse(
            User owner,
            Folder folder
    );

    List<StoredFile> findByOwnerAndDeletedTrue(User owner);

    List<StoredFile> findByOwnerAndNameContainingIgnoreCaseAndDeletedFalse(
            User owner,
            String name
    );

    // Used for permanently deleting all files inside a folder
    List<StoredFile> findByFolder(Folder folder);
    boolean existsByOwnerAndFolderAndNameAndDeletedFalse(
            User owner,
            Folder folder,
            String name
    );

}