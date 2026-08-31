package com.cloudstorage.controller;

import com.cloudstorage.dto.CreateShareRequest;
import com.cloudstorage.model.Share;
import com.cloudstorage.model.User;
import com.cloudstorage.service.ShareService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @PostMapping("/{fileId}/share")
    public ResponseEntity<Share> shareFile(
            @PathVariable Long fileId,
            @Valid @RequestBody CreateShareRequest request,
            Authentication authentication) {

        User owner = (User) authentication.getPrincipal();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        shareService.shareFile(
                                fileId,
                                request.getEmail(),
                                request.getRole(),
                                owner
                        )
                );
    }

    @GetMapping("/{fileId}/shares")
    public ResponseEntity<List<Share>> getFileShares(
            @PathVariable Long fileId,
            Authentication authentication) {

        User owner = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                shareService.getFileShares(
                        fileId,
                        owner
                )
        );
    }

    @GetMapping("/shared-with-me")
    public ResponseEntity<List<Share>> getSharedWithMe(
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                shareService.getSharedWithMe(user)
        );
    }

    @DeleteMapping("/{fileId}/share/{shareId}")
    public ResponseEntity<String> revokeShare(
            @PathVariable Long fileId,
            @PathVariable Long shareId,
            Authentication authentication) {

        User owner = (User) authentication.getPrincipal();

        shareService.revokeShare(
                fileId,
                shareId,
                owner
        );

        return ResponseEntity.ok(
                "Share revoked successfully"
        );
    }
    @PutMapping("/{fileId}/share/{shareId}")
    public ResponseEntity<Share> updateShareRole(
            @PathVariable Long fileId,
            @PathVariable Long shareId,
            @RequestParam String role,
            Authentication authentication) {

        User owner = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                shareService.updateShareRole(
                        fileId,
                        shareId,
                        role,
                        owner
                )
        );
    }
}