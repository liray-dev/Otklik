package com.normilinet.otklik.web.controller;

import com.normilinet.otklik.domain.enums.Role;
import com.normilinet.otklik.domain.model.User;
import com.normilinet.otklik.security.CustomUserDetails;
import com.normilinet.otklik.service.FileStorageService;
import com.normilinet.otklik.service.ProfileService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/profile")
    public String me(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        User u = profileService.getByUsername(user.getUsername());
        model.addAttribute("user", u);
        model.addAttribute("isMe", true);
        model.addAttribute("isStudent", u.getRole() == Role.STUDENT);
        model.addAttribute("isComplete", profileService.isProfileComplete(u));
        return "profile/me";
    }

    @PostMapping("/profile")
    public String save(@AuthenticationPrincipal CustomUserDetails user,
                       @RequestParam(required = false) String fullName,
                       @RequestParam(required = false) String phone,
                       @RequestParam(required = false) String telegram,
                       @RequestParam(required = false) String aboutMe,
                       @RequestParam(required = false) String universityGroup) {
        profileService.updateBasics(user.getUsername(), fullName, phone, telegram, aboutMe, universityGroup);
        return "redirect:/profile?saved";
    }

    @PostMapping("/profile/avatar")
    public String uploadAvatar(@AuthenticationPrincipal CustomUserDetails user,
                               @RequestParam("avatar") MultipartFile avatar) throws IOException {
        profileService.uploadAvatar(user.getUsername(), avatar);
        return "redirect:/profile?saved";
    }

    @PostMapping("/profile/avatar/delete")
    public String deleteAvatar(@AuthenticationPrincipal CustomUserDetails user) throws IOException {
        profileService.deleteAvatar(user.getUsername());
        return "redirect:/profile?saved";
    }

    @GetMapping("/profile/{userId}")
    public String view(@AuthenticationPrincipal CustomUserDetails me,
                       @PathVariable UUID userId,
                       Model model) {
        User u = profileService.getById(userId);
        model.addAttribute("user", u);
        model.addAttribute("isMe", me.getUsername().equals(u.getUsername()));
        model.addAttribute("isStudent", u.getRole() == Role.STUDENT);
        return "profile/view";
    }

    @GetMapping("/profile/{userId}/avatar")
    public ResponseEntity<UrlResource> avatar(@PathVariable UUID userId) throws IOException {
        User u = profileService.getById(userId);
        Path p = profileService.resolveAvatar(u);
        if (p == null) return ResponseEntity.notFound().build();
        UrlResource resource = new UrlResource(p.toUri());
        String mime = FileStorageService.guessMime(p.getFileName().toString());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
    }
}
