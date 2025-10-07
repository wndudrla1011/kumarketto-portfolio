package org.dsa11.team1.kumarketto.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.AdminReplyDTO;
import org.dsa11.team1.kumarketto.domain.dto.SupportDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.domain.entity.SupportBoardEntity;
import org.dsa11.team1.kumarketto.domain.enums.IsPublic;
import org.dsa11.team1.kumarketto.domain.enums.Status;
import org.dsa11.team1.kumarketto.repository.AdminReplyRepository;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.repository.SupportBoardRepository;
import org.dsa11.team1.kumarketto.service.AdminReplyService;
import org.dsa11.team1.kumarketto.service.InquiryAttachmentService;
import org.dsa11.team1.kumarketto.service.SupportBoardService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URL;
import java.security.Principal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Controller
@Slf4j
@RequiredArgsConstructor
public class SupportBoardController {
    private final SupportBoardService supportBoardService;
    private final SupportBoardRepository supportBoardRepository;
    private final AdminReplyRepository adminReplyRepository;
    private final AdminReplyService adminReplyService;
    private final InquiryAttachmentService inquiryAttachmentService;
    private final MemberRepository memberRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadPath;


    @GetMapping("inquires")
    public String inquires(Model model, @RequestParam(value="page", defaultValue="0") int page){
        // 1. 페이지 요청 정보 생성
        // page: 현재 페이지 (0부터 시작)
        // size: 한 페이지에 보여줄 게시글 수
        Pageable pageable = PageRequest.of(page, 5);

        // 2. 서비스에서 페이징된 데이터 가져오기
        Page<SupportDTO> paging = supportBoardService.findList(pageable);


        // 3. 페이지네이션 바를 위한 시작/끝 페이지 번호 계산
        int nowPage = paging.getPageable().getPageNumber() + 1; // 현재 페이지 (1부터 시작)
        int blockSize = 5; // 한 블록에 표시할 페이지 수
        int startPage = Math.max(1, nowPage - (blockSize / 2));
        int endPage = Math.min(paging.getTotalPages(), startPage + blockSize - 1);

        // 블록의 시작 페이지를 재조정 (끝 페이지에 맞춤)
        if (endPage - startPage + 1 < blockSize) {
            startPage = Math.max(1, endPage - blockSize + 1);
        }

        // 4. 모델에 데이터 추가
        model.addAttribute("paging", paging);
        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "/community/inquires";
    }

    @GetMapping("inquires/inquiresWrite")
    public String inquiresWrite(Model model, Principal principal){
        if (principal != null) {
            String username = principal.getName();
            model.addAttribute("currentUserId", principal.getName());
            Optional<MemberEntity> memberOptional = memberRepository.findByUserId(username);
            if (memberOptional.isPresent()) {
                MemberEntity member = memberOptional.get();
                Long userNo = member.getUserNo();
                model.addAttribute("currentUserNo", userNo);
                model.addAttribute("currentUsername", username);
            } else {
                log.warn("현재 로그인된 사용자 '{}'의 정보를 DB에서 찾을 수 없습니다.", username);
            }
        }
        return "/community/inquiresWrite";
    }

    @PostMapping("inquires/inquiresWrite")
    public String inquiresWrite(@ModelAttribute SupportDTO dto,
                                @RequestParam(name="upload", required=false) MultipartFile upload,
                                RedirectAttributes ra) {
        try {
            supportBoardService.write(dto, "(unused)", upload);
            if (dto.getInquiryId() != null && upload != null && !upload.isEmpty()) {
                inquiryAttachmentService.attachOrReplace(dto.getInquiryId(), upload);
            }
            ra.addFlashAttribute("msg", "문의가 등록되었습니다.");
            return "redirect:/inquires";
        } catch (Exception e) {
            log.error("문의 작성 실패", e);
            ra.addFlashAttribute("error", "문의 작성 중 오류가 발생했습니다.");
            return "redirect:/inquires";
        }
    }


