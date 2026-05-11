package com.normilinet.otklik.service;

import com.normilinet.otklik.domain.model.Notification;
import com.normilinet.otklik.domain.model.User;
import com.normilinet.otklik.domain.repository.NotificationRepository;
import com.normilinet.otklik.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification push(User user, String type, String message, String link) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setMessage(message);
        n.setLink(link);
        n.setRead(false);
        return notificationRepository.save(n);
    }

    @Transactional
    public Notification push(UUID userId, String type, String message, String link) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        return push(u, type, message, link);
    }

    @Transactional(readOnly = true)
    public List<Notification> list(String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(u.getId());
    }

    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        return userRepository.findByUsername(username)
                .map(u -> notificationRepository.countByUserIdAndReadFalse(u.getId()))
                .orElse(0L);
    }

    @Transactional
    public void markRead(String username, UUID id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Уведомление не найдено"));
        if (!n.getUser().getUsername().equals(username)) {
            throw new SecurityException("Не ваше уведомление");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        for (Notification n : notificationRepository.findAllByUserIdOrderByCreatedAtDesc(u.getId())) {
            if (!n.isRead()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        }
    }
}
