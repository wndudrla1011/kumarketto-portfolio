package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MemberResponseDTO {    //서버->클라이언트
    private Long userNo;
    private String userId;
    private Role role;
    private String nickname;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private Boolean enabled;
}
