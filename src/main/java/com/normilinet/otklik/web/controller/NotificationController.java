package com.normilinet.otklik.web.controller;

import com.normilinet.otklik.security.CustomUserDetails;
import com.normilinet.otklik.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("notifications", notificationService.list(user.getUsername()));
        return "notifications/list";
    }

    @PostMapping("/{id}/read")
    public String markRead(@AuthenticationPrincipal CustomUserDetails user, @PathVariable UUID id) {
        notificationService.markRead(user.getUsername(), id);
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllRead(@AuthenticationPrincipal CustomUserDetails user) {
        notificationService.markAllRead(user.getUsername());
        return "redirect:/notifications";
    }
}
