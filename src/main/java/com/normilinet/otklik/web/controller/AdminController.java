package com.normilinet.otklik.web.controller;

import com.normilinet.otklik.domain.enums.Role;
import com.normilinet.otklik.domain.model.Invite;
import com.normilinet.otklik.domain.model.User;
import com.normilinet.otklik.domain.repository.InviteRepository;
import com.normilinet.otklik.domain.repository.UserRepository;
import com.normilinet.otklik.domain.repository.CampaignRepository;
import com.normilinet.otklik.service.AdminService;
import com.normilinet.otklik.service.InviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final InviteRepository inviteRepository;
    private final InviteService inviteService;
    private final AdminService adminService;
    private final CampaignRepository campaignRepository;

    @GetMapping
    public String dashboard(Model model) {
        long users = userRepository.count();
        long invites = inviteRepository.count();
        long activeInvites = inviteRepository.findAll().stream().filter(Invite::isActive).count();
        model.addAttribute("usersCount", users);
        model.addAttribute("invitesCount", invites);
        model.addAttribute("activeInvitesCount", activeInvites);
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(Model model) {
        List<User> users = userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .toList();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/invites")
    public String invites(Model model) {
        List<Invite> invites = inviteRepository.findAll().stream()
                .sorted(Comparator.comparing(Invite::getCreatedAt).reversed())
                .toList();
        model.addAttribute("invites", invites);
        model.addAttribute("roles", Role.values());
        return "admin/invites";
    }

    @PostMapping("/invites")
    public String createInvite(@RequestParam String code,
                               @RequestParam String role,
                               @RequestParam(defaultValue = "1") int usagesLimit,
                               @RequestParam(required = false) String validUntil) {
        LocalDateTime until = (validUntil != null && !validUntil.isBlank()) ? LocalDateTime.parse(validUntil) : null;
        inviteService.createInvite(code, Role.valueOf(role), usagesLimit, until);
        return "redirect:/admin/invites";
    }

    @PostMapping("/invites/{id}/revoke")
    public String revoke(@PathVariable UUID id) {
        Invite invite = inviteRepository.findById(id).orElseThrow();
        invite.setActive(false);
        inviteRepository.save(invite);
        return "redirect:/admin/invites";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable UUID id) {
        adminService.deleteUser(id);
        return "redirect:/admin/users";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("campaigns", campaignRepository.findAll());
        return "admin/settings";
    }

    @PostMapping("/settings/reset-campaign")
    public String resetCampaign(@RequestParam UUID campaignId) {
        adminService.resetCampaign(campaignId);
        return "redirect:/admin/settings?reset";
    }

    @PostMapping("/settings/reset-all")
    public String resetAll() {
        adminService.resetAll();
        return "redirect:/admin/settings?resetAll";
    }
}
