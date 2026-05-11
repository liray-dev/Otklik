package com.normilinet.otklik.service;

import com.normilinet.otklik.domain.model.Campaign;
import com.normilinet.otklik.domain.model.Invite;
import com.normilinet.otklik.domain.model.Tag;
import com.normilinet.otklik.domain.model.User;
import com.normilinet.otklik.domain.repository.CampaignRepository;
import com.normilinet.otklik.domain.repository.InviteRepository;
import com.normilinet.otklik.domain.repository.TagRepository;
import com.normilinet.otklik.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final InviteRepository inviteRepository;

    @Transactional(readOnly = true)
    public List<Tag> listAll() {
        return tagRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Tag getById(UUID id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Тэг не найден"));
    }

    @Transactional
    public Tag createTag(String name, String description) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("Имя тэга обязательно");
        return tagRepository.findByName(n).orElseGet(() -> {
            Tag t = new Tag();
            t.setName(n);
            t.setDescription(description);
            return tagRepository.save(t);
        });
    }

    @Transactional
    public void deleteTag(UUID id) {
        Tag t = getById(id);
        for (User u : userRepository.findAll()) {
            if (u.getTags().removeIf(x -> x.getId().equals(id))) userRepository.save(u);
        }
        for (Campaign c : campaignRepository.findAll()) {
            if (c.getTags().removeIf(x -> x.getId().equals(id))) campaignRepository.save(c);
        }
        for (Invite i : inviteRepository.findAll()) {
            if (i.getTags().removeIf(x -> x.getId().equals(id))) inviteRepository.save(i);
        }
        tagRepository.delete(t);
    }

    @Transactional
    public void addToUser(UUID userId, UUID tagId) {
        User u = userRepository.findById(userId).orElseThrow();
        Tag t = getById(tagId);
        u.getTags().add(t);
        userRepository.save(u);
    }

    @Transactional
    public void removeFromUser(UUID userId, UUID tagId) {
        User u = userRepository.findById(userId).orElseThrow();
        u.getTags().removeIf(x -> x.getId().equals(tagId));
        userRepository.save(u);
    }

    @Transactional
    public void bulkAddTag(Collection<UUID> userIds, UUID tagId) {
        Tag t = getById(tagId);
        for (UUID uid : userIds) {
            userRepository.findById(uid).ifPresent(u -> {
                u.getTags().add(t);
                userRepository.save(u);
            });
        }
    }

    @Transactional
    public void bulkRemoveTag(Collection<UUID> userIds, UUID tagId) {
        for (UUID uid : userIds) {
            userRepository.findById(uid).ifPresent(u -> {
                u.getTags().removeIf(x -> x.getId().equals(tagId));
                userRepository.save(u);
            });
        }
    }

    @Transactional
    public void applyInviteTags(User user, Invite invite) {
        if (invite == null || invite.getTags() == null || invite.getTags().isEmpty()) return;
        user.getTags().addAll(invite.getTags());
        userRepository.save(user);
    }

    @Transactional
    public Set<Tag> resolveTagIds(Collection<UUID> tagIds) {
        Set<Tag> out = new HashSet<>();
        if (tagIds == null) return out;
        for (UUID id : tagIds) {
            if (id == null) continue;
            tagRepository.findById(id).ifPresent(out::add);
        }
        return out;
    }
}
