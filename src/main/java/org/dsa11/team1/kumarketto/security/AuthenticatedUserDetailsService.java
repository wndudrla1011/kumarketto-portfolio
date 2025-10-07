package org.dsa11.team1.kumarketto.security;

import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthenticatedUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {

        MemberEntity memberEntity = memberRepository.findByUserId(userId)
                .orElseThrow(() -> { return new UsernameNotFoundException("사용자 없음"); });

        AuthenticatedUser authenticatedUser = AuthenticatedUser.builder()
                .userNo(memberEntity.getUserNo())
                .userId(memberEntity.getUserId())
                .password(memberEntity.getPassword())
                .role(memberEntity.getRole())
                .enabled(memberEntity.getEnabled())
                .build();
        return authenticatedUser;
    }
}
