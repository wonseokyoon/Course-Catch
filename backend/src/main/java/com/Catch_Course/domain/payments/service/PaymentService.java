package com.Catch_Course.domain.payments.service;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.payments.dto.PaymentDto;
import com.Catch_Course.domain.payments.entity.Payment;
import com.Catch_Course.domain.payments.entity.PaymentStatus;
import com.Catch_Course.domain.payments.repository.PaymentRepository;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import com.Catch_Course.domain.reservation.repository.ReservationRepository;
import com.Catch_Course.domain.reservation.service.ReservationService;
import com.Catch_Course.global.exception.ServiceException;
import com.Catch_Course.global.kafka.dto.PaymentCancelRequest;
import com.Catch_Course.global.kafka.producer.PaymentCancelProducer;
import com.Catch_Course.global.payment.TossPaymentsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final TossPaymentsService tossPaymentsService;
    private final PaymentCancelProducer paymentCancelProducer;
    private final ApplicationEventPublisher eventPublisher;



    public PaymentDto getPayment(Member member, Long reservationId) {

        // reservation 이력 조회
        Reservation reservation = reservationRepository.findByIdAndStudentAndStatus(reservationId, member, ReservationStatus.COMPLETED)
                .orElseThrow(() -> new ServiceException("404-3", "수강신청 이력이 없습니다."));

        Payment payment = paymentRepository.findByReservation(reservation)
                .orElseThrow(() -> new ServiceException("404-5", "결제 정보가 없습니다."));

        return new PaymentDto(payment);
    }

    public List<PaymentDto> getPayments(Member member) {

        List<Payment> payments = paymentRepository.findByMemberAndStatus(member, PaymentStatus.PAID);

        if (payments.isEmpty()) {
            throw new ServiceException("404-5", "결제 정보가 없습니다.");
        }

        return payments.stream()
                .map(PaymentDto::new)
                .toList();
    }

    @Transactional
    public PaymentDto requestPayment(Member member, Long reservationId) {

        Reservation reservation = reservationService.findByIdAndStudent(reservationId, member);
        Optional<Payment> opPayment = paymentRepository.findByReservation(reservation);

        if (opPayment.isPresent()) {
            Payment payment = opPayment.get();
            if (payment.getStatus().equals(PaymentStatus.PAID)) {
                throw new ServiceException("409-2", "이미 처리된 결제입니다.");
            } else if (payment.getStatus().equals(PaymentStatus.CANCELLED)) {
                // todo: 취소된 결제는 로그 남겨서 삭제처리
                throw new ServiceException("409-2", "이미 취소된 결제입니다.");
            } else {
                // 이미 테이블에 있는 경우(FAIL 이나 PENDING 상태)
                payment.setStatus(PaymentStatus.PENDING);
                return new PaymentDto(paymentRepository.save(payment));
            }
        }

        String merchantUid = UUID.randomUUID().toString();
        long amount = reservation.getPrice();

        Payment payment = Payment.builder()
                .reservation(reservation)
                .member(member)
                .merchantUid(merchantUid)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();

        return new PaymentDto(paymentRepository.save(payment));
    }

    @Transactional(noRollbackFor = ServiceException.class)
    public PaymentDto confirmPayment(String paymentKey, String orderId, Long amount) {

        Payment payment = paymentRepository.findByMerchantUid(orderId)
                .orElseThrow(() -> new ServiceException("404-5", "결제 정보가 없습니다."));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ServiceException("409-2", "이미 처리된 결제입니다.");
        }
        if (payment.getAmount() != amount) {
            throw new ServiceException("400-4", "결제 금액이 일치하지 않습니다.");
        }
        try {
            // 최종 승인
            tossPaymentsService.confirm(paymentKey, orderId, amount);

            // 상태 변경
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaymentKey(paymentKey);
            payment.getReservation().setStatus(ReservationStatus.COMPLETED);
        } catch (ServiceException e) {
            // 결제 실패
            // todo: 메세지를 남겨서 후처리
            log.error("결제 승인 실패: orderId={}, reason={}", orderId, e.getMessage());

            payment.setStatus(PaymentStatus.FAILED);
            throw new ServiceException("500-1", "결제 중 알 수 없는 오류가 발생했습니다.");
        } finally {
            // 상태 저장
            paymentRepository.save(payment);
        }

        return new PaymentDto(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentDto deletePayment(Member member, Long reservationId) {
        // reservation 이력 조회
        Reservation reservation = reservationRepository.findByIdAndStudentAndStatus(reservationId, member, ReservationStatus.COMPLETED)
                .orElseThrow(() -> new ServiceException("404-3", "수강신청 이력이 없습니다."));

        Payment payment = paymentRepository.findByReservation(reservation)
                .orElseThrow(() -> new ServiceException("404-5", "결제 정보가 없습니다."));

        // 이미 취소 요청 중
        if (payment.getStatus() == PaymentStatus.CANCEL_REQUESTED) {
            throw new ServiceException("409-4", "이미 취소 처리중인 결제입니다.");
        } else if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new ServiceException("409-2", "이미 취소된 결제입니다.");
        }

        // 취소 요청 상태
        payment.setStatus(PaymentStatus.CANCEL_REQUESTED);

        // 메세지 직접 발행 대신 내부 이벤트로 발행
        String cancelReason = "고객 요청";
        PaymentCancelRequest request = new PaymentCancelRequest(payment, cancelReason, reservation, member);
        eventPublisher.publishEvent(request);

        return new PaymentDto(paymentRepository.save(payment));
    }

    // deletePayment 메서드가 커밋면 메세지 발행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)     // 트랜잭션에서 제외
    public void handlePaymentCancelEvent(PaymentCancelRequest request) {
        log.info("DB 트랜잭션 커밋 완료. Kafka 에 결제 취소 메시지를 발행. DTO: {}", request);
        paymentCancelProducer.send(request);
    }
}
