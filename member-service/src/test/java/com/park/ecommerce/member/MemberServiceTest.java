package com.park.ecommerce.member;

import com.park.ecommerce.exception.AuthErrorCode;
import com.park.ecommerce.exception.AuthException;
import com.park.ecommerce.member.domain.Member;
import com.park.ecommerce.member.domain.Provider;
import com.park.ecommerce.member.dto.MemberResponse;
import com.park.ecommerce.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("존재하는 회원 ID로 조회하면 회원 정보를 반환한다")
    void returnsMemberInfoWhenMemberExists() {
        Member member = member();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        MemberResponse response = memberService.getMyInfo(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.nickname()).isEqualTo("테스트유저");
        assertThat(response.provider()).isEqualTo(Provider.KAKAO);
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 조회하면 예외를 던진다")
    void throwsExceptionWhenMemberNotFound() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMyInfo(1L))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.MEMBER_NOT_FOUND);
    }

    private static Member member() {
        Member member = Member.builder()
                .provider(Provider.KAKAO)
                .providerId("12345")
                .email("test@example.com")
                .nickname("테스트유저")
                .build();
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }
}
