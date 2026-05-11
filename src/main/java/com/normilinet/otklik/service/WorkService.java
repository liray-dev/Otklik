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
