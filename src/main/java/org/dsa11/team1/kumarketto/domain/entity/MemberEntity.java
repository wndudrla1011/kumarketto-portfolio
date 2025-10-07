package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.enums.Role;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@DynamicInsert
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "member")
public class MemberEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_no")
    private Long userNo; // 회원 ID

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId; // 로그인 ID

    @Column(name = "password", nullable = false, length = 255)
    private String password; // 비밀번호

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER; // 권한

    @Column(nullable = false, unique = true, length = 20)
    private String nickname; // 닉네임

    @Column(nullable = false, unique = true, length = 100)
    private String email; // 이메일

    @Column(length = 15)
    private String phone; // 연락처

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate; // 생년월일

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true; // 활성 여부

    @OneToMany(mappedBy = "member")
    private List<Product> products = new ArrayList<>(); // 판매 상품

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Store store; // 상점

    @PrePersist
    public void prePersist() {
        if (nickname == null || nickname.isEmpty()) {
            nickname = userId; // nickname 기본값 처리
        }
    }

    /* 연관 관계 편의 메서드 */
    public void bindStore(Store store) {
        this.store = store;
    }

}