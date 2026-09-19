package com.park.ecommerce.member;

import com.park.ecommerce.exception.AuthErrorCode;
import com.park.ecommerce.exception.AuthException;
import com.park.ecommerce.member.domain.Member;
import com.park.ecommerce.member.dto.MemberResponse;
import com.park.ecommerce.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    // 회원 정보 조회
    @Transactional(readOnly = true)
    public MemberResponse getMyInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.MEMBER_NOT_FOUND));

        return MemberResponse.from(member);
    }
}