    /**
     * 게시글 상세보기 및 답글 조회 (접근 제어 로직 추가)
     */
    @GetMapping("inquires/detail/{inquiryId}")
    public String inquiresDetail(Model model, @PathVariable("inquiryId") Long inquiryId, Principal principal, RedirectAttributes ra){
        Optional<SupportBoardEntity> result = supportBoardRepository.findById(inquiryId);

        if(result.isPresent()){
            SupportBoardEntity entity = result.get();

            // === 접근 제어 로직 시작 ===
            if (entity.getIsPublic() == IsPublic.PRIVATE) {
                // 현재 인증된 사용자 정보 가져오기
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                // 사용자가 관리자(ADMIN) 권한을 가졌는지 확인
                boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                // 로그인한 사용자가 게시글 작성자인지 확인
                boolean isAuthor = principal != null && principal.getName().equals(entity.getUserNo().getUserId());

                // 관리자도 아니고, 작성자도 아닐 경우 접근 차단
                if (!isAdmin && !isAuthor) {
                    ra.addFlashAttribute("error", "비공개 글입니다. 접근 권한이 없습니다.");
                    return "redirect:/inquires";
                }
            }
            // === 접근 제어 로직 끝 ===

            List<AdminReplyDTO> inquiries = adminReplyService.findList(inquiryId);
            model.addAttribute("inquiries",inquiries);

            model.addAttribute("data", entity);
            if(!inquiries.isEmpty()){
                entity.setStatus(Status.COMPLETED);
                supportBoardRepository.save(entity);
            } else {
                entity.setStatus(Status.PENDING);
                supportBoardRepository.save(entity);
            }
            String key = entity.getAttachmentUrl();
            if (key != null && !key.isBlank()) {
                model.addAttribute("attachmentName", extractOriginalNameFromKey(key));
            }
            return "/community/inquiresDetail";
        }

        ra.addFlashAttribute("error", "존재하지 않는 게시글입니다.");
        return "redirect:/inquires";
    }

    private String extractOriginalNameFromKey(String key) {
        String base = key.substring(key.lastIndexOf('/') + 1);
        int idx = base.indexOf('_');
        return (idx >= 0 && idx + 1 < base.length()) ? base.substring(idx + 1) : base;
    }

    @GetMapping("inquires/inquiresedit/{inquiryId}")
    public String edit(Model model, @PathVariable("inquiryId") Long inquiryId) {
        Optional<SupportBoardEntity> result = supportBoardRepository.findById(inquiryId);
        if (result.isPresent()){
            model.addAttribute("data", result.get());
            String key = result.get().getAttachmentUrl();
            if (key != null && !key.isBlank()) {
                model.addAttribute("attachmentName", extractOriginalNameFromKey(key));
            }
            return "/community/inquiresEdit";
        } else {
            return "redirect:/inquires";
        }
    }

    @PostMapping("inquires/inquiresedit")
    public String edit(@ModelAttribute SupportDTO dto,
                       @RequestParam(name="upload", required=false) MultipartFile upload) {
        supportBoardService.supportedit(dto);
        if (upload != null && !upload.isEmpty()) {
            inquiryAttachmentService.attachOrReplace(dto.getInquiryId(), upload);
        }
        return "redirect:/inquires";
    }

    @GetMapping("inquires/inquiresdelete/{inquiryId}")
    public String inquiresdelete(@PathVariable("inquiryId") Long inquiryId){
        supportBoardService.delete(inquiryId);
        return "redirect:/inquires";
    }

    @GetMapping("/inquires/download/{id}")
    public String download(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            URL url = inquiryAttachmentService.presignedDownloadUrl(id, Duration.ofMinutes(5));
            return "redirect:" + url.toString();
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", "첨부 파일이 없습니다.");
            return "redirect:/inquires";
        } catch (Exception e) {
            log.error("다운로드 실패 id={}", id, e);
            ra.addFlashAttribute("error", "다운로드 중 오류가 발생했습니다.");
            return "redirect:/inquires";
        }
    }
}