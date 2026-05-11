package com.normilinet.otklik.web.controller;

import com.normilinet.otklik.domain.enums.FileKind;
import com.normilinet.otklik.domain.enums.Role;
import com.normilinet.otklik.domain.model.ReviewAttachment;
import com.normilinet.otklik.domain.model.WorkAssignment;
import com.normilinet.otklik.domain.model.WorkAttachment;
import com.normilinet.otklik.domain.repository.ReviewAttachmentRepository;
import com.normilinet.otklik.domain.repository.WorkAssignmentRepository;
import com.normilinet.otklik.domain.repository.WorkAttachmentRepository;
import com.normilinet.otklik.security.CustomUserDetails;
import com.normilinet.otklik.service.DocumentRenderService;
import com.normilinet.otklik.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class FileController {

    private final WorkAttachmentRepository workAttachmentRepository;
    private final ReviewAttachmentRepository reviewAttachmentRepository;
    private final WorkAssignmentRepository assignmentRepository;
    private final FileStorageService storage;
    private final DocumentRenderService renderer;

    @GetMapping("/files/work/{id}")
    public ResponseEntity<UrlResource> serveWorkAttachment(@AuthenticationPrincipal CustomUserDetails user,
                                                           @PathVariable UUID id,
                                                           @RequestParam(defaultValue = "inline") String disposition) throws IOException {
        WorkAttachment att = workAttachmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден"));
        if (!hasAccessToWork(user, att)) {
            return ResponseEntity.status(403).build();
        }
        Path file = storage.resolve(att.getStoredPath());
        UrlResource resource = new UrlResource(file.toUri());
        String mime = att.getMimeType() != null ? att.getMimeType() : FileStorageService.guessMime(att.getOriginalFilename());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(disposition, att.getOriginalFilename()))
                .body(resource);
    }

    @GetMapping("/files/work/{id}/preview")
    public String previewWorkAttachment(@AuthenticationPrincipal CustomUserDetails user,
                                        @PathVariable UUID id,
                                        Model model) throws IOException {
        WorkAttachment att = workAttachmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден"));
        if (!hasAccessToWork(user, att)) {
            throw new SecurityException("Доступ запрещён");
        }
        Path file = storage.resolve(att.getStoredPath());
        model.addAttribute("attachment", att);
        model.addAttribute("kind", att.getKind().name());
        if (att.getKind() == FileKind.DOCUMENT && att.getOriginalFilename() != null && att.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            model.addAttribute("pdfUrl", "/files/work/" + id);
            return "viewer/pdf";
        }
        if (att.getKind() == FileKind.IMAGE) {
            model.addAttribute("src", "/files/work/" + id);
            return "viewer/image";
        }
        if (att.getKind() == FileKind.AUDIO) {
            model.addAttribute("src", "/files/work/" + id);
            return "viewer/audio";
        }
        if (att.getKind() == FileKind.VIDEO) {
            model.addAttribute("src", "/files/work/" + id);
            return "viewer/video";
        }
        if (att.getKind() == FileKind.CODE) {
            String content = Files.readString(file);
            model.addAttribute("language", FileStorageService.extension(att.getOriginalFilename()));
            model.addAttribute("content", content);
            return "viewer/code";
        }
        String html = renderer.renderToHtml(file, att.getOriginalFilename());
        model.addAttribute("html", html);
        return "viewer/html";
    }

    @GetMapping("/files/review/{id}")
    public ResponseEntity<UrlResource> serveReviewAttachment(@AuthenticationPrincipal CustomUserDetails user,
                                                             @PathVariable UUID id,
                                                             @RequestParam(defaultValue = "inline") String disposition) throws IOException {
        ReviewAttachment att = reviewAttachmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден"));
        if (!hasAccessToReview(user, att)) {
            return ResponseEntity.status(403).build();
        }
        if (att.getStoredPath() == null) return ResponseEntity.notFound().build();
        Path file = storage.resolve(att.getStoredPath());
        UrlResource resource = new UrlResource(file.toUri());
        String mime = att.getMimeType() != null ? att.getMimeType() : FileStorageService.guessMime(att.getOriginalFilename());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(disposition, att.getOriginalFilename() != null ? att.getOriginalFilename() : "voice.webm"))
                .body(resource);
    }

    private boolean hasAccessToWork(CustomUserDetails user, WorkAttachment att) {
        if (user == null) return false;
        Role role = Role.valueOf(user.getAuthorities().iterator().next().getAuthority());
        if (role == Role.ADMIN) return true;
        UUID userId = user.getId();
        UUID studentId = att.getWork().getStudent().getId();
        UUID organizerId = att.getWork().getCampaign().getOrganizer() != null
                ? att.getWork().getCampaign().getOrganizer().getId() : null;
        if (role == Role.ORGANIZER && organizerId != null && organizerId.equals(userId)) return true;
        if (role == Role.STUDENT && studentId.equals(userId)) return true;
        for (WorkAssignment a : assignmentRepository.findAllByWorkId(att.getWork().getId())) {
            if (a.getReviewer().getId().equals(userId)) return true;
        }
        return false;
    }

    private boolean hasAccessToReview(CustomUserDetails user, ReviewAttachment att) {
        if (user == null) return false;
        Role role = Role.valueOf(user.getAuthorities().iterator().next().getAuthority());
        if (role == Role.ADMIN) return true;
        UUID userId = user.getId();
        var work = att.getReview().getAssignment().getWork();
        if (role == Role.ORGANIZER && work.getCampaign().getOrganizer() != null
                && work.getCampaign().getOrganizer().getId().equals(userId)) return true;
        if (att.getReview().getAssignment().getReviewer().getId().equals(userId)) return true;
        if (role == Role.STUDENT && work.getStudent().getId().equals(userId)) return true;
        return false;
    }

    private String contentDisposition(String disposition, String filename) {
        String name = filename != null ? filename : "file";
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        return disposition + "; filename*=UTF-8''" + encoded;
    }
}
