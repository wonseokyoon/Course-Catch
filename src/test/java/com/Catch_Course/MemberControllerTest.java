package com.Catch_Course;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.repository.MemberRepository;
import com.Catch_Course.domain.member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class MemberControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;

    @Test
    public void testSaveAndFind() {
        // 새 사용자 저장
        Member member = Member.builder()
                .username("testUser")
                .password("testPassword")
                .nickname("testNickname")
                .apiKey("testApiKey")
                .build();
        memberRepository.save(member);

        // 저장된 사용자 조회
        Member findUser = memberRepository.findById(member.getId()).orElse(null);
        assertThat(findUser).isNotNull();
        assertThat(findUser.getNickname()).isEqualTo("testNickname");
    }
}