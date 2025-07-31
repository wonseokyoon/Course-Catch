package com.Catch_Course.domain.reservation.controller;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.reservation.dto.ReservationDto;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.service.ReservationService;
import com.Catch_Course.global.Rq;
import com.Catch_Course.global.dto.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "reservationController", description = "수강신청 관련 API")
@RestController
@RequestMapping("/api/reserve")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final Rq rq;

//    record ReserveReqBody(
//            @NotBlank Long courseId
//    ) {
//    }

    @Operation(summary = "수강 신청")
    @PostMapping()
    @Transactional
    public RsData<ReservationDto> reserve(@RequestParam Long courseId) {

        Member member = rq.getMember(rq.getDummyMember());  // 실제 멤버 객체

        Reservation reservation = reservationService.reserve(member, courseId);

        return new RsData<>(
                "200-1",
                "신청이 완료되었습니다.",
                new ReservationDto(reservation)
        );
    }

}
