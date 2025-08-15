package com.Catch_Course.domain.payments.controller;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.payments.dto.PaymentDto;
import com.Catch_Course.domain.payments.service.PaymentService;
import com.Catch_Course.global.Rq;
import com.Catch_Course.global.dto.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "paymentController", description = "결제 관련 API")
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final Rq rq;


    @Operation(summary = "수강신청 정보로 결제 정보 조회")
    @GetMapping("/{reservationId}")
    public RsData<PaymentDto> getPayment(@PathVariable Long reservationId) {

        Member member = rq.getMember(rq.getDummyMember());  // 실제 멤버 객체

        PaymentDto paymentDto = paymentService.getPayment(member, reservationId);

        return new RsData<>(
                "200-1",
                "결제 정보 조회가 완료되었습니다.",
                paymentDto
        );
    }

    @Operation(summary = "결제 목록 조회")
    @GetMapping()
    public RsData<List<PaymentDto>> getPayments() {

        Member member = rq.getMember(rq.getDummyMember());  // 실제 멤버 객체

        List<PaymentDto> paymentDtos = paymentService.getPayments(member);

        return new RsData<>(
                "200-1",
                "신청 목록 조회가 완료되었습니다.",
                paymentDtos
        );
    }


    @Operation(summary = "결제 생성 및 요청")
    @PostMapping("/request")
    public RsData<PaymentDto> requestPayment(@RequestParam Long reservationId) {

        Member member = rq.getMember(rq.getDummyMember());  // 실제 멤버 객체

        PaymentDto paymentDto = paymentService.requestPayment(member, reservationId);

        return new RsData<>(
                "200-1",
                "신청 목록 조회가 완료되었습니다.",
                paymentDto
        );
    }

    record confirmPaymentReqBody(@NotBlank String paymentKey,
                                 @NotBlank @Length(min = 3) String orderId,
                                 @NotNull Long amount) {
    }

    @Operation(summary = "결제 승인")
    @PostMapping("/confirm")
    public RsData<PaymentDto> confirmPayment(@RequestBody @Valid confirmPaymentReqBody body) {

        PaymentDto paymentDto = paymentService.confirmPayment(body.paymentKey, body.orderId, body.amount);

        return new RsData<>(
                "200-1",
                "신청 목록 조회가 완료되었습니다.",
                paymentDto
        );
    }

    @Operation(summary = "결제 취소")
    @DeleteMapping("/{reservationId}")
    public RsData<PaymentDto> getReservations(@PathVariable Long reservationId) {
        Member member = rq.getMember(rq.getDummyMember());  // 실제 멤버 객체

        PaymentDto paymentDto = paymentService.deletePayment(member, reservationId);

        return new RsData<>(
                "200-1",
                "결제 취소가 완료되었습니다.",
                paymentDto
        );
    }

}
