package com.normilinet.otklik.web.controller;

import com.normilinet.otklik.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            String role = userDetails.getAuthorities().iterator().next().getAuthority();
            if ("ADMIN".equals(role)) return "redirect:/admin";
            if ("STUDENT".equals(role)) return "redirect:/student/campaigns";
            if ("EXPERT".equals(role)) return "redirect:/expert/reviews";
            model.addAttribute("username", userDetails.getUsername());
            model.addAttribute("role", role);
        }
        return "home";
    }
}
