package com.cloudstorage.service;

import com.cloudstorage.model.Share;
import com.cloudstorage.model.StoredFile;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.ShareRepository;
import com.cloudstorage.repository.StoredFileRepository;
import com.cloudstorage.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShareService {

    private final ShareRepository shareRepository;
    private final StoredFileRepository fileRepository;
    private final UserRepository userRepository;

    public ShareService(
            ShareRepository shareRepository,
            StoredFileRepository fileRepository,
            UserRepository userRepository) {

        this.shareRepository = shareRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
    }

    public Share shareFile(
            Long fileId,
            String email,
            String role,
            User owner) {

        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() ->
                        new RuntimeException("File not found"));

        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException(
                    "Only the file owner can share this file");
        }

        if (file.isDeleted()) {
            throw new RuntimeException(
                    "Cannot share a file in Trash");
        }

        User sharedWith = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User with this email not found"));

        if (sharedWith.getId().equals(owner.getId())) {
            throw new RuntimeException(
                    "You cannot share a file with yourself");
        }

        Share existingShare =
                shareRepository.findByFileAndSharedWith(
                        file,
                        sharedWith
                ).orElse(null);

        if (existingShare != null) {
            existingShare.setRole(role.toUpperCase());

            return shareRepository.save(existingShare);
        }

        Share share = Share.builder()
                .file(file)
                .sharedWith(sharedWith)
                .role(role.toUpperCase())
                .createdAt(LocalDateTime.now())
                .build();

        return shareRepository.save(share);
    }

    public List<Share> getFileShares(
            Long fileId,
            User owner) {

        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() ->
                        new RuntimeException("File not found"));

        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException(
                    "Only the file owner can view shares");
        }

        return shareRepository.findByFile(file);
    }

    public List<Share> getSharedWithMe(User user) {
        return shareRepository.findBySharedWith(user);
    }

    public Share getShare(
            Long fileId,
            User user) {

        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() ->
                        new RuntimeException("File not found"));

        if (file.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "Owner does not need a share");
        }

        return shareRepository
                .findByFileIdAndSharedWithId(
                        fileId,
                        user.getId()
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "You do not have access to this file"));
    }

    public boolean canEdit(
            Long fileId,
            User user) {

        System.out.println("========== CAN EDIT DEBUG ==========");

        System.out.println("File ID = " + fileId);
        System.out.println("Current User ID = " + user.getId());
        System.out.println("Current User Email = " + user.getEmail());

        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() ->
                        new RuntimeException("File not found"));

        System.out.println(
                "File Owner ID = " +
                        file.getOwner().getId());

        System.out.println(
                "File Owner Email = " +
                        file.getOwner().getEmail());

        if (file.getOwner().getId().equals(user.getId())) {

            System.out.println("User is OWNER");
            System.out.println("====================================");

            return true;
        }

        Share share =
                shareRepository.findByFileAndSharedWith(
                        file,
                        user
                ).orElse(null);

        if (share == null) {

            System.out.println("SHARE NOT FOUND");

            System.out.println("====================================");

            return false;
        }

        System.out.println(
                "Share ID = " +
                        share.getId());

        System.out.println(
                "Shared User ID = " +
                        share.getSharedWith().getId());

        System.out.println(
                "Shared User Email = " +
                        share.getSharedWith().getEmail());

        System.out.println(
                "Share Role = [" +
                        share.getRole() +
                        "]");

        boolean editor =
                "EDITOR".equalsIgnoreCase(
                        share.getRole());

        System.out.println(
                "Is Editor = " +
                        editor);

        System.out.println("====================================");

        return editor;
    }

    public boolean canView(
            Long fileId,
            User user) {

        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() ->
                        new RuntimeException("File not found"));

        if (file.getOwner().getId().equals(user.getId())) {
            return true;
        }

        return shareRepository
                .findByFileAndSharedWith(file, user)
                .isPresent();
    }

    public void revokeShare(
            Long fileId,
            Long shareId,
            User owner) {

        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() ->
                        new RuntimeException("File not found"));

        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException(
                    "Only the file owner can revoke a share");
        }

        Share share = shareRepository.findById(shareId)
                .orElseThrow(() ->
                        new RuntimeException("Share not found"));

        if (!share.getFile().getId().equals(fileId)) {
            throw new RuntimeException(
                    "Share does not belong to this file");
        }

        shareRepository.delete(share);
    }
    public Share updateShareRole(
            Long fileId,
            Long shareId,
            String role,
            User owner) {

        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() ->
                        new RuntimeException("File not found"));

        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException(
                    "Only the file owner can update a share");
        }

        Share share = shareRepository.findById(shareId)
                .orElseThrow(() ->
                        new RuntimeException("Share not found"));

        if (!share.getFile().getId().equals(fileId)) {
            throw new RuntimeException(
                    "Share does not belong to this file");
        }

        if (!role.equalsIgnoreCase("VIEWER")
                && !role.equalsIgnoreCase("EDITOR")) {
            throw new RuntimeException(
                    "Role must be VIEWER or EDITOR");
        }

        share.setRole(role.toUpperCase());

        return shareRepository.save(share);
    }


}