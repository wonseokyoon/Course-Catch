package com.Catch_Course.domain.reservation.controller;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.reservation.dto.PageDto;
import com.Catch_Course.domain.reservation.dto.ReservationDto;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.service.ReservationService;
import com.Catch_Course.global.Rq;
import com.Catch_Course.global.aop.CheckTime;
import com.Catch_Course.global.dto.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "reservationController", description = "수강신청 관련 API")
@RestController
@RequestMapping("/api/reserve")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final Rq rq;

    @Operation(summary = "수강 신청(대기열)")
    @PostMapping()
    @CheckTime
    public RsData<ReservationDto> reserve(@RequestParam Long courseId) {

        Member member = rq.getMember(rq.getDummyMember());  // 실제 멤버 객체

        // 대기열에 등록
        Reservation reservation = reservationService.addToQueue(member, courseId);

        return new RsData<>(
                "200-1",
                "신청이 접수되었습니다. 잠시 기다려주세요.",
                new ReservationDto(reservation)
        );
    }

    @Operation(summary = "수강 신청 취소")
    @DeleteMapping()
    public RsData<ReservationDto> cancelReservation(@RequestParam Long courseId) {

        Member member = rq.getMember(rq.getDummyMember());  // 실제 멤버 객체
        ReservationDto reservationDto = reservationService.cancelReserve(member, courseId);

        return new RsData<>(
                "200-1",
                "수강 취소되었습니다.",
                reservationDto
        );
    }

    @Operation(summary = "수강 목록 조회(결제 완료)")
    @GetMapping("/me")
    public RsData<PageDto> getReservationsCompleted(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "5") int pageSize) {
        Member member = rq.getMember(rq.getDummyMember());  // 실제 멤버 객체

        Page<Reservation> reservationPage = reservationService.getReservations(member, page, pageSize);

        return new RsData<>(
                "200-1",
                "신청 목록 조회가 완료되었습니다.",
                new PageDto(reservationPage)
        );
    }

    @Operation(summary = "수강 목록 조회(결제 대기)")
    @GetMapping("/me/pending")
    public RsData<PageDto> getReservations(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "5") int pageSize) {
        Member member = rq.getMember(rq.getDummyMember());  // 실제 멤버 객체

        Page<Reservation> reservationPage = reservationService.getReservationsPending(member, page, pageSize);

        return new RsData<>(
                "200-1",
                "신청 목록 조회가 완료되었습니다.",
                new PageDto(reservationPage)
        );
    }

}
