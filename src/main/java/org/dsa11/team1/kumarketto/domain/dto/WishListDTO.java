package org.dsa11.team1.kumarketto.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WishListDTO {

    private Long wishId;
    private Long wishPId;
    private Long wishUserNo;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}
