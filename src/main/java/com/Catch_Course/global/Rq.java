package com.Catch_Course.global;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import com.Catch_Course.global.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Optional;

// Request, Response, Session, Cookie, Header
@Component
@RequiredArgsConstructor
@RequestScope   // 요청마다 값을 생성(값 갱신)
public class Rq {

    private final HttpServletRequest request;
    private final MemberService memberService;

    public Member getAuthenticatedActor() {

        String authorizationValue = request.getHeader("Authorization");
        String apiKey = authorizationValue.substring("Bearer ".length());
        Optional<Member> opActor = memberService.findByApiKey(apiKey);

        if(opActor.isEmpty()) {
            throw new ServiceException("401-1", "잘못된 비밀번호 입니다.");
        }

        return opActor.get();

    }
}
