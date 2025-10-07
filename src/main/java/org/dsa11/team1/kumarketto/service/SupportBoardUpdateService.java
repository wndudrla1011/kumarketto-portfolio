package org.dsa11.team1.kumarketto.service;

import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.SupportBoardEntity;
import org.dsa11.team1.kumarketto.domain.enums.IsPublic;
import org.dsa11.team1.kumarketto.repository.SupportBoardRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SupportBoardUpdateService {

    private final SupportBoardRepository supportBoardRepository;
    private final InquiryAttachmentService attachmentService;

    @Transactional
    public void update(Long inquiryId,
                       String title,
                       String content,
                       IsPublic isPublic,
                       @Nullable MultipartFile newUpload,
                       boolean removeAttachment) {

        SupportBoardEntity e = supportBoardRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의가 없습니다. id=" + inquiryId));

        e.setTitle(title);
        e.setContent(content);
        e.setIsPublic(isPublic);

        if (removeAttachment) {
            attachmentService.remove(inquiryId);
        } else if (newUpload != null && !newUpload.isEmpty()) {
            attachmentService.attachOrReplace(inquiryId, newUpload);
        }
    }
}