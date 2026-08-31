package com.cloudstorage.repository;

import com.cloudstorage.model.Share;
import com.cloudstorage.model.StoredFile;
import com.cloudstorage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShareRepository extends JpaRepository<Share, Long> {

    List<Share> findByFile(StoredFile file);

    Optional<Share> findByFileAndSharedWith(
            StoredFile file,
            User sharedWith
    );

    Optional<Share> findByFileIdAndSharedWithId(
            Long fileId,
            Long userId
    );

    List<Share> findBySharedWith(User user);

    void deleteById(Long id);

    void deleteByFile(StoredFile file);
}