package com.normilinet.otklik.service;

import com.normilinet.otklik.domain.enums.WorkStatus;
import com.normilinet.otklik.domain.model.Campaign;
import com.normilinet.otklik.domain.model.User;
import com.normilinet.otklik.domain.model.Work;
import com.normilinet.otklik.domain.repository.CampaignRepository;
import com.normilinet.otklik.domain.repository.UserRepository;
import com.normilinet.otklik.domain.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkService {

    private final WorkRepository workRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;

    @Transactional
    public Work submitWork(UUID campaignId, String username, String contentText, String filePath) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid campaign Id"));
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user"));
        
        Work work = new Work();
        work.setCampaign(campaign);
        work.setStudent(student);
        work.setContentText(contentText);
        work.setFilePath(filePath);
        work.setStatus(WorkStatus.SUBMITTED);
        return workRepository.save(work);
    }

    @Transactional(readOnly = true)
    public List<Work> getWorksByStudent(String username) {
        return workRepository.findAll().stream()
                .filter(w -> w.getStudent().getUsername().equals(username))
                .toList(); // Optimize with a specific repository query later if needed
    }
}
