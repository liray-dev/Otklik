package com.normilinet.otklik.service;

import com.normilinet.otklik.domain.enums.Role;
import com.normilinet.otklik.domain.model.User;
import com.normilinet.otklik.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final FileStorageService storage;

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }

    @Transactional
    public User updateBasics(String username,
                             String fullName,
                             String phone,
                             String telegram,
                             String aboutMe,
                             String universityGroup) {
        User u = getByUsername(username);
        u.setFullName(trimOrNull(fullName));
        u.setPhone(trimOrNull(phone));
        u.setTelegram(trimOrNull(telegram));
        u.setAboutMe(trimOrNull(aboutMe));
        if (u.getRole() == Role.STUDENT) {
            u.setUniversityGroup(trimOrNull(universityGroup));
        }
        return userRepository.save(u);
    }

    @Transactional
    public User uploadAvatar(String username, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл пуст");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new IllegalArgumentException("Аватар не должен быть больше 5 МБ");
        }
        User u = getByUsername(username);
        FileStorageService.StoredFile stored = storage.store(file, "avatars/" + u.getId());
        if (u.getAvatarPath() != null) {
            try { storage.delete(u.getAvatarPath()); } catch (Exception ignored) {}
        }
        u.setAvatarPath(stored.relativePath());
        return userRepository.save(u);
    }

    @Transactional
    public User deleteAvatar(String username) throws IOException {
        User u = getByUsername(username);
        if (u.getAvatarPath() != null) {
            try { storage.delete(u.getAvatarPath()); } catch (Exception ignored) {}
            u.setAvatarPath(null);
        }
        return userRepository.save(u);
    }

    public boolean isProfileComplete(User u) {
        if (u.getFullName() == null || u.getFullName().isBlank()) return false;
        if (u.getRole() == Role.STUDENT && (u.getUniversityGroup() == null || u.getUniversityGroup().isBlank())) {
            return false;
        }
        return true;
    }

    public Path resolveAvatar(User u) {
        if (u == null || u.getAvatarPath() == null) return null;
        Path p = storage.resolve(u.getAvatarPath());
        return Files.exists(p) ? p : null;
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
