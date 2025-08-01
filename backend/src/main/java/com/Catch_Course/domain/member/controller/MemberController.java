package com.Catch_Course.domain.member.controller;

import com.Catch_Course.domain.member.dto.MemberDto;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import com.Catch_Course.global.Rq;
import com.Catch_Course.global.dto.RsData;
import com.Catch_Course.global.exception.ServiceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MemberController", description = "회원 관련 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final Rq rq;

    record JoinReqBody(@NotBlank @Length(min = 3) String username,
                       @NotBlank @Length(min = 3) String password,
                       @NotBlank @Length(min = 3) String nickname) {
    }

    @Operation(summary = "회원 가입")
    @PostMapping("/join")
    public RsData<MemberDto> join(@RequestBody @Valid JoinReqBody body) {

        memberService.findByUsername(body.username())
                .ifPresent(member -> {
                    throw new ServiceException("400-1", "중복된 아이디입니다.");
                });


        Member member = memberService.join(body.username(), body.password(), body.nickname());

        return new RsData<>(
                "201-1",
                "회원 가입이 완료되었습니다.",
                new MemberDto(member)
        );
    }


    record LoginReqBody(@NotBlank @Length(min = 3) String username,
                        @NotBlank @Length(min = 3) String password) {
    }

    record LoginResBody(MemberDto memberDto, String apiKey, String accessToken) {
    }

    @Operation(summary = "로그인", description = "로그인 성공 시 ApiKey와 AccessToken 반환. 쿠키로도 반환")
    @PostMapping("/login")
    public RsData<LoginResBody> login(@RequestBody @Valid LoginReqBody body, HttpServletResponse response) {

        Member member = memberService.findByUsername(body.username())
                .orElseThrow(() -> new ServiceException("401-2", "아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!member.getPassword().equals(body.password())) {
            throw new ServiceException("401-2", "아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        String accessToken = memberService.getAccessToken(member);

        // 쿠키에 추가
        rq.addCookie("accessToken", accessToken);
        rq.addCookie("apiKey", member.getApiKey());

        return new RsData<>(
                "200-1",
                "%s님 환영합니다.".formatted(member.getNickname()),
                new LoginResBody(
                        new MemberDto(member),
                        member.getApiKey(),
                        accessToken
                )
        );
    }

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public RsData<MemberDto> me() {
        Member dummyMember = rq.getDummyMember();
        Member member = rq.getMember(dummyMember);  // 실제 객체

        return new RsData<>(
                "200-1",
                "내 정보 조회가 완료되었습니다.",
                new MemberDto(member)
        );
    }

    @Operation(summary = "로그아웃")
    @DeleteMapping("/logout")
    public RsData<Void> logout() {

        // 쿠키에서 제거
        rq.removeCookie("accessToken");
        rq.removeCookie("apiKey");

        return new RsData<>(
                "200-1",
                "로그아웃이 완료되었습니다."
        );
    }
}
