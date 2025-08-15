package com.Catch_Course.domain.payments.service;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.payments.dto.PaymentDto;
import com.Catch_Course.domain.payments.entity.Payment;
import com.Catch_Course.domain.payments.entity.PaymentStatus;
import com.Catch_Course.domain.payments.repository.PaymentRepository;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import com.Catch_Course.domain.reservation.repository.ReservationRepository;
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

        Reservation reservation = reservationRepository.findByIdAndStudentAndStatus(reservationId, member, ReservationStatus.PENDING)
                .orElseThrow(() -> new ServiceException("404-3", "수강신청 이력이 없습니다."));

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
    public PaymentDto deletePaymentRequest(Member member, Long reservationId) {
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

    @Transactional
    public void syncPaymentStatus(String orderId) {
        log.info("결제 상태 동기화 시작: orderId={}", orderId);

        // 1. 우리 DB에서 해당 주문 ID를 가진 Payment를 조회
        Optional<Payment> opPayment = paymentRepository.findByMerchantUid(orderId);

        if (opPayment.isEmpty()) {
            log.warn("웹훅으로 수신된 주문 ID에 해당하는 결제 정보가 DB에 없습니다. orderId={}", orderId);
            return;
        }

        Payment payment = opPayment.get();

        // 2. 이미 처리된 건(PAID, CANCELLED 등)이면 로직 종료
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("이미 처리된 결제 건입니다. 동기화를 종료합니다. status={}", payment.getStatus());
            return;
        }

        // 3. PENDING 상태라면, 최종적으로 '결제 완료' 상태로 변경
        //    (실제로는 Toss API로 한 번 더 확인하는 로직을 추가하면 더 안전함)
        log.info("PENDING 상태의 결제를 PAID로 변경합니다. paymentId={}", payment.getId());
        payment.setStatus(PaymentStatus.PAID);
        payment.getReservation().setStatus(ReservationStatus.COMPLETED);

        // 변경 감지에 의해 트랜잭션 커밋 시점에 DB에 반영됨
    }
}
