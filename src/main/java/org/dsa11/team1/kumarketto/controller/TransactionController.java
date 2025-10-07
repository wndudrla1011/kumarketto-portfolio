package org.dsa11.team1.kumarketto.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.TransactionApprovalRequestDTO;
import org.dsa11.team1.kumarketto.domain.dto.TransactionRequestDTO;
import org.dsa11.team1.kumarketto.domain.dto.TransactionResponseDTO;
import org.dsa11.team1.kumarketto.domain.dto.TransactionTypeUpdateRequestDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.dsa11.team1.kumarketto.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final MemberRepository memberRepository;

    /**
     * 거래 요청
     * @param requestDTO    요청 데이터
     * @param userDetails   요청자(구매자)
     * @return              201 응답(TransactionResponseDTO)
     */
    @PostMapping
    public ResponseEntity<TransactionResponseDTO> createTransaction(
            @RequestBody TransactionRequestDTO requestDTO,
            @AuthenticationPrincipal AuthenticatedUser userDetails) {

        log.info("거래 요청으로 거래 생성");

        // 회원 객체 조회
        MemberEntity member = getMember(userDetails);

        // 응답 DTO 생성
        TransactionResponseDTO responseDTO = transactionService.createTransaction(
                requestDTO.getProductId(),
                member.getUserNo()
        );

        // 201 응답
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);

    }

    /**
     * 판매자 거래 승인 및 거절
     * @param transactionId     거래 ID
     * @param requestDto        거래 상태
     * @param userDetails       회원 정보
     * @return                  ResponseEntity<TransactionResponseDTO>
     */
    @PatchMapping("/{transactionId}/approval")
    public ResponseEntity<TransactionResponseDTO> approveTransaction(
            @PathVariable Long transactionId,
            @RequestBody TransactionApprovalRequestDTO requestDto,
            @AuthenticationPrincipal AuthenticatedUser userDetails) {

        log.info("판매자 >>>> 거래 응답");

        // 회원 객체 조회
        MemberEntity member = getMember(userDetails);

        TransactionResponseDTO responseDto = transactionService.processTransactionApproval(
                transactionId,
                member.getUserNo(),
                requestDto.getStatus()
        );
        return ResponseEntity.ok(responseDto);

    }

    /**
     * 수동 구매 확정 처리
     * @param transactionId     거래 ID
     * @param userDetails       구매자 ID
     * @return  ResponseEntity<Void>
     */
    @PostMapping("/{transactionId}/confirm")
    public ResponseEntity<Void> confirmPurchase(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal AuthenticatedUser userDetails) {

        log.info("구매자 구매 확정 버튼 클릭 처리");

        // 회원 객체 조회
        MemberEntity member = getMember(userDetails);

        transactionService.confirmPurchase(transactionId, member.getUserNo());
        return ResponseEntity.ok().build();

    }

    /**
     * 회원 객체 추출
     * @param userDetails   회원 객체
     * @return              Member
     */
    public MemberEntity getMember(AuthenticatedUser userDetails) {
        return memberRepository.findByUserId(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("회원을 찾지 못했습니다."));
    }

    /**
     * 거래 조회
     * @param transactionId 현재 거래 중인 거래 ID
     * @return  현재 거래 중인 정보
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDTO> getTransaction(
            @PathVariable Long transactionId) {
        // 해당 거래를 볼 권한이 있는지 확인하는 로직 필요
        TransactionResponseDTO responseDTO = transactionService.getTransactionDetails(transactionId);
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * 구매자가 거래 방식을 선택
     * @param transactionId 거래 ID
     * @param requestDto    거래 방식, 결제 방식
     * @return              업데이트된 거래 정보
     */
    @PatchMapping("/{transactionId}/type")
    public ResponseEntity<TransactionResponseDTO> updateTransactionType(
            @PathVariable Long transactionId,
            @RequestBody TransactionTypeUpdateRequestDTO requestDto,
            @AuthenticationPrincipal AuthenticatedUser userDetails) {

        MemberEntity member = getMember(userDetails);
        TransactionResponseDTO responseDto = transactionService.updateTransactionType(
                transactionId,
                member.getUserNo(), // 구매자 ID
                requestDto.getDeliveryService(),
                requestDto.getPaymentMethod()
        );

        return ResponseEntity.ok(responseDto);
    }

    /**
     * [신규] 판매자가 상품 전달 완료를 알림
     * @param transactionId 거래 ID
     * @param userDetails   판매자 정보
     * @return ResponseEntity<Void>
     */
    @PostMapping("/{transactionId}/hand-over")
    public ResponseEntity<Void> handOverItem(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal AuthenticatedUser userDetails) {
        log.info("판매자 상품 전달 완료 처리");
        MemberEntity member = getMember(userDetails);
        transactionService.handOverItem(transactionId, member.getUserNo());
        return ResponseEntity.ok().build();
    }


    /**
     * [신규] 구매자가 상품 수령 확인
     * @param transactionId 거래 ID
     * @param userDetails   구매자 정보
     * @return ResponseEntity<Void>
     */
    @PostMapping("/{transactionId}/receive-item")
    public ResponseEntity<Void> receiveItem(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal AuthenticatedUser userDetails) {
        log.info("구매자 상품 수령 확인 처리");
        MemberEntity member = getMember(userDetails);
        transactionService.confirmItemReceived(transactionId, member.getUserNo());
        return ResponseEntity.ok().build();
    }

}
