package com.normilinet.otklik.service;

import com.normilinet.otklik.domain.enums.Role;
import com.normilinet.otklik.domain.model.Invite;
import com.normilinet.otklik.domain.repository.InviteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteRepository inviteRepository;
    private final TagService tagService;

    @Transactional
    public Invite createInvite(String code, Role role, int usagesLimit, LocalDateTime validUntil) {
        return createInvite(code, role, usagesLimit, validUntil, java.util.Collections.emptyList());
    }

    @Transactional
    public Invite createInvite(String code, Role role, int usagesLimit, LocalDateTime validUntil,
                               java.util.Collection<UUID> tagIds) {
        if (inviteRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Invite with code " + code + " already exists");
        }

        Invite invite = new Invite();
        invite.setCode(code);
        invite.setRole(role);
        invite.setUsagesLimit(usagesLimit);
        invite.setValidUntil(validUntil);
        invite.setActive(true);
        invite.getTags().addAll(tagService.resolveTagIds(tagIds));

        return inviteRepository.save(invite);
    }

    @Transactional
    public Invite validateAndUseInvite(String code) {
        Invite invite = inviteRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite code"));

        if (!invite.isActive()) {
            throw new IllegalArgumentException("Invite code is deactivated");
        }
        
        if (invite.getValidUntil() != null && invite.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invite code has expired");
        }

        if (invite.getActivationsCount() >= invite.getUsagesLimit()) {
            throw new IllegalArgumentException("Invite code usage limit exceeded");
        }

        invite.setActivationsCount(invite.getActivationsCount() + 1);
        if (invite.getActivationsCount() >= invite.getUsagesLimit()) {
            invite.setActive(false);
        }
        
        return inviteRepository.save(invite);
    }
}
