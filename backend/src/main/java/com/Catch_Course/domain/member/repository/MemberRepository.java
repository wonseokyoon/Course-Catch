package com.Catch_Course.domain.member.repository;

import com.Catch_Course.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>{
    Optional<Member> findByUsernameAndDeleteFlagFalse(String username);
    Optional<Member> findByEmail(String email);
    List<Member> findByDeleteFlagTrueAndModifiedDateBefore(LocalDateTime oneYearAgo);
    Optional<Member> findByUsername(String username);
}