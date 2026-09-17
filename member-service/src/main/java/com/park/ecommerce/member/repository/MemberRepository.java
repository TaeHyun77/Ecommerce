package com.park.ecommerce.member.repository;

import com.park.ecommerce.member.domain.Member;
import com.park.ecommerce.member.domain.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByProviderAndProviderId(Provider provider, String providerId);
}
