package org.dsa11.team1.kumarketto.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.ReportsEntity;
import org.dsa11.team1.kumarketto.domain.enums.ActionType;
import org.dsa11.team1.kumarketto.domain.enums.ReportStatus;
import org.dsa11.team1.kumarketto.domain.dto.AdminResponseReportDTO;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.dsa11.team1.kumarketto.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/report")
@Controller
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/reportRequest")
    public String reportRequest(@RequestParam("productId") Long productId, Model model) {
        model.addAttribute("productId", productId);
        return "reportRequest";
    }
//    @GetMapping("/reports")
//    public String reports(Model model) {
//        List<AdminResponseReportDTO> adminResponseReportDTOList = reportService.reports();
//        model.addAttribute("reports", adminResponseReportDTOList);
//        return "reports";
//    }

    @GetMapping("/reportDetail")
    public String reportDetail(@RequestParam("reportId") Long reportId, Model model) {
        AdminResponseReportDTO adminResponseReportDTO = reportService.reportDetail(reportId);
        model.addAttribute("reportDetail", adminResponseReportDTO);
        model.addAttribute("isPending", adminResponseReportDTO.getReportStatus() == ReportStatus.PENDING);
        return "reportDetail";
    }

    @GetMapping("/reports")
    public String listReports(
            HttpServletRequest request,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "status", defaultValue = "ALL") String status,
            Model model) {

        model.addAttribute("url", request.getRequestURI());

        Pageable pageable = PageRequest.of(page, 20, Sort.by("createdDate").descending());
        Page<AdminResponseReportDTO> reportPage = reportService.getReports(status, pageable);

        int blockSize = 5;
        int currentPage = reportPage.getNumber();
        int startBlockPage = (currentPage / blockSize) * blockSize;
        int endBlockPage = Math.min(startBlockPage + blockSize - 1, reportPage.getTotalPages() - 1);

        model.addAttribute("reportList", reportPage.getContent());
        model.addAttribute("isFirst", reportPage.isFirst());
        model.addAttribute("isLast", reportPage.isLast());
        model.addAttribute("status", status);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startBlockPage", startBlockPage);
        model.addAttribute("endBlockPage", endBlockPage);
        model.addAttribute("totalPages", reportPage.getTotalPages());

        return "reports";
    }

}
