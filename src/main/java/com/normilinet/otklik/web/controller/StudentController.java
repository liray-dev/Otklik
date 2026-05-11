package com.normilinet.otklik.web.controller;

import com.normilinet.otklik.security.CustomUserDetails;
import com.normilinet.otklik.service.CampaignService;
import com.normilinet.otklik.service.WorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final CampaignService campaignService;
    private final WorkService workService;

    @GetMapping("/campaigns")
    public String listCampaigns(Model model) {
        model.addAttribute("campaigns", campaignService.getActiveCampaigns());
        return "student/campaigns";
    }
    
    @GetMapping("/works")
    public String myWorks(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("works", workService.getWorksByStudent(user.getUsername()));
        return "student/works";
    }

    @GetMapping("/campaigns/{id}/submit")
    public String submitWorkForm(@PathVariable UUID id, Model model) {
        model.addAttribute("campaign", campaignService.getCampaignById(id));
        return "student/work_form";
    }

    @PostMapping("/campaigns/{id}/submit")
    public String submitWork(@PathVariable UUID id,
                             @AuthenticationPrincipal CustomUserDetails user,
                             @RequestParam String contentText,
                             @RequestParam(required = false) MultipartFile file) {
        String filePath = "";
        if (file != null && !file.isEmpty()) {
            filePath = "uploaded/" + file.getOriginalFilename();
        }
        workService.submitWork(id, user.getUsername(), contentText, filePath);
        return "redirect:/student/works";
    }
}
