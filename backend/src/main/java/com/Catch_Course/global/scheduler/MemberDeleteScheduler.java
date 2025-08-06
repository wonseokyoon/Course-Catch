package com.Catch_Course.global.scheduler;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MemberDeleteScheduler {
    private final MemberRepository memberRepository;

    @Scheduled(cron = "0 0 0 * * *")
//    @Scheduled(cron = "30 53 17 * * *")
    public void deleteMember() {
        // 탈퇴 후 1년이 지난 회원 조회
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        List<Member> deleteList = memberRepository.findByDeleteFlagTrueAndModifiedDateBefore(oneYearAgo);

        if(!deleteList.isEmpty()) {
            memberRepository.deleteAll(deleteList);
        }
    }
}
