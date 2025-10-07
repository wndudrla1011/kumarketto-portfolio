package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MemberRequestDTO { //일반 유저->서버
    private String userId;
    private String password;
    private String nickname;
    private String email;
    private String phone;

    //signUp.html select에서 받는 임시 필드
    private String year;
    private String month;
    private String day;

    //Entity에 넣을 LocalDate
    private LocalDate birthDate;
}
