package org.dsa11.team1.kumarketto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.AdminResponseReportDTO;
import org.dsa11.team1.kumarketto.domain.dto.UserRequestReportDTO;
import org.dsa11.team1.kumarketto.domain.dto.UserResponseReportDTO;
import org.dsa11.team1.kumarketto.domain.entity.*;
import org.dsa11.team1.kumarketto.domain.enums.ActionType;
import org.dsa11.team1.kumarketto.domain.enums.ProductStatus;
import org.dsa11.team1.kumarketto.domain.enums.ReportStatus;
import org.dsa11.team1.kumarketto.repository.*;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.lang.module.ResolutionException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class ReportService {
    
    public final ReportRepository reportRepository;
    public final MemberRepository memberRepository;
    public final ProductRepository productRepository;
    private final WishListRepository wishListRepository;
    private final ProductElasticsearchRepository productElasticsearchRepository;

    public void reportRequest(UserRequestReportDTO userRequestReportDTO, MultipartFile imageFile) {

        MemberEntity member = memberRepository.findByUserId(userRequestReportDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(userRequestReportDTO.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setStatus(ProductStatus.REPORTED);

        Long likeCount = wishListRepository.countByProduct(product); // 찜 수 조회

        productElasticsearchRepository.save(ProductDocument.fromProduct(product, likeCount)); // ES 동기화

        String imageUrl = null;

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String originalFilename = imageFile.getOriginalFilename();
                String fileExt = originalFilename.substring(originalFilename.lastIndexOf('.'));
                String newFileName = UUID.randomUUID() + fileExt;  // 랜덤 파일명
                Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");       // 저장 경로 예시
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                Path filePath = uploadPath.resolve(newFileName);
                imageFile.transferTo(filePath.toFile());

                // 실제 접근 가능한 URL 생성 (서버 URL 또는 static 매핑 경로)
                imageUrl = "uploads/" + newFileName;

            } catch (IOException e) {
                throw new RuntimeException("Failed to save the file.", e);
            }
        }

        ReportsEntity reportsEntity = ReportsEntity.builder().
                memberEntity(member).
                product(product).
                reportReason(userRequestReportDTO.getReportReason()).
                description(userRequestReportDTO.getDescription()).
                imageUrl(imageUrl).
                build();

        productRepository.save(product);

        reportRepository.save(reportsEntity);
    }

    public List<AdminResponseReportDTO> reports() {
        List<ReportsEntity> reportsEntities = reportRepository.findAllByOrderByCreatedDateDesc();
        List<AdminResponseReportDTO> adminResponseReportDTOList = new ArrayList<>();


        for(ReportsEntity reportsEntity : reportsEntities) {
            AdminResponseReportDTO adminResponseReportDTO = AdminResponseReportDTO.builder()
                    .reportId(reportsEntity.getReportId())
                    .userId(reportsEntity.getMemberEntity().getUserId())
                    .productId(reportsEntity.getProduct().getPid())
                    .title(reportsEntity.getProduct().getName())
                    .reportReason(reportsEntity.getReportReason())
                    .createdDate(reportsEntity.getCreatedDate())
                    .reportStatus(reportsEntity.getReportStatus())
                    .build();
            adminResponseReportDTOList.add(adminResponseReportDTO);
        }

        return adminResponseReportDTOList;
    }

    public AdminResponseReportDTO reportDetail(Long reportId) {
        ReportsEntity reportsEntity = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        AdminResponseReportDTO adminResponseReportDTO = AdminResponseReportDTO.builder()
                .reportId(reportsEntity.getReportId())
                .userId(reportsEntity.getMemberEntity().getUserId())
                .productId(reportsEntity.getProduct().getPid())
                .title(reportsEntity.getProduct().getName())
                .reportReason(reportsEntity.getReportReason())
                .description(reportsEntity.getDescription())
                .imageUrl(reportsEntity.getImageUrl())
                .createdDate(reportsEntity.getCreatedDate())
                .reportStatus(reportsEntity.getReportStatus())
                .actionDate(reportsEntity.getModifiedDate())
                .actionType(
                        reportsEntity.getActionResultsEntity() != null
                        ? reportsEntity.getActionResultsEntity().getActionType()
                        : null
                )
                .build();

        return adminResponseReportDTO;
    }

    public Optional<ReportsEntity> findById(Long reportId) {
        return reportRepository.findById(reportId);
    }

    public void rejectReport(Long reportId) {
        ReportsEntity reportsEntity = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalStateException("Report not found with id: " + reportId));

        if(reportsEntity.getReportStatus() == ReportStatus.RESOLVED) {
            throw new IllegalStateException("This report has already been resolved.");
        }

        reportsEntity.setReportStatus(ReportStatus.RESOLVED);

        ActionResultsEntity actionResultsEntity = ActionResultsEntity.builder()
                .reportsEntity(reportsEntity)
                .actionType(ActionType.REJECTED)
                .build();

        reportsEntity.setActionResultsEntity(actionResultsEntity);

        reportRepository.save(reportsEntity);
    }

    public void hidePost(Long productId, Long reportId, AuthenticatedUser user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("Product not found: " + productId));

        if(reportId == null) {
            product.setStatus(ProductStatus.REPORTED);
            productRepository.save(product);
            return;
        }

        if(!user.hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        product.setStatus(ProductStatus.REPORTED);
        productRepository.save(product);

        // 수정 사항 ES 반영
        Long likeCount = wishListRepository.countByProduct(product); // 찜 수 조회

        productElasticsearchRepository.save(ProductDocument.fromProduct(product, likeCount)); // ES 동기화

        List<ReportsEntity> reportsEntities = reportRepository.findAllByProduct_Pid(productId);

        for(ReportsEntity reportsEntity : reportsEntities) {
            if(reportsEntity.getReportStatus() != ReportStatus.RESOLVED) {
                reportsEntity.setReportStatus(ReportStatus.RESOLVED);

                ActionResultsEntity actionResultsEntity = ActionResultsEntity.builder()
                        .reportsEntity(reportsEntity)
                        .actionType(ActionType.HIDE_POST)
                        .build();

                reportsEntity.setActionResultsEntity(actionResultsEntity);
                reportRepository.save(reportsEntity);
            }
        }

//        ReportsEntity reportsEntity = reportRepository.findById(reportId)
//                .orElseThrow(() -> new IllegalStateException("Report not found: " + reportId));


    }

    public Page<UserResponseReportDTO> getReports(int page, int size, String userId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        log.debug("userId는 nnn111입니까? {}", userId);
        return reportRepository.findByMemberEntity_UserId(userId, pageable)
                .map(this::toUserResponseReportDTO);
    }

    public Page<UserResponseReportDTO> getSanctions(int page, int size, String userId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        return reportRepository.findByProduct_Member_UserIdAndActionResultsEntity_ActionTypeNot(userId, ActionType.REJECTED, pageable)
                .map(this::toUserResponseReportDTO);
    }

    private UserResponseReportDTO toUserResponseReportDTO(ReportsEntity reportsEntity) {
        return UserResponseReportDTO.builder()
                .title(reportsEntity.getProduct().getName())
                .pid(reportsEntity.getProduct().getPid())
                .reportReason(reportsEntity.getReportReason().getReportReason())
                .modifiedDate(reportsEntity.getModifiedDate())
                .reportStatus(reportsEntity.getReportStatus().getReportStatus())
                .actionType(reportsEntity.getActionResultsEntity() != null
                        ? reportsEntity.getActionResultsEntity().getActionType().getActionType()
                        : null)
                .build();
    }

    public Page<AdminResponseReportDTO> getReports(String status, Pageable pageable) {
        Page<ReportsEntity> reportsEntities = "ALL".equals(status) ?
                findAll(pageable) :
                findByReportStatus(status, pageable);

        return reportsEntities.map(entity -> {
            AdminResponseReportDTO dto = new AdminResponseReportDTO();
            dto.setReportId(entity.getReportId());
            dto.setUserId(entity.getMemberEntity().getUserId());
            dto.setProductId(entity.getProduct().getPid());
            dto.setTitle(entity.getProduct().getName());
            dto.setReportReason(entity.getReportReason());
            dto.setDescription(entity.getDescription());
            dto.setCreatedDate(entity.getCreatedDate());
            if (entity.getActionResultsEntity() != null) {
                dto.setActionDate(entity.getActionResultsEntity().getActionDate());
                dto.setActionType(entity.getActionResultsEntity().getActionType());
            }
            dto.setReportStatus(entity.getReportStatus());
            dto.setImageUrl(entity.getImageUrl());
            return dto;
        });
    }

    public Page<ReportsEntity> findAll(Pageable pageable) {
        return reportRepository.findAll(pageable);
    }

    public Page<ReportsEntity> findByReportStatus(String status, Pageable pageable) {
        return reportRepository.findByReportStatus(ReportStatus.valueOf(status), pageable);
    }
}
