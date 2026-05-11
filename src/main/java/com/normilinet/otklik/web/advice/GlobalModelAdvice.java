package com.normilinet.otklik.web.advice;

import com.normilinet.otklik.domain.enums.Role;
import com.normilinet.otklik.domain.model.User;
import com.normilinet.otklik.service.NotificationService;
import com.normilinet.otklik.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final ProfileService profileService;
    private final NotificationService notificationService;

    @ModelAttribute("currentUser")
    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) return null;
        try {
            return profileService.getByUsername(auth.getName());
        } catch (Exception ignored) {
            return null;
        }
    }

    @ModelAttribute("unreadCount")
    public long unreadCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) return 0L;
        try {
            return notificationService.unreadCount(auth.getName());
        } catch (Exception ignored) {
            return 0L;
        }
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        User u = currentUser();
        return u != null && u.getRole() == Role.ADMIN;
    }
}
