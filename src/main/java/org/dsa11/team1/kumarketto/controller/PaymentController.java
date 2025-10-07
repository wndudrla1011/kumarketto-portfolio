package org.dsa11.team1.kumarketto.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.PaymentIntentRequestDTO;
import org.dsa11.team1.kumarketto.domain.dto.PaymentIntentResponseDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.dsa11.team1.kumarketto.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://127.0.0.1:5501")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final MemberRepository memberRepository;

    /**
     * 결제 요청
     * @param requestDTO    결제 요청
     * @param userDetails   구매자
     * @return ResponseEntity<PaymentIntentResponseDTO>
     */
    @PostMapping("/intent")
    public ResponseEntity<PaymentIntentResponseDTO> createPaymentIntent(
            @RequestBody PaymentIntentRequestDTO requestDTO,
            @AuthenticationPrincipal AuthenticatedUser userDetails) {

        log.info("Stripe PaymentIntent 생성 요청");

        // 회원 객체
        MemberEntity member = getMember(userDetails);

        // 결제 응답 DTO
        PaymentIntentResponseDTO responseDTO = paymentService.createPaymentIntent(
                requestDTO.getTransactionId(),
                member.getUserNo() // 구매자 ID
        );

        return ResponseEntity.ok(responseDTO);

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

}
