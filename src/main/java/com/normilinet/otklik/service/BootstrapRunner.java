package com.normilinet.otklik.service;

import com.normilinet.otklik.domain.enums.Role;
import com.normilinet.otklik.domain.model.Invite;
import com.normilinet.otklik.domain.repository.InviteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BootstrapRunner implements ApplicationRunner {

    private final InviteRepository inviteRepository;

    @Value("${app.bootstrap.master-invite.enabled:true}")
    private boolean enabled;

    @Value("${app.bootstrap.master-invite.code:MASTER-ADMIN-INVITE-777}")
    private String code;

    @Value("${app.bootstrap.master-invite.role:ADMIN}")
    private String role;

    @Value("${app.bootstrap.master-invite.usages-limit:10}")
    private int usagesLimit;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        if (inviteRepository.findByCode(code).isPresent()) return;
        Invite invite = new Invite();
        invite.setCode(code);
        invite.setRole(Role.valueOf(role));
        invite.setUsagesLimit(usagesLimit);
        invite.setActivationsCount(0);
        invite.setActive(true);
        inviteRepository.save(invite);
    }
}
