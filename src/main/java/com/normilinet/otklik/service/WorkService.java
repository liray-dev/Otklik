package com.normilinet.otklik.service;

import com.normilinet.otklik.domain.enums.CampaignStatus;
import com.normilinet.otklik.domain.enums.WorkStatus;
import com.normilinet.otklik.domain.model.Campaign;
import com.normilinet.otklik.domain.model.User;
import com.normilinet.otklik.domain.model.Work;
import com.normilinet.otklik.domain.model.WorkAttachment;
import com.normilinet.otklik.domain.repository.CampaignRepository;
import com.normilinet.otklik.domain.repository.UserRepository;
import com.normilinet.otklik.domain.repository.WorkAttachmentRepository;
import com.normilinet.otklik.domain.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkService {

    private final WorkRepository workRepository;
    private final WorkAttachmentRepository attachmentRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final FileStorageService storage;
    private final com.normilinet.otklik.domain.repository.WorkAssignmentRepository workAssignmentRepository;

    @Transactional
    public Work submitWork(UUID campaignId,
                           String username,
                           String title,
                           String contentText,
                           String externalLink,
                           List<MultipartFile> files) throws IOException {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Кампания не найдена"));
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Студент не найден"));

        java.util.Optional<Work> existing = workRepository.findByCampaignIdAndStudentId(campaignId, student.getId());
        if (existing.isPresent()) {
            return updateWork(existing.get().getId(), username, title, contentText, externalLink, files);
        }
        if (campaign.getDeadline() != null && java.time.LocalDateTime.now().isAfter(campaign.getDeadline())) {
            throw new IllegalStateException("Срок отправки работ истёк");
        }

        Work work = new Work();
        work.setCampaign(campaign);
        work.setStudent(student);
        work.setTitle(title != null && !title.isBlank() ? title : "Работа без названия");
        work.setContentText(contentText);
        work.setExternalLink(externalLink);
        WorkStatus initial = campaign.getStatus() == CampaignStatus.ACTIVE
                ? WorkStatus.IN_QUEUE
                : WorkStatus.UPLOADED;
        work.setStatus(initial);
        work = workRepository.save(work);

        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                attachFile(work, file);
            }
        }
        return work;
    }

    @Transactional
    public Work updateWork(UUID workId,
                           String username,
                           String title,
                           String contentText,
                           String externalLink,
                           List<MultipartFile> files) throws IOException {
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new IllegalArgumentException("Работа не найдена"));
        if (!work.getStudent().getUsername().equals(username)) {
            throw new SecurityException("Не ваша работа");
        }
        if (!isEditable(work)) {
            throw new IllegalStateException("Работа уже взята в проверку и не может быть изменена");
        }
        if (title != null && !title.isBlank()) work.setTitle(title);
        work.setContentText(contentText);
        if (externalLink != null && !externalLink.isBlank()) work.setExternalLink(externalLink);
        boolean wasRevision = work.getStatus() == WorkStatus.NEEDS_REVISION;
        if (wasRevision || work.getStatus() == WorkStatus.UPLOADED) {
            if (work.getCampaign().getStatus() == CampaignStatus.ACTIVE) {
                work.setStatus(WorkStatus.IN_QUEUE);
            }
        }
        work = workRepository.save(work);
        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                attachFile(work, file);
            }
        }
        if (wasRevision && work.getStatus() == WorkStatus.IN_QUEUE) {
            reassignToLastReviewer(work);
        }
        return work;
    }

    @Transactional
    public void reassignToLastReviewer(Work work) {
        java.util.List<com.normilinet.otklik.domain.model.WorkAssignment> abandoned =
                workAssignmentRepository.findAllByWorkId(work.getId()).stream()
                        .filter(a -> a.getStatus() == com.normilinet.otklik.domain.enums.AssignmentStatus.ABANDONED)
                        .sorted((x, y) -> {
                            java.time.LocalDateTime xa = x.getCompletedAt() != null ? x.getCompletedAt() : x.getCreatedAt();
                            java.time.LocalDateTime ya = y.getCompletedAt() != null ? y.getCompletedAt() : y.getCreatedAt();
                            return ya.compareTo(xa);
                        }).toList();
        if (abandoned.isEmpty()) return;
        com.normilinet.otklik.domain.model.WorkAssignment latest = abandoned.get(0);
        boolean alreadyHas = workAssignmentRepository.findAllByWorkId(work.getId()).stream()
                .anyMatch(a -> a.getReviewer().getId().equals(latest.getReviewer().getId())
                        && a.getStatus() != com.normilinet.otklik.domain.enums.AssignmentStatus.ABANDONED);
        if (alreadyHas) return;
        com.normilinet.otklik.domain.model.WorkAssignment fresh = new com.normilinet.otklik.domain.model.WorkAssignment();
        fresh.setWork(work);
        fresh.setReviewer(latest.getReviewer());
        fresh.setStatus(com.normilinet.otklik.domain.enums.AssignmentStatus.IN_PROGRESS);
        fresh.setTakenAt(java.time.LocalDateTime.now());
        workAssignmentRepository.save(fresh);
        work.setStatus(WorkStatus.UNDER_REVIEW);
        workRepository.save(work);
    }

    @Transactional
    public void deleteAttachment(UUID workId, UUID attachmentId, String username) throws IOException {
        Work work = workRepository.findById(workId)
                .orElseThrow(() -> new IllegalArgumentException("Работа не найдена"));
        if (!work.getStudent().getUsername().equals(username)) {
            throw new SecurityException("Не ваша работа");
        }
        if (!isEditable(work)) {
            throw new IllegalStateException("Работа уже взята в проверку");
        }
        WorkAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден"));
        if (!att.getWork().getId().equals(work.getId())) {
            throw new SecurityException("Файл не относится к этой работе");
        }
        if (att.getStoredPath() != null) {
            try { storage.delete(att.getStoredPath()); } catch (Exception ignored) {}
        }
        attachmentRepository.delete(att);
    }

    public boolean isEditable(Work work) {
        WorkStatus s = work.getStatus();
        if (s == WorkStatus.UNDER_REVIEW || s == WorkStatus.REVIEWED) return false;
        return true;
    }

    @Transactional
    public Work saveDraft(UUID campaignId,
                          String username,
                          String title,
                          String contentText,
                          String externalLink) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Кампания не найдена"));
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Студент не найден"));
        java.util.Optional<Work> existing = workRepository.findByCampaignIdAndStudentId(campaignId, student.getId());
        Work work = existing.orElseGet(() -> {
            Work w = new Work();
            w.setCampaign(campaign);
            w.setStudent(student);
            w.setStatus(WorkStatus.UPLOADED);
            return w;
        });
        if (!isEditable(work)) {
            throw new IllegalStateException("Работа уже взята в проверку и не может быть изменена");
        }
        if (title != null && !title.isBlank()) work.setTitle(title);
        else if (work.getTitle() == null) work.setTitle("Черновик");
        work.setContentText(contentText);
        work.setExternalLink(externalLink);
        return workRepository.save(work);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Work> findExistingForStudent(UUID campaignId, String username) {
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Студент не найден"));
        return workRepository.findByCampaignIdAndStudentId(campaignId, student.getId());
    }

    @Transactional
    public WorkAttachment attachFile(Work work, MultipartFile file) throws IOException {
        String folder = "works/" + work.getCampaign().getId() + "/" + work.getId();
        FileStorageService.StoredFile stored = storage.store(file, folder);
        WorkAttachment att = new WorkAttachment();
        att.setWork(work);
        att.setKind(FileStorageService.classify(stored.originalName(), stored.mimeType()));
        att.setOriginalFilename(stored.originalName());
        att.setStoredPath(stored.relativePath());
        att.setMimeType(stored.mimeType() != null ? stored.mimeType() : FileStorageService.guessMime(stored.originalName()));
        att.setSizeBytes(stored.size());
        att.setVoice(false);
        return attachmentRepository.save(att);
    }

    @Transactional
    public WorkAttachment attachVoice(Work work, byte[] audio, Long durationMs) throws IOException {
        String folder = "works/" + work.getCampaign().getId() + "/" + work.getId() + "/voice";
        FileStorageService.StoredFile stored = storage.storeVoice(audio, folder);
        WorkAttachment att = new WorkAttachment();
        att.setWork(work);
        att.setKind(com.normilinet.otklik.domain.enums.FileKind.AUDIO);
        att.setOriginalFilename("voice.webm");
        att.setStoredPath(stored.relativePath());
        att.setMimeType("audio/webm");
        att.setSizeBytes(stored.size());
        att.setVoice(true);
        att.setDurationMs(durationMs);
        return attachmentRepository.save(att);
    }

    @Transactional(readOnly = true)
    public List<Work> getWorksByStudent(String username) {
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Студент не найден"));
        return workRepository.findAllByStudentIdOrderByCreatedAtDesc(student.getId());
    }

    @Transactional(readOnly = true)
    public List<WorkAttachment> getAttachments(UUID workId) {
        return attachmentRepository.findAllByWorkIdOrderByCreatedAtAsc(workId);
    }

    @Transactional(readOnly = true)
    public Work getById(UUID id) {
        return workRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Работа не найдена"));
    }
}
