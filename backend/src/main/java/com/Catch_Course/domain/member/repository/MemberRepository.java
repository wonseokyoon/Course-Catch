package com.Catch_Course.domain.member.repository;

import com.Catch_Course.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>{
    Optional<Member> findByUsername(String username);
    Optional<Member> findByApiKey(String apiKey);

    boolean existsByEmail(String email);
}