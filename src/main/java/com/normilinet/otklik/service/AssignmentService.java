package com.normilinet.otklik.service;

import com.normilinet.otklik.domain.enums.AssignmentStatus;
import com.normilinet.otklik.domain.enums.CampaignMode;
import com.normilinet.otklik.domain.enums.Role;
import com.normilinet.otklik.domain.enums.WorkStatus;
import com.normilinet.otklik.domain.model.User;
import com.normilinet.otklik.domain.model.Work;
import com.normilinet.otklik.domain.model.WorkAssignment;
import com.normilinet.otklik.domain.repository.UserRepository;
import com.normilinet.otklik.domain.repository.WorkAssignmentRepository;
import com.normilinet.otklik.domain.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final WorkRepository workRepository;
    private final WorkAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Work> listAvailableForReviewer(String username) {
        User reviewer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        List<Work> queue = workRepository.findAllByStatus(WorkStatus.IN_QUEUE);
        Set<UUID> mine = collectMyWorkIds(reviewer);
        List<Work> result = new ArrayList<>();
        for (Work w : queue) {
            if (!isReviewerEligible(w, reviewer)) continue;
            if (mine.contains(w.getId())) continue;
            result.add(w);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<WorkAssignment> listMyActive(String username) {
        User reviewer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        return assignmentRepository.findAllByReviewerIdAndStatus(reviewer.getId(), AssignmentStatus.IN_PROGRESS);
    }

    @Transactional(readOnly = true)
    public List<WorkAssignment> listMyAll(String username) {
        User reviewer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        return assignmentRepository.findAllByReviewerIdOrderByCreatedAtDesc(reviewer.getId());
    }

    @Transactional
    public WorkAssignment take(UUID workId, String username) {
        User reviewer = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        Work work = workRepository.findByIdForUpdate(workId)
                .orElseThrow(() -> new IllegalArgumentException("Работа не найдена"));
        if (work.getStatus() != WorkStatus.IN_QUEUE && work.getStatus() != WorkStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Эта работа недоступна для взятия");
        }
        if (!isReviewerEligible(work, reviewer)) {
            throw new IllegalStateException("Вы не можете рецензировать эту работу");
        }
        boolean already = assignmentRepository.findAllByWorkId(workId).stream()
                .anyMatch(a -> a.getReviewer().getId().equals(reviewer.getId())
                        && a.getStatus() != AssignmentStatus.ABANDONED);
        if (already) {
            throw new IllegalStateException("Вы уже взяли эту работу");
        }
        WorkAssignment a = new WorkAssignment();
        a.setWork(work);
        a.setReviewer(reviewer);
        a.setStatus(AssignmentStatus.IN_PROGRESS);
        a.setTakenAt(LocalDateTime.now());
        work.setStatus(WorkStatus.UNDER_REVIEW);
        workRepository.save(work);
        return assignmentRepository.save(a);
    }

    @Transactional
    public void abandon(UUID assignmentId, String username) {
        WorkAssignment a = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Назначение не найдено"));
        if (!a.getReviewer().getUsername().equals(username)) {
            throw new SecurityException("Не ваше назначение");
        }
        a.setStatus(AssignmentStatus.ABANDONED);
        assignmentRepository.save(a);
        long activeOnWork = assignmentRepository.findAllByWorkId(a.getWork().getId()).stream()
                .filter(x -> x.getStatus() == AssignmentStatus.IN_PROGRESS || x.getStatus() == AssignmentStatus.COMPLETED)
                .count();
        if (activeOnWork == 0) {
            Work w = a.getWork();
            w.setStatus(WorkStatus.IN_QUEUE);
            workRepository.save(w);
        }
    }

    public boolean isReviewerEligible(Work work, User reviewer) {
        CampaignMode mode = work.getCampaign().getMode();
        Role role = reviewer.getRole();
        switch (mode) {
            case EXPERT -> {
                return role == Role.EXPERT || role == Role.ORGANIZER || role == Role.ADMIN;
            }
            case PEER_TO_PEER -> {
                if (role != Role.STUDENT) return false;
                return !work.getStudent().getId().equals(reviewer.getId());
            }
            case CONTEST -> {
                if (role == Role.STUDENT) {
                    return !work.getStudent().getId().equals(reviewer.getId());
                }
                return role == Role.EXPERT || role == Role.ORGANIZER || role == Role.ADMIN;
            }
        }
        return false;
    }

    private Set<UUID> collectMyWorkIds(User reviewer) {
        return assignmentRepository.findAllByReviewerIdOrderByCreatedAtDesc(reviewer.getId()).stream()
                .filter(a -> a.getStatus() != AssignmentStatus.ABANDONED)
                .map(a -> a.getWork().getId())
                .collect(java.util.stream.Collectors.toSet());
    }
}
