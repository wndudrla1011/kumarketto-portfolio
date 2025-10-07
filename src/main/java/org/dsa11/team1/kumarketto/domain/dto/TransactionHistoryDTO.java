package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.enums.TransactionStatus;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistoryDTO {

    private Long pid;           // 상품 ID
    private String name;        // 상품명
    private Integer price;      // 상품 가격
    private String imageUrl;    // 상품 이미지 URL
    private String regionName;  // 상품 지역명

    private LocalDateTime confirmTime; // 거래 확정 시간
    private TransactionStatus status;

    public TransactionHistoryDTO(Long pid, String name, Integer price, String imageUrl,
                                 String prefName, String muniName,
                                 LocalDateTime confirmTime, TransactionStatus status) {
        this.pid = pid;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.regionName = (prefName != null && muniName != null) ? prefName + " · " + muniName : null;
        this.confirmTime = confirmTime;
        this.status = status;
    }
}
