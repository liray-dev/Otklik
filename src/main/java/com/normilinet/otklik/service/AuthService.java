package com.normilinet.otklik.service;

import com.normilinet.otklik.domain.model.Invite;
import com.normilinet.otklik.domain.model.User;
import com.normilinet.otklik.domain.repository.UserRepository;
import com.normilinet.otklik.dto.RegistrationRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final InviteService inviteService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegistrationRequestDto request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username is already taken");
        }

        Invite invite = inviteService.validateAndUseInvite(request.getInviteCode());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole(invite.getRole());
        user.setInvite(invite);

        userRepository.save(user);
    }
}
