package com.Catch_Course.domain.member.controller;

import com.Catch_Course.domain.email.dto.TempMemberInfo;
import com.Catch_Course.domain.email.service.EmailService;
import com.Catch_Course.domain.member.dto.MemberDto;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import com.Catch_Course.global.Rq;
import com.Catch_Course.global.dto.RsData;
import com.Catch_Course.global.exception.ServiceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MemberController", description = "회원 관련 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final Rq rq;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    record JoinReqBody(@NotBlank @Length(min = 3) String username,
                       @NotBlank @Length(min = 3) String password,
                       @NotBlank @Length(min = 3) String nickname,
                       @NotBlank @Email String email,
                       String profileImageUrl) {
    }

    @Operation(summary = "회원 가입 1단계", description = "이메일 인증 코드 발송")
    @PostMapping("/send-code")
    public RsData<MemberDto> sendCode(@RequestBody @Valid JoinReqBody body) {
        // 인증 전 체크
        memberService.checkVerification(body.username, body.email);

        // 인증 코드 생성
        String verificationCode = emailService.createVerificationCode();

        // 임시 정보 생성
        emailService.saveTempMemberInfo(body.email, body.username, body.nickname, body.password, body.profileImageUrl, verificationCode);

        // 메일 전송
        emailService.sendVerificationCode(body.email, verificationCode);

        return new RsData<>(
                "201-1",
                "인증 코드가 메일로 전송되었습니다."
        );
    }

    record JoinReqBody2(@NotBlank @Email String email,
                        @NotBlank String verificationCode) {
    }

    @Operation(summary = "회원 가입 2단계", description = "인증 코드 확인 및 최종 가입")
    @PostMapping("/verify-code")
    public RsData<MemberDto> verifyAndJoin(@RequestBody @Valid JoinReqBody2 body) {
        // 인증 코드 검증 및 임시 회원 정보 반환
        TempMemberInfo tempMemberInfo = emailService.verifyCode(body.email, body.verificationCode);

        // 회원 생성
        Member member = memberService.join(tempMemberInfo.getUsername(), tempMemberInfo.getPassword(),
                tempMemberInfo.getNickname(), tempMemberInfo.getEmail(), tempMemberInfo.getProfileImageUrl());

        // 임시 회원 정보 삭제
        emailService.deleteTempMemberInfo(body.email);

        return new RsData<>(
                "201-2",
                "인증이 완료되었습니다. 회원가입을 축하합니다.",
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
    public RsData<LoginResBody> login(@RequestBody @Valid LoginReqBody body) {

        Member member = memberService.findByUsername(body.username())
                .orElseThrow(() -> new ServiceException("401-2", "아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(body.password(), member.getPassword())) {
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
    public RsData<Void> logout(HttpSession session) {
        // 쿠키에서 제거
        rq.removeCookie("accessToken");
        rq.removeCookie("apiKey");

        // 세션 무효화
        session.invalidate();
        return new RsData<>(
                "200-1",
                "로그아웃이 완료되었습니다."
        );
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/withdraw")
    public RsData<Void> withdraw(HttpSession session) {
        Member member = rq.getMember(rq.getDummyMember());

        memberService.withdraw(member.getId());

        // 탈퇴 후 로그아웃 처리
        rq.removeCookie("accessToken");
        rq.removeCookie("apiKey");
        session.invalidate();

        return new RsData<>(
                "200-1",
                "회원탈퇴가 완료되었습니다."
        );
    }

    @Operation(summary = "계정 복구 1단계", description = "이메일 인증 코드 발송")
    @PostMapping("/restore-send")
    public RsData<MemberDto> restoreSendCode(@RequestBody @Valid RestoreReqBody2 body) {
        // 인증 코드 생성
        String verificationCode = emailService.createVerificationCode();

        // Redis에 이메일과 인증 코드 저장(임시 회원정보를 생성하는 대신)
        emailService.saveEmailAndVerificationCode(body.email, verificationCode);

        // 메일 전송
        emailService.sendVerificationCode(body.email, verificationCode);

        return new RsData<>(
                "201-1",
                "인증 코드가 메일로 전송되었습니다."
        );

    }

    record RestoreReqBody2(@NotBlank @Email String email) {
    }

    @Operation(summary = "계정 복구 2단계", description = "인증 코드 확인 및 최종 복구")
    @PostMapping("/restore-verify")
    public RsData<?> verifyAndRestore(@RequestBody @Valid JoinReqBody2 body) {
        // DB에 없는 메일
        Member member = memberService.findByEmail(body.email)
                .orElseThrow(() -> new ServiceException("403-2", "복구 가능한 메일이 아닙니다."));

        // 인증 코드 검증
        emailService.restoreVerifyCode(body.email, body.verificationCode);

        // 계정 복구
        member = memberService.restoreMember(member);

        // Redis에서 인증 정보 삭제
        emailService.deleteRestoreData(body.email);

        return new RsData<>(
                "201-3",
                "계정이 복구되었습니다.",
                new MemberDto(member)
        );
    }

    record UpdateProfileReqBody(String nickname,
                                String newPassword,
                                @Email String email,
                                String profileImageUrl) {
    }

    @Operation(summary = "프로필 수정")
    @PutMapping("/me")
    public RsData<MemberDto> updateProfile(@RequestBody @Valid UpdateProfileReqBody body) {
        Member dummyMember = rq.getDummyMember();
        Long memberId = dummyMember.getId();    // 동시성 고려해서 실제 객체 대신 id 전달

        Member updatedMember = memberService.updateProfile(memberId, body.nickname,body.newPassword,body.email, body.profileImageUrl);

        return new RsData<>(
                "200-1",
                "프로필 수정이 완료되었습니다. 다시 로그인 해주세요.",
                new MemberDto(updatedMember)
        );
    }

    record verifyPasswordReqBody(@NotBlank @Length(min = 3) String password) {
    }

    @Operation(summary = "비밀번호 인증")
    @PostMapping("/verify-password")
    public RsData<MemberDto> updateProfile(@RequestBody @Valid verifyPasswordReqBody body) {
        Member dummyMember = rq.getDummyMember();
        Long memberId = dummyMember.getId();    // 동시성 고려해서 실제 객체 대신 id 전달

        Member member = memberService.checkPassword(memberId, body.password());

        return new RsData<>(
                "200-1",
                "인증되었습니다.",
                new MemberDto(member)
        );
    }


}

