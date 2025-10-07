package org.dsa11.team1.kumarketto.service;

import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.domain.dto.MemberRequestDTO;
import org.dsa11.team1.kumarketto.domain.dto.MemberResponseDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@RequiredArgsConstructor
@Transactional
@Service
public class MemberService {

    public final MemberRepository memberRepository;
    public final BCryptPasswordEncoder passwordEncoder;

    public void signUp(MemberRequestDTO memberRequestDTO) {

        LocalDate birthDate = LocalDate.of(
                Integer.parseInt(memberRequestDTO.getYear()),
                Integer.parseInt(memberRequestDTO.getMonth()),
                Integer.parseInt(memberRequestDTO.getDay())
        );

        MemberEntity memberEntity = MemberEntity.builder().
                userId(memberRequestDTO.getUserId()).
                password(passwordEncoder.encode(memberRequestDTO.getPassword())).
                nickname(memberRequestDTO.getNickname()).
                email(memberRequestDTO.getEmail()).
                phone(memberRequestDTO.getPhone()).
                birthDate(birthDate).
                build();

        memberRepository.save(memberEntity);
    }

    public String findIdByEmail(String email) {
        return memberRepository.findByEmail(email)
                .map(MemberEntity::getUserId)
                .orElse(null);
    }

    public boolean existsByIdAndBirth(MemberRequestDTO memberRequestDTO) {
        String userId = memberRequestDTO.getUserId();
        LocalDate birthDate = LocalDate.of(
                Integer.parseInt(memberRequestDTO.getYear()),
                Integer.parseInt(memberRequestDTO.getMonth()),
                Integer.parseInt(memberRequestDTO.getDay())
        );
        return memberRepository.existsByUserIdAndBirthDate(userId, birthDate);
    }

    public void resetPw(String userId, String password) {
        MemberEntity memberEntity = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        memberEntity.setPassword(passwordEncoder.encode(password));
        memberRepository.save(memberEntity);
    }

    public boolean existsByUserIdAndEnabledTrue(String userId) {
        return memberRepository.existsByUserIdAndEnabledTrue(userId);
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    public boolean existsByNickname(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    public MemberResponseDTO findUserById(String userId) {
        MemberEntity memberEntity = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return MemberResponseDTO.builder()
                .userNo(memberEntity.getUserNo())
                .userId(memberEntity.getUserId())
                .role(memberEntity.getRole())
                .nickname(memberEntity.getNickname())
                .email(memberEntity.getEmail())
                .phone(memberEntity.getPhone())
                .birthDate(memberEntity.getBirthDate())
                .createdDate(memberEntity.getCreatedDate())
                .modifiedDate(memberEntity.getModifiedDate())
                .enabled(memberEntity.getEnabled())
                .build();
    }

    public void updateProfile(String userId, MemberRequestDTO memberRequestDTO) {
        MemberEntity memberEntity = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("ユーザーが存在しません。"));

        if (memberRequestDTO.getPassword() != null && !memberRequestDTO.getPassword().isBlank()) {
            memberEntity.setPassword(passwordEncoder.encode(memberRequestDTO.getPassword()));
        }

        memberEntity.setEmail(memberRequestDTO.getEmail());
        memberEntity.setPhone(memberRequestDTO.getPhone());
        memberEntity.setNickname(memberRequestDTO.getNickname());

        LocalDate birthDate = LocalDate.of(
                Integer.parseInt(memberRequestDTO.getYear()),
                Integer.parseInt(memberRequestDTO.getMonth()),
                Integer.parseInt(memberRequestDTO.getDay())
        );

        memberEntity.setBirthDate(birthDate);

        memberRepository.save(memberEntity);
    }

    public void disableUser(AuthenticatedUser user) {
        MemberEntity memberEntity = memberRepository.findByUserId(user.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        memberEntity.setEnabled(false);
    }

    @Transactional(readOnly = true)
    public MemberEntity findMemberByUserId(String userId) {
        return memberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));}
}
