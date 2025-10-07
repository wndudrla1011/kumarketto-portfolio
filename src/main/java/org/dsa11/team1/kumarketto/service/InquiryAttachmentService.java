package org.dsa11.team1.kumarketto.service;

import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.SupportBoardEntity;
import org.dsa11.team1.kumarketto.repository.SupportBoardRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InquiryAttachmentService {

    private final SupportBoardRepository supportBoardRepository;
    private final S3Client s3;
    private final S3Presigner presigner;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    /** 첨부가 없던 글에 업로드 or 기존 첨부 교체 */
    @Transactional // // DB 작업과 파일 업로드를 하나의 "거래"처럼 묶어서 처리 (중간에 실패하면 되돌림)
    public void attachOrReplace(Long inquiryId, MultipartFile upload) {
        // 업로드 파일이 없거나 비어 있으면 그냥 아무 것도 하지 않고 종료
        if (upload == null || upload.isEmpty()) return;
        // 업로드 파일이 규칙에 맞는지 검사 (확장자, 크기 등 체크)
        validateUpload(upload);

        // 글 번호(inquiryId)에 해당하는 게시글을 DB에서 찾아옴
        // 없으면 예외 발생시켜버림
        SupportBoardEntity e = supportBoardRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의가 없습니다. id=" + inquiryId));
        // 기존에 DB에 저장되어 있던 첨부파일 경로(있을 수도, 없을 수도 있음)
        String oldKey = e.getAttachmentUrl();

        // 새로 저장할 파일의 경로(key) 만들기 → "inquiries/{id}/{UUID}_{원래파일명}" 같은 형식
        String newKey = buildKey(inquiryId, upload.getOriginalFilename());


        // S3에 업로드할 요청(Request) 만들기
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket) // 어느 S3 버킷에 넣을지
                .key(newKey) // 저장할 위치 (폴더/파일명 역할)
                .contentType(Optional.ofNullable(upload.getContentType()).orElse("application/octet-stream"))
                .contentDisposition(contentDispositionForDownload(upload.getOriginalFilename()))
                .build();

        try {
            s3.putObject(put, RequestBody.fromInputStream(upload.getInputStream(), upload.getSize()));
        } catch (Exception ex) {
            throw new RuntimeException("S3 업로드 실패", ex);
        }
        // DB에 새로운 첨부파일 경로 저장
        e.setAttachmentUrl(newKey);

        // 트랜잭션 결과에 맞춰 정리
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                if (oldKey != null && !oldKey.isBlank()) safeDelete(oldKey);
            }
            @Override public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) safeDelete(newKey);
            }
        });
    }

    /** 프리사인드 다운로드 URL (기본 5분) */
    @Transactional(readOnly = true)
    public URL presignedDownloadUrl(Long inquiryId, Duration validFor) {
        SupportBoardEntity e = supportBoardRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의가 없습니다. id=" + inquiryId));

        String key = e.getAttachmentUrl();
        if (key == null || key.isBlank())
            throw new IllegalStateException("첨부 파일이 없습니다.");

        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket).key(key).build();

        GetObjectPresignRequest preq = GetObjectPresignRequest.builder()
                .signatureDuration(validFor != null ? validFor : Duration.ofMinutes(5))
                .getObjectRequest(get)
                .build();

        PresignedGetObjectRequest pre = presigner.presignGetObject(preq);
        return pre.url();
    }

    /** 첨부 삭제 (S3 + DB 비우기) */
    @Transactional
    public void remove(Long inquiryId) {
        SupportBoardEntity e = supportBoardRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의가 없습니다. id=" + inquiryId));

        String key = e.getAttachmentUrl();
        if (key == null || key.isBlank()) return;

        safeDelete(key);
        e.setAttachmentUrl(null);
    }

    // ===== 유틸 =====

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of(
            "jpg","jpeg","png","gif","pdf","txt","doc","docx","xls","xlsx","ppt","pptx"
    );

    private void validateUpload(MultipartFile file) {
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("파일은 최대 5MB까지 업로드 가능합니다.");
        }
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED.contains(ext)) {
            throw new IllegalArgumentException("허용되지 않은 파일 형식입니다.");
        }
    }

    private String buildKey(Long inquiryId, String originalName) {
        String base = Optional.ofNullable(originalName).orElse("file");
        // 유니코드 글자/숫자 + . _ - 만 허용
        String safe = base.replaceAll("[^\\p{L}\\p{N}._-]", "_");
        return "inquiries/" + inquiryId + "/" + UUID.randomUUID() + "_" + safe;
    }

    private String contentDispositionForDownload(String originalName) {
        String ascii = (originalName == null ? "file" : originalName).replaceAll("[^\\x20-\\x7E]", "_");
        String encoded = URLEncoder.encode(
                Optional.ofNullable(originalName).orElse("file"), StandardCharsets.UTF_8);
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }

    private void safeDelete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception ignore) { /* 없어도 무시 */ }
    }
}