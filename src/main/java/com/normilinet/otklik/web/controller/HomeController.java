package com.normilinet.otklik.web.controller;

import com.normilinet.otklik.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(@AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) return "redirect:/login";
        String role = user.getAuthorities().iterator().next().getAuthority();
        return switch (role) {
            case "ADMIN" -> "redirect:/admin";
            case "ORGANIZER" -> "redirect:/organizer";
            case "EXPERT" -> "redirect:/expert/queue";
            case "STUDENT" -> "redirect:/student/works";
            default -> "redirect:/login";
        };
    }
}
