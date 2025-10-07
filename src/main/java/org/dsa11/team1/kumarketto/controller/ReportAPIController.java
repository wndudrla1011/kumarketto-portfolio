package org.dsa11.team1.kumarketto.controller;

import co.elastic.clients.elasticsearch.nodes.Http;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.UserRequestReportDTO;
import org.dsa11.team1.kumarketto.domain.entity.ReportsEntity;
import org.dsa11.team1.kumarketto.domain.enums.ActionType;
import org.dsa11.team1.kumarketto.domain.enums.ReportStatus;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.dsa11.team1.kumarketto.service.ReportService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/report")
@RestController
public class ReportAPIController {

    public final ReportService reportService;

    @PostMapping("/reportRequest")
    public String reportRequest(@ModelAttribute UserRequestReportDTO userRequestReportDTO,
                                @RequestParam("image") MultipartFile imageFile,
                                @AuthenticationPrincipal UserDetails userDetails) {
        userRequestReportDTO.setUserId(userDetails.getUsername());
        reportService.reportRequest(userRequestReportDTO, imageFile);

        return "success";
    }

    @GetMapping("/reportDetail/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("filePath") String filePath) {
        Path path = Paths.get(filePath);
        Resource resource = new FileSystemResource(path);
        log.debug("컨트롤러까지 왔음, 패스는: {}", path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + path.getFileName().toString() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }


    @PostMapping("/reject")
    public ResponseEntity<Void> rejectReport(@RequestParam("reportId") Long reportId,
                                             @AuthenticationPrincipal AuthenticatedUser user) {

        // 현재 로그인 유저가 관리자 권한인지 확인
        if (user == null || !user.hasRole("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        reportService.rejectReport(reportId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/hide")
    public ResponseEntity<Void> hidePost(@RequestParam("productId") Long productId,
                                         @RequestParam(value = "reportId", required = false) Long reportId,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        log.debug("reportId 컨트롤러로 정상적으로 받음: {}", reportId);

        reportService.hidePost(productId, reportId, user);
        return ResponseEntity.ok().build();
    }
}
