package org.dsa11.team1.kumarketto.controller;

import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatApiController {

    private final S3Service s3Service;

    /**
     * 채팅 이미지 업로드를 처리합니다.
     * 이미지를 받아 S3에 업로드하고 공개 URL을 반환합니다.
     * @param imageFile 클라이언트에서 전송된 이미지 파일.
     * @return `imageUrl`을 담은 JSON 객체.
     */
    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadChatImage(@RequestParam("image") MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "이미지 파일이 필요합니다."));
        }
        try {
            // 기존 S3Service를 사용하여 파일 업로드
            String imageUrl = s3Service.uploadFile(imageFile);
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (IOException e) {
            // 실제 애플리케이션에서는 이 에러를 로깅하는 것이 좋습니다.
            return ResponseEntity.internalServerError().body(Map.of("error", "이미지 업로드에 실패했습니다."));
        }
    }
}