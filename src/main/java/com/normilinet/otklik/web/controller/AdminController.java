package com.normilinet.otklik.web.controller;

import com.normilinet.otklik.domain.enums.Role;
import com.normilinet.otklik.domain.model.Invite;
import com.normilinet.otklik.domain.model.User;
import com.normilinet.otklik.domain.repository.InviteRepository;
import com.normilinet.otklik.domain.repository.UserRepository;
import com.normilinet.otklik.domain.repository.CampaignRepository;
import com.normilinet.otklik.service.AdminService;
import com.normilinet.otklik.service.InviteService;
import com.normilinet.otklik.service.TagService;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
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
    private final TagService tagService;

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
    public String users(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String role,
                        @RequestParam(required = false) String invite,
                        @RequestParam(required = false) UUID tag,
                        @RequestParam(required = false) String group,
                        Model model) {
        List<User> users = filterUsers(q, role, invite, tag, group);
        model.addAttribute("users", users);
        model.addAttribute("allTags", tagService.listAll());
        model.addAttribute("filterQ", q);
        model.addAttribute("filterRole", role);
        model.addAttribute("filterInvite", invite);
        model.addAttribute("filterTag", tag);
        model.addAttribute("filterGroup", group);
        model.addAttribute("usersBase", "/admin/users");
        return "users/list";
    }

    @PostMapping("/users/tag/apply")
    public String bulkApplyTag(@RequestParam UUID tagId,
                               @RequestParam(value = "userIds", required = false) List<UUID> userIds,
                               jakarta.servlet.http.HttpServletRequest req) {
        if (userIds != null) tagService.bulkAddTag(userIds, tagId);
        return "redirect:" + (req.getHeader("Referer") != null ? req.getHeader("Referer") : "/admin/users");
    }

    @PostMapping("/users/tag/remove")
    public String bulkRemoveTag(@RequestParam UUID tagId,
                                @RequestParam(value = "userIds", required = false) List<UUID> userIds,
                                jakarta.servlet.http.HttpServletRequest req) {
        if (userIds != null) tagService.bulkRemoveTag(userIds, tagId);
        return "redirect:" + (req.getHeader("Referer") != null ? req.getHeader("Referer") : "/admin/users");
    }

    @GetMapping("/tags")
    public String tags(Model model) {
        model.addAttribute("tags", tagService.listAll());
        return "admin/tags";
    }

    @PostMapping("/tags")
    public String createTag(@RequestParam String name,
                            @RequestParam(required = false) String description) {
        tagService.createTag(name, description);
        return "redirect:/admin/tags";
    }

    @PostMapping("/tags/{id}/delete")
    public String deleteTag(@PathVariable UUID id) {
        tagService.deleteTag(id);
        return "redirect:/admin/tags";
    }

    private List<User> filterUsers(String q, String role, String invite, UUID tag, String group) {
        return userRepository.findAll().stream()
                .filter(u -> q == null || q.isBlank()
                        || (u.getUsername() != null && u.getUsername().toLowerCase().contains(q.toLowerCase()))
                        || (u.getFullName() != null && u.getFullName().toLowerCase().contains(q.toLowerCase()))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q.toLowerCase())))
                .filter(u -> role == null || role.isBlank() || u.getRole().name().equals(role))
                .filter(u -> invite == null || invite.isBlank()
                        || (u.getInvite() != null && u.getInvite().getCode() != null
                            && u.getInvite().getCode().toLowerCase().contains(invite.toLowerCase())))
                .filter(u -> tag == null
                        || (u.getTags() != null && u.getTags().stream().anyMatch(t -> t.getId().equals(tag))))
                .filter(u -> group == null || group.isBlank()
                        || (u.getUniversityGroup() != null && u.getUniversityGroup().toLowerCase().contains(group.toLowerCase())))
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .toList();
    }

    @GetMapping("/invites")
    public String invites(Model model) {
        List<Invite> invites = inviteRepository.findAll().stream()
                .sorted(Comparator.comparing(Invite::getCreatedAt).reversed())
                .toList();
        model.addAttribute("invites", invites);
        model.addAttribute("roles", Role.values());
        model.addAttribute("allTags", tagService.listAll());
        return "admin/invites";
    }

    @PostMapping("/invites")
    public String createInvite(@RequestParam String code,
                               @RequestParam String role,
                               @RequestParam(defaultValue = "1") int usagesLimit,
                               @RequestParam(required = false) String validUntil,
                               @RequestParam(value = "tagIds", required = false) List<UUID> tagIds) {
        LocalDateTime until = (validUntil != null && !validUntil.isBlank()) ? LocalDateTime.parse(validUntil) : null;
        inviteService.createInvite(code, Role.valueOf(role), usagesLimit, until, tagIds);
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
