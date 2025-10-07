package org.dsa11.team1.kumarketto.service;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.SupportDTO;
import org.dsa11.team1.kumarketto.domain.dto.UserInquiryDTO;
import org.dsa11.team1.kumarketto.domain.entity.SupportBoardEntity;
import org.dsa11.team1.kumarketto.domain.enums.Status;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.repository.SupportBoardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Transactional
@Slf4j
public class SupportBoardService {
    private final SupportBoardRepository supportBoardRepository;

    private final MemberRepository memberRepository;
    private final InquiryAttachmentService inquiryAttachmentService;

    @PersistenceContext
    private EntityManager em;

    public Page<UserInquiryDTO> getUserInquiries(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        return supportBoardRepository.findByUserNo_UserId(userId, pageable)
                .map(entity -> UserInquiryDTO.builder()
                        .inquiryId(entity.getInquiryId())
                        .title(entity.getTitle())
                        .createdDate(entity.getCreatedDate())
                        .status(entity.getStatus().getTitle())
                        .isPublic(entity.getIsPublic().getTitle())
                        .build());
    }



    public List<SupportDTO> findList() {
        List<SupportBoardEntity> entityList = supportBoardRepository.findAll();
        List<SupportDTO> dtoList = new ArrayList<>();
        for (SupportBoardEntity entity : entityList){
            SupportDTO dto = SupportDTO.builder()
                    .inquiryId(entity.getInquiryId())
                    .userNo(entity.getUserNo())
                    .title(entity.getTitle())
                    .content(entity.getContent())
                    .status(entity.getStatus())
                    .createdDate(entity.getCreatedDate())
                    .modifiedDate(entity.getModifiedDate())
                    .isPublic(entity.getIsPublic())
                    .build();
            dtoList.add(dto);
        };
        log.debug("inquiries:{}", dtoList);
        return dtoList;
    }

    public void write(SupportDTO dto, String uploadPath, MultipartFile upload) {
        SupportBoardEntity entity = new SupportBoardEntity();
        entity.setTitle(dto.getTitle());
        entity.setUserNo(dto.getUserNo());          // 지금은 그대로 패스 원하셨던 부분
        entity.setContent(dto.getContent());
        entity.setStatus(Status.PENDING);
        entity.setCreatedDate(dto.getCreatedDate());
        entity.setIsPublic(dto.getIsPublic());
        if (upload != null && !upload.isEmpty()) {
            entity.setAttachmentUrl(upload.getOriginalFilename()); // 임시 기록
        }
        SupportBoardEntity saved = supportBoardRepository.save(entity);

        // ★★ 여기서 PK를 DTO에 실어 컨트롤러로 되돌려 보냄
        dto.setInquiryId(saved.getInquiryId());
    }

    @Transactional
    public void supportedit(SupportDTO dto) {
        SupportBoardEntity ent = supportBoardRepository.findById(dto.getInquiryId()).orElse(null);

        ent.setTitle(dto.getTitle());
        ent.setContent(dto.getContent());
        ent.setIsPublic(dto.getIsPublic());
        //ent.setAttachmentUrl(dto.getAttachmentUrl()); //파일첨부

    }

    /**
     * 문의 게시글 삭제
     * @param inquiryId 게시글 번호로 식별 합니다
     */
    public void delete(Long inquiryId) {

        // 1) 첨부 제거 (S3 삭제 + 컬럼 null)  ← InquiryAttachmentService가 처리
        inquiryAttachmentService.remove(inquiryId);
        supportBoardRepository.deleteById(inquiryId);
    }


    /**
     * 페이징 처리된 문의글 목록을 조회합니다.
     * @param pageable 페이징 요청 정보
     * @return 페이징된 SupportDTO
     */
    public Page<SupportDTO> findList(Pageable pageable) {
        // Repository에서 Page<Entity>를 받아옴
        Page<SupportBoardEntity> entityPage = supportBoardRepository.findAllByOrderByInquiryIdDesc(pageable);

        // Page<Entity>를 Page<DTO>로 변환하여 반환
        // .map()을 사용하면 페이징 정보는 그대로 유지되고 내용물(content)만 DTO로 변환됩니다.
        return entityPage.map(entity -> SupportDTO.builder()
                .inquiryId(entity.getInquiryId())
                .userNo(entity.getUserNo())
                .title(entity.getTitle())
                .content(entity.getContent())
                .status(entity.getStatus())
                .createdDate(entity.getCreatedDate())
                .modifiedDate(entity.getModifiedDate())
                .isPublic(entity.getIsPublic())
                .build());
    }
}
