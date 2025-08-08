package com.Catch_Course.domain.member.controller;

import com.Catch_Course.domain.member.dto.MemberDto;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.ProfileService;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "ProfileController", description = "프로필 관련 API")
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final Rq rq;

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

    record UpdateProfileReqBody(String nickname,
                                String newPassword,
                                @Email String email,
                                String profileImageUrl) {
    }

    @Operation(summary = "프로필 수정")
    @PutMapping("/me")
    public RsData<MemberDto> updateProfile(@RequestBody @Valid UpdateProfileReqBody body,HttpSession session) {
        Object attribute = session.getAttribute("passwordVerified");
        boolean isVerified = attribute != null ? (boolean) attribute : false;

        if(!isVerified) {
            throw new ServiceException("403-4","비밀번호 인증이 필요합니다.");
        }

        Member dummyMember = rq.getDummyMember();
        Long memberId = dummyMember.getId();    // 동시성 고려해서 실제 객체 대신 id 전달

        Member updatedMember = profileService.updateProfile(memberId, body.nickname,body.newPassword,body.email, body.profileImageUrl,session);

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
    public RsData<MemberDto> updateProfile(@RequestBody @Valid verifyPasswordReqBody body,HttpSession session) {
        Member dummyMember = rq.getDummyMember();
        Long memberId = dummyMember.getId();    // 동시성 고려해서 실제 객체 대신 id 전달

        profileService.checkPassword(memberId, body.password(), session);

        return new RsData<>(
                "200-1",
                "인증되었습니다."
        );
    }
}

