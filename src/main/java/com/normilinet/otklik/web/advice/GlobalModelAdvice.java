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

    @ModelAttribute("roleSidebar")
    public java.util.List<java.util.Map<String, String>> roleSidebar() {
        User u = currentUser();
        if (u == null) return java.util.List.of();
        return switch (u.getRole()) {
            case ADMIN -> java.util.List.of(
                    java.util.Map.of("key","overview","label","Обзор","href","/admin","icon","▦"),
                    java.util.Map.of("key","users","label","Пользователи","href","/admin/users","icon","▤"),
                    java.util.Map.of("key","invites","label","Инвайты","href","/admin/invites","icon","✎"),
                    java.util.Map.of("key","settings","label","Настройки","href","/admin/settings","icon","⚙"));
            case ORGANIZER, EXPERT -> java.util.List.of(
                    java.util.Map.of("key","overview","label","Панель организатора","href","/organizer","icon","▦"),
                    java.util.Map.of("key","queue","label","Очередь проверок","href","/expert/queue","icon","▤"),
                    java.util.Map.of("key","review","label","Интерфейс оценки","href","/expert/in-progress","icon","✎"),
                    java.util.Map.of("key","results","label","Панель результатов","href","/expert/completed","icon","▣"));
            case STUDENT -> java.util.List.of(
                    java.util.Map.of("key","works","label","Мои работы","href","/student/works","icon","▦"),
                    java.util.Map.of("key","cycles","label","Доступные кампании","href","/student/cycles","icon","▤"));
        };
    }

    @ModelAttribute("roleSidebarSubtitle")
    public String roleSidebarSubtitle() {
        User u = currentUser();
        if (u == null) return "СИСТЕМА ПРОВЕРКИ";
        return switch (u.getRole()) {
            case ADMIN -> "АДМИНИСТРИРОВАНИЕ";
            case ORGANIZER, EXPERT -> "СИСТЕМА ПРОВЕРКИ";
            case STUDENT -> "ЛИЧНЫЙ КАБИНЕТ";
        };
    }
}
