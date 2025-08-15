package com.Catch_Course.global.kafka.consumer;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.repository.MemberRepository;
import com.Catch_Course.domain.payments.entity.CancelHistory;
import com.Catch_Course.domain.payments.entity.Payment;
import com.Catch_Course.domain.payments.entity.PaymentStatus;
import com.Catch_Course.domain.payments.repository.CancelHistoryRepository;
import com.Catch_Course.domain.payments.repository.PaymentRepository;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.repository.ReservationRepository;
import com.Catch_Course.global.exception.ServiceException;
import com.Catch_Course.global.kafka.dto.PaymentCancelRequest;
import com.Catch_Course.global.kafka.dto.ReservationCancelRequest;
import com.Catch_Course.global.kafka.producer.ReservationCancelProducer;
import com.Catch_Course.global.payment.TossPaymentsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCancelConsumer {
    private final TossPaymentsService tossPaymentsService;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final CancelHistoryRepository cancelHistoryRepository;
    private final ReservationCancelProducer reservationCancelProducer;
    // 구독
    @KafkaListener(topics = "payment_cancel", groupId = "course", errorHandler = "myErrorHandler")
    @Transactional
    public void consume(PaymentCancelRequest paymentCancelRequest) {
        log.info("결제 취소 요청 처리 시작: {}", paymentCancelRequest);

        Payment payment = paymentRepository.findById(paymentCancelRequest.getPaymentId())
                        .orElseThrow(() -> new ServiceException("404-5","결제 정보가 존재하지 않습니다."));

        if(payment.getStatus() == PaymentStatus.CANCELLED) {
            log.warn("이미 취소 처리된 결제입니다. 중복 처리를 방지합니다. paymentId: {}", paymentCancelRequest.getPaymentId());
            return;
        }

        // 외부 API 요청
        tossPaymentsService.cancel(payment.getPaymentKey(),paymentCancelRequest.getCancelReason());

        // DB 후처리 로직
        cancelProcess(payment,paymentCancelRequest);
    }

    public void cancelProcess(Payment payment,PaymentCancelRequest paymentCancelRequest) {

        Reservation reservation = reservationRepository.findById(paymentCancelRequest.getReservationId())
                .orElseThrow(() -> new ServiceException("404-3","수강신청 이력이 없습니다."));

        Member member = memberRepository.findById(paymentCancelRequest.getMemberId())
                .orElseThrow(() -> new ServiceException("404-4","회원을 찾을 수 없습니다."));

        // 상태 변경
        payment.setStatus(PaymentStatus.CANCELLED);

        // 예약 취소 메세지 전송
        reservationCancelProducer.send(new ReservationCancelRequest(reservation.getId(), member.getId(), paymentCancelRequest.getCourseId()));

        // 결제 취소 이력 저장
        saveCancelHistory(payment, reservation, member);

        log.info("결제 취소 DB 후속 처리 완료: paymentId={}", paymentCancelRequest.getPaymentId());
    }

    public void saveCancelHistory(Payment payment, Reservation reservation, Member member) {

        cancelHistoryRepository.save(CancelHistory.builder()
                .paymentId(payment.getId())
                .reservationId(reservation.getId())
                .orderId(payment.getMerchantUid())
                .memberNickname(member.getNickname())
                .courseTitle(reservation.getCourse().getTitle())
                .amount(payment.getAmount())
                .build());
    }
}
