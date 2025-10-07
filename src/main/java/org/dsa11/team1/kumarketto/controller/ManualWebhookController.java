package org.dsa11.team1.kumarketto.controller;

import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class ManualWebhookController {

    private final PaymentService paymentService;

    /**
     * 로컬 테스트를 위해 수동으로 결제 성공을 처리하는 웹훅
     * @param transactionId 성공 처리할 거래 ID
     * @return 성공 메시지
     */
    @GetMapping("/manual-webhook/{transactionId}")
    @ResponseBody
    public ResponseEntity<String> manualWebhook(@PathVariable Long transactionId) {
        try {
            paymentService.manuallyConfirmPayment(transactionId);
            return ResponseEntity.ok("Transaction " + transactionId + " successfully marked as PAID.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing transaction: " + e.getMessage());
        }
    }
}
