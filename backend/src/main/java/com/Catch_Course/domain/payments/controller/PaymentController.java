package com.Catch_Course.domain.payments.controller;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.payments.dto.PaymentDto;
import com.Catch_Course.domain.payments.service.PaymentService;
import com.Catch_Course.global.Rq;
import com.Catch_Course.global.dto.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "paymentController", description = "결제 관련 API")
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final Rq rq;


    @Operation(summary = "결제 정보 조회")
    @GetMapping()
    public RsData<PaymentDto> getPayment(@RequestParam Long reservationId) {
        Member member = rq.getMember(rq.getDummyMember());  // 실제 멤버 객체

        PaymentDto paymentDto = paymentService.getPayment(member, reservationId);

        return new RsData<>(
                "200-1",
                "신청 목록 조회가 완료되었습니다.",
                paymentDto
        );
    }

    @Operation(summary = "결제 목록 조회")
    @GetMapping()
    public RsData<List<PaymentDto>> getPayments(@RequestParam Long reservationId) {
        Member member = rq.getMember(rq.getDummyMember());  // 실제 멤버 객체

        List<PaymentDto> paymentDtos = paymentService.getPayments(member);

        return new RsData<>(
                "200-1",
                "신청 목록 조회가 완료되었습니다.",
                paymentDtos
        );
    }


}
