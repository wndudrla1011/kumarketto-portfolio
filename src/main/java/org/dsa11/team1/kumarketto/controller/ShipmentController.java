package org.dsa11.team1.kumarketto.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.ShipmentRequestDTO;
import org.dsa11.team1.kumarketto.domain.dto.ShipmentResponseDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.dsa11.team1.kumarketto.service.ShipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions/{transactionId}/shipment")
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final MemberRepository memberRepository;

    /**
     * 배송 요청에 대한 응답
     * @param transactionId     진행 중 거래
     * @param requestDTO        송장번호 입력 요청
     * @param userDetails       판매자
     * @return  ResponseEntity<ShipmentResponseDTO>
     */
    @PostMapping
    public ResponseEntity<ShipmentResponseDTO> createShipment(
            @PathVariable Long transactionId,
            @RequestBody ShipmentRequestDTO requestDTO,
            @AuthenticationPrincipal AuthenticatedUser userDetails) {

        log.info("배송 정보 생성");

        // 회원 객체 조회
        MemberEntity member = getMember(userDetails);

        // 배송 응답 생성
        ShipmentResponseDTO responseDTO = shipmentService.createShipment(transactionId,
                member.getUserNo(), // 판매자 ID
                requestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);

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
