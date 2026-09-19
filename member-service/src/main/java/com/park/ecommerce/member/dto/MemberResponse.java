package com.park.ecommerce.member.dto;

import com.park.ecommerce.member.domain.Member;
import com.park.ecommerce.member.domain.Provider;
import com.park.ecommerce.member.domain.Role;

public record MemberResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl,
        Provider provider,
        Role role
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getProvider(),
                member.getRole()
        );
    }
}
