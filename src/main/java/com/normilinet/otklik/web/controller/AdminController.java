package com.normilinet.otklik.web.controller;

import com.normilinet.otklik.domain.enums.CampaignMode;
import com.normilinet.otklik.domain.model.Campaign;
import com.normilinet.otklik.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CampaignService campaignService;

    @GetMapping
    public String dashboard(Model model) {
        return "admin/dashboard";
    }

    @GetMapping("/campaigns")
    public String listCampaigns(Model model) {
        model.addAttribute("campaigns", campaignService.getAllCampaigns());
        return "admin/campaigns";
    }

    @PostMapping("/campaigns/create")
    public String createCampaign(@RequestParam String title, 
                                 @RequestParam String description, 
                                 @RequestParam String mode,
                                 @RequestParam(required = false) String deadline) {
        LocalDateTime dt = (deadline != null && !deadline.isEmpty()) ? LocalDateTime.parse(deadline) : null;
        campaignService.createCampaign(title, description, CampaignMode.valueOf(mode), dt);
        return "redirect:/admin/campaigns";
    }

    @GetMapping("/campaigns/{id}")
    public String viewCampaign(@PathVariable UUID id, Model model) {
        Campaign campaign = campaignService.getCampaignById(id);
        model.addAttribute("campaign", campaign);
        model.addAttribute("criteria", campaignService.getCriteriaForCampaign(id));
        return "admin/campaign_view";
    }
    
    @PostMapping("/campaigns/{id}/start")
    public String startCampaign(@PathVariable UUID id) {
        campaignService.startCampaign(id);
        return "redirect:/admin/campaigns/" + id;
    }

    @PostMapping("/campaigns/{id}/criteria")
    public String addCriterion(@PathVariable UUID id,
                               @RequestParam String name, 
                               @RequestParam String description, 
                               @RequestParam int maxScore) {
        campaignService.addCriterion(id, name, description, maxScore);
        return "redirect:/admin/campaigns/" + id;
    }
}
